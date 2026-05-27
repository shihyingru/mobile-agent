package com.luna.morningagent.data.tempplan

import kotlinx.serialization.Serializable

@Serializable
data class TempTask(
    val id: String,
    val title: String,
    val checked: Boolean = false,
    val dayIndex: Int? = null,
    val promotedToNotionId: String? = null,
)
