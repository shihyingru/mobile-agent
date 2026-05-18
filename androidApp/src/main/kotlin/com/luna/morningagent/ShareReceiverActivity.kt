package com.luna.morningagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.SaveResult
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

        if (rawText.isEmpty()) {
            Toast.makeText(this, R.string.share_saved_failed_toast, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val appContext = applicationContext
        val repo = SharedPostsRepository(TokenStore(appContext))

        // App-scoped coroutine so the save survives the immediate finish().
        appScope.launch {
            val result = runCatching { repo.save(rawText, subject) }
                .getOrElse { SaveResult.SavedPending }   // Cached-only is still a save.
            val toastRes = when (result) {
                SaveResult.SavedToNotion -> R.string.share_saved_toast
                SaveResult.SavedPending  -> R.string.share_saved_pending_toast
                SaveResult.EmptyInput    -> R.string.share_saved_failed_toast
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(appContext, toastRes, Toast.LENGTH_SHORT).show()
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
