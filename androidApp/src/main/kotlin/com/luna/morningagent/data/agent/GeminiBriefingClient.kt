package com.luna.morningagent.data.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
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
// Token usage isn't yet surfaced through Koog's `agent.run(): String`, so we
// return 0 and the UI hides the count. // TODO(Phase 2 step3 polish: pull tokens
// off Koog's response once the API surface is confirmed).
class GeminiBriefingClient(
    private val tokenStore: TokenStore,
) : BriefingGenerator {

    override suspend fun generate(tasks: List<Task>): BriefingDraft {
        val apiKey = tokenStore.getGeminiKey()
            ?: throw GeminiKeyMissingException("Gemini API key not set in Settings")

        if (tasks.isEmpty()) {
            return BriefingDraft(
                summary      = "No high-priority tasks today. Take the morning back.",
                tipsByTaskId = emptyMap(),
                model        = MODEL_DISPLAY,
                tokens       = 0,
            )
        }

        val agent = AIAgent(
            promptExecutor = simpleGoogleAIExecutor(apiKey),
            llmModel       = MODEL,
            systemPrompt   = SYSTEM_PROMPT,
            temperature    = 0.6,
        )

        val response = runWithRetry { agent.run(buildUserPrompt(tasks)) }
        val parsed = parseResponse(response)

        return BriefingDraft(
            summary      = parsed.summary.ifBlank { FALLBACK_SUMMARY },
            tipsByTaskId = parsed.tips,
            model        = MODEL_DISPLAY,
            tokens       = 0,
        )
    }

    // Retry transient Google AI failures (503 UNAVAILABLE, model overloaded, 429
    // RESOURCE_EXHAUSTED). Other failures — auth, parse, network down — fail fast.
    private suspend fun runWithRetry(block: suspend () -> String): String {
        for (attempt in RETRY_BACKOFFS_MS.indices) {
            try {
                return block()
            } catch (e: Exception) {
                if (!isTransient(e)) throw e
                delay(RETRY_BACKOFFS_MS[attempt])
            }
        }
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

    private fun buildUserPrompt(tasks: List<Task>): String = buildString {
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
        private val MODEL = GoogleModels.Gemini2_5Flash
        private const val MODEL_DISPLAY = "gemini-2.5-flash"
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