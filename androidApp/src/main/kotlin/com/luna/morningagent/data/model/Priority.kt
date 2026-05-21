package com.luna.morningagent.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class Priority(val weight: Int) {
    HIGH(3),
    MID(2),
    LOW(1),
}
