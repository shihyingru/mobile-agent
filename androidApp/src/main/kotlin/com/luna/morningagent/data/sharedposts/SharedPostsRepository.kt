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
            return SaveResult.SavedPending
        }

        // Notion write — on failure, leave the cache entry pendingSync = true
        // and let the banner flush later. The save itself still succeeded.
        return runCatching { notionClient.createPage(dbId, post) }
            .map { notionId ->
                updateCache(post.localId) { it.copy(notionId = notionId, pendingSync = false) }
                SaveResult.SavedToNotion
            }
            .getOrElse { error ->
                updateCache(post.localId) { it.copy(pendingSync = true) }
                SaveResult.SavedPending // Treat network/Notion errors like missing-DB: still cached.
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
    /** Post was cached and mirrored to Notion successfully. */
    data object SavedToNotion : SaveResult
    /** Post was cached but the Notion mirror is pending (no DB id, or write failed). */
    data object SavedPending  : SaveResult
    /** Input was empty / whitespace — nothing was saved. */
    data object EmptyInput    : SaveResult
}
