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
import com.luna.morningagent.data.agent.GeminiBriefingClient
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
class MorningAgentWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)
        // Skip silently when the user hasn't finished onboarding — better than
        // posting an error notification before they've saved their keys.
        if (store.getGeminiKey().isNullOrEmpty() ||
            store.getNotionToken().isNullOrEmpty() ||
            store.getNotionDatabaseId().isNullOrEmpty()
        ) {
            return Result.success()
        }

        val repo = AgentRepository(
            notionTaskSource  = NotionRestClient(store),
            briefingGenerator = GeminiBriefingClient(store),
            tokenStore        = store,
        )

        return try {
            val briefing = repo.runAgent()
            postBriefingNotification(applicationContext, briefing)
            Result.success()
        } catch (_: Exception) {
            // Cap retries via WorkManager runAttemptCount — gives us up to 3
            // chances spaced by exponential backoff (default 30s, 60s, 120s)
            // before giving up until tomorrow's scheduled run.
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "morning_agent_daily"
        const val CHANNEL_ID = "morning_briefing"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_RETRIES = 3

        // Stub preview — same channel/icon/layout as the real worker, but skips the
        // agent so the user can verify visuals + permissions without burning quota.
        fun postSampleNotification(context: Context) {
            ensureNotificationChannel(context)
            if (!hasPostPermission(context)) return

            val title = context.getString(R.string.notification_title, 3)
            val body  = context.getString(R.string.notification_sample_body)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orb_splash)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
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

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_orb_splash)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pi)
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
