package com.luna.morningagent.data.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Briefing(
    val generatedAt: Instant,
    val summary: String,
    val tasks: List<Task>,
    // Agent metadata shown in the briefing card footer (e.g. "gemini-2.5", 312)
    val model: String,
    val tokens: Int,
)
