package com.luna.morningagent.data

import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.Priority
import com.luna.morningagent.data.model.Task
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object PreviewData {
    val sampleBriefing = Briefing(
        generatedAt = Clock.System.now(),
        model       = "gemini-2.5",
        tokens      = 312,
        summary     = "Focus on the SDK doc first — it unblocks two downstream tasks and Maya's PR review can wait until after lunch when you'll be sharper.",
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
}
