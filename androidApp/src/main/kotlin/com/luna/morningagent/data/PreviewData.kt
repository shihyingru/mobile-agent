package com.luna.morningagent.data

import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.BriefingKind
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.model.Task
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object PreviewData {
    val sampleProposedActions: List<ProposedAction> = listOf(
        ProposedAction.MarkDone(
            taskId = "3",
            reason = "Looks like you already replied yesterday.",
        ),
        ProposedAction.Reschedule(
            taskId = "2",
            reason = "PR review fits better tomorrow with a 1-hour block.",
            newDate = "2026-05-27",
        ),
    )

    val sampleBriefing = Briefing(
        generatedAt = Clock.System.now(),
        model       = "gemini-2.5",
        tokens      = 312,
        summary     = "Focus on the SDK doc first — it unblocks two downstream tasks and Maya's PR review can wait until after lunch when you'll be sharper.",
        actions     = sampleProposedActions,
        tasks = listOf(
            Task(
                id               = "1",
                title            = "Write SDK documentation",
                priority         = Priority.HIGH,
                tip              = "Start with the public API surface. Skip examples until structure is solid.",
                estimatedMinutes = 120,
                notionUrl        = "https://notion.so/...",
                notionRef        = "NOT-1042",
                area             = "Engineering",
            ),
            Task(
                id               = "2",
                title            = "Review PR #482",
                priority         = Priority.HIGH,
                tip              = "The diff is large but most changes are mechanical. Focus review on the auth module.",
                estimatedMinutes = 45,
                notionUrl        = "https://notion.so/...",
                notionRef        = "NOT-1043",
                area             = "Engineering",
            ),
            Task(
                id               = "3",
                title            = "Reply to design feedback",
                priority         = Priority.MID,
                tip              = "Batch all responses into one message rather than scattered replies.",
                estimatedMinutes = 20,
                notionUrl        = "https://notion.so/...",
                notionRef        = "NOT-1044",
                area             = "Design",
            ),
        ),
    )

    val sampleEveningReflection = Briefing(
        generatedAt = Clock.System.now(),
        model       = "gemini-2.5",
        tokens      = 198,
        summary     = "Good day — the SDK doc is drafted and the PR review is done. Design feedback slipped; carry it to tomorrow with a fresh block.",
        kind        = BriefingKind.EVENING,
        actions     = listOf(
            ProposedAction.Reschedule(
                taskId  = "3",
                reason  = "Didn't get to it today — move to tomorrow morning.",
                newDate = "2026-05-28",
            ),
            ProposedAction.MarkDone(
                taskId = "2",
                reason = "PR #482 was merged after your review.",
            ),
        ),
        tasks = listOf(
            Task(
                id               = "3",
                title            = "Reply to design feedback",
                priority         = Priority.MID,
                tip              = "Carry over — first thing tomorrow.",
                estimatedMinutes = 20,
                notionUrl        = "https://notion.so/...",
                notionRef        = "NOT-1044",
                area             = "Design",
            ),
            Task(
                id               = "2",
                title            = "Review PR #482",
                priority         = Priority.HIGH,
                tip              = "Done — merged.",
                estimatedMinutes = 45,
                notionUrl        = "https://notion.so/...",
                notionRef        = "NOT-1043",
                area             = "Engineering",
            ),
        ),
    )
}
