package com.luna.morningagent.data.notion

import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

// REST implementation of NotionTaskSource. Hits the user's Notion database via
// api.notion.com/v1/databases/{id}/query with the integration token from TokenStore.
//
// Assumed database schema (Phase 2 step 3+ convention — make configurable later):
//   • "Task"     — title       — task title
//   • "Priority" — select      — values "High" / "Medium" / "Low"
//   • "Status"   — status      — filter: != "Done"
//   • "Date"     — date        — filter: <= today; sort: descending
//   • "Area"     — relation    — page id resolved to a name via /pages/{id}
//
// The today-comparison uses the device's local timezone — fine for personal use.
class NotionRestClient(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient = defaultClient(),
) : NotionTaskSource {

    override suspend fun fetchTodayTasks(): List<Task> {
        val token = tokenStore.getNotionToken()
            ?: throw NotionConfigMissingException("Notion integration token not set in Settings")
        val dbId = tokenStore.getNotionDatabaseId()
            ?: throw NotionConfigMissingException("Notion database ID not set in Settings")

        val today = LocalDate.now(ZoneId.systemDefault()).toString()  // YYYY-MM-DD

        val response: NotionQueryResponse = httpClient.post("$NOTION_API_BASE/databases/$dbId/query") {
            authHeaders(token)
            contentType(ContentType.Application.Json)
            setBody(buildQueryBody(today))
        }.body()

        // Parse the rows first, then resolve Area page-ids to names in parallel
        // (one /pages/{id} request per unique relation target).
        val rows = response.results.map { it.toRow() }
        val areaNamesById = resolveAreaNames(token, rows.flatMap { it.areaIds }.distinct())
        // Client-side priority sort: Notion's `select` properties sort
        // alphabetically server-side (High → Low → Medium), which isn't
        // priority order. `sortedByDescending` is stable, so within each
        // priority bucket the Notion-returned date-desc order is preserved.
        return rows
            .map { it.toTask(areaNamesById) }
            .sortedByDescending { it.priority.weight }
    }

    private fun buildQueryBody(today: String): JsonObject = buildJsonObject {
        putJsonObject("filter") {
            putJsonArray("and") {
                addJsonObject {
                    put("property", PROP_STATUS)
                    putJsonObject("status") { put("does_not_equal", STATUS_DONE) }
                }
                addJsonObject {
                    put("property", PROP_DATE)
                    putJsonObject("date") { put("on_or_before", today) }
                }
            }
        }
        putJsonArray("sorts") {
            addJsonObject {
                put("property", PROP_DATE)
                put("direction", "descending")
            }
        }
    }

    private suspend fun resolveAreaNames(
        token: String,
        pageIds: List<String>,
    ): Map<String, String> {
        if (pageIds.isEmpty()) return emptyMap()
        return coroutineScope {
            pageIds.map { id ->
                async { id to fetchPageTitle(token, id) }
            }.awaitAll()
        }.mapNotNull { (id, name) -> name?.let { id to it } }.toMap()
    }

    private suspend fun fetchPageTitle(token: String, pageId: String): String? = try {
        val page: JsonObject = httpClient.get("$NOTION_API_BASE/pages/$pageId") {
            authHeaders(token)
        }.body()
        // Notion's title property name varies per database (often "Name"). Find
        // any property whose payload has a populated `title` array and read its
        // first plain_text entry.
        page["properties"]?.jsonObject?.values?.firstNotNullOfOrNull { prop ->
            (prop.jsonObject["title"] as? JsonArray)
                ?.firstOrNull()?.jsonObject?.get("plain_text")?.jsonPrimitive?.content
        }
    } catch (_: Exception) {
        // Fail open: missing area name shouldn't block the briefing — the card
        // just renders without the tag.
        null
    }

    private fun HttpRequestBuilder.authHeaders(token: String) {
        headers {
            append("Authorization", "Bearer $token")
            append("Notion-Version", NOTION_API_VERSION)
        }
    }

    private fun NotionPage.toRow(): TaskRow = TaskRow(
        id               = id,
        title            = readTitle(properties[PROP_NAME]) ?: "(Untitled)",
        priority         = readPriority(properties[PROP_PRIORITY]),
        estimatedMinutes = 0,                                    // Not in current schema
        url              = url,
        areaIds          = readRelationIds(properties[PROP_AREA]),
    )

    private fun TaskRow.toTask(areaNamesById: Map<String, String>): Task = Task(
        id               = id,
        title            = title,
        priority         = priority,
        tip              = "",                                  // Gemini fills this
        estimatedMinutes = estimatedMinutes,
        notionUrl        = url,
        notionRef        = "NOT-${id.takeLast(4).uppercase()}",
        area             = areaIds.firstNotNullOfOrNull { areaNamesById[it] },
    )

    // Notion sends `{"select": null}` / `{"title": null}` / `{"relation": null}`
    // for properties that exist on the schema but are unset on a specific row.
    // The `.jsonObject` / `.jsonArray` extensions throw on JsonNull, so use
    // `as? JsonObject` / `as? JsonArray` to short-circuit cleanly.

    private fun readTitle(prop: JsonElement?): String? =
        ((prop as? JsonObject)?.get("title") as? JsonArray)
            ?.firstOrNull()?.jsonObject?.get("plain_text")?.jsonPrimitive?.content

    private fun readPriority(prop: JsonElement?): Priority {
        val select = (prop as? JsonObject)?.get("select") as? JsonObject
            ?: return Priority.MID
        val name = select["name"]?.jsonPrimitive?.content
        return when (name?.lowercase()) {
            "high" -> Priority.HIGH
            "low"  -> Priority.LOW
            else   -> Priority.MID
        }
    }

    private fun readRelationIds(prop: JsonElement?): List<String> =
        ((prop as? JsonObject)?.get("relation") as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.get("id")?.jsonPrimitive?.content }
            ?: emptyList()

    companion object {
        private const val NOTION_API_BASE    = "https://api.notion.com/v1"
        private const val NOTION_API_VERSION = "2022-06-28"
        private const val PROP_NAME      = "Task"
        private const val PROP_PRIORITY  = "Priority"
        private const val PROP_STATUS    = "Status"
        private const val PROP_DATE      = "Date"
        private const val PROP_AREA      = "Area"
        private const val STATUS_DONE    = "Done"

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}

// --- Wire types (internal) -------------------------------------------------

private data class TaskRow(
    val id: String,
    val title: String,
    val priority: Priority,
    val estimatedMinutes: Int,
    val url: String,
    val areaIds: List<String>,
)

@kotlinx.serialization.Serializable
private data class NotionQueryResponse(val results: List<NotionPage> = emptyList())

@kotlinx.serialization.Serializable
private data class NotionPage(
    val id: String,
    val url: String,
    val properties: Map<String, JsonElement> = emptyMap(),
)
