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

        val rawUrl = extractFirstUrl(trimmed)
        val url    = rawUrl?.let { cleanTrackingParams(it) }
        val source = detectSource(url)
        // Strip the trailing URL when it's clearly appended to the text so the
        // post content reads cleanly. Threads share format: "<body>\n\n<url>".
        // Use the raw URL — that's what's literally present in the source text.
        val content = stripTrailingUrl(trimmed, rawUrl).ifBlank { trimmed }
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

    /**
     * Pull the latest state of every (non-archived) page from the Notion DB and
     * merge it into the local cache. Notion is treated as the read-side source
     * of truth — its values win on `content`, `categories`, `summary`, `status`.
     * App-managed signals (`localId`, `pendingSync`, `pendingCategorization`)
     * are preserved from the local entry.
     *
     * Rules:
     *   · Local pendingSync entries (no notionId) are preserved — they're the
     *     outbox waiting to flush.
     *   · Local entries whose notionId is no longer in the remote list get
     *     dropped — Notion archived = soft delete on the app side.
     *   · Remote-only pages (notionId we've never seen) are appended.
     *   · Taxonomy is rebuilt from the merged posts' categories so deletions
     *     in Notion also remove the corresponding filter chip locally; the
     *     "Misc" seed is always kept.
     *
     * No-op when sharedPostsDbId isn't set yet.
     */
    suspend fun refreshFromNotion() {
        val dbId = tokenStore.getSharedPostsDbId() ?: return
        val remote = runCatching { notionClient.listDatabase(dbId) }.getOrNull() ?: return
        synchronized(this) {
            val local      = readCache()
            val localByNid = local.associateBy { it.notionId }.filterKeys { it != null }
            val remoteIds  = remote.mapNotNull { it.notionId }.toSet()

            val merged = mutableListOf<SharedPost>()

            // 1. Outbox first (no notionId, pendingSync=true) — these survive every fetch.
            local.filter { it.notionId == null }.forEach { merged.add(it) }

            // 2. For each remote row, merge with local-if-present. Notion
            //    always wins on the content/categories/summary fields — the
            //    AI-mid-flight race is narrow enough that protecting against
            //    it locks edits out permanently when the AI never returns
            //    (no key, empty result). Trust Notion as backend.
            remote.forEach { remoteRow ->
                val nid       = remoteRow.notionId ?: return@forEach
                val cachedRow = localByNid[nid]
                if (cachedRow != null) {
                    merged.add(cachedRow.copy(
                        content               = remoteRow.content,
                        author                = remoteRow.author ?: cachedRow.author,
                        url                   = remoteRow.url ?: cachedRow.url,
                        categories            = remoteRow.categories,
                        summary               = remoteRow.summary,
                        status                = remoteRow.status,
                        savedAt               = remoteRow.savedAt,
                        pendingCategorization = false,
                    ))
                } else {
                    // Brand-new in Notion (manual add, or other-device share).
                    merged.add(remoteRow)
                }
            }

            // 3. Local entries whose notionId is gone from Notion (archived)
            //    get dropped implicitly — we only re-add from `remote` and the
            //    pendingSync outbox. `remoteIds` retained for future audit logging.
            @Suppress("UNUSED_VARIABLE") val ignored = remoteIds

            // 4. Sort newest-first so the UI doesn't have to.
            writeCache(merged.sortedByDescending { it.savedAt })

            // 5. Rebuild taxonomy from the merged posts so deletions in Notion
            //    drop the corresponding chip locally. Always keep the "Misc"
            //    seed (the categorizer's empty-list fallback expects it).
            val rebuilt = (listOf(SEED_CATEGORY) + merged.flatMap { it.categories })
                .filter { it.isNotBlank() }
                .distinct()
            tokenStore.saveSharedPostsTaxonomy(rebuilt)
        }
    }

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
        // Seed category — always kept in the taxonomy so empty Notion DBs and
        // first-launch states still show a usable filter chip + categorizer
        // fallback.
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

    /** Post was cached and mirrored to Notion successfully. */
    data class SavedToNotion(override val post: SharedPost) : SaveResult
    /** Post was cached but the Notion mirror is pending (no DB id, or write failed). */
    data class SavedPending(override val post: SharedPost)  : SaveResult
    /** Input was empty / whitespace — nothing was saved. */
    data object EmptyInput : SaveResult { override val post: SharedPost? = null }
}
