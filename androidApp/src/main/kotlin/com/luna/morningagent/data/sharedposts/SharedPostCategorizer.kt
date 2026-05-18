package com.luna.morningagent.data.sharedposts

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.anthropic.AnthropicModels
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleAnthropicExecutor
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.params.LLMParams
import com.luna.morningagent.data.secure.TokenStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Categorizes a shared post and writes a short "why this mattered" summary.
 *
 * Always uses the cheapest tier — Gemini Flash-Lite preferred (it's the fastest
 * + has the most generous free quota), falling back to Claude Haiku 4.5 when
 * only a Claude key is configured. If neither key is set, returns null and the
 * post stays `pendingCategorization = true` so a future save can retry.
 *
 * The user's Home/Settings model pick is intentionally ignored — categorization
 * is a low-stakes classification task and shouldn't burn Pro/Sonnet tokens.
 *
 * Hybrid taxonomy: the agent prefers existing categories, but may suggest new
 * ones when nothing fits. New names get auto-added by the repository to the
 * TokenStore taxonomy so the list grows organically from the `["Misc"]` seed.
 */
class SharedPostCategorizer(
    private val tokenStore: TokenStore,
) {

    suspend fun categorize(
        post: SharedPost,
        existingTaxonomy: List<String>,
    ): CategorizationResult? {
        val executor = buildExecutor() ?: return null
        val model    = preferredModel()
        val prompt   = buildCategorizationPrompt(post, existingTaxonomy)

        return runCatching {
            val responses = executor.execute(prompt = prompt, model = model)
            val assistant = responses.filterIsInstance<Message.Assistant>().firstOrNull()
                ?: return@runCatching null
            parseCategorizationResponse(assistant.content)
        }.getOrNull()
    }

    // --- Provider selection -------------------------------------------------

    /**
     * `Any` because `simpleGoogleAIExecutor` and `simpleAnthropicExecutor` return
     * different concrete types but share the `execute(prompt, model)` shape.
     * Captured here as a small adapter to keep the call site uniform.
     */
    private fun buildExecutor(): Executor? {
        tokenStore.getGeminiKey()?.takeIf { it.isNotBlank() }?.let { key ->
            val koog = simpleGoogleAIExecutor(key)
            return Executor { prompt, model -> koog.execute(prompt = prompt, model = model) }
        }
        tokenStore.getClaudeKey()?.takeIf { it.isNotBlank() }?.let { key ->
            val koog = simpleAnthropicExecutor(key)
            return Executor { prompt, model -> koog.execute(prompt = prompt, model = model) }
        }
        return null
    }

    private fun preferredModel(): LLModel {
        return if (!tokenStore.getGeminiKey().isNullOrBlank()) {
            GoogleModels.Gemini2_5FlashLite
        } else {
            AnthropicModels.Haiku_4_5
        }
    }

    private fun interface Executor {
        suspend fun execute(prompt: Prompt, model: LLModel): List<Message>
    }
}

// --- Prompt + parsing ------------------------------------------------------

@Serializable
data class CategorizationResult(
    val categories: List<String> = emptyList(),
    val summary:    String = "",
)

private const val CATEGORIZER_TEMPERATURE = 0.3   // Classification — keep it tight.
private const val CATEGORIZER_SYSTEM_PROMPT =
    "You categorize saved posts for a personal reading queue. Be concise and concrete. " +
    "No emojis, no markdown, no headings — return only the requested JSON."

private val CATEGORIZER_JSON = Json {
    ignoreUnknownKeys = true
    isLenient         = true
}

internal fun buildCategorizationPrompt(
    post: SharedPost,
    existingTaxonomy: List<String>,
): Prompt = prompt(
    id     = "shared-post-categorize",
    params = LLMParams(temperature = CATEGORIZER_TEMPERATURE),
) {
    system(CATEGORIZER_SYSTEM_PROMPT)
    user(buildCategorizerUserMessage(post, existingTaxonomy))
}

internal fun buildCategorizerUserMessage(
    post: SharedPost,
    existingTaxonomy: List<String>,
): String = buildString {
    appendLine("Existing categories: ${existingTaxonomy.joinToString(prefix = "[", postfix = "]")}")
    appendLine()
    appendLine("Post (from ${post.source}${post.author?.let { " by $it" } ?: ""}):")
    appendLine("\"\"\"")
    appendLine(post.content.take(2000))   // Truncate very long shares for prompt length.
    appendLine("\"\"\"")
    post.url?.let { appendLine("URL: $it") }
    appendLine()
    appendLine("Task:")
    appendLine("1. Pick 1–3 categories. Prefer existing ones. Only suggest new categories")
    appendLine("   if nothing in the list fits well — keep new names short (1–2 words).")
    appendLine("2. Write a 1-sentence summary of why this post mattered enough to save.")
    appendLine()
    appendLine("Return ONLY a JSON object, no markdown fences, matching this shape:")
    appendLine("""{"categories": ["<name>", ...], "summary": "<one sentence>"}""")
}

internal fun parseCategorizationResponse(raw: String): CategorizationResult? {
    val trimmed = raw.trim()
        .removePrefix("```json").removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = trimmed.indexOf('{')
    val end   = trimmed.lastIndexOf('}')
    val jsonText = if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    return runCatching {
        CATEGORIZER_JSON.decodeFromString(CategorizationResult.serializer(), jsonText)
    }.getOrNull()?.takeIf {
        // Don't accept empty results — let the post stay pendingCategorization=true
        // and try again next save.
        it.categories.isNotEmpty() || it.summary.isNotBlank()
    }
}
