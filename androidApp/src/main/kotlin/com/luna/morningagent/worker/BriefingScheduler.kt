package com.luna.morningagent.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.luna.morningagent.data.secure.TokenStore
import java.time.Duration
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

// Schedules / cancels the daily briefing.
//
// We use OneTimeWorkRequest, not PeriodicWorkRequest, so the schedule stays
// anchored to wall-clock time. PeriodicWorkRequest measures its next fire from
// the previous fire's start in elapsed real-time — under Doze or device-off the
// schedule drifts. With OneTimeWorkRequest, the worker re-enqueues itself for
// the next wall-clock target on each completion, so a late run today still
// fires at exactly the picked time tomorrow.
//
// - MainActivity calls ensure() on launch: KEEP policy, so we don't reset the
//   next firing window every cold start.
// - Settings calls replace() when the user toggles on or changes the time:
//   REPLACE so the new initialDelay takes effect immediately.
// - The worker itself calls scheduleNext() after each fire.
object BriefingScheduler {

    fun ensure(context: Context) {
        val store = TokenStore(context)
        if (!store.getDailyBriefingEnabled()) return
        enqueue(context, store, ExistingWorkPolicy.KEEP)
    }

    fun replace(context: Context) {
        val store = TokenStore(context)
        enqueue(context, store, ExistingWorkPolicy.REPLACE)
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MorningAgentWorker.UNIQUE_WORK_NAME)
    }

    // Called from inside MorningAgentWorker.doWork() after the briefing completes,
    // succeeds-but-skipped, or hits a terminal failure. Always re-anchors to the
    // next wall-clock occurrence rather than +24h from "now", so a deferred run
    // doesn't bleed into the picked slot.
    fun scheduleNext(context: Context) {
        val store = TokenStore(context)
        if (!store.getDailyBriefingEnabled()) return
        enqueue(context, store, ExistingWorkPolicy.REPLACE)
    }

    private fun enqueue(
        context: Context,
        store: TokenStore,
        policy: ExistingWorkPolicy,
    ) {
        val target = LocalTime.of(store.getDailyBriefingHour(), store.getDailyBriefingMinute())
        val initialDelayMillis = computeInitialDelayMillis(
            now    = ZonedDateTime.now(ZoneId.systemDefault()),
            target = target,
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<MorningAgentWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
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