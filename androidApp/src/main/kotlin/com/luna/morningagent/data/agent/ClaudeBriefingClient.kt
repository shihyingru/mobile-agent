package com.luna.morningagent.data.agent

import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.message.Message
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.data.secure.TokenStore

// Koog-based implementation of BriefingGenerator for Anthropic Claude. Mirrors
// GeminiBriefingClient one-for-one — same prompt, same retry/backoff policy,
// same JSON shape, different executor + model registry. The shared helpers
// in BriefingPrompt.kt keep the two clients from drifting.
//
// Anthropic's token usage lives in the same Message.Assistant.metaInfo slot,
// so the BriefingCard footer renders identically across providers.
class ClaudeBriefingClient(
    private val tokenStore: TokenStore,
) : BriefingGenerator {

    override suspend fun generate(
        tasks: List<Task>,
        onAttempt: (Int, Int) -> Unit,
    ): BriefingDraft {
        val apiKey = tokenStore.getClaudeKey()
            ?: throw ClaudeKeyMissingException("Claude API key not set in Settings")

        val option = ClaudeModelOption.fromId(tokenStore.getClaudeModel())

        if (tasks.isEmpty()) {
            return BriefingDraft(
                summary         = "No high-priority tasks today. Take the morning back.",
                tipsByTaskId    = emptyMap(),
                proposedActions = emptyList(),
                model           = option.id,
                tokens          = 0,
            )
        }

        val executor = simpleAnthropicExecutor(apiKey)
        val responses = runBriefingWithRetry(onAttempt) {
            executor.execute(prompt = buildBriefingPrompt(tasks), model = option.koogModel)
        }

        val assistant = responses.filterIsInstance<Message.Assistant>().firstOrNull()
            ?: error("Claude returned no assistant response")
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

class ClaudeKeyMissingException(message: String) : IllegalStateException(message)
