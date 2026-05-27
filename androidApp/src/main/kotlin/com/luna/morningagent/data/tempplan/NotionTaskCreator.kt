package com.luna.morningagent.data.tempplan

import com.luna.morningagent.data.notion.NotionConfigMissingException
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
import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class NotionTaskCreator(
    private val tokenStore: TokenStore,
    private val httpClient: HttpClient = defaultClient(),
) {

    suspend fun createTaskPage(title: String): String {
        val token = tokenStore.getNotionToken()
            ?: throw NotionConfigMissingException("Notion integration token not set in Settings")
        val dbId = tokenStore.getNotionDatabaseId()
            ?: throw NotionConfigMissingException("Notion database ID not set in Settings")

        val body = buildJsonObject {
            putJsonObject("parent") { put("database_id", dbId) }
            putJsonObject("properties") {
                putJsonObject(PROP_NAME) {
                    putJsonArray("title") {
                        add(buildJsonObject {
                            putJsonObject("text") { put("content", title) }
                        })
                    }
                }
                putJsonObject(PROP_STATUS) {
                    putJsonObject("status") { put("name", STATUS_NOT_STARTED) }
                }
                putJsonObject(PROP_DATE) {
                    putJsonObject("date") { put("start", LocalDate.now().toString()) }
                }
                putJsonObject(PROP_PRIORITY) {
                    putJsonObject("select") { put("name", PRIORITY_MEDIUM) }
                }
            }
        }

        val response = httpClient.post("$NOTION_API_BASE/pages") {
            headers {
                append("Authorization", "Bearer $token")
                append("Notion-Version", NOTION_API_VERSION)
            }
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body<JsonObject>()

        return response["id"]?.jsonPrimitive?.content
            ?: error("Notion returned no page id")
    }

    companion object {
        private const val NOTION_API_BASE    = "https://api.notion.com/v1"
        private const val NOTION_API_VERSION = "2022-06-28"
        private const val PROP_NAME          = "Task"
        private const val PROP_STATUS        = "Status"
        private const val PROP_DATE          = "Date"
        private const val PROP_PRIORITY      = "Priority"
        private const val STATUS_NOT_STARTED = "Not started"
        private const val PRIORITY_MEDIUM    = "Medium"

        private fun defaultClient(): HttpClient = HttpClient(OkHttp) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
}
