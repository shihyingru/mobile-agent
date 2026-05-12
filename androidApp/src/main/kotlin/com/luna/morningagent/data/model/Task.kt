package com.luna.morningagent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val title: String,
    val priority: Priority,
    val tip: String,
    val estimatedMinutes: Int,
    val notionUrl: String,
    // Short Notion reference ID shown below the task title (e.g. "NOT-1042")
    val notionRef: String,
    // Resolved name of the linked Area page (Notion relation). Null when the
    // task has no Area set or the lookup failed; UI hides the tag in both cases.
    val area: String? = null,
)
