package com.luna.morningagent.data.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Briefing(
    val generatedAt: Instant,
    val summary: String,
    val tasks: List<Task>,
    // Agent metadata shown in the briefing card footer (e.g. "gemini-2.5", 312)
    val model: String,
    val tokens: Int,
    // Reversible mutations the agent suggested for today. Empty when the model
    // declines; capped at 2 in AgentRepository. Default keeps old cached
    // briefings (written before this field existed) decoding cleanly.
    val actions: List<ProposedAction> = emptyList(),
    // Stable keys of actions dismissed (or applied) on this briefing. Persisted
    // alongside the briefing in TokenStore so dismissals survive process death.
    // Reset implicitly when runAgent() writes a fresh Briefing — new actions
    // get new keys anyway. Default keeps old caches decoding cleanly.
    val dismissedActionIds: Set<String> = emptySet(),
    // MORNING (default) for the 9am briefing; EVENING for the wrap-up.
    // Default keeps caches written before this field existed decoding cleanly.
    val kind: BriefingKind = BriefingKind.MORNING,
)
