package com.luna.morningagent.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.luna.morningagent.data.secure.TokenStore
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

// Schedules / cancels the daily briefing PeriodicWorkRequest.
//
// - MainActivity calls ensure() on launch: KEEP-policy enqueue, so we don't reset
//   the next firing window every cold start.
// - Settings calls replace() when the user changes the time or toggles on: UPDATE
//   policy so the new initialDelay takes effect immediately.
object BriefingScheduler {

    // Called from app startup. KEEP policy → no-op if already scheduled.
    fun ensure(context: Context) {
        val store = TokenStore(context)
        enqueue(context, store, ExistingPeriodicWorkPolicy.KEEP)
    }

    // Called when the user toggles on or changes the time in Settings. UPDATE
    // policy applies the new initialDelay without losing the unique work slot.
    fun replace(context: Context) {
        val store = TokenStore(context)
        enqueue(context, store, ExistingPeriodicWorkPolicy.UPDATE)
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MorningAgentWorker.UNIQUE_WORK_NAME)
    }

    private fun enqueue(
        context: Context,
        store: TokenStore,
        policy: ExistingPeriodicWorkPolicy,
    ) {
        val target = LocalTime.of(store.getDailyBriefingHour(), store.getDailyBriefingMinute())
        val initialDelayMillis = computeInitialDelayMillis(
            now    = ZonedDateTime.now(ZoneId.systemDefault()),
            target = target,
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<MorningAgentWorker>(
            repeatInterval         = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
        )
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MorningAgentWorker.UNIQUE_WORK_NAME,
            policy,
            request,
        )
    }

    // Returns ms from `now` until the next `target` time of day. If already past
    // today's target, schedules for tomorrow — no surprise mid-day fire when the
    // user changes the time. Tests can pass an explicit `now`.
    internal fun computeInitialDelayMillis(
        now: ZonedDateTime,
        target: LocalTime,
    ): Long {
        val todayTarget = now.with(target)
        val nextRun = if (now.isBefore(todayTarget)) todayTarget else todayTarget.plusDays(1)
        return Duration.between(now, nextRun).toMillis()
    }

    private const val WORK_TAG = "morning_briefing"
}
