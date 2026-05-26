package com.luna.morningagent.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Reversible mutations the briefing agent can propose. Validated against the
// fetched task set in AgentRepository — hallucinated taskIds never reach the UI.
// Capped at 2 per briefing (also AgentRepository). Read-only in PR 1; PR 2 wires
// the Apply button via NotionTaskMutator.
@Serializable
sealed class ProposedAction {
    abstract val taskId: String
    abstract val reason: String

    // Stable identity used for dismissal tracking (PR 2) and persistence (PR 3).
    // Type + payload so "same task, different priority change" gets distinct keys.
    abstract fun stableKey(): String

    @Serializable
    @SerialName("mark_done")
    data class MarkDone(
        override val taskId: String,
        override val reason: String,
    ) : ProposedAction() {
        override fun stableKey() = "mark_done:$taskId"
    }

    @Serializable
    @SerialName("reschedule")
    data class Reschedule(
        override val taskId: String,
        override val reason: String,
        val newDate: String,  // ISO yyyy-mm-dd; model-supplied, not parsed yet
    ) : ProposedAction() {
        override fun stableKey() = "reschedule:$taskId:$newDate"
    }

    @Serializable
    @SerialName("change_priority")
    data class ChangePriority(
        override val taskId: String,
        override val reason: String,
        val newPriority: Priority,
    ) : ProposedAction() {
        override fun stableKey() = "change_priority:$taskId:${newPriority.name}"
    }
}
