package com.luna.morningagent.ui.sharedposts

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.ResolvedPlace
import com.luna.morningagent.data.sharedposts.SharedPost
import com.luna.morningagent.data.sharedposts.SharedPostBodyFetcher
import com.luna.morningagent.data.sharedposts.SharedPostCategorizer
import com.luna.morningagent.data.sharedposts.SharedPostsNotionClient
import com.luna.morningagent.data.sharedposts.SharedPostsRepository
import com.luna.morningagent.ui.settings.extractNotionDatabaseId
import kotlinx.coroutines.launch

/**
 * State + actions for the Saved Posts screen.
 *
 * Source of truth is the JSON cache in TokenStore (read via repository). All
 * filter / search state is in-memory and recomputed per access — fine at the
 * size Luna will hit for years (linear scan, no precomputed index).
 *
 * Setup flow (when sharedPostsDbId is null): the user pastes a Notion *page*
 * URL — that's the parent under which we provision the SharedPosts DB. Once
 * created, we flush every cached pendingSync post via createPage and stamp
 * the resulting notionId back into each cache entry.
 */
class SavedPostsViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore   = TokenStore(application)
    private val notionClient = SharedPostsNotionClient(tokenStore)
    private val repo         = SharedPostsRepository(tokenStore, notionClient)
    private val bodyFetcher  = SharedPostBodyFetcher(tokenStore)
    private val categorizer  = SharedPostCategorizer(tokenStore)

    /** True while [resolvePending] is enriching freshly-shared posts. Cards whose
     *  post is still `pendingEnrich` show a "resolving" state while this is on.
     *  Also guards against overlapping runs (init + each ON_RESUME). */
    var resolving: Boolean by mutableStateOf(false)
        private set

    var posts: List<SharedPost> by mutableStateOf(emptyList())
        private set

    var search: String by mutableStateOf("")
        private set

    /** null = "All". Any other value matches a single category name. */
    var activeCategory: String? by mutableStateOf(null)
        private set

    var dbId: String? by mutableStateOf(tokenStore.getSharedPostsDbId())
        private set

    var setupUrlDraft: String by mutableStateOf("")
        private set

    var setupState: SetupState by mutableStateOf(SetupState.Idle)
        private set

    /** True while [refreshFromNotion] is in flight — pull-to-refresh binds to this. */
    var isRefreshing: Boolean by mutableStateOf(false)
        private set

    init {
        refresh()
        resolvePending()
    }

    fun refresh() {
        posts = repo.listAll()
        dbId  = tokenStore.getSharedPostsDbId()
    }

    /**
     * Foreground enrichment pass — called on init and on every screen ON_RESUME.
     *
     * For each post flagged [SharedPost.pendingEnrich] (set at share time), run
     * the network-heavy work the share receiver deferred: recover the body if the
     * share-time scrape couldn't, sync to Notion, categorize, and resolve places.
     * Runs here (foreground) on purpose — the OS blocks the app's *background*
     * network on metered connections under Battery Saver / restricted background
     * data, but foreground is exempt. Refreshes after each post so pins/categories
     * appear as they land; clears the flag only on a successful body so a
     * transient miss retries next open.
     */
    fun resolvePending() {
        if (resolving) return
        val pending = repo.listAll().filter { it.pendingEnrich }
        if (pending.isEmpty()) return
        resolving = true
        viewModelScope.launch {
            for (post in pending) {
                runCatching { enrichOne(post) }
                refresh()
            }
            resolving = false
        }
    }

    private suspend fun enrichOne(post: SharedPost) {
        var working = post
        // Recover body / image if the share-time scrape didn't (offline then, or
        // skipped). fetch() is best-effort and never throws fatally.
        val url = post.url
        if (url != null && (post.content == url || post.content.length < 80 || post.imageUrl == null)) {
            runCatching { bodyFetcher.fetch(url) }.getOrNull()?.let { meta ->
                if (!meta.body.isNullOrBlank() && (working.content == url || working.content.length < 80)) {
                    repo.updateContent(post.localId, meta.body)
                    working = working.copy(content = meta.body)
                }
                if (!meta.imageUrl.isNullOrBlank() && working.imageUrl == null) {
                    repo.updateImageUrl(post.localId, meta.imageUrl)
                    working = working.copy(imageUrl = meta.imageUrl)
                }
            }
        }

        repo.syncToNotion(working.localId)

        val categories = categorizer.categorize(working, tokenStore.getSharedPostsCategories())
        if (categories != null) {
            repo.applyCategorization(
                localId    = working.localId,
                categories = categories.categories,
                summary    = categories.summary.ifBlank { null },
            )
        }

        val places = runCatching {
            bodyFetcher.resolvePostLocations(working.content, working.imageUrl)
        }.getOrNull().orEmpty()
        repo.updateLocations(working.localId, places)

        // Done unless the body still couldn't be recovered (likely a transient
        // failure) — leave it flagged so the next open retries.
        val stillBareUrl = url != null && working.content == url
        if (!stillBareUrl) repo.clearPendingEnrich(working.localId)
    }

    /**
     * Fetch the Notion DB and merge into the local cache. Cached posts render
     * immediately from `refresh()`; this call updates them in the background
     * and re-reads. Silent on network failure — cached state stands.
     */
    fun refreshFromNotion() {
        if (isRefreshing) return
        isRefreshing = true
        viewModelScope.launch {
            runCatching { repo.refreshFromNotion() }
            refresh()
            isRefreshing = false
        }
    }

    fun onSearchChange(value: String) { search = value }
    fun onCategorySelect(name: String?) { activeCategory = name }
    fun onSetupUrlChange(value: String) {
        setupUrlDraft = value
        if (setupState is SetupState.Error) setupState = SetupState.Idle
    }

    /** Category names sourced from the live taxonomy, augmented with anything
     *  that shows up on a cached post (defensive — the agent might have added a
     *  category that didn't make it back to the taxonomy in some failure mode). */
    val allCategories: List<String>
        get() {
            val fromTaxonomy = tokenStore.getSharedPostsCategories().map { it.name }
            val fromPosts    = posts.flatMap { it.categories }
            return (fromTaxonomy + fromPosts).distinct()
        }

    val filteredPosts: List<SharedPost>
        get() {
            val q = search.trim().lowercase()
            return posts.filter { post ->
                val matchesCategory = activeCategory == null || activeCategory in post.categories
                if (!matchesCategory) return@filter false
                if (q.isEmpty()) return@filter true
                post.content.contains(q, ignoreCase = true) ||
                    (post.summary?.contains(q, ignoreCase = true) == true) ||
                    (post.author?.contains(q, ignoreCase = true) == true) ||
                    post.categories.any { it.contains(q, ignoreCase = true) }
            }
        }

    val pendingSyncCount: Int
        get() = posts.count { it.pendingSync }

    // Locations are resolved once at share time (the share workers) and stored on
    // the post; the Saved card reads post.locations directly and opens the map
    // (or a places sheet) on tap — no on-demand resolve here.

    /** Remove one resolved place from a post — manual cleanup of a wrong or
     *  irrelevant pin from the places sheet. Clearing the last one hides the pin. */
    fun deletePlace(post: SharedPost, place: ResolvedPlace) {
        repo.setLocations(post.localId, post.locations.filterNot { it == place })
        refresh()
    }

    // --- Delete -------------------------------------------------------------

    fun delete(post: SharedPost) {
        viewModelScope.launch {
            // Archive the Notion mirror first — failures here shouldn't strand
            // the post in the cache, so fall through to the local remove either
            // way. Worst case Luna re-deletes the orphan in Notion manually.
            post.notionId?.let { pageId ->
                runCatching { notionClient.archivePage(pageId) }
            }
            repo.remove(post.localId)
            refresh()
        }
    }

    // --- Setup --------------------------------------------------------------

    /**
     * One-time setup. Accepts either:
     *  - A Notion **page** URL — provisions a fresh Shared Posts DB under it.
     *  - A Notion **database** URL — reconnects to an existing DB (e.g. the
     *    Settings was wiped but the user still has the original DB they want
     *    to keep using).
     *
     * URLs that contain `?v=<viewId>` are Notion database-view URLs; anything
     * else is treated as a page URL. The 32-hex id is extracted the same way
     * either path.
     */
    fun runSetup() {
        val rawUrl = setupUrlDraft.trim()
        if (rawUrl.isEmpty()) {
            setupState = SetupState.Error("Paste a Notion page or database URL.")
            return
        }
        val id = extractNotionDatabaseId(rawUrl)
        if (id.length != 32 || !id.all { c -> c.isDigit() || c in 'a'..'f' }) {
            setupState = SetupState.Error("That doesn't look like a Notion URL.")
            return
        }
        val isDatabaseUrl = "?v=" in rawUrl
        setupState = SetupState.InProgress
        viewModelScope.launch {
            val finalDbId = if (isDatabaseUrl) {
                // Reuse an existing Shared Posts DB. Skip createDatabase —
                // Notion rejects "createDatabase parented by a database".
                id
            } else {
                runCatching { notionClient.createDatabase(id) }
                    .getOrElse { e ->
                        setupState = SetupState.Error(
                            if (e is NotionConfigMissingException) "Notion token not set in Settings."
                            else "Couldn't create the DB — ${e.message ?: e::class.simpleName}",
                        )
                        return@launch
                    }
            }
            tokenStore.saveSharedPostsDbId(finalDbId)
            dbId = finalDbId
            flushPendingPosts(finalDbId)
            setupState = SetupState.Done
            setupUrlDraft = ""
            refresh()
        }
    }

    private suspend fun flushPendingPosts(dbId: String) {
        // Snapshot the pending ids first — repo.update mutates the underlying
        // list, and we don't want to re-process posts that succeed on the first
        // pass.
        val pending = repo.listAll().filter { it.pendingSync }
        for (post in pending) {
            runCatching { notionClient.createPage(dbId, post) }
                .onSuccess { notionId ->
                    repo.update(post.localId) {
                        it.copy(notionId = notionId, pendingSync = false)
                    }
                }
            // Failures stay pendingSync = true — Luna can retry by re-opening
            // setup. Don't surface per-post errors; the count diff will do it.
        }
    }
}

sealed interface SetupState {
    data object Idle       : SetupState
    data object InProgress : SetupState
    data object Done       : SetupState
    data class  Error(val message: String) : SetupState
}
