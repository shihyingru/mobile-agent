package com.luna.morningagent.data.agent

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.data.secure.TokenStore
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Koog-based implementation of BriefingGenerator.
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
                summary      = "No high-priority tasks today. Take the morning back.",
                tipsByTaskId = emptyMap(),
                model        = option.id,
                tokens       = 0,
            )
        }

        val executor = simpleGoogleAIExecutor(apiKey)
        val responses = runWithRetry(onAttempt) {
            executor.execute(prompt = buildPrompt(tasks), model = option.koogModel)
        }

        val assistant = responses.filterIsInstance<Message.Assistant>().firstOrNull()
            ?: error("Gemini returned no assistant response")
        val parsed = parseResponse(assistant.content)

        return BriefingDraft(
            summary      = parsed.summary.ifBlank { FALLBACK_SUMMARY },
            tipsByTaskId = parsed.tips,
            model        = option.id,
            tokens       = assistant.metaInfo.totalTokensCount ?: 0,
        )
    }

    private fun buildPrompt(tasks: List<Task>): Prompt = prompt(
        id     = "morning-briefing",
        params = LLMParams(temperature = TEMPERATURE),
    ) {
        system(SYSTEM_PROMPT)
        user(buildUserMessage(tasks))
    }

    // Retry transient Google AI failures (503 UNAVAILABLE, model overloaded, 429
    // RESOURCE_EXHAUSTED). Other failures — auth, parse, network down — fail fast.
    // Fires onAttempt before each try so the UI can show "Retrying… (n/total)".
    private suspend fun <T> runWithRetry(
        onAttempt: (Int, Int) -> Unit,
        block: suspend () -> T,
    ): T {
        val total = RETRY_BACKOFFS_MS.size + 1
        for (i in RETRY_BACKOFFS_MS.indices) {
            onAttempt(i + 1, total)
            try {
                return block()
            } catch (e: Exception) {
                if (!isTransient(e)) throw e
                delay(RETRY_BACKOFFS_MS[i])
            }
        }
        onAttempt(total, total)
        return block()
    }

    private fun isTransient(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            val msg = cur.message.orEmpty()
            if (msg.contains("503") ||
                msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("overloaded", ignoreCase = true) ||
                msg.contains("429") ||
                msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true)
            ) return true
            cur = cur.cause
        }
        return false
    }

    private fun buildUserMessage(tasks: List<Task>): String = buildString {
        appendLine("Today's high-priority tasks:")
        appendLine()
        tasks.forEach { t ->
            append("- id=").append(t.id)
            append(" | title=").append(t.title)
            if (t.estimatedMinutes > 0) append(" | est=${t.estimatedMinutes}m")
            appendLine()
        }
        appendLine()
        appendLine("Return ONLY a JSON object, no markdown fences, matching this shape:")
        appendLine("""{"summary": "<one short paragraph>", "tips": {"<task id>": "<one-sentence tip>"}}""")
        appendLine("Every task id above must appear as a key in \"tips\".")
    }

    private fun parseResponse(raw: String): GeminiBriefingPayload {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        // Defensive: clip to outermost braces in case the model adds preamble.
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        val jsonText = if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
        return JSON.decodeFromString(GeminiBriefingPayload.serializer(), jsonText)
    }

    @Serializable
    private data class GeminiBriefingPayload(
        val summary: String = "",
        val tips: Map<String, String> = emptyMap(),
    )

    companion object {
        private const val TEMPERATURE = 0.6
        // 4 attempts total: try, wait 1s, try, wait 3s, try, wait 7s, try.
        private val RETRY_BACKOFFS_MS = longArrayOf(1_000L, 3_000L, 7_000L)
        private const val FALLBACK_SUMMARY =
            "Here's today's plan. Start with the first task — it sets up the rest."
        private const val SYSTEM_PROMPT =
            "You are a calm, precise morning briefing assistant for a software engineer named Luna. " +
            "Be direct and specific. No fluff, no emojis, no headings. " +
            "Tips should be concrete (one short sentence) and grounded in the task title."

        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}
