package com.luna.morningagent.data.sharedposts

import android.text.Html
import android.util.Log
import com.luna.morningagent.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
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
            .onFailure { Log.w(TAG, "aiExtract failed: ${it.message}") }
            .getOrNull()
        Log.i(TAG, "aiExtract result len=${ai?.length ?: 0} preview=${ai?.take(120)}")
        return FetchedOgMeta(body = ai, imageUrl = scraped.imageUrl)
    }

    // --- Phase 1: scrape ----------------------------------------------------

    private suspend fun scrape(url: String): ScrapeResult {
        // Threads, X, Instagram return JS-only shells (no meta tags) to browser
        // UAs but serve real OG meta to known crawlers. Pose as facebookexternalhit
        // so og:description holds the actual post body.
        val html: String = httpClient.get(url) {
            headers {
                append("User-Agent",
                    "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                append("Accept", "text/html,application/xhtml+xml")
                append("Accept-Language", "en;q=0.9,zh;q=0.8")
            }
        }.bodyAsText()

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

    // --- HTML helpers -------------------------------------------------------

    private fun decodeHtmlEntities(raw: String): String =
        Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString()

    companion object {
        private const val TAG = "BodyFetcher"
        private const val MAX_BODY_CHARS        = 4000
        private const val GEMINI_API_BASE       = "https://generativelanguage.googleapis.com/v1beta"
        private const val AI_FALLBACK_MODEL     = "gemini-2.5-flash"

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
        }
    }
}

/** Result of scraping a shared-post URL for OG/Twitter meta. */
data class FetchedOgMeta(
    /** Best caption candidate. Null when neither scrape nor AI recovered anything. */
    val body: String? = null,
    /** First absolute http(s) image URL from `og:image` / `twitter:image`. */
    val imageUrl: String? = null,
)
