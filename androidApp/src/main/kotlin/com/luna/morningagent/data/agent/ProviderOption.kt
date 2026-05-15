package com.luna.morningagent.data.agent

// Which LLM provider the agent layer talks to. Stored in TokenStore by `id`, so
// adding a third provider (OpenAI, OpenRouter, …) is a single entry here plus a
// new BriefingGenerator implementation — no schema migration. Default is Gemini
// for back-compat with users who already had Gemini keys saved pre-Claude.
enum class ProviderOption(
    val id: String,
    val displayName: String,
) {
    Gemini(id = "gemini", displayName = "Gemini"),
    Claude(id = "claude", displayName = "Claude");

    companion object {
        val Default = Gemini
        fun fromId(id: String?): ProviderOption =
            entries.firstOrNull { it.id == id } ?: Default
    }
}
