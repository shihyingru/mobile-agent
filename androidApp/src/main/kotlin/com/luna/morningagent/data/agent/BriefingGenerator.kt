package com.luna.morningagent.data.agent

import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.model.BriefingKind
import com.luna.morningagent.data.model.ProposedAction
import com.luna.morningagent.data.model.Task

// Boundary for "given today's task list, produce a briefing." The active
// implementations today are GeminiBriefingClient and ClaudeBriefingClient (both
// Koog-backed). Kept narrow so the agent layer can be swapped without touching
// the repository or the UI.
interface BriefingGenerator {
    // `kind` picks the prompt: MORNING runs the focus-the-day prompt, EVENING
    // runs the wrap-up / carry-over prompt. `morningContext` is consulted only
    // for EVENING — the evening prompt diffs this morning's task list against
    // the current Notion state to detect what shipped vs what slipped.
    //
    // onAttempt fires before each attempt (current=1..total) so the UI can show
    // a "Retrying… (n/total)" hint when transient errors are being absorbed.
    suspend fun generate(
        tasks: List<Task>,
        kind: BriefingKind = BriefingKind.MORNING,
        morningContext: Briefing? = null,
        onAttempt: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): BriefingDraft
}

data class BriefingDraft(
    val summary: String,
    val tipsByTaskId: Map<String, String>,
    // Raw, syntactically-valid proposed actions from the model. AgentRepository
    // validates taskIds against the fetched set and caps the list before they
    // reach the Briefing.
    val proposedActions: List<ProposedAction>,
    val model: String,
    val tokens: Int,
)

class GeminiKeyMissingException(message: String) : IllegalStateException(message)