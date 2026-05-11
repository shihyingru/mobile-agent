package com.luna.morningagent.data

import com.luna.morningagent.data.agent.BriefingGenerator
import com.luna.morningagent.data.agent.GeminiKeyMissingException
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.notion.NotionTaskSource
import com.luna.morningagent.data.secure.TokenStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json

// Pulls the morning briefing together: fetch high-priority Notion tasks, ask Gemini
// for a summary + per-task tips, merge the tips back onto the tasks, return a Briefing.
@OptIn(ExperimentalTime::class)
class AgentRepository(
    private val notionTaskSource: NotionTaskSource,
    private val briefingGenerator: BriefingGenerator,
    private val tokenStore: TokenStore? = null,
) {
    // Throws AgentConfigMissingException when keys aren't set, AgentNetworkException
    // for transport failures, IllegalStateException for parse / unexpected errors.
    // Callers (HomeViewModel) translate these to UI states.
    suspend fun runAgent(onAttempt: (Int, Int) -> Unit = { _, _ -> }): Briefing {
        val tasks = try {
            notionTaskSource.fetchHighPriorityTasks()
        } catch (e: NotionConfigMissingException) {
            throw AgentConfigMissingException(e.message ?: "Notion not configured", e)
        } catch (e: Exception) {
            throw AgentNetworkException("Couldn't reach Notion. ${e.message ?: ""}".trim(), e)
        }

        val draft = try {
            briefingGenerator.generate(tasks, onAttempt)
        } catch (e: GeminiKeyMissingException) {
            throw AgentConfigMissingException(e.message ?: "Gemini not configured", e)
        } catch (e: Exception) {
            throw AgentNetworkException("Couldn't reach Gemini. ${e.message ?: ""}".trim(), e)
        }

        val tasksWithTips = tasks.map { t ->
            t.copy(tip = draft.tipsByTaskId[t.id]?.trim().orEmpty())
        }

        val briefing = Briefing(
            generatedAt = Clock.System.now(),
            summary     = draft.summary,
            tasks       = tasksWithTips,
            model       = draft.model,
            tokens      = draft.tokens,
        )

        // Persist so a cold app launch can show the morning's briefing without
        // re-hitting the network. Failures here shouldn't sink the result.
        tokenStore?.let { store ->
            runCatching { store.saveLastBriefingJson(briefingJson.encodeToString(Briefing.serializer(), briefing)) }
        }

        return briefing
    }

    // Reads the most recent briefing written by runAgent() or the WorkManager job.
    // Returns null on first run or after a parse failure (schema migration etc.).
    fun getLastBriefing(): Briefing? {
        val store = tokenStore ?: return null
        val json = store.getLastBriefingJson() ?: return null
        return runCatching { briefingJson.decodeFromString(Briefing.serializer(), json) }.getOrNull()
    }

    private companion object {
        val briefingJson = Json { ignoreUnknownKeys = true }
    }
}

class AgentConfigMissingException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

class AgentNetworkException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)