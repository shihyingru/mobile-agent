package com.luna.morningagent.data.sharedposts

import android.text.Html
import android.util.Base64
import android.util.Log
import com.luna.morningagent.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Backfills the real post body (and a thumbnail image URL) when the share
 * intent only carried a URL.
 *
 * Social apps (Threads, X, Instagram, …) ship just the post URL in
 * `Intent.EXTRA_TEXT` — the body text never crosses the share boundary. We
 * recover it post-save:
 *
 *  1. **Scrape** the URL with a browser-shaped User-Agent and pull
 *     `og:description` (best caption), `og:image` (thumbnail) and the
 *     usual `twitter:*` / plain `description` / `<title>` fallbacks. Fast
 *     (~200 ms), no AI cost. Works for Threads, most blogs.
 *  2. **AI fallback** for the body only — when scrape returns nothing
 *     useful (truly empty), call Gemini 2.5 Flash with the `url_context`
 *     tool and ask for the verbatim body. Heavier but handles JS-only
 *     pages. Image URL only comes from scrape — Gemini can't fetch CDN
 *     blob URLs without a vision-context detour we're not paying for.
 *
 * No length floor: any non-blank scrape result is preferred over the URL.
 * The AI fallback only fires when scrape is fully empty.
 *
 * Returns a [FetchedOgMeta] — both fields nullable; the caller decides
 * which (if any) to write back to the cache.
 */
