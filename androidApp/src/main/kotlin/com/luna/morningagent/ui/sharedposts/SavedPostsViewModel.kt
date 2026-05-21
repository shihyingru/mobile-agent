package com.luna.morningagent.ui.sharedposts

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luna.morningagent.data.notion.NotionConfigMissingException
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.SharedPost
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
    }

    fun refresh() {
        posts = repo.listAll()
        dbId  = tokenStore.getSharedPostsDbId()
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
