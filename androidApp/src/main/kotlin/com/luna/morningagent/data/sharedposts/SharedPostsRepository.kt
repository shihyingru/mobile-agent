package com.luna.morningagent.data.sharedposts

import android.util.Log
import com.luna.morningagent.data.secure.TokenStore
import kotlin.time.Clock
import kotlinx.serialization.json.Json

/**
 * Repository for shared posts. The local JSON cache in TokenStore is the
 * source of truth — Notion is a mirror that may lag behind.
 *
 * Pipeline (orchestrated by ShareReceiverActivity):
 *  1. `save()` — parses the shared text and appends to the cache. Always
 *     marks `pendingSync = true`. Does NOT touch Notion — that's deferred
 *     so the body fetcher can enrich URL-only shares before the first
 *     Notion write sees them.
 *  2. (caller) body fetch + `updateContent()` if the share was a bare URL.
 *  3. `syncToNotion()` — calls createPage with the now-enriched post.
 *     Failures leave pendingSync = true; the Saved-screen setup flow's
 *     flushPendingPosts() retry-sweeps them.
 *  4. (caller) categorize → `applyCategorization()`.
 *
 * Categorization is a no-op here — that's the SavedPostCategorizer job,
 * which mutates the same cache entry once the agent returns.
 */
class SharedPostsRepository(
    private val tokenStore: TokenStore,
    private val notionClient: SharedPostsNotionClient = SharedPostsNotionClient(tokenStore),
) {

    /**
     * Parse a shared text payload and append it to the local cache. Notion is
     * NOT touched here — the caller is expected to enrich the body (for
     * URL-only shares) before calling [syncToNotion]. That way the first
     * Notion write already has the real post body instead of a bare URL,
     * which prevents `refreshFromNotion()` from later overwriting an
     * enriched local cache with Notion's stale URL.
     *
     * Idempotent on [localId]: the share worker generates one stable id per
     * share and may re-run (retry on a network failure), so a second call with
     * the same id returns the existing post instead of appending a duplicate.
     */
    suspend fun save(rawText: String, subject: String?, localId: String): SaveResult {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return SaveResult.EmptyInput

        readCache().firstOrNull { it.localId == localId }?.let { return SaveResult.SavedPending(it) }

        val rawUrl = extractFirstUrl(trimmed)
        val url    = rawUrl?.let { cleanTrackingParams(it) }
        val source = detectSource(url)
        // Strip the trailing URL when it's clearly appended to the text so the
        // post content reads cleanly. Threads share format: "<body>\n\n<url>".
        // Use the raw URL — that's what's literally present in the source text.
        // When the share was URL-only, prefer the cleaned URL as the content
        // placeholder so the body-fetcher gate (`content == url`) recognises
        // the "no real content yet" state regardless of which tracking params
        // came in on the original text.
        val content = stripTrailingUrl(trimmed, rawUrl).ifBlank { url ?: trimmed }
        val author  = subject?.trim()?.takeIf { it.isNotBlank() }
            ?: url?.let { authorFromUrl(it) }

        val post = SharedPost(
            localId               = localId,
            content               = content,
            source                = source,
            author                = author,
            url                   = url,
            savedAt               = Clock.System.now(),
            pendingSync           = true,
            pendingCategorization = true,
            pendingEnrich         = true,
        )

        appendToCache(post)
        return SaveResult.SavedPending(post)
    }

    /**
     * Push a cached post to Notion. Called after the body fetcher has had a
     * chance to enrich content, so Notion's first createPage already carries
     * the real post body. No-op when the DB isn't configured (post stays
     * pendingSync = true; the setup-flow flush will pick it up later) or when
     * the post has already been synced (notionId set).
     *
     * On success: stamps notionId, clears pendingSync.
     * On failure: pendingSync stays true; the Home banner / setup flow's
     * flushPendingPosts() will retry.
     */
    suspend fun syncToNotion(localId: String) {
        val dbId = tokenStore.getSharedPostsDbId() ?: return
        val post = readCache().firstOrNull { it.localId == localId } ?: return
        if (post.notionId != null) return

        runCatching { notionClient.createPage(dbId, post) }
            .onSuccess { notionId ->
                Log.i(TAG, "syncToNotion ok localId=$localId nid=$notionId img=${post.imageUrl != null}")
                updateCache(localId) { it.copy(notionId = notionId, pendingSync = false) }
            }
            .onFailure { Log.w(TAG, "syncToNotion FAILED localId=$localId: ${it.message}") }
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

        val currentCategories = tokenStore.getSharedPostsCategories()
        val existingNames     = currentCategories.map { it.name }.toSet()
        val newCategoryNames  = cleaned.filter { it !in existingNames }
        if (newCategoryNames.isNotEmpty()) {
            tokenStore.saveSharedPostsCategories(
                currentCategories + newCategoryNames.map { CategoryDefinition(name = it) },
            )
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

    /**
     * Pull the latest state of every (non-archived) page from the Notion DB and
     * merge it into the local cache. Notion is treated as the read-side source
     * of truth — its values win on `content`, `categories`, `summary`, `status`.
     * App-managed + local-only signals (`localId`, `imageUrl`, `pendingSync`,
     * `pendingCategorization`) are preserved from the local entry.
     *
     * Rules:
     *   · Every local entry is preserved. A remote match overlays Notion's
     *     read-side fields; local-only signals (notably the fetched `imageUrl`,
     *     which Notion doesn't store) are kept.
     *   · Local entries absent from the remote list are NOT dropped — the list
     *     is fetched outside the lock, so a just-synced post can be missing from
     *     a stale snapshot (TOCTOU). Dropping it there wiped the local-only
     *     imageUrl and made freshly-shared posts return image-less. Deletion is
     *     an explicit in-app action (which archives in Notion) instead.
     *   · Remote-only pages (notionId we've never seen) are appended.
     *   · User-added categories are preserved; names seen on posts are merged in.
     *     The "Misc" seed is always kept.
     *
     * No-op when sharedPostsDbId isn't set yet.
     */
    suspend fun refreshFromNotion() {
        val dbId = tokenStore.getSharedPostsDbId() ?: return
        val remote = runCatching { notionClient.listDatabase(dbId) }.getOrNull() ?: return
        synchronized(CACHE_LOCK) {
            val local       = readCache()
            val remoteByNid = remote.mapNotNull { r -> r.notionId?.let { it to r } }.toMap()

            val merged = mutableListOf<SharedPost>()

            // 1. EVERY local post survives. When Notion has a matching row,
            //    overlay its read-side fields (Notion wins on content / categories
            //    / summary / status), but keep local-only signals — crucially the
            //    locally-fetched imageUrl, which Notion doesn't store. Posts with
            //    no notionId (outbox) have no remote match and pass through as-is.
            //
            //    We intentionally do NOT drop a synced post just because it's
            //    absent from this remote list: the list is fetched outside the
            //    lock, so a just-synced post can be missing from a stale snapshot
            //    (TOCTOU) — dropping it there destroyed the local-only imageUrl and
            //    made freshly-shared posts come back image-less. Deletion is an
            //    explicit in-app action (which archives in Notion), not a side
            //    effect of a refresh seeing a transiently-absent row.
            local.forEach { localRow ->
                val remoteRow = localRow.notionId?.let { remoteByNid[it] }
                if (remoteRow != null) {
                    merged.add(localRow.copy(
                        content               = remoteRow.content,
                        author                = remoteRow.author ?: localRow.author,
                        url                   = remoteRow.url ?: localRow.url,
                        categories            = remoteRow.categories,
                        summary               = remoteRow.summary,
                        status                = remoteRow.status,
                        savedAt               = remoteRow.savedAt,
                        pendingCategorization = false,
                    ))
                } else {
                    merged.add(localRow)
                }
            }

            // 2. Remote-only rows we've never seen locally (manual Notion add or
            //    a share from another device).
            val localNids = local.mapNotNull { it.notionId }.toSet()
            remote.forEach { remoteRow ->
                val nid = remoteRow.notionId ?: return@forEach
                if (nid !in localNids) merged.add(remoteRow)
            }

            // 3. Sort newest-first so the UI doesn't have to.
            val absentFromRemote = local.count { it.notionId != null && it.notionId !in remoteByNid }
            Log.i(
                TAG,
                "refresh local=${local.size} outbox=${local.count { it.notionId == null }} " +
                    "remote=${remote.size} merged=${merged.size} keptAbsent=$absentFromRemote",
            )
            writeCache(merged.sortedByDescending { it.savedAt })

            // 5. Merge categories seen on posts INTO the existing taxonomy —
            //    don't rebuild from posts alone. User-added categories that no
            //    post uses yet must survive a sync; removal is an explicit
            //    Settings action (removeCategory), not a side effect of a fetch.
            //    Existing entries keep their order + keyword hints; brand-new
            //    names found on remote posts are appended. The "Misc" seed is
            //    always present.
            val existing      = tokenStore.getSharedPostsCategories()
            val existingNames = existing.map { it.name }.toSet()
            val seedIfMissing = if (SEED_CATEGORY in existingNames) emptyList()
                                else listOf(CategoryDefinition(name = SEED_CATEGORY))
            val newFromPosts  = merged.flatMap { it.categories }
                .filter { it.isNotBlank() && it !in existingNames }
                .distinct()
                .map { CategoryDefinition(name = it) }
            tokenStore.saveSharedPostsCategories(seedIfMissing + existing + newFromPosts)
        }
    }

    // --- Category CRUD (Settings) -------------------------------------------

    /**
     * Append a new user-defined category. No-op when [name] is blank or already
     * exists (case-sensitive name match — see `Settings` for client-side input
     * normalisation). [keywords] are stored verbatim minus blanks and dupes.
     */
    fun addCategory(name: String, keywords: List<String> = emptyList()) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        val current = tokenStore.getSharedPostsCategories()
        if (current.any { it.name == trimmedName }) return
        tokenStore.saveSharedPostsCategories(
            current + CategoryDefinition(name = trimmedName, keywords = cleanKeywords(keywords)),
        )
    }

    /**
     * Update an existing category. If [newName] differs from [oldName] and the
     * category is in use on cached posts (and mirrored to Notion), the rename
     * is propagated to those entries' `categories` lists and patched to Notion.
     * No-op when [oldName] doesn't exist or [newName] is blank.
     */
    suspend fun updateCategory(
        oldName: String,
        newName: String,
        keywords: List<String>,
    ) {
        val cleanedNewName = newName.trim()
        if (cleanedNewName.isBlank()) return
        val current = tokenStore.getSharedPostsCategories()
        val idx     = current.indexOfFirst { it.name == oldName }
        if (idx < 0) return
        // Collision-on-rename: drop the source and merge keywords into the
        // existing target so we don't end up with two entries sharing a name.
        val targetIdx = current.indexOfFirst { it.name == cleanedNewName }
        val updated   = current.toMutableList()
        val mergedKeywords = cleanKeywords(keywords)
        if (cleanedNewName != oldName && targetIdx >= 0) {
            val combined = (current[targetIdx].keywords + mergedKeywords).distinct()
            updated[targetIdx] = current[targetIdx].copy(keywords = combined)
            updated.removeAt(idx)
        } else {
            updated[idx] = CategoryDefinition(name = cleanedNewName, keywords = mergedKeywords)
        }
        tokenStore.saveSharedPostsCategories(updated)

        if (cleanedNewName != oldName) propagateRenameToPosts(oldName, cleanedNewName)
    }

    /**
     * Remove a user-defined category and strip it from every cached post that
     * used it (with Notion patch when synced). "Misc" can't be removed —
     * the categorizer falls back to it when the model returns nothing.
     */
    suspend fun removeCategory(name: String) {
        if (name == SEED_CATEGORY) return
        val current = tokenStore.getSharedPostsCategories()
        val remaining = current.filterNot { it.name == name }
        if (remaining.size == current.size) return
        tokenStore.saveSharedPostsCategories(remaining)
        propagateRemovalToPosts(name)
    }

    private suspend fun propagateRenameToPosts(oldName: String, newName: String) {
        val affected = mutableListOf<SharedPost>()
        readCache().forEach { post ->
            if (oldName !in post.categories) return@forEach
            val swapped = post.categories.map { if (it == oldName) newName else it }.distinct()
            updateCache(post.localId) { it.copy(categories = swapped) }
            if (post.notionId != null) affected += post.copy(categories = swapped)
        }
        affected.forEach { p ->
            runCatching { notionClient.updatePageCategoriesAndSummary(p.notionId!!, p.categories, p.summary) }
        }
    }

    private suspend fun propagateRemovalToPosts(name: String) {
        val affected = mutableListOf<SharedPost>()
        readCache().forEach { post ->
            if (name !in post.categories) return@forEach
            val stripped = post.categories.filterNot { it == name }
            updateCache(post.localId) { it.copy(categories = stripped) }
            if (post.notionId != null) affected += post.copy(categories = stripped)
        }
        affected.forEach { p ->
            runCatching { notionClient.updatePageCategoriesAndSummary(p.notionId!!, p.categories, p.summary) }
        }
    }

    private fun cleanKeywords(keywords: List<String>): List<String> =
        keywords.map { it.trim() }.filter { it.isNotBlank() }.distinct()

    /** Replace the cached post entirely — used by the categorizer (commit 2). */
    fun update(localId: String, transform: (SharedPost) -> SharedPost) {
        updateCache(localId, transform)
    }

    /**
     * Replace just the post body in both the local cache and the Notion mirror
     * (if synced). Used by the body fetcher after enriching a URL-only share
     * with the actual post text. Notion patch failure leaves the local cache
     * winning — the next categorization pass can re-push if it matters.
     */
    suspend fun updateContent(localId: String, content: String) {
        var notionIdToPatch: String? = null
        updateCache(localId) { existing ->
            notionIdToPatch = existing.notionId
            existing.copy(content = content)
        }
        notionIdToPatch?.let { pageId ->
            runCatching { notionClient.updatePageContent(pageId, content) }
        }
    }

    /**
     * Stamp the og:image URL onto a cached post. Local-only for now — the
     * Notion mirror doesn't carry images yet. Used by the body fetcher right
     * after `updateContent` so the Saved card has a thumbnail to render.
     */
    fun updateImageUrl(localId: String, imageUrl: String) {
        updateCache(localId) { it.copy(imageUrl = imageUrl) }
    }

    /**
     * Stamp the resolved places onto a cached post. Local-only, like
     * [updateImageUrl] — Notion doesn't store them, and `refreshFromNotion`
     * preserves them. Resolved once at share time; a non-empty list is what makes
     * the Saved card show its map pin (one → opens directly, many → sheet).
     */
    fun updateLocations(localId: String, places: List<ResolvedPlace>) {
        if (places.isEmpty()) return
        updateCache(localId) { it.copy(locations = places) }
    }

    /**
     * Overwrite a post's resolved places — including with an empty list, which
     * [updateLocations] refuses (it guards against the resolver clobbering good
     * data with a no-match). Used by the user's manual delete-place action, where
     * removing the last place legitimately clears the pin.
     */
    fun setLocations(localId: String, places: List<ResolvedPlace>) {
        updateCache(localId) { it.copy(locations = places) }
    }

    /** Clear the freshly-shared flag once the Saved screen's foreground
     *  resolution pass has finished enriching this post. */
    fun clearPendingEnrich(localId: String) {
        updateCache(localId) { it.copy(pendingEnrich = false, enrichAttempts = 0) }
    }

    /** Record a foreground enrich pass that couldn't recover the body. Keeps the
     *  post pending (so it retries on the next open — covers offline/transient
     *  failures) until [maxAttempts], then gives up so it stops re-running the
     *  paid Gemini + Places work on every open. */
    fun recordFailedEnrich(localId: String, maxAttempts: Int) {
        updateCache(localId) {
            val attempts = it.enrichAttempts + 1
            it.copy(enrichAttempts = attempts, pendingEnrich = attempts < maxAttempts)
        }
    }

    fun remove(localId: String) {
        synchronized(CACHE_LOCK) {
            writeCache(readCache().filterNot { it.localId == localId })
        }
    }

    // --- Cache I/O ----------------------------------------------------------

    // The post cache lives in TokenStore's process-shared map, but each caller
    // (Activity, ViewModel) has its own repository, so @Synchronized (which locks
    // the instance) wouldn't serialize them. Lock on a process-wide monitor so
    // every read-modify-write of the cache across all instances is atomic and
    // can't lose an update.
    private fun appendToCache(post: SharedPost) {
        synchronized(CACHE_LOCK) {
            val current = readCache().toMutableList()
            // Newest first so the UI doesn't have to sort.
            current.add(0, post)
            writeCache(current)
        }
    }

    private fun updateCache(localId: String, transform: (SharedPost) -> SharedPost) {
        synchronized(CACHE_LOCK) {
            val current = readCache().toMutableList()
            val idx = current.indexOfFirst { it.localId == localId }
            if (idx < 0) return
            current[idx] = transform(current[idx])
            writeCache(current)
        }
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
     * Drop tracking query params from a shared URL while preserving everything
     * else (path, fragment, structurally meaningful params). Uses a per-host
     * allowlist plus a universal set of cross-site trackers. Returns the input
     * unchanged when there's nothing to strip.
     */
    private fun cleanTrackingParams(url: String): String {
        val qIdx = url.indexOf('?')
        if (qIdx < 0) return url
        val hashIdx = url.indexOf('#', qIdx)
        val base     = url.substring(0, qIdx)
        val query    = if (hashIdx > 0) url.substring(qIdx + 1, hashIdx) else url.substring(qIdx + 1)
        val fragment = if (hashIdx > 0) url.substring(hashIdx) else ""
        if (query.isEmpty()) return base + fragment

        val host = runCatching { android.net.Uri.parse(url).host?.lowercase() }.getOrNull()
            ?: return url
        val drop = trackingParamsFor(host)
        if (drop.isEmpty()) return url

        val originalPairs = query.split('&')
        val keptPairs = originalPairs.filter { pair ->
            val name = pair.substringBefore('=')
            !drop.any { p ->
                if (p.endsWith("*")) name.startsWith(p.dropLast(1)) else name == p
            }
        }
        if (keptPairs.size == originalPairs.size) return url
        return if (keptPairs.isEmpty()) base + fragment
               else base + "?" + keptPairs.joinToString("&") + fragment
    }

    private fun trackingParamsFor(host: String): Set<String> {
        val perHost: Set<String> = when {
            host.endsWith("threads.com") || host.endsWith("threads.net") -> setOf("xmt", "slof")
            host.endsWith("twitter.com") || host.endsWith("x.com")       -> setOf("t", "s")
            host.endsWith("instagram.com")                               -> setOf("igsh", "igshid")
            else                                                         -> emptySet()
        }
        return UNIVERSAL_TRACKING_PARAMS + perHost
    }

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
        // Process-wide monitor: the post cache is shared across all repository
        // instances (via TokenStore's shared map), so its read-modify-write must
        // be serialized on a single lock, not per-instance.
        private val CACHE_LOCK = Any()

        // Seed category — always kept in the taxonomy so empty Notion DBs and
        // first-launch states still show a usable filter chip + categorizer
        // fallback.
        private const val TAG = "SharedPostsRepo"
        private const val SEED_CATEGORY = "Misc"

        // Matches https?://[^\s]+ — good enough for shared text payloads.
        private val URL_REGEX = Regex("""https?://[^\s]+""")

        // Cross-site analytics params dropped regardless of host. Names ending
        // in `*` match by prefix (e.g. utm_* covers utm_source, utm_medium, …).
        private val UNIVERSAL_TRACKING_PARAMS = setOf(
            "utm_*", "fbclid", "gclid", "mc_eid", "mc_cid", "_branch_match_id",
        )

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

    /** Post was appended to the cache. Notion sync happens after body enrichment. */
    data class SavedPending(override val post: SharedPost) : SaveResult
    /** Input was empty / whitespace — nothing was saved. */
    data object EmptyInput : SaveResult { override val post: SharedPost? = null }
}
