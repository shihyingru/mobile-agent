package com.luna.morningagent.data

import com.luna.morningagent.data.agent.BriefingGenerator
import com.luna.morningagent.data.agent.GeminiKeyMissingException
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.BriefingKind
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.notion.NotionTaskSource
import com.luna.morningagent.data.secure.TokenStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json

// Pulls the morning briefing together: fetch today's Notion tasks (sorted
// priority desc, overdue included), ask the briefing model for a summary + per-task
// tips focused on high-priority work, merge the tips back, return a Briefing.
@OptIn(ExperimentalTime::class)
class AgentRepository(
    private val notionTaskSource: NotionTaskSource,
    private val briefingGenerator: BriefingGenerator,
    private val tokenStore: TokenStore? = null,
) {
    // Throws AgentConfigMissingException when keys aren't set, AgentNetworkException
    // for transport failures, IllegalStateException for parse / unexpected errors.
    // Callers (HomeViewModel, workers) translate these to UI states.
    //
    // `kind` picks morning briefing vs evening reflection: prompt, persistence
    // cache key, and the morning-context lookup all branch on it. Default is
    // MORNING so existing call sites stay one-arg.
    suspend fun runAgent(
        kind: BriefingKind = BriefingKind.MORNING,
        onAttempt: (Int, Int) -> Unit = { _, _ -> },
    ): Briefing {
        val tasks = try {
            notionTaskSource.fetchTodayTasks()
        } catch (e: NotionConfigMissingException) {
            throw AgentConfigMissingException(e.message ?: "Notion not configured", e)
        } catch (e: Exception) {
            throw AgentNetworkException("Couldn't reach Notion. ${e.message ?: ""}".trim(), e)
        }

        // Evening reflection diffs the morning's list to detect what shipped.
        // Null is fine — the reflection prompt handles "no morning context."
        val morningContext = if (kind == BriefingKind.EVENING) getLastBriefing() else null

        val draft = try {
            briefingGenerator.generate(tasks, kind, morningContext, onAttempt)
        } catch (e: GeminiKeyMissingException) {
            throw AgentConfigMissingException(e.message ?: "Gemini not configured", e)
        } catch (e: Exception) {
            throw AgentNetworkException("Couldn't reach Gemini. ${e.message ?: ""}".trim(), e)
        }

        val tasksWithTips = tasks.map { t ->
            t.copy(tip = draft.tipsByTaskId[t.id]?.trim().orEmpty())
        }

        // Validate proposed actions against the fetched task set — a hallucinated
        // taskId never reaches the UI. Cap at 2 so a noisy model can't spam chips.
        val taskIds = tasks.mapTo(mutableSetOf()) { it.id }
        val validatedActions = draft.proposedActions
            .filter { it.taskId in taskIds }
            .take(2)

        val briefing = Briefing(
            generatedAt = Clock.System.now(),
            summary     = draft.summary,
            tasks       = tasksWithTips,
            model       = draft.model,
            tokens      = draft.tokens,
            actions     = validatedActions,
            kind        = kind,
        )

        // Persist to the per-kind cache so a cold launch shows the freshest one
        // without re-hitting the network. Failures here shouldn't sink the result.
        tokenStore?.let { store ->
            val json = briefingJson.encodeToString(Briefing.serializer(), briefing)
            runCatching {
                when (kind) {
                    BriefingKind.MORNING -> store.saveLastBriefingJson(json)
                    BriefingKind.EVENING -> store.saveLastReflectionJson(json)
                }
            }
        }

        return briefing
    }

    // Reads the most recent evening reflection written by runAgent(EVENING) or
    // the evening worker. Returns null on first run or after a parse failure.
    fun getLastReflection(): Briefing? {
        val store = tokenStore ?: return null
        val json = store.getLastReflectionJson() ?: return null
        return runCatching { briefingJson.decodeFromString(Briefing.serializer(), json) }.getOrNull()
    }

    // Reads the most recent briefing written by runAgent() or the WorkManager job.
    // Returns null on first run or after a parse failure (schema migration etc.).
    fun getLastBriefing(): Briefing? {
        val store = tokenStore ?: return null
        val json = store.getLastBriefingJson() ?: return null
        return runCatching { briefingJson.decodeFromString(Briefing.serializer(), json) }.getOrNull()
    }

    // Adds an action's stable key to the cached briefing's dismissed set and
    // re-persists. No-op if there's no cached briefing or the id is already
    // dismissed. Persistence failure is swallowed — worst case the dismissal
    // doesn't survive process death (in-memory state still updated by caller).
    fun saveDismissal(actionId: String) {
        val store = tokenStore ?: return

        getLastBriefing()?.let { morning ->
            if (actionId !in morning.dismissedActionIds &&
                morning.actions.any { it.stableKey() == actionId }
            ) {
                val updated = morning.copy(dismissedActionIds = morning.dismissedActionIds + actionId)
                runCatching { store.saveLastBriefingJson(briefingJson.encodeToString(Briefing.serializer(), updated)) }
                return
            }
        }

        getLastReflection()?.let { evening ->
            if (actionId !in evening.dismissedActionIds &&
                evening.actions.any { it.stableKey() == actionId }
            ) {
                val updated = evening.copy(dismissedActionIds = evening.dismissedActionIds + actionId)
                runCatching { store.saveLastReflectionJson(briefingJson.encodeToString(Briefing.serializer(), updated)) }
            }
        }
    }

    private companion object {
        val briefingJson = Json { ignoreUnknownKeys = true }
    }
}

class AgentConfigMissingException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

class AgentNetworkException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)