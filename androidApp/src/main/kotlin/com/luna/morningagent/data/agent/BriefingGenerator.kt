package com.luna.morningagent.data.agent

import com.luna.morningagent.data.model.Task

// Boundary for "given today's high-priority tasks, produce a morning briefing." The
// active implementation today is GeminiBriefingClient (Koog → Google AI). Kept narrow
// so the agent layer can be swapped (different LLM, different framework) without
// touching the repository or the UI.
interface BriefingGenerator {
    // onAttempt fires before each attempt (current=1..total) so the UI can show
    // a "Retrying… (n/total)" hint when transient errors are being absorbed.
    suspend fun generate(
        tasks: List<Task>,
        onAttempt: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): BriefingDraft
}

data class BriefingDraft(
    val summary: String,
    val tipsByTaskId: Map<String, String>,
    val model: String,
    val tokens: Int,
)

class GeminiKeyMissingException(message: String) : IllegalStateException(message)