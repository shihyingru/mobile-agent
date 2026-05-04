package com.luna.morningagent.data.model

data class Task(
    val id: String,
    val title: String,
    val priority: Priority,
    val tip: String,
    val estimatedMinutes: Int,
    val notionUrl: String,
    // Short Notion reference ID shown below the task title (e.g. "NOT-1042")
    val notionRef: String,
)
