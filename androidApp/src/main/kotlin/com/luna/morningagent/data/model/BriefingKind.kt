package com.luna.morningagent.data.model

import kotlinx.serialization.Serializable

// Discriminates the morning briefing from the evening reflection. Drives prompt
// selection in the LLM clients, cache key in TokenStore, and slot-aware UI in
// HomeScreen (PR 2 of evening-reflection). Default MORNING keeps pre-existing
// cached Briefing JSON decoding cleanly after this field is added.
@Serializable
enum class BriefingKind { MORNING, EVENING }
