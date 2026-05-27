package com.luna.morningagent.data.agent

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.Task
import java.time.LocalDate
import java.time.ZoneId

// Evening wrap-up prompt. Forward-looking voice — past-tense for what shipped,
// imperative for what carries over. Reuses the same JSON output shape as the
// morning prompt (BriefingPayload + proposedActions) so BriefingActions chips
// work identically — only the system prompt + user message differ.
//
// The model diffs this morning's task list against the current open list to
// detect what got done. Proposed actions favour `reschedule` to tomorrow for
// stale items.

internal const val REFLECTION_SYSTEM_PROMPT =
    "You are a calm, precise evening wrap-up assistant for a software engineer named Luna. " +
    "Today is winding down; tomorrow needs to be set. Voice is forward-looking — past tense " +
    "for what shipped today, imperative for what carries over. No fluff, no emojis, no headings. " +
    "Tips should be concrete (one short sentence) and grounded in the task title."

internal fun buildReflectionPrompt(
    currentTasks: List<Task>,
    morningContext: Briefing?,
    today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
): Prompt = prompt(
    id     = "evening-reflection",
    params = LLMParams(temperature = BRIEFING_TEMPERATURE),
) {
    system(REFLECTION_SYSTEM_PROMPT)
    user(buildReflectionUserMessage(currentTasks, morningContext, today))
}

internal fun buildReflectionUserMessage(
    currentTasks: List<Task>,
    morningContext: Briefing?,
    today: LocalDate,
): String = buildString {
    val tomorrow = today.plusDays(1)
    appendLine("Today is $today; tomorrow is $tomorrow.")
    appendLine()

    if (morningContext != null && morningContext.tasks.isNotEmpty()) {
        appendLine("This morning's briefing listed these tasks:")
        morningContext.tasks.forEach { t ->
            append("- id=").append(t.id)
            append(" | title=").append(t.title)
            append(" | priority=").append(t.priority.name)
            appendLine()
        }
        appendLine()
    }

    appendLine("Currently open in Notion (still on the list, grouped by priority):")
    val byPriority = currentTasks.groupBy { it.priority }
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

    appendLine("Task:")
    appendLine("1. Write one short paragraph as the summary. Acknowledge what shipped")
    appendLine("   (items that were in this morning's list but are no longer in the open")
    appendLine("   list got done). Then set up tomorrow in one phrase.")
    appendLine("2. For every currently-open task above, write a one-sentence concrete tip")
    appendLine("   grounded in the title. Tips are per-card UI; every id needs one.")
    appendLine("3. Propose at most 2 reversible carry-over actions to leave tomorrow set.")
    appendLine("   Favour rescheduling stale items to $tomorrow. Each action has:")
    appendLine("     - \"type\": one of \"mark_done\" | \"reschedule\" | \"change_priority\"")
    appendLine("     - \"taskId\": MUST match an id from the currently-open list above")
    appendLine("     - \"reason\": one short sentence")
    appendLine("     - type-specific fields:")
    appendLine("         mark_done       — no extra fields")
    appendLine("         reschedule      — \"newDate\" as ISO yyyy-mm-dd, MUST be on or after $tomorrow")
    appendLine("         change_priority — \"newPriority\" as \"High\" | \"Medium\" | \"Low\"")
    appendLine("   Omit the \"proposedActions\" field entirely if nothing is worth proposing.")
    appendLine()
    appendLine("Return ONLY a JSON object, no markdown fences, matching this shape:")
    appendLine("""{"summary": "...", "tips": {"<id>": "..."}, "proposedActions": [{"type": "reschedule", "taskId": "<id>", "reason": "...", "newDate": "$tomorrow"}]}""")
    appendLine("Every currently-open task id must appear as a key in \"tips\". \"proposedActions\" may be absent or have at most 2 entries.")
}