class SharedPostBodyFetcher(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient = defaultClient(),
) {

    suspend fun fetch(url: String): FetchedOgMeta {
        Log.i(TAG, "fetch url=$url")
        val scraped = runCatching { scrape(url) }
            .onFailure { Log.w(TAG, "scrape failed: ${it.message}") }
            .getOrNull()
            ?: ScrapeResult(body = null, imageUrl = null)
        Log.i(
            TAG,
            "scrape body.len=${scraped.body?.length ?: 0} image=${scraped.imageUrl != null} " +
                "preview=${scraped.body?.take(120)}",
        )
        if (!scraped.body.isNullOrBlank()) {
            return FetchedOgMeta(body = scraped.body, imageUrl = scraped.imageUrl)
        }

        // Scrape gave us no caption — try AI for the body. Image URL stays as
        // whatever scrape returned (often non-null even when og:description is
        // empty for image-only posts).
        val ai = runCatching { aiExtract(url) }
            .onFailure { Log.w(TAG, "aiExtract failed: ${redactKey(it.message)}") }
            .getOrNull()
        Log.i(TAG, "aiExtract result len=${ai?.length ?: 0} preview=${ai?.take(120)}")
        return FetchedOgMeta(body = ai, imageUrl = scraped.imageUrl)
    }

    // --- Phase 1: scrape ----------------------------------------------------

    private suspend fun scrape(url: String): ScrapeResult {
        // Threads, X, Instagram return JS-only shells (no meta tags) to browser
        // UAs but serve real OG meta to known crawlers, so try facebookexternalhit
        // first. If that's refused — some hosts 403 / redirect the FB crawler
        // (Wikipedia, a few news sites) — retry once as a real browser so the OG
        // meta (caption AND image) gets a genuine second chance instead of being
        // lost to a silently-parsed error page. Throws when neither UA yields 2xx
        // HTML; fetch() logs that as a scrape failure and falls through to the AI
        // body recovery.
        val html = fetchHtml(url, CRAWLER_UA)
            ?: fetchHtml(url, BROWSER_UA)
            ?: error("no 2xx HTML for $url")

        val bodyCandidates = listOfNotNull(
            META_OG_DESCRIPTION.find(html)?.groupValues?.getOrNull(1),
            META_TW_DESCRIPTION.find(html)?.groupValues?.getOrNull(1),
            META_DESCRIPTION.find(html)?.groupValues?.getOrNull(1),
            HTML_TITLE.find(html)?.groupValues?.getOrNull(1),
        )
        val body = bodyCandidates
            .map { decodeHtmlEntities(it).trim() }
            .filter { it.isNotBlank() }
            .maxByOrNull { it.length }
            ?.take(MAX_BODY_CHARS)

        val imageRaw = listOfNotNull(
            META_OG_IMAGE.find(html)?.groupValues?.getOrNull(1),
            META_TW_IMAGE.find(html)?.groupValues?.getOrNull(1),
        ).firstOrNull()
        val imageUrl = imageRaw
            ?.let { decodeHtmlEntities(it).trim() }
            ?.takeIf { it.startsWith("http") }

        return ScrapeResult(body = body, imageUrl = imageUrl)
    }

    /**
     * GET [url] with the given [ua] and return the HTML only on a 2xx response.
     * A non-2xx (the FB crawler getting 403'd, or a redirect surfacing as 4xx/5xx)
     * returns null + a log line instead of being parsed as if it were the real
     * page — that silent parse of an error body was why a blocked scrape looked
     * like "the post simply has no image".
     */
    private suspend fun fetchHtml(url: String, ua: String): String? {
        val response = httpClient.get(url) {
            headers {
                append("User-Agent", ua)
                append("Accept", "text/html,application/xhtml+xml")
                append("Accept-Language", "en;q=0.9,zh;q=0.8")
            }
        }
        if (!response.status.isSuccess()) {
            Log.w(TAG, "scrape ${response.status.value} ua=${ua.substringBefore('/')} url=$url")
            return null
        }
        return response.bodyAsText()
    }

    private data class ScrapeResult(val body: String?, val imageUrl: String?)

    // --- Phase 2: AI fallback ----------------------------------------------

    private suspend fun aiExtract(url: String): String? {
        val token = tokenStore.getGeminiKey() ?: return null
        val response: JsonObject = httpClient.post(
            "$GEMINI_API_BASE/models/$AI_FALLBACK_MODEL:generateContent?key=$token",
        ) {
            contentType(ContentType.Application.Json)
            setBody(buildAiExtractBody(url))
        }.body()
        if (response["error"] != null) return null
        return response["candidates"]
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")
            ?.jsonObject?.get("parts")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")
            ?.jsonPrimitive?.content
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.take(MAX_BODY_CHARS)
    }

    private fun buildAiExtractBody(url: String): JsonObject = buildJsonObject {
        putJsonArray("tools") {
            addJsonObject { putJsonObject("url_context") {} }
        }
        putJsonObject("generationConfig") {
            put("temperature", 0.2)
            put("maxOutputTokens", 1024)
        }
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    addJsonObject {
                        put("text", buildString {
                            appendLine("Fetch the URL below and return ONLY the verbatim body text of the post or article — no metadata, no commentary, no surrounding quotes, no \"Here is the…\" preamble.")
                            appendLine("Preserve the original language. Preserve paragraph breaks.")
                            appendLine("If the page is login-walled or the body is unrecoverable, return the empty string.")
                            appendLine()
                            append("URL: $url")
                        })
                    }
                }
            }
        }
    }

    // --- Phase 1b: multimodal image cross-analysis -------------------------

    /**
     * Cross-analyses the post [imageUrl] against its scraped [body] via Gemini
     * multimodal: OCR + landmark + scene type, reconciled into deduplicated
     * location signals. Caller-driven — never invoked from [fetch]. Only call
     * when scrape returned a non-blank body (this enriches a successful scrape;
     * it is not a body-recovery substitute for the Phase 2 fallback).
     *
     * Gemini takes image *bytes*, not a URL, so we download the image through
     * the same client and send it as base64 `inline_data`. Returns null on a
     * missing key, download/network error, or unparseable model output.
     */
    suspend fun analyzeImage(body: String?, imageUrl: String): ImageAnalysisResult? {
        val token = tokenStore.getGeminiKey()
        if (token == null) { Log.w(TAG, "analyzeImage: no Gemini key"); return null }
        return runCatching {
            val (bytes, mime) = downloadImage(imageUrl)
            Log.i(TAG, "analyzeImage img bytes=${bytes.size} mime=$mime")
            val response: JsonObject = httpClient.post(
                "$GEMINI_API_BASE/models/$VISION_MODEL:generateContent?key=$token",
            ) {
                contentType(ContentType.Application.Json)
                setBody(buildImageAnalysisBody(body, bytes, mime))
            }.body()
            response["error"]?.let { Log.w(TAG, "analyzeImage gemini error: $it"); return@runCatching null }
            val text = firstCandidateText(response) ?: run {
                Log.w(TAG, "analyzeImage: no candidate text"); return@runCatching null
            }
            // Strict parse first; on any malformation (the model occasionally
            // emits unescaped chars in the mixed CJK/emoji text), salvage just the
            // locationSignals array — the only field the resolver consumes.
            val parsed = runCatching { analysisJson.decodeFromString<ImageAnalysisResult>(text) }
                .getOrElse {
                    Log.w(TAG, "analyzeImage strict parse failed (${it.message}); salvaging signals")
                    ImageAnalysisResult(locationSignals = salvageSignals(text))
                }
            // A confidently-named landmark is a location signal too — fold it in so
            // it isn't lost when the model puts it only in the landmark field.
            val signals = (parsed.locationSignals + listOfNotNull(parsed.landmark)).dedupe()
            Log.i(TAG, "analyzeImage places=${parsed.places.map { it.name }} signals=$signals landmark=${parsed.landmark}")
            parsed.copy(locationSignals = signals)
        }.onFailure { Log.w(TAG, "analyzeImage failed: ${redactKey(it.message)}") }.getOrNull()
    }

    /**
     * Best-effort extraction of the `locationSignals` array when the model emits
     * JSON the strict parser rejects. Pulls the array body, then every quoted
     * string inside it — good enough to keep the resolver working off a partial
     * or slightly-malformed response instead of discarding everything.
     */
    private fun salvageSignals(text: String): List<String> {
        val start = LOCATION_SIGNALS_KEY.find(text) ?: return emptyList()
        // Take everything after the opening "[" up to the closing "]" if present,
        // else the whole tail (truncated response). QUOTED_STRING only matches
        // fully-closed strings, so a partial trailing element is dropped cleanly.
        val body = text.substring(start.range.last + 1).substringBefore(']')
        return QUOTED_STRING.findAll(body).map { it.groupValues[1] }.toList()
    }

    /**
     * Text-only location-signal extraction from a post caption. Cheaper, faster
     * and far less error-prone than the image path (no base64 image, smaller
     * response, no 503-magnet vision load), so [resolvePostLocations] runs it
     * first. Returns an empty list on a missing key, model error, or no signals.
     */
    suspend fun analyzeText(content: String): ImageAnalysisResult? {
        val token = tokenStore.getGeminiKey()
        if (token == null) { Log.w(TAG, "analyzeText: no Gemini key"); return null }
        return runCatching {
            val response: JsonObject = httpClient.post(
                "$GEMINI_API_BASE/models/$VISION_MODEL:generateContent?key=$token",
            ) {
                contentType(ContentType.Application.Json)
                setBody(buildTextSignalsBody(content))
            }.body()
            response["error"]?.let { Log.w(TAG, "analyzeText gemini error: $it"); return@runCatching null }
            val text = firstCandidateText(response) ?: return@runCatching null
            val parsed = runCatching { analysisJson.decodeFromString<ImageAnalysisResult>(text) }
                .getOrElse { ImageAnalysisResult(locationSignals = salvageSignals(text)) }
            val signals = (parsed.locationSignals + listOfNotNull(parsed.landmark)).dedupe()
            Log.i(TAG, "analyzeText places=${parsed.places.map { it.name }} signals=$signals")
            parsed.copy(locationSignals = signals)
        }.onFailure { Log.w(TAG, "analyzeText failed: ${redactKey(it.message)}") }.getOrNull()
    }

    private fun buildTextSignalsBody(content: String): JsonObject = buildJsonObject {
        putJsonObject("generationConfig") {
            put("temperature", 0.2)
            put("maxOutputTokens", 1024)
            put("responseMimeType", "application/json")
            putJsonObject("thinkingConfig") { put("thinkingBudget", 0) }
        }
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    addJsonObject { put("text", textSignalsPrompt(content)) }
                }
            }
        }
    }

    private fun textSignalsPrompt(content: String): String = buildString {
        appendLine("Extract concrete location signals from this social-media post caption.")
        appendLine("Return \"places\": every DISTINCT real-world destination the post recommends or is about — restaurants, cafés, shops, landmarks. For each, give a short \"name\" and a complete geocodable \"query\" of the form \"venue, neighbourhood, city\" (e.g. \"Hippo, Yeonnam-dong, Seoul\") so a bare name isn't matched to the wrong city.")
        appendLine("Order by prominence (the main place first). Do NOT add a bare city, district, or country as its own place — use those only inside each query. Skip near-duplicates. Return at most 12. Empty array if no specific place is mentioned.")
        appendLine()
        appendLine("Respond with ONLY this JSON object — no markdown, no commentary:")
        appendLine("""{"places": [{"name": string, "query": string}], "locationSignals": string[]}""")
        appendLine()
        appendLine("Caption:")
        append(content)
    }

    private suspend fun downloadImage(url: String): Pair<ByteArray, String> {
        val response = httpClient.get(url)
        val mime = response.contentType()
            ?.let { "${it.contentType}/${it.contentSubtype}" }
            ?: "image/jpeg"
        return response.body<ByteArray>() to mime
    }

    private fun buildImageAnalysisBody(
        body: String?,
        imageBytes: ByteArray,
        mimeType: String,
    ): JsonObject = buildJsonObject {
        putJsonObject("generationConfig") {
            put("temperature", 0.2)
            put("maxOutputTokens", 2048)
            put("responseMimeType", "application/json")
            // gemini-2.5-flash is a thinking model: left on, it spends the output
            // budget on hidden reasoning and truncates the JSON mid-array. This is
            // a deterministic extraction, not a reasoning task — turn thinking off
            // so the whole budget goes to the response.
            putJsonObject("thinkingConfig") { put("thinkingBudget", 0) }
        }
        putJsonArray("contents") {
            addJsonObject {
                put("role", "user")
                putJsonArray("parts") {
                    addJsonObject { put("text", imageAnalysisPrompt(body)) }
                    addJsonObject {
                        putJsonObject("inline_data") {
                            put("mime_type", mimeType)
                            put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                        }
                    }
                }
            }
        }
    }

    private fun imageAnalysisPrompt(body: String?): String = buildString {
        appendLine("You are extracting location clues from an image attached to a social-media post.")
        appendLine("Read any text in the image (signs, menus, banners) and note any recognisable landmark or named venue, then combine those with the post body text below.")
        appendLine("Return \"places\": every DISTINCT real-world destination identifiable from the image and caption — restaurants, cafés, shops, landmarks. For each, give a short \"name\" and a complete geocodable \"query\" of the form \"venue, neighbourhood, city\" (e.g. \"Hippo, Yeonnam-dong, Seoul\") so a bare name isn't matched to the wrong city.")
        appendLine("Order by prominence (the main place first). Do NOT add a bare city, district, or country as its own place — use those only inside each query. Skip near-duplicates. Return at most 12. Empty array if none.")
        appendLine()
        appendLine("Respond with ONLY this JSON object — no markdown, no commentary, and DO NOT dump the raw OCR text:")
        appendLine("""{"places": [{"name": string, "query": string}], "locationSignals": string[], "landmark": string|null}""")
        appendLine()
        appendLine("Post body text:")
        append(body?.takeIf { it.isNotBlank() } ?: "(none)")
    }

    // --- Location resolution: Google Places Text Search --------------------

    /**
     * Resolves ALL of a post's places once, at share time. Text-first: pulls the
     * destination list from the caption [content] via the cheap [analyzeText]
     * path; only when the text yields nothing does it pay for image OCR
     * ([analyzeImage]). Each destination's geocodable query is resolved to a real
     * place via Google Places, deduped by canonical name and capped.
     *
     * Returns an empty list when the Places key is missing, no destination is
     * found, or none resolve — the caller persists it and the card hides the pin.
     * Never throws.
     */
    suspend fun resolvePostLocations(content: String?, imageUrl: String?): List<ResolvedPlace> {
        val key = tokenStore.getGooglePlacesKey()
        if (key == null) { Log.w(TAG, "resolveLocations: no Places key"); return emptyList() }

        // Text-first; fall back to the heavier image OCR when the caption is dry.
        val text = content?.takeIf { it.isNotBlank() }?.let { analyzeText(it) }
        val analysis = text?.takeIf { it.places.isNotEmpty() || it.locationSignals.isNotEmpty() }
            ?: imageUrl?.let { analyzeImage(content, it) }
            ?: return emptyList()

        // Prefer the structured destinations; if a malformed response left only
        // salvaged flat signals, resolve the single most-specific one.
        val queries = analysis.places
            .map { it.query.ifBlank { it.name } }
            .filter { it.isNotBlank() }
            .ifEmpty { listOfNotNull(analysis.locationSignals.firstOrNull()) }
            .distinct()
            .take(MAX_PLACES)

        // LinkedHashMap → dedupe by canonical name, preserve the model's order.
        val resolved = LinkedHashMap<String, ResolvedPlace>()
        for (q in queries) {
            val place = runCatching { placesTextSearch(q, key) }
                .onFailure { Log.w(TAG, "places lookup failed for '$q': ${redactKey(it.message)}") }
                .getOrNull() ?: continue
            resolved.putIfAbsent(
                place.displayName,
                ResolvedPlace(
                    name    = place.displayName,
                    address = place.formattedAddress,
                    mapsUri = place.googleMapsUri,
                ),
            )
        }
        Log.i(TAG, "resolveLocations queries=${queries.size} resolved=${resolved.size}")
        return resolved.values.toList()
    }

    private suspend fun placesTextSearch(query: String, apiKey: String): PlaceResult? {
        val response: JsonObject = httpClient.post(PLACES_SEARCH_URL) {
            contentType(ContentType.Application.Json)
            headers {
                append("X-Goog-Api-Key", apiKey)
                append("X-Goog-FieldMask", PLACES_FIELD_MASK)
            }
            setBody(buildJsonObject { put("textQuery", query) })
        }.body()

        response["error"]?.let { Log.w(TAG, "places error: $it") }
        val place = response["places"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val location = place["location"]?.jsonObject
        val lat = location?.get("latitude")?.jsonPrimitive?.content?.toDoubleOrNull()
        val lng = location?.get("longitude")?.jsonPrimitive?.content?.toDoubleOrNull()
        if (lat == null || lng == null) return null
        return PlaceResult(
            displayName = place["displayName"]?.jsonObject?.get("text")?.jsonPrimitive?.content ?: query,
            formattedAddress = place["formattedAddress"]?.jsonPrimitive?.content ?: "",
            latitude = lat,
            longitude = lng,
            googleMapsUri = place["googleMapsUri"]?.jsonPrimitive?.content,
        )
    }

    private fun List<String>.dedupe(): List<String> =
        map { it.trim() }.filter { it.isNotBlank() }.distinct()

    /** Strip a `key=<token>` query param out of an error message before logging —
     *  Ktor embeds the full request URL (including the Gemini API key) in timeout
     *  / failure exception messages. */
    private fun redactKey(message: String?): String =
        message.orEmpty().replace(Regex("key=[A-Za-z0-9_-]+"), "key=***")

    private fun firstCandidateText(response: JsonObject): String? =
        response["candidates"]
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("content")
            ?.jsonObject?.get("parts")
            ?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("text")
            ?.jsonPrimitive?.content
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    // --- HTML helpers -------------------------------------------------------

    private fun decodeHtmlEntities(raw: String): String =
        Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()

    companion object {
        private const val TAG = "BodyFetcher"
        private const val MAX_BODY_CHARS        = 4000
        // Upper bound on destinations resolved per post — caps Places calls for a
        // long listicle while comfortably covering a "best N spots" post.
        private const val MAX_PLACES            = 12

        // Crawler UA first (social apps gate real OG meta behind it); a real
        // browser UA is the retry for hosts that refuse the FB crawler.
        private const val CRAWLER_UA =
            "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)"
        private const val BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val GEMINI_API_BASE       = "https://generativelanguage.googleapis.com/v1beta"
        private const val AI_FALLBACK_MODEL     = "gemini-2.5-flash"
        private const val VISION_MODEL          = "gemini-2.5-flash"

        private const val PLACES_SEARCH_URL     = "https://places.googleapis.com/v1/places:searchText"
        private const val PLACES_FIELD_MASK     =
            "places.displayName,places.formattedAddress,places.location,places.googleMapsUri"

        // Parses Gemini's JSON image-analysis payload (responseMimeType=application/json).
        // Lenient: even in JSON mode the model occasionally emits trailing commas or
        // unquoted tokens in mixed CJK/emoji text. A hard parse failure here used to
        // silently drop every location signal (see salvageSignals for the backstop).
        private val analysisJson = Json {
            ignoreUnknownKeys = true
            allowTrailingComma = true
            isLenient = true
        }

        // Salvage parsing — locate the locationSignals array opener, then pull the
        // quoted strings that follow (works even if the array was never closed).
        private val LOCATION_SIGNALS_KEY = Regex(""""locationSignals"\s*:\s*\[""")
        private val QUOTED_STRING = Regex(""""((?:[^"\\]|\\.)*)"""")

        // Both `property` and `name` are seen in the wild; quote chars vary too.
        private val META_OG_DESCRIPTION = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']og:description["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val META_TW_DESCRIPTION = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']twitter:description["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val META_DESCRIPTION = Regex(
            """<meta[^>]+name\s*=\s*["']description["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        // `content` can come BEFORE the property attr — match in either order
        // (some pages emit `<meta content="…" property="og:image" />`).
        private val META_OG_IMAGE = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']og:image["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val META_TW_IMAGE = Regex(
            """<meta[^>]+(?:property|name)\s*=\s*["']twitter:image["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE,
        )
        private val HTML_TITLE = Regex(
            """<title[^>]*>([^<]*)</title>""",
            RegexOption.IGNORE_CASE,
        )

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            // OkHttp defaults to a ~10s socket timeout — too short for a Gemini
            // vision call carrying a base64 image, which was timing out before it
            // could return any location signals. Generous ceilings; the fast
            // scrape / Notion calls finish well under them.
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis  = 60_000
            }
        }
    }
}

/** Result of scraping a shared-post URL for OG/Twitter meta. */
data class FetchedOgMeta(
    /** Best caption candidate. Null when neither scrape nor AI recovered anything. */
    val body: String? = null,
    /** First absolute http(s) image URL from `og:image` / `twitter:image`. */
    val imageUrl: String? = null,
    /**
     * Multimodal cross-analysis of [imageUrl] vs [body]. Null until the caller
     * runs [SharedPostBodyFetcher.analyzeImage] — never populated by [fetch].
     */
    val imageAnalysis: ImageAnalysisResult? = null,
)
