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
            val toastRes = when (result) {
                is SaveResult.SavedToNotion -> R.string.share_saved_toast
                is SaveResult.SavedPending  -> R.string.share_saved_pending_toast
                SaveResult.EmptyInput, null -> R.string.share_saved_failed_toast
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, toastRes, Toast.LENGTH_SHORT).show()
            }

            // Threads/X/Instagram share intents carry only the post URL, never
            // the body. Enrich content from the URL before categorizing so the
            // agent has real text to summarise.
            val enriched = result?.post?.let { saved ->
                val needsBody = saved.url != null &&
                    (saved.content == saved.url || saved.content.length < 80)
                if (!needsBody) return@let saved
                val fetched = runCatching { bodyFetcher.fetchBody(saved.url!!) }.getOrNull()
                if (fetched.isNullOrBlank()) return@let saved
                repo.updateContent(saved.localId, fetched)
                saved.copy(content = fetched)
            }

            // Categorize the post in the background once it's cached. Fire even
            // when only locally cached — the agent's output sticks in the cache,
            // and the Notion patch happens later when the post syncs.
            enriched?.let { saved ->
                val categories = categorizer.categorize(
                    post              = saved,
                    existingTaxonomy  = tokenStore.getSharedPostsTaxonomy(),
                )
                if (categories != null) {
                    repo.applyCategorization(
                        localId    = saved.localId,
                        categories = categories.categories,
                        summary    = categories.summary.ifBlank { null },
                    )
                }
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
