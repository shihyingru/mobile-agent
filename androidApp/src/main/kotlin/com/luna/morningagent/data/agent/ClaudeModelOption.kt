package com.luna.morningagent.data.agent

import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.llm.LLModel

// User-selectable Claude variants. Mirrors GeminiModelOption — same three-tier
// shape (fastest / default / smartest) so the Home picker can swap provider
// without changing layout. Persisted by `id`. Anthropic's free tier is
// pay-as-you-go credits rather than per-model quota, so the taglines don't
// promise "free quota" — they emphasize speed vs depth instead.
enum class ClaudeModelOption(
    val id: String,
    val displayName: String,
    val tagline: String,
    val koogModel: LLModel,
) {
    Haiku(
        id          = "claude-haiku-4-5",
        displayName = "Haiku 4.5",
        tagline     = "Fastest · cheapest",
        koogModel   = AnthropicModels.Haiku_4_5,
    ),
    Sonnet(
        id          = "claude-sonnet-4-6",
        displayName = "Sonnet 4.6",
        tagline     = "Default · balanced",
        koogModel   = AnthropicModels.Sonnet_4_6,
    ),
    Opus(
        id          = "claude-opus-4-6",
        displayName = "Opus 4.6",
        tagline     = "Smartest · pricey",
        koogModel   = AnthropicModels.Opus_4_6,
    );

    companion object {
        val Default = Sonnet
        fun fromId(id: String?): ClaudeModelOption =
            entries.firstOrNull { it.id == id } ?: Default
    }
}
