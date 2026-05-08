package com.luna.morningagent.data

import com.luna.morningagent.data.agent.BriefingGenerator
import com.luna.morningagent.data.agent.GeminiKeyMissingException
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.notion.NotionTaskSource
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// Pulls the morning briefing together: fetch high-priority Notion tasks, ask Gemini
// for a summary + per-task tips, merge the tips back onto the tasks, return a Briefing.
@OptIn(ExperimentalTime::class)
class AgentRepository(
    private val notionTaskSource: NotionTaskSource,
    private val briefingGenerator: BriefingGenerator,
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

        return Briefing(
            generatedAt = Clock.System.now(),
            summary     = draft.summary,
            tasks       = tasksWithTips,
            model       = draft.model,
            tokens      = draft.tokens,
        )
    }

    // TODO(Phase 2 step4: read last briefing from local cache once WorkManager runs persist.)
    suspend fun getLastBriefing(): Briefing? = null
}

class AgentConfigMissingException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

class AgentNetworkException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)