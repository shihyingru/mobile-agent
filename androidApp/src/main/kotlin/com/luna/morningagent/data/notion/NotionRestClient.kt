package com.luna.morningagent.data.notion

import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// REST implementation of NotionTaskSource. Hits the user's Notion database via
// api.notion.com/v1/databases/{id}/query with the integration token from TokenStore.
//
// Assumed database schema (Phase 2 step 2 convention — make configurable later if needed):
//   • "Task"           — title       — task title
//   • "Priority"       — select      — values "High" / "Medium" / "Low"
//   • "Estimated Time" — number      — minutes (optional; defaults to 0 if absent)
class NotionRestClient(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient = defaultClient(),
) : NotionTaskSource {

    override suspend fun fetchHighPriorityTasks(): List<Task> {
        val token = tokenStore.getNotionToken()
            ?: throw NotionConfigMissingException("Notion integration token not set in Settings")
        val dbId = tokenStore.getNotionDatabaseId()
            ?: throw NotionConfigMissingException("Notion database ID not set in Settings")

        val response: NotionQueryResponse = httpClient.post("$NOTION_API_BASE/databases/$dbId/query") {
            headers {
                append("Authorization", "Bearer $token")
                append("Notion-Version", NOTION_API_VERSION)
            }
            contentType(ContentType.Application.Json)
            setBody(
                NotionQueryRequest(
                    filter = NotionFilter(
                        property = PROP_PRIORITY,
                        select   = NotionSelectFilter(equals = PRIORITY_HIGH),
                    ),
                ),
            )
        }.body()

        return response.results.map { it.toTask() }
    }

    private fun NotionPage.toTask(): Task = Task(
        id               = id,
        title            = readTitle(properties[PROP_NAME]) ?: "(Untitled)",
        priority         = readPriority(properties[PROP_PRIORITY]),
        tip              = "",                                  // Phase 2 step 3 (Gemini) fills this
        estimatedMinutes = readNumber(properties[PROP_ESTIMATE]) ?: 0,
        notionUrl        = url,
        notionRef        = "NOT-${id.takeLast(4).uppercase()}",
    )

    private fun readTitle(prop: JsonElement?): String? =
        prop?.jsonObject?.get("title")?.jsonArray
            ?.firstOrNull()?.jsonObject?.get("plain_text")?.jsonPrimitive?.content

    private fun readPriority(prop: JsonElement?): Priority {
        val name = prop?.jsonObject?.get("select")?.jsonObject
            ?.get("name")?.jsonPrimitive?.content
        return when (name?.lowercase()) {
            "high" -> Priority.HIGH
            "low"  -> Priority.LOW
            else   -> Priority.MID
        }
    }

    private fun readNumber(prop: JsonElement?): Int? =
        prop?.jsonObject?.get("number")?.jsonPrimitive?.intOrNull

    companion object {
        private const val NOTION_API_BASE   = "https://api.notion.com/v1"
        private const val NOTION_API_VERSION = "2022-06-28"
        private const val PROP_NAME      = "Task"
        private const val PROP_PRIORITY  = "Priority"
        private const val PROP_ESTIMATE  = "Estimated Time"
        private const val PRIORITY_HIGH  = "High"

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}

// --- Wire types (internal) -------------------------------------------------

@Serializable
private data class NotionQueryRequest(val filter: NotionFilter? = null)

@Serializable
private data class NotionFilter(val property: String, val select: NotionSelectFilter)

@Serializable
private data class NotionSelectFilter(val equals: String)

@Serializable
private data class NotionQueryResponse(val results: List<NotionPage> = emptyList())

@Serializable
private data class NotionPage(
    val id: String,
    val url: String,
    val properties: Map<String, JsonElement> = emptyMap(),
)
