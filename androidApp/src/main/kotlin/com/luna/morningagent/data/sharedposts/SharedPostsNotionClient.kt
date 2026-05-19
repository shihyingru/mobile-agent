package com.luna.morningagent.data.sharedposts

import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.util.UUID
import kotlin.time.Clock
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
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
 * Notion REST calls for the SharedPosts mirror DB.
 *
 * Three operations:
 *  - `createDatabase(parentPageId)` — provisions the DB with the agreed schema
 *    under a Notion page Luna owns; returns the new DB id. Called once by the
 *    Settings setup flow (commit 3); the id is then persisted to TokenStore.
 *  - `createPage(dbId, post)` — inserts one row per saved post. Returns the
 *    Notion page id so the local cache can be patched with `notionId`.
 *  - `archivePage(pageId)` — soft-delete (Notion has no hard delete via API).
 *
 * Auth + version headers mirror NotionRestClient. Title content is capped at
 * 1900 chars (Notion's 2000 limit minus a safety margin).
 */
class SharedPostsNotionClient(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient = defaultClient(),
) {

    suspend fun createDatabase(parentPageId: String, title: String = DEFAULT_DB_TITLE): String {
        val token = requireToken()
        val response: JsonObject = httpClient.post("$NOTION_API_BASE/databases") {
            authHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildCreateDatabaseBody(parentPageId, title))
        }.body()
        throwIfNotionError(response, "createDatabase")
        return response["id"]?.jsonPrimitive?.content
            ?: error("Notion createDatabase returned no id")
    }

    suspend fun createPage(dbId: String, post: SharedPost): String {
        val token = requireToken()
        val response: JsonObject = httpClient.post("$NOTION_API_BASE/pages") {
            authHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildCreatePageBody(dbId, post))
        }.body()
        throwIfNotionError(response, "createPage")
        return response["id"]?.jsonPrimitive?.content
            ?: error("Notion createPage returned no id")
    }

    suspend fun archivePage(pageId: String) {
        val token = requireToken()
        val response: JsonObject = httpClient.patch("$NOTION_API_BASE/pages/$pageId") {
            authHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("archived", true) })
        }.body()
        throwIfNotionError(response, "archivePage")
    }

    /**
     * Query every (non-archived) page in the SharedPosts database, paginating
     * via Notion's `next_cursor`. Returns posts ready to merge into the local
     * cache — `localId` is freshly generated; the caller is expected to reuse
     * the existing localId when notionId matches a cached entry.
     */
    suspend fun listDatabase(dbId: String): List<SharedPost> {
        val token = requireToken()
        val accumulated = mutableListOf<SharedPost>()
        var cursor: String? = null
        do {
            val response: JsonObject = httpClient.post(
                "$NOTION_API_BASE/databases/$dbId/query",
            ) {
                authHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(buildJsonObject {
                    put("page_size", 100)
                    cursor?.let { put("start_cursor", it) }
                })
            }.body()
            throwIfNotionError(response, "listDatabase")

            val results = response["results"]?.jsonArray ?: JsonArray(emptyList())
            results.forEach { node ->
                val page = node.jsonObject
                if (page["archived"]?.jsonPrimitive?.content == "true") return@forEach
                runCatching { notionPageToSharedPost(page) }.getOrNull()?.let { accumulated.add(it) }
            }

            cursor = if (response["has_more"]?.jsonPrimitive?.content == "true") {
                response["next_cursor"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
            } else null
        } while (cursor != null)
        return accumulated
    }

    /** Reverse of [buildCreatePageBody]. Unknown / missing props degrade gracefully. */
    private fun notionPageToSharedPost(page: JsonObject): SharedPost {
        val notionId = page["id"]?.jsonPrimitive?.content
            ?: error("Notion page missing id")
        val props    = page["properties"]?.jsonObject ?: JsonObject(emptyMap())

        val content    = readTitle(props[PROP_CONTENT]?.jsonObject)
        val source     = readSelect(props[PROP_SOURCE]?.jsonObject) ?: SharedPost.SOURCE_OTHER
        val author     = readRichText(props[PROP_AUTHOR]?.jsonObject)
        val url        = props[PROP_URL]?.jsonObject?.get("url")
            ?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
        val categories = readMultiSelect(props[PROP_CATEGORIES]?.jsonObject)
        val summary    = readRichText(props[PROP_SUMMARY]?.jsonObject)
        val savedAt    = readDate(props[PROP_SAVED_AT]?.jsonObject)
            ?: page["created_time"]?.jsonPrimitive?.content?.let(::parseInstantOrNull)
            ?: Clock.System.now()
        val status     = readSelect(props[PROP_STATUS]?.jsonObject) ?: SharedPost.STATUS_UNREAD

        return SharedPost(
            localId               = UUID.randomUUID().toString(),
            notionId              = notionId,
            content               = content,
            source                = source,
            author                = author,
            url                   = url,
            categories            = categories,
            summary               = summary,
            savedAt               = savedAt,
            status                = status,
            pendingSync           = false,
            pendingCategorization = false,
        )
    }

    private fun readTitle(prop: JsonObject?): String =
        prop?.get("title")?.jsonArray?.joinToString("") {
            it.jsonObject["plain_text"]?.jsonPrimitive?.content.orEmpty()
        } ?: ""

    private fun readRichText(prop: JsonObject?): String? =
        prop?.get("rich_text")?.jsonArray?.joinToString("") {
            it.jsonObject["plain_text"]?.jsonPrimitive?.content.orEmpty()
        }?.takeIf { it.isNotBlank() }

    private fun readSelect(prop: JsonObject?): String? =
        prop?.get("select")?.takeIf { it !is JsonNull }?.jsonObject
            ?.get("name")?.jsonPrimitive?.content

    private fun readMultiSelect(prop: JsonObject?): List<String> =
        prop?.get("multi_select")?.jsonArray
            ?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content }
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun readDate(prop: JsonObject?): kotlin.time.Instant? =
        prop?.get("date")?.takeIf { it !is JsonNull }?.jsonObject
            ?.get("start")?.jsonPrimitive?.content
            ?.let(::parseInstantOrNull)

    private fun parseInstantOrNull(raw: String): kotlin.time.Instant? =
        runCatching { kotlin.time.Instant.parse(raw) }.getOrNull()

    /**
     * Replace the title (Content) property on an existing Notion page. Called
     * after the body fetcher backfills a real post body that was originally
     * shared as a URL-only intent.
     */
    suspend fun updatePageContent(pageId: String, content: String) {
        val token = requireToken()
        val response: JsonObject = httpClient.patch("$NOTION_API_BASE/pages/$pageId") {
            authHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonObject("properties") {
                    putJsonObject(PROP_CONTENT) {
                        putJsonArray("title") {
                            addJsonObject {
                                put("type", "text")
                                putJsonObject("text") { put("content", content.take(TITLE_MAX_CHARS)) }
                            }
                        }
                    }
                }
            })
        }.body()
        throwIfNotionError(response, "updatePageContent")
    }

    /**
     * Partial update — writes only Categories + Summary. Notion's pages PATCH
     * endpoint merges per property, so other fields (Source, URL, …) stay put.
     * Called by the repository when the agent finishes categorizing a post.
     */
    suspend fun updatePageCategoriesAndSummary(
        pageId: String,
        categories: List<String>,
        summary: String?,
    ) {
        val token = requireToken()
        val response: JsonObject = httpClient.patch("$NOTION_API_BASE/pages/$pageId") {
            authHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                putJsonObject("properties") {
                    putJsonObject(PROP_CATEGORIES) {
                        putJsonArray("multi_select") {
                            categories.forEach { addJsonObject { put("name", it) } }
                        }
                    }
                    summary?.takeIf { it.isNotBlank() }?.let { s ->
                        putJsonObject(PROP_SUMMARY) {
                            putJsonArray("rich_text") {
                                addJsonObject {
                                    put("type", "text")
                                    putJsonObject("text") { put("content", s) }
                                }
                            }
                        }
                    }
                }
            })
        }.body()
        throwIfNotionError(response, "updatePage")
    }

    private fun requireToken(): String = tokenStore.getNotionToken()
        ?: throw NotionConfigMissingException("Notion integration token not set in Settings")

    /**
     * Ktor's default HttpClient has `expectSuccess = false`, so 4xx responses
     * deserialize as a Notion error envelope instead of throwing. Surface the
     * underlying `code` / `message` so callers don't get a generic "no id"
     * fallback when the real issue is e.g. `unauthorized` or `object_not_found`.
     */
    private fun throwIfNotionError(response: JsonObject, op: String) {
        val obj = response["object"]?.jsonPrimitive?.content ?: return
        if (obj != "error") return
        val code    = response["code"]?.jsonPrimitive?.content ?: "unknown"
        val message = response["message"]?.jsonPrimitive?.content ?: "no message"
        error("Notion $op · $code: $message")
    }

    private fun HttpRequestBuilder.authHeaders(token: String) {
        headers {
            append("Authorization", "Bearer $token")
            append("Notion-Version", NOTION_API_VERSION)
        }
    }

    // --- Body builders ------------------------------------------------------

    private fun buildCreateDatabaseBody(parentPageId: String, title: String): JsonObject =
        buildJsonObject {
            putJsonObject("parent") {
                put("type", "page_id")
                put("page_id", parentPageId)
            }
            putJsonArray("title") {
                addJsonObject {
                    put("type", "text")
                    putJsonObject("text") { put("content", title) }
                }
            }
            putJsonObject("properties") {
                putJsonObject(PROP_CONTENT)    { putJsonObject("title") {} }
                putJsonObject(PROP_SOURCE)     { putJsonObject("select")       { putJsonArray("options") {
                    SOURCE_OPTIONS.forEach { addJsonObject { put("name", it) } }
                } } }
                putJsonObject(PROP_AUTHOR)     { putJsonObject("rich_text") {} }
                putJsonObject(PROP_URL)        { putJsonObject("url") {} }
                putJsonObject(PROP_CATEGORIES) { putJsonObject("multi_select") { putJsonArray("options") {} } }
                putJsonObject(PROP_SUMMARY)    { putJsonObject("rich_text") {} }
                putJsonObject(PROP_SAVED_AT)   { putJsonObject("date") {} }
                putJsonObject(PROP_STATUS)     { putJsonObject("select")       { putJsonArray("options") {
                    STATUS_OPTIONS.forEach { addJsonObject { put("name", it) } }
                } } }
            }
        }

    private fun buildCreatePageBody(dbId: String, post: SharedPost): JsonObject = buildJsonObject {
        putJsonObject("parent") { put("database_id", dbId) }
        putJsonObject("properties") {
            putJsonObject(PROP_CONTENT) {
                putJsonArray("title") {
                    addJsonObject {
                        put("type", "text")
                        putJsonObject("text") { put("content", post.content.take(TITLE_MAX_CHARS)) }
                    }
                }
            }
            putJsonObject(PROP_SOURCE) {
                putJsonObject("select") { put("name", post.source) }
            }
            post.author?.takeIf { it.isNotBlank() }?.let { author ->
                putJsonObject(PROP_AUTHOR) {
                    putJsonArray("rich_text") {
                        addJsonObject {
                            put("type", "text")
                            putJsonObject("text") { put("content", author) }
                        }
                    }
                }
            }
            post.url?.takeIf { it.isNotBlank() }?.let { url ->
                putJsonObject(PROP_URL) { put("url", url) }
            }
            if (post.categories.isNotEmpty()) {
                putJsonObject(PROP_CATEGORIES) {
                    putJsonArray("multi_select") {
                        post.categories.forEach { addJsonObject { put("name", it) } }
                    }
                }
            }
            post.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                putJsonObject(PROP_SUMMARY) {
                    putJsonArray("rich_text") {
                        addJsonObject {
                            put("type", "text")
                            putJsonObject("text") { put("content", summary) }
                        }
                    }
                }
            }
            putJsonObject(PROP_SAVED_AT) {
                putJsonObject("date") { put("start", post.savedAt.toString()) }
            }
            putJsonObject(PROP_STATUS) {
                putJsonObject("select") { put("name", post.status) }
            }
        }
    }

    companion object {
        const val DEFAULT_DB_TITLE = "Shared Posts"
        const val TITLE_MAX_CHARS  = 1900

        const val PROP_CONTENT    = "Content"
        const val PROP_SOURCE     = "Source"
        const val PROP_AUTHOR     = "Author"
        const val PROP_URL        = "URL"
        const val PROP_CATEGORIES = "Categories"
        const val PROP_SUMMARY    = "Summary"
        const val PROP_SAVED_AT   = "SavedAt"
        const val PROP_STATUS     = "Status"

        private val SOURCE_OPTIONS = listOf(
            SharedPost.SOURCE_THREADS,
            SharedPost.SOURCE_TWITTER,
            SharedPost.SOURCE_WEB,
            SharedPost.SOURCE_OTHER,
        )
        private val STATUS_OPTIONS = listOf(
            SharedPost.STATUS_UNREAD,
            SharedPost.STATUS_READING,
            SharedPost.STATUS_DONE,
        )

        private const val NOTION_API_BASE    = "https://api.notion.com/v1"
        private const val NOTION_API_VERSION = "2022-06-28"

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}

/** Unused outside this file but kept for the future archive() unit test. */
@Suppress("unused")
private fun nowInstant(): Instant = Instant.fromEpochMilliseconds(System.currentTimeMillis())
