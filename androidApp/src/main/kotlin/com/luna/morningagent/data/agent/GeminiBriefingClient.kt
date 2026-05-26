package com.luna.morningagent.data.agent

import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.message.Message
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.data.secure.TokenStore

// Koog-based implementation of BriefingGenerator for Google Gemini.
//
// One-shot prompt: the agent receives the already-fetched Notion tasks and returns
// a single JSON object with a summary + per-task tips. We deliberately don't give
// Gemini Notion as a tool — adding tool-calling latency and failure modes for a
// daily briefing isn't worth it. The interface keeps this swappable.
//
// Uses the lower-level executor.execute() (not AIAgent.run) so we can read token
// counts off Message.Assistant.metaInfo.
class GeminiBriefingClient(
    private val tokenStore: TokenStore,
) : BriefingGenerator {

    override suspend fun generate(
        tasks: List<Task>,
        onAttempt: (Int, Int) -> Unit,
    ): BriefingDraft {
        val apiKey = tokenStore.getGeminiKey()
            ?: throw GeminiKeyMissingException("Gemini API key not set in Settings")

        // Resolved fresh on each call so today's pick from the Home picker takes
        // effect on the next Run Now without restarting the app.
        val option = GeminiModelOption.fromId(tokenStore.getGeminiModel())

        if (tasks.isEmpty()) {
            return BriefingDraft(
                summary         = "No high-priority tasks today. Take the morning back.",
                tipsByTaskId    = emptyMap(),
                proposedActions = emptyList(),
                model           = option.id,
                tokens          = 0,
            )
        }

        val executor = simpleGoogleAIExecutor(apiKey)
        val responses = runBriefingWithRetry(onAttempt) {
            executor.execute(prompt = buildBriefingPrompt(tasks), model = option.koogModel)
        }

        val assistant = responses.filterIsInstance<Message.Assistant>().firstOrNull()
            ?: error("Gemini returned no assistant response")
        val parsed = parseBriefingResponse(assistant.content)

        return BriefingDraft(
            summary         = parsed.summary.ifBlank { BRIEFING_FALLBACK_SUMMARY },
            tipsByTaskId    = parsed.tips,
            proposedActions = parsed.proposedActions.toProposedActions(),
            model           = option.id,
            tokens          = assistant.metaInfo.totalTokensCount ?: 0,
        )
    }
}
