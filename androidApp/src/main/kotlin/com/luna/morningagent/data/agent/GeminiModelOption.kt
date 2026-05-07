package com.luna.morningagent.data.agent

import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.llm.LLModel

// User-selectable Gemini variants. Persisted by `id`; UI shows `displayName` and
// `tagline`. Adding a new option is a one-line change here — no other layer needs
// to learn about it. `id` doubles as the briefing footer's model label.
enum class GeminiModelOption(
    val id: String,
    val displayName: String,
    val tagline: String,
    val koogModel: LLModel,
) {
    FlashLite(
        id          = "gemini-2.5-flash-lite",
        displayName = "Flash-Lite",
        tagline     = "Fastest · most free quota",
        koogModel   = GoogleModels.Gemini2_5FlashLite,
    ),
    Flash(
        id          = "gemini-2.5-flash",
        displayName = "Flash",
        tagline     = "Default · solid free quota",
        koogModel   = GoogleModels.Gemini2_5Flash,
    ),
    Pro(
        id          = "gemini-2.5-pro",
        displayName = "Pro",
        tagline     = "Smartest · tight free quota",
        koogModel   = GoogleModels.Gemini2_5Pro,
    );

    companion object {
        val Default = Flash
        fun fromId(id: String?): GeminiModelOption =
            entries.firstOrNull { it.id == id } ?: Default
    }
}