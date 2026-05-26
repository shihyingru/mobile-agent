package com.luna.morningagent.data.notion

import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.secure.TokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// REST implementation of NotionTaskMutator. PATCHes /v1/pages/{id} with the
// minimal property body for each mutation type. PATCH bodies were verified
// against Notion's API in the PR 0 spike — see tasks/pr0-spike.md.
//
// `expectSuccess = true` differs from NotionRestClient: for the mutator we
// want 4xx/5xx to throw (caller surfaces an ErrorBanner) rather than
// silently no-op the way a JSON error body would.
class NotionRestMutator(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient = defaultClient(),
) : NotionTaskMutator {

    override suspend fun markDone(taskId: String) {
        patchPage(taskId, buildJsonObject {
            putJsonObject("properties") {
                putJsonObject(PROP_STATUS) {
                    putJsonObject("status") {
                        put("name", STATUS_DONE)
                    }
                }
            }
        })
    }

    override suspend fun reschedule(taskId: String, newDate: String) {
        // Parse to validate the model's date string. Notion accepts whatever
        // string we send; we'd rather fail fast on bad input than push a
        // garbage value into Luna's Notion DB. Throws DateTimeParseException.
        LocalDate.parse(newDate)
        patchPage(taskId, buildJsonObject {
            putJsonObject("properties") {
                putJsonObject(PROP_DATE) {
                    putJsonObject("date") {
                        put("start", newDate)
                    }
                }
            }
        })
    }

    override suspend fun changePriority(taskId: String, newPriority: Priority) {
        // Notion select option names must exist on the schema, case-sensitive.
        // Map enum to the canonical labels Luna's DB uses.
        val notionName = when (newPriority) {
            Priority.HIGH -> "High"
            Priority.MID  -> "Medium"
            Priority.LOW  -> "Low"
        }
        patchPage(taskId, buildJsonObject {
            putJsonObject("properties") {
                putJsonObject(PROP_PRIORITY) {
                    putJsonObject("select") {
                        put("name", notionName)
                    }
                }
            }
        })
    }

    private suspend fun patchPage(taskId: String, body: JsonObject) {
        val token = tokenStore.getNotionToken()
            ?: throw NotionConfigMissingException("Notion integration token not set in Settings")
        httpClient.patch("$NOTION_API_BASE/pages/$taskId") {
            headers {
                append("Authorization", "Bearer $token")
                append("Notion-Version", NOTION_API_VERSION)
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<JsonObject>()  // .body() forces the response chain and surfaces 4xx via expectSuccess
    }

    companion object {
        private const val NOTION_API_BASE    = "https://api.notion.com/v1"
        private const val NOTION_API_VERSION = "2022-06-28"
        private const val PROP_STATUS   = "Status"
        private const val PROP_DATE     = "Date"
        private const val PROP_PRIORITY = "Priority"
        private const val STATUS_DONE   = "Done"

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
