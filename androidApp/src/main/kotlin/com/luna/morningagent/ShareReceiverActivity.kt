package com.luna.morningagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.SharedPostsRepository
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Lightweight receiver for ACTION_SEND text/plain shares from any app.
 *
 * No UI — Theme.NoDisplay means the activity never shows a window, so Luna stays
 * inside the source app. A NoDisplay activity MUST finish() inside onCreate, so
 * this does the minimum here: append the post to the local cache (no network)
 * and finish immediately.
 *
 * All network enrichment — body + og:image scrape, AI body fallback, categorize,
 * location resolve, Notion sync — runs in
 * [com.luna.morningagent.ui.sharedposts.SavedPostsViewModel.resolvePending] when
 * the app is next opened. That's deliberate: Android blocks an app's *background*
 * network on metered connections under Battery Saver / restricted background
 * data (a background worker just fails DNS), but foreground is exempt. Posts are
 * saved with `pendingEnrich = true`; the Saved screen enriches them on open.
 */
class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rawText = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val subject = intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        Log.i(TAG, "intent EXTRA_TEXT=\"$rawText\" EXTRA_SUBJECT=\"$subject\"")

        if (rawText.isEmpty()) {
            Toast.makeText(this, R.string.share_saved_failed_toast, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val appContext = applicationContext
        val tokenStore = TokenStore(appContext)
        val repo       = SharedPostsRepository(tokenStore)

        val hasDb    = tokenStore.getSharedPostsDbId() != null
        val toastRes = if (hasDb) R.string.share_saved_toast else R.string.share_saved_pending_toast
        Toast.makeText(appContext, toastRes, Toast.LENGTH_SHORT).show()

        // Save only — local cache write, no network. Quick enough to complete
        // before the process is backgrounded; the shared in-memory cache makes it
        // visible to the Saved screen immediately.
        scope.launch { repo.save(rawText, subject, UUID.randomUUID().toString()) }

        finish()
    }

    companion object {
        private const val TAG = "ShareReceiver"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
