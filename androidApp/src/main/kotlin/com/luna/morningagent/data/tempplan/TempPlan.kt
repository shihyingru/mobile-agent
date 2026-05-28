package com.luna.morningagent.data.tempplan

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class TempPlan(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val tasks: List<TempTask> = emptyList(),
    val archived: Boolean = false,
    val createdAt: Instant,
)
