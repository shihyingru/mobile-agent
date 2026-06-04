package com.luna.morningagent.worker

import android.content.Context
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

/**
 * Runs the full "save a shared post" pipeline as a background job.
 *
 * This used to run in an app-scoped coroutine launched from the NoDisplay
 * [com.luna.morningagent.ShareReceiverActivity], which `finish()`es instantly —
 * so the process went cached and Android froze (or killed) it mid-pipeline,
 * leaving sync / categorize / location-resolve unfinished until the app was
 * reopened. WorkManager keeps the process alive while the job runs, survives
 * process death (the request is persisted on enqueue), and starts promptly when
 * expedited.
 *
 * Pipeline: save → enrich body + image → sync to Notion → categorize →
 * resolve location (text-first, image-fallback) → persist. Every step degrades
 * silently (the repository keeps unsynced posts in its outbox), so the job
 * always returns success — it never re-runs `save` and so can't duplicate a post.
 */
class SharedPostEnrichWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val rawText = inputData.getString(KEY_TEXT)?.trim().orEmpty()
        if (rawText.isEmpty()) return Result.success()
        val subject = inputData.getString(KEY_SUBJECT)

        val ctx         = applicationContext
        val tokenStore  = TokenStore(ctx)
        val repo        = SharedPostsRepository(tokenStore)
        val categorizer = SharedPostCategorizer(tokenStore)
        val bodyFetcher = SharedPostBodyFetcher(tokenStore)

        val saved = (repo.save(rawText, subject) as? SaveResult.SavedPending)?.post
            ?: return Result.success()

        // 1. Enrich body + image from the URL (scrape + AI fallback). Image URL
        //    only comes from the scrape.
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

        // 2. Push to Notion now that content is enriched (no-op when DB unset;
        //    failures leave pendingSync = true for the setup-flow flush).
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

        // 4. Resolve every place once (text-first, image OCR fallback) and persist
        //    so the Saved card shows an accurate map pin. One place → tap opens it;
        //    several → tap opens a places sheet. Empty = no pin.
        val places = runCatching {
            bodyFetcher.resolvePostLocations(enriched.content, enriched.imageUrl)
        }.getOrNull().orEmpty()
        repo.updateLocations(enriched.localId, places)

        return Result.success()
    }

    companion object {
        private const val KEY_TEXT    = "shared_text"
        private const val KEY_SUBJECT = "shared_subject"

        /**
         * Enqueue the pipeline for a freshly-shared payload. A plain (non-
         * expedited) job: WorkManager already guarantees it runs while keeping
         * the process alive and survives process death, and expedited work on
         * minSdk 26–30 would force a foreground notification on every share.
         */
        fun enqueue(context: Context, text: String, subject: String?) {
            val request = OneTimeWorkRequestBuilder<SharedPostEnrichWorker>()
                .setInputData(workDataOf(KEY_TEXT to text, KEY_SUBJECT to subject))
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
