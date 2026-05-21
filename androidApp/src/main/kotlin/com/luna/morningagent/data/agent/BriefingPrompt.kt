package com.luna.morningagent.data.agent

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.Task
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Provider-neutral pieces of the briefing pipeline. Both Gemini and Claude
// implementations build the same prompt, parse the same JSON shape, and apply
// the same transient-failure retry policy — only the executor + model id differ.

internal const val BRIEFING_TEMPERATURE = 0.6

// 4 attempts total: try, wait 1s, try, wait 3s, try, wait 7s, try.
internal val BRIEFING_RETRY_BACKOFFS_MS = longArrayOf(1_000L, 3_000L, 7_000L)

internal const val BRIEFING_FALLBACK_SUMMARY =
    "Here's today's plan. Start with the first task — it sets up the rest."

internal const val BRIEFING_SYSTEM_PROMPT =
    "You are a calm, precise morning briefing assistant for a software engineer named Luna. " +
    "Be direct and specific. No fluff, no emojis, no headings. " +
    "Tips should be concrete (one short sentence) and grounded in the task title."

internal val BRIEFING_JSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

@Serializable
internal data class BriefingPayload(
    val summary: String = "",
    val tips: Map<String, String> = emptyMap(),
)

internal fun buildBriefingPrompt(tasks: List<Task>): Prompt = prompt(
    id     = "morning-briefing",
    params = LLMParams(temperature = BRIEFING_TEMPERATURE),
) {
    system(BRIEFING_SYSTEM_PROMPT)
    user(buildBriefingUserMessage(tasks))
}

internal fun buildBriefingUserMessage(tasks: List<Task>): String = buildString {
    appendLine("Today's tasks (overdue or due today, grouped by priority):")
    appendLine()
    val byPriority = tasks.groupBy { it.priority }
    listOf(Priority.HIGH, Priority.MID, Priority.LOW).forEach { p ->
        val bucket = byPriority[p].orEmpty()
        if (bucket.isEmpty()) return@forEach
        appendLine("${p.name} priority:")
        bucket.forEach { t ->
            append("- id=").append(t.id)
            append(" | title=").append(t.title)
            if (t.estimatedMinutes > 0) append(" | est=${t.estimatedMinutes}m")
            t.area?.takeIf { it.isNotBlank() }?.let { append(" | area=").append(it) }
            appendLine()
        }
        appendLine()
    }
    val highCount = byPriority[Priority.HIGH]?.size ?: 0
    val otherCount = tasks.size - highCount
    appendLine("Task:")
    appendLine("1. Write one short paragraph as the summary. Focus on the HIGH-priority")
    appendLine("   items only (there are $highCount of them). If $otherCount lower-priority")
    appendLine("   items also exist, acknowledge them in one phrase (\"plus N lower-priority")
    appendLine("   items in the queue\") without elaborating on any individual one.")
    appendLine("2. For every task above — high, mid, AND low — write a one-sentence concrete")
    appendLine("   tip grounded in the title. Tips are per-card UI; every id needs one.")
    appendLine()
    appendLine("Return ONLY a JSON object, no markdown fences, matching this shape:")
    appendLine("""{"summary": "<one short paragraph>", "tips": {"<task id>": "<one-sentence tip>"}}""")
    appendLine("Every task id above must appear as a key in \"tips\".")
}

internal fun parseBriefingResponse(raw: String): BriefingPayload {
    val trimmed = raw.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```")
        .trim()
    // Defensive: clip to outermost braces in case the model adds preamble.
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    val jsonText = if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    return BRIEFING_JSON.decodeFromString(BriefingPayload.serializer(), jsonText)
}

// Retry transient provider failures (503/UNAVAILABLE/overloaded/429/RESOURCE_EXHAUSTED).
// Other failures — auth, parse, network down — fail fast. Fires onAttempt before each
// try so the UI can show "Retrying… (n/total)".
internal suspend fun <T> runBriefingWithRetry(
    onAttempt: (Int, Int) -> Unit,
    block: suspend () -> T,
): T {
    val total = BRIEFING_RETRY_BACKOFFS_MS.size + 1
    for (i in BRIEFING_RETRY_BACKOFFS_MS.indices) {
        onAttempt(i + 1, total)
        try {
            return block()
        } catch (e: Exception) {
            if (!isBriefingTransient(e)) throw e
            delay(BRIEFING_RETRY_BACKOFFS_MS[i])
        }
    }
    onAttempt(total, total)
    return block()
}

private fun isBriefingTransient(e: Throwable): Boolean {
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
