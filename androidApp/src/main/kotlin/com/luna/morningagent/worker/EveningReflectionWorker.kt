package com.luna.morningagent.worker

import android.Manifest
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
import com.luna.morningagent.data.model.BriefingKind
import com.luna.morningagent.data.notion.NotionRestClient
import com.luna.morningagent.data.secure.TokenStore

// Mirror of MorningAgentWorker for the evening wrap-up. Runs at the user-picked
// evening time (default 19:00), produces a Briefing(kind=EVENING) that
// AgentRepository writes to lastReflectionJson, and posts a notification on the
// shared "Daily briefing" channel with a wrap-up title.
//
// Self-rescheduling: after each terminal Result the worker calls
// EveningScheduler.scheduleNext so tomorrow's fire anchors to wall-clock.
class EveningReflectionWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = TokenStore(applicationContext)
        val provider = ProviderOption.fromId(store.getSelectedProvider())
        // Skip silently when keys aren't configured — same policy as morning.
        if (providerKey(store, provider).isNullOrEmpty() ||
            store.getNotionToken().isNullOrEmpty() ||
            store.getNotionDatabaseId().isNullOrEmpty()
        ) {
            EveningScheduler.scheduleNext(applicationContext)
            return Result.success()
        }

        val repo = AgentRepository(
            notionTaskSource  = NotionRestClient(store),
            briefingGenerator = buildBriefingGenerator(store, provider),
            tokenStore        = store,
        )

        val result = try {
            val briefing = repo.runAgent(kind = BriefingKind.EVENING)
            postReflectionNotification(applicationContext, briefing)
            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.success()
        }

        if (result !is Result.Retry) {
            EveningScheduler.scheduleNext(applicationContext)
        }
        return result
    }

    companion object {
        const val UNIQUE_WORK_NAME = "evening_reflection_daily"

        private const val NOTIFICATION_ID = 1002
        private const val MAX_RETRIES = 3

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

        private fun buildOpenAppPendingIntent(context: Context): PendingIntent {
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MorningAgentWorker.EXTRA_FROM_NOTIFICATION, true)
            }
            return PendingIntent.getActivity(
                context,
                1,                                          // requestCode distinct from morning's 0
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun postReflectionNotification(context: Context, briefing: Briefing) {
            MorningAgentWorker.ensureNotificationChannel(context)
            if (!hasPostPermission(context)) return

            val title = context.getString(
                R.string.notification_title_evening,
                briefing.actions.size,
            )
            val body = briefing.summary.lineSequence().firstOrNull()?.trim().orEmpty()

            val notification = NotificationCompat.Builder(context, MorningAgentWorker.CHANNEL_ID)
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
