package com.luna.morningagent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.worker.SharedPostEnrichWorker

/**
 * Lightweight receiver for ACTION_SEND text/plain shares from any app.
 *
 * No UI — Theme.NoDisplay in the manifest means the activity never shows a
 * window, so Luna stays inside the source app (Threads, Twitter, etc.) while
 * the post is saved + enriched in the background. A single Toast confirms it.
 *
 * The save → enrich → sync → categorize → resolve-location pipeline runs in
 * [SharedPostEnrichWorker], NOT here: this activity `finish()`es immediately, so
 * any work left running in-process gets frozen/killed once the process is
 * cached. WorkManager keeps its process alive for the job and survives process
 * death, so the pipeline actually completes.
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

        // Toast copy reflects whether the Notion mirror is configured; the actual
        // Notion write is deferred into the worker after body enrichment, so its
        // outcome isn't known here.
        val hasDb = TokenStore(applicationContext).getSharedPostsDbId() != null
        val toastRes = if (hasDb) R.string.share_saved_toast else R.string.share_saved_pending_toast
        Toast.makeText(applicationContext, toastRes, Toast.LENGTH_SHORT).show()

        SharedPostEnrichWorker.enqueue(applicationContext, rawText, subject)
        finish()
    }
}
