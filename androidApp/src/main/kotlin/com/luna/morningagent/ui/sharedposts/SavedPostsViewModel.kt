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
        viewModelScope.launch {
            runCatching { repo.refreshFromNotion() }
            refresh()
        }
    }

    fun onSearchChange(value: String) { search = value }
    fun onCategorySelect(name: String?) { activeCategory = name }
    fun onSetupUrlChange(value: String) {
        setupUrlDraft = value
        if (setupState is SetupState.Error) setupState = SetupState.Idle
    }

    /** Categories sourced from the live taxonomy, augmented with anything that
     *  shows up on a cached post (defensive — the agent might have added a
     *  category that didn't make it back to the taxonomy in some failure mode). */
    val allCategories: List<String>
        get() {
            val fromTaxonomy = tokenStore.getSharedPostsTaxonomy()
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
     * One-time setup. Parses a Notion page URL → 32-hex page id, asks Notion
     * to create the SharedPosts DB under it, stores the new dbId, then flushes
     * any cached pendingSync posts to the new DB.
     */
    fun runSetup() {
        val rawUrl = setupUrlDraft.trim()
        if (rawUrl.isEmpty()) {
            setupState = SetupState.Error("Paste a Notion page URL to host the DB.")
            return
        }
        val parentPageId = extractNotionDatabaseId(rawUrl)
        if (parentPageId.length != 32 || !parentPageId.all { it.isDigit() || it in 'a'..'f' }) {
            setupState = SetupState.Error("That doesn't look like a Notion page URL.")
            return
        }
        setupState = SetupState.InProgress
        viewModelScope.launch {
            val newDbId = runCatching { notionClient.createDatabase(parentPageId) }
                .getOrElse { e ->
                    setupState = SetupState.Error(
                        if (e is NotionConfigMissingException) "Notion token not set in Settings."
                        else "Couldn't create the DB — ${e.message ?: e::class.simpleName}",
                    )
                    return@launch
                }
            tokenStore.saveSharedPostsDbId(newDbId)
            dbId = newDbId
            flushPendingPosts(newDbId)
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
