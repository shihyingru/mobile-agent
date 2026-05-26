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

    @Serializable
    @SerialName("mark_done")
    data class MarkDone(
        override val taskId: String,
        override val reason: String,
    ) : ProposedAction()

    @Serializable
    @SerialName("reschedule")
    data class Reschedule(
        override val taskId: String,
        override val reason: String,
        val newDate: String,  // ISO yyyy-mm-dd; model-supplied, not parsed yet
    ) : ProposedAction()

    @Serializable
    @SerialName("change_priority")
    data class ChangePriority(
        override val taskId: String,
        override val reason: String,
        val newPriority: Priority,
    ) : ProposedAction()
}
