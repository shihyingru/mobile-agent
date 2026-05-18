package com.luna.morningagent.data.sharedposts

import com.luna.morningagent.data.secure.TokenStore
import java.util.UUID
import kotlin.time.Clock
import kotlinx.serialization.json.Json

/**
 * Repository for shared posts. The local JSON cache in TokenStore is the
 * source of truth — Notion is a mirror that may lag behind.
 *
 * On save:
 *  1. Build a SharedPost from the raw shared text (extract URL, guess source).
 *  2. Append to the local cache immediately so the UI sees it on next read.
 *  3. If TokenStore.sharedPostsDbId is set, try createPage() — on success,
 *     stamp `notionId` into the cached entry; on failure, mark pendingSync.
 *  4. If unset, mark pendingSync; the Home banner (commit 3) will surface
 *     the setup flow later.
 *
 * Categorization is a no-op here — that's commit 2's SavedPostCategorizer
 * job, which mutates the same cache entry once the agent returns.
 */
class SharedPostsRepository(
    private val tokenStore: TokenStore,
    private val notionClient: SharedPostsNotionClient = SharedPostsNotionClient(tokenStore),
) {

    /**
     * Save a shared text payload (post body, optional subject).
     *
     * Result tells the caller whether the post landed in Notion or got cached
     * as pendingSync — so the receiver activity can pick the right toast copy.
     */
    suspend fun save(rawText: String, subject: String?): SaveResult {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return SaveResult.EmptyInput

        val url    = extractFirstUrl(trimmed)
        val source = detectSource(url)
        // Strip the trailing URL when it's clearly appended to the text so the
        // post content reads cleanly. Threads share format: "<body>\n\n<url>".
        val content = stripTrailingUrl(trimmed, url).ifBlank { trimmed }
        val author  = subject?.trim()?.takeIf { it.isNotBlank() }
            ?: url?.let { authorFromUrl(it) }

        val post = SharedPost(
            localId               = UUID.randomUUID().toString(),
            content               = content,
            source                = source,
            author                = author,
            url                   = url,
            savedAt               = Clock.System.now(),
            pendingSync           = tokenStore.getSharedPostsDbId() == null,
            pendingCategorization = true,
        )

        appendToCache(post)

        val dbId = tokenStore.getSharedPostsDbId()
        if (dbId == null) {
            return SaveResult.SavedPending(post)
        }

        // Notion write — on failure, leave the cache entry pendingSync = true
        // and let the banner flush later. The save itself still succeeded.
        return runCatching { notionClient.createPage(dbId, post) }
            .map { notionId ->
                val synced = post.copy(notionId = notionId, pendingSync = false)
                updateCache(post.localId) { synced }
                SaveResult.SavedToNotion(synced)
            }
            .getOrElse { _ ->
                updateCache(post.localId) { it.copy(pendingSync = true) }
                SaveResult.SavedPending(post)
            }
    }

    /**
     * Apply the agent's categorization to a cached post. Auto-adds any new
     * category names to the TokenStore taxonomy so the list grows organically
     * from the `["Misc"]` seed. If the post is already mirrored to Notion
     * (notionId set), patches the remote page too — failures here only flag
     * the post for retry; the local cache update stands.
     */
    suspend fun applyCategorization(
        localId: String,
        categories: List<String>,
        summary: String?,
    ) {
        // Filter out blanks and de-dupe (preserve agent order for the chip layout).
        val cleaned = categories
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (cleaned.isEmpty() && summary.isNullOrBlank()) return

        val currentTaxonomy = tokenStore.getSharedPostsTaxonomy()
        val newCategoryNames = cleaned.filter { it !in currentTaxonomy }
        if (newCategoryNames.isNotEmpty()) {
            tokenStore.saveSharedPostsTaxonomy(currentTaxonomy + newCategoryNames)
        }

        var notionIdToPatch: String? = null
        updateCache(localId) { existing ->
            notionIdToPatch = existing.notionId
            existing.copy(
                categories            = cleaned,
                summary               = summary?.trim()?.takeIf { it.isNotBlank() },
                pendingCategorization = false,
            )
        }

        notionIdToPatch?.let { pageId ->
            runCatching { notionClient.updatePageCategoriesAndSummary(pageId, cleaned, summary) }
                .onFailure {
                    // Local cache stands; flip the post back to pendingCategorization
                    // so a future run can re-push to Notion without losing the data.
                    updateCache(localId) { it.copy(pendingCategorization = true) }
                }
        }
    }

    fun listAll(): List<SharedPost> = readCache()

    /** Replace the cached post entirely — used by the categorizer (commit 2). */
    fun update(localId: String, transform: (SharedPost) -> SharedPost) {
        updateCache(localId, transform)
    }

    fun remove(localId: String) {
        val current = readCache().filterNot { it.localId == localId }
        writeCache(current)
    }

    /**
     * Rename a category across the taxonomy + every cached post that uses it.
     * If `new` already exists on a post (i.e. both old + new are present),
     * the rename de-dupes. Notion mirror gets a patch per affected post.
     * No-ops when `old` isn't in the taxonomy or `new` is blank / same as old.
     */
    suspend fun renameCategory(old: String, new: String) {
        val oldName = old.trim()
        val newName = new.trim()
        if (oldName.isBlank() || newName.isBlank() || oldName == newName) return

        val taxonomy = tokenStore.getSharedPostsTaxonomy()
        if (oldName !in taxonomy) return
        // Preserve order; drop duplicates that would arise if `new` was already there.
        val nextTaxonomy = taxonomy.map { if (it == oldName) newName else it }.distinct()
        tokenStore.saveSharedPostsTaxonomy(nextTaxonomy)

        val affected = mutableListOf<SharedPost>()
        readCache().forEach { post ->
            if (oldName in post.categories) {
                val swapped = post.categories.map { if (it == oldName) newName else it }.distinct()
                updateCache(post.localId) { it.copy(categories = swapped) }
                if (post.notionId != null) {
                    affected.add(post.copy(categories = swapped))
                }
            }
        }
        affected.forEach { p ->
            runCatching { notionClient.updatePageCategoriesAndSummary(p.notionId!!, p.categories, p.summary) }
        }
    }

    /**
     * Delete a category from the taxonomy and strip it from every cached post.
     * Notion-synced posts get a patch with the trimmed categories list.
     * No-op when `name` isn't in the taxonomy.
     */
    suspend fun deleteCategory(name: String) {
        val target = name.trim()
        if (target.isBlank()) return
        val taxonomy = tokenStore.getSharedPostsTaxonomy()
        if (target !in taxonomy) return
        tokenStore.saveSharedPostsTaxonomy(taxonomy.filterNot { it == target })

        val affected = mutableListOf<SharedPost>()
        readCache().forEach { post ->
            if (target in post.categories) {
                val trimmed = post.categories.filterNot { it == target }
                updateCache(post.localId) { it.copy(categories = trimmed) }
                if (post.notionId != null) {
                    affected.add(post.copy(categories = trimmed))
                }
            }
        }
        affected.forEach { p ->
            runCatching { notionClient.updatePageCategoriesAndSummary(p.notionId!!, p.categories, p.summary) }
        }
    }

    // --- Cache I/O ----------------------------------------------------------

    @Synchronized
    private fun appendToCache(post: SharedPost) {
        val current = readCache().toMutableList()
        // Newest first so the UI doesn't have to sort.
        current.add(0, post)
        writeCache(current)
    }

    @Synchronized
    private fun updateCache(localId: String, transform: (SharedPost) -> SharedPost) {
        val current = readCache().toMutableList()
        val idx = current.indexOfFirst { it.localId == localId }
        if (idx < 0) return
        current[idx] = transform(current[idx])
        writeCache(current)
    }

    private fun readCache(): List<SharedPost> {
        val raw = tokenStore.getSharedPostsCacheJson() ?: return emptyList()
        return runCatching { cacheJson.decodeFromString<List<SharedPost>>(raw) }
            .getOrDefault(emptyList())
    }

    private fun writeCache(posts: List<SharedPost>) {
        tokenStore.saveSharedPostsCacheJson(cacheJson.encodeToString(posts))
    }

    // --- Parsing helpers ----------------------------------------------------

    /** Detect source from URL host. Falls back to Other for pure-text shares. */
    private fun detectSource(url: String?): String {
        if (url == null) return SharedPost.SOURCE_OTHER
        val lower = url.lowercase()
        return when {
            "threads.net" in lower || "threads.com" in lower -> SharedPost.SOURCE_THREADS
            "twitter.com" in lower || "x.com" in lower       -> SharedPost.SOURCE_TWITTER
            lower.startsWith("http")                          -> SharedPost.SOURCE_WEB
            else                                              -> SharedPost.SOURCE_OTHER
        }
    }

    /** Pull the first URL out of the shared text. */
    private fun extractFirstUrl(text: String): String? =
        URL_REGEX.find(text)?.value

    /**
     * If the URL sits at the very end of the text (typical share format), drop
     * it from the body so the content doesn't repeat what's already in `url`.
     * Leaves embedded URLs alone — only trims when the URL is the trailing
     * token after a blank line / whitespace.
     */
    private fun stripTrailingUrl(text: String, url: String?): String {
        if (url == null) return text
        val tail = text.trimEnd()
        if (!tail.endsWith(url)) return text
        return tail.removeSuffix(url).trimEnd()
    }

    /** Best-effort author from a Threads / Twitter URL path. */
    private fun authorFromUrl(url: String): String? {
        val match = USERNAME_IN_PATH.find(url) ?: return null
        return match.groupValues[1].let { if (it.startsWith("@")) it else "@$it" }
    }

    companion object {
        // Matches https?://[^\s]+ — good enough for shared text payloads.
        private val URL_REGEX = Regex("""https?://[^\s]+""")

        // Pulls /@username/ or /username/ from a URL path (Threads, Twitter, X).
        private val USERNAME_IN_PATH = Regex("""(?:threads\.(?:net|com)|twitter\.com|x\.com)/(@?[\w.]+)""")

        private val cacheJson = Json {
            ignoreUnknownKeys      = true
            encodeDefaults         = true
            prettyPrint            = false
        }
    }
}

sealed interface SaveResult {
    /** Convenience accessor — null when the save was rejected as empty input. */
    val post: SharedPost?

    /** Post was cached and mirrored to Notion successfully. */
    data class SavedToNotion(override val post: SharedPost) : SaveResult
    /** Post was cached but the Notion mirror is pending (no DB id, or write failed). */
    data class SavedPending(override val post: SharedPost)  : SaveResult
    /** Input was empty / whitespace — nothing was saved. */
    data object EmptyInput : SaveResult { override val post: SharedPost? = null }
}
