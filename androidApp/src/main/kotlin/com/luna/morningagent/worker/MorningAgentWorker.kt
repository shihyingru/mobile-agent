package com.luna.morningagent.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.luna.morningagent.MainActivity
import com.luna.morningagent.R
import com.luna.morningagent.data.AgentRepository
import com.luna.morningagent.data.agent.BriefingGenerator
import com.luna.morningagent.data.agent.ClaudeBriefingClient
import com.luna.morningagent.data.agent.GeminiBriefingClient
import com.luna.morningagent.data.agent.ProviderOption
import com.luna.morningagent.data.model.Briefing
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.secure.TokenStore

// Runs the agent in the background at 9:00 AM (scheduled by BriefingScheduler),
// caches the result for HomeViewModel, and posts a notification so Luna sees the
// briefing without opening the app.
//
// Retries on failure with WorkManager's exponential backoff — Gemini's transient
// 503/429 are already retried inside GeminiBriefingClient with bounded attempts,
// so a Result.retry() here covers the rarer cases (no network at 9:00 AM).
//
// Self-rescheduling: after each terminal Result (success or final retry), the
// worker calls BriefingScheduler.scheduleNext() so tomorrow's fire anchors to
// the user's picked wall-clock time, not +24h from this run's start.
class MorningAgentWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)
        val provider = ProviderOption.fromId(store.getSelectedProvider())
        // Skip silently when the user hasn't finished onboarding — better than
        // posting an error notification before they've saved their keys. We
        // check only the active provider's key; the other provider's key
        // staying empty doesn't block today's run.
        if (providerKey(store, provider).isNullOrEmpty() ||
            store.getNotionToken().isNullOrEmpty() ||
            store.getNotionDatabaseId().isNullOrEmpty()
        ) {
            BriefingScheduler.scheduleNext(applicationContext)
            return Result.success()
        }

        val repo = AgentRepository(
            notionTaskSource  = NotionRestClient(store),
            briefingGenerator = buildBriefingGenerator(store, provider),
            tokenStore        = store,
        )

        val result = try {
            val briefing = repo.runAgent()
            postBriefingNotification(applicationContext, briefing)
            Result.success()
        } catch (_: Exception) {
            // Cap retries via WorkManager runAttemptCount — gives us up to 3
            // chances spaced by exponential backoff (default 30s, 60s, 120s)
            // before giving up until tomorrow's scheduled run.
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        }

        // Re-anchor for tomorrow on every terminal result. Skip on retry so we
        // don't queue duplicates — WorkManager will hand us another shot first.
        if (result !is Result.Retry) {
            BriefingScheduler.scheduleNext(applicationContext)
        }
        return result
    }

    companion object {
        const val UNIQUE_WORK_NAME = "morning_agent_daily"
        const val CHANNEL_ID = "morning_briefing"

        private fun providerKey(store: TokenStore, provider: ProviderOption): String? =
            when (provider) {
                ProviderOption.Gemini -> store.getGeminiKey()
                ProviderOption.Claude -> store.getClaudeKey()
            }

        private fun buildBriefingGenerator(
            store: TokenStore,
            provider: ProviderOption,
        ): BriefingGenerator = when (provider) {
            ProviderOption.Gemini -> GeminiBriefingClient(store)
            ProviderOption.Claude -> ClaudeBriefingClient(store)
        }

        // Intent extra set by the notification's PendingIntent. MainActivity reads it
        // on cold launch and via onNewIntent so the app jumps straight to Home —
        // tapping a "your briefing is ready" notification shouldn't dump the user on
        // the Launch screen or whatever screen they last left the app on.
        const val EXTRA_FROM_NOTIFICATION = "com.luna.morningagent.FROM_NOTIFICATION"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_RETRIES = 3

        private fun buildOpenAppPendingIntent(context: Context): PendingIntent {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_FROM_NOTIFICATION, true)
            }
            return PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        // Stub preview — same channel/icon/layout as the real worker, but skips the
        // agent so the user can verify visuals + permissions without burning quota.
        fun postSampleNotification(context: Context) {
            ensureNotificationChannel(context)
            if (!hasPostPermission(context)) return

            val title = context.getString(R.string.notification_title, 3)
            val body  = context.getString(R.string.notification_sample_body)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orb_splash)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(buildOpenAppPendingIntent(context))
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        // Idempotent — calling on every app start is fine. The system dedupes by id.
        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return
            if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_briefing),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notification_channel_briefing_desc)
            }
            mgr.createNotificationChannel(channel)
        }

        private fun postBriefingNotification(context: Context, briefing: Briefing) {
            ensureNotificationChannel(context)
            if (!hasPostPermission(context)) return

            val title = context.getString(
                R.string.notification_title,
                briefing.tasks.size,
            )
            val body = briefing.summary.lineSequence().firstOrNull()?.trim().orEmpty()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orb_splash)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(buildOpenAppPendingIntent(context))
                .build()

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun hasPostPermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
