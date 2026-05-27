package com.luna.morningagent.data

import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.model.Task
import com.luna.morningagent.data.tempplan.TempPlan
import com.luna.morningagent.data.tempplan.TempTask
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

    val sampleTempPlan = TempPlan(
        id        = "plan-1",
        name      = "Tokyo 5D4N",
        startDate = "2026-06-01",
        endDate   = "2026-06-05",
        createdAt = Clock.System.now(),
        tasks     = listOf(
            TempTask(id = "t1", title = "Book Shinkansen tickets", checked = true),
            TempTask(id = "t2", title = "Reserve TeamLab Borderless", checked = true, promotedToNotionId = "abc"),
            TempTask(id = "t3", title = "Pack portable charger", dayIndex = 1),
            TempTask(id = "t4", title = "Confirm Airbnb check-in", dayIndex = 1),
            TempTask(id = "t5", title = "Exchange JPY at airport", dayIndex = 1),
        ),
    )
}
