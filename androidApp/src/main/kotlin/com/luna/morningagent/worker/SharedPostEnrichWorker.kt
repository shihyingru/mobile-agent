package com.luna.morningagent.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.data.sharedposts.SaveResult
import com.luna.morningagent.data.sharedposts.SharedPostBodyFetcher
import com.luna.morningagent.data.sharedposts.SharedPostCategorizer
import com.luna.morningagent.data.sharedposts.SharedPostsRepository
import java.util.concurrent.TimeUnit

/**
 * Runs the whole "save a shared post" pipeline as one WorkManager job:
 * save → enrich body + image → sync to Notion → categorize → resolve places.
 *
 * Why one worker (not a save→enrich chain): TokenStore serves reads from a
 * per-instance in-memory snapshot taken at construction and persists writes
 * asynchronously, so a second worker's fresh TokenStore can't reliably see a
 * post the first just saved. Keeping save + enrich in one invocation shares one
 * TokenStore, so the cache is coherent.
 *
 * Offline handling (without a CONNECTED constraint, which would also delay the
 * save): the post is saved immediately so it appears right away, and if the
 * device is offline the job returns [Result.retry] (bounded, exponential
 * backoff) to re-run when connectivity is back. The save is idempotent on a
 * stable localId, so retries never duplicate the post; every enrich step
 * overwrites, so they're safe to repeat.
 */
class SharedPostEnrichWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val localId = inputData.getString(KEY_LOCAL_ID) ?: return Result.failure()
        val rawText = inputData.getString(KEY_TEXT)?.trim().orEmpty()
        if (rawText.isEmpty()) return Result.failure()
        val subject = inputData.getString(KEY_SUBJECT)

        val ctx         = applicationContext
        val tokenStore  = TokenStore(ctx)
        val repo        = SharedPostsRepository(tokenStore)
        val categorizer = SharedPostCategorizer(tokenStore)
        val bodyFetcher = SharedPostBodyFetcher(tokenStore)

        // Save (idempotent on localId) so the post appears immediately — even
        // offline, even on a retry.
        val saved = (repo.save(rawText, subject, localId) as? SaveResult.SavedPending)?.post
            ?: return Result.failure()

        // 1. Enrich body + image from the URL (scrape + AI fallback).
        val enriched = run {
            val url = saved.url ?: return@run saved
            val needsBody = saved.content == url || saved.content.length < 80
            val meta = runCatching { bodyFetcher.fetch(url) }.getOrNull() ?: return@run saved
            var working = saved
            if (needsBody && !meta.body.isNullOrBlank()) {
                repo.updateContent(saved.localId, meta.body)
                working = working.copy(content = meta.body)
            }
            if (!meta.imageUrl.isNullOrBlank()) {
                repo.updateImageUrl(saved.localId, meta.imageUrl)
                working = working.copy(imageUrl = meta.imageUrl)
            }
            working
        }

        // 2. Push to Notion (no-op when DB unset or already synced).
        repo.syncToNotion(enriched.localId)

        // 3. Categorize → write categories + summary into the cache (and Notion).
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

        // 4. Resolve every place once (text-first, image OCR fallback).
        val places = runCatching {
            bodyFetcher.resolvePostLocations(enriched.content, enriched.imageUrl)
        }.getOrNull().orEmpty()
        repo.updateLocations(enriched.localId, places)

        // If we're offline the enrichment couldn't have succeeded — retry (bounded)
        // so it completes once the network returns, instead of stranding the post
        // as a bare-URL card. Online failures (e.g. login-walled) just finish.
        return if (!isOnline(ctx) && runAttemptCount < MAX_ATTEMPTS) Result.retry()
               else Result.success()
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        /** Tag the Saved screen observes so it refreshes when enrichment finishes. */
        const val TAG = "share-enrich"

        private const val KEY_TEXT     = "shared_text"
        private const val KEY_SUBJECT  = "shared_subject"
        private const val KEY_LOCAL_ID = "local_id"
        private const val MAX_ATTEMPTS = 8

        /**
         * Enqueue the pipeline for a freshly-shared payload. [localId] is a stable
         * id generated by the caller so retries don't duplicate the post.
         * Exponential backoff retries the enrichment when offline.
         */
        fun enqueue(context: Context, localId: String, text: String, subject: String?) {
            val request = OneTimeWorkRequestBuilder<SharedPostEnrichWorker>()
                .addTag(TAG)
                .setInputData(
                    workDataOf(KEY_LOCAL_ID to localId, KEY_TEXT to text, KEY_SUBJECT to subject),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
