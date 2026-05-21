package com.luna.morningagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.SaveResult
import com.luna.morningagent.data.sharedposts.SharedPostBodyFetcher
import com.luna.morningagent.data.sharedposts.SharedPostCategorizer
import com.luna.morningagent.data.sharedposts.SharedPostsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Lightweight receiver for ACTION_SEND text/plain shares from any app.
 *
 * No UI — Theme.NoDisplay in the manifest means the activity never shows a
 * window, so Luna stays inside the source app (Threads, Twitter, etc.) while
 * the post is cached and (when configured) mirrored to Notion in the
 * background. A single Toast confirms the save.
 *
 * Coroutine scope is tied to the application context, not this activity —
 * `finish()` fires immediately after dispatching the save, so an activity-
 * scoped scope would be cancelled before the Notion write completes.
 */
class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        Log.i("ShareReceiver", "intent EXTRA_TEXT=\"$rawText\" EXTRA_SUBJECT=\"$subject\"")

        if (rawText.isEmpty()) {
            Toast.makeText(this, R.string.share_saved_failed_toast, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val appContext = applicationContext
        val tokenStore = TokenStore(appContext)
        val repo = SharedPostsRepository(tokenStore)
        val categorizer = SharedPostCategorizer(tokenStore)
        val bodyFetcher = SharedPostBodyFetcher(tokenStore)

        // App-scoped coroutine so the save + categorization survive the
        // immediate finish() — the activity is gone but the work runs to
        // completion in the same process.
        appScope.launch {
            val result = runCatching { repo.save(rawText, subject) }.getOrNull()

            // Toast picks copy from the DB-configured state (not the save
            // result — Notion sync is now deferred until after body enrichment,
            // so its outcome isn't known yet).
            val hasDb = tokenStore.getSharedPostsDbId() != null
            val toastRes = when (result) {
                is SaveResult.SavedPending  ->
                    if (hasDb) R.string.share_saved_toast
                    else       R.string.share_saved_pending_toast
                SaveResult.EmptyInput, null -> R.string.share_saved_failed_toast
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, toastRes, Toast.LENGTH_SHORT).show()
            }

            val saved = result?.post ?: return@launch

            // 1. Enrich content. Threads/X/Instagram intents carry only the
            //    URL — scrape og:description (with a Gemini url_context
            //    fallback) so the cache (and later Notion) sees the real
            //    post body, not a bare URL.
            val enriched = run {
                val needsBody = saved.url != null &&
                    (saved.content == saved.url || saved.content.length < 80)
                if (!needsBody) return@run saved
                val fetched = runCatching { bodyFetcher.fetchBody(saved.url!!) }.getOrNull()
                if (fetched.isNullOrBlank()) return@run saved
                repo.updateContent(saved.localId, fetched)
                saved.copy(content = fetched)
            }

            // 2. NOW push to Notion. createPage carries the enriched content
            //    so Notion's first write isn't a bare URL — important because
            //    refreshFromNotion() treats Notion as source-of-truth on
            //    content and would otherwise overwrite the local body back to
            //    the URL on the next Saved-screen entry.
            repo.syncToNotion(enriched.localId)

            // 3. Categorize. applyCategorization writes categories + summary
            //    into the cache and patches the (now-existing) Notion page.
            val categories = categorizer.categorize(
                post               = enriched,
                existingCategories = tokenStore.getSharedPostsCategories(),
            )
            if (categories != null) {
                repo.applyCategorization(
                    localId    = enriched.localId,
                    categories = categories.categories,
                    summary    = categories.summary.ifBlank { null },
                )
            }
        }

        finish()
    }

    companion object {
        // Process-wide SupervisorJob so one failed save doesn't kill the pipeline
        // for subsequent shares within the same app process.
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
