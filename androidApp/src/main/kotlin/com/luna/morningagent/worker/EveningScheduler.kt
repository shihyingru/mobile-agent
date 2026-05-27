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

// Mirror of BriefingScheduler for the evening wrap-up. Reads the user-picked
// evening time from TokenStore (default 19:00). Same wall-clock anchoring +
// self-rescheduling pattern as the morning scheduler — see BriefingScheduler.
object EveningScheduler {

    fun ensure(context: Context) {
        val store = TokenStore(context)
        if (!store.getDailyEveningEnabled()) return
        enqueue(context, store, ExistingWorkPolicy.KEEP)
    }

    fun replace(context: Context) {
        val store = TokenStore(context)
        enqueue(context, store, ExistingWorkPolicy.REPLACE)
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(EveningReflectionWorker.UNIQUE_WORK_NAME)
    }

    fun scheduleNext(context: Context) {
        val store = TokenStore(context)
        if (!store.getDailyEveningEnabled()) return
        enqueue(context, store, ExistingWorkPolicy.REPLACE)
    }

    private fun enqueue(
        context: Context,
        store: TokenStore,
        policy: ExistingWorkPolicy,
    ) {
        val target = LocalTime.of(store.getDailyEveningHour(), store.getDailyEveningMinute())
        val initialDelayMillis = computeInitialDelayMillis(
            now    = ZonedDateTime.now(ZoneId.systemDefault()),
            target = target,
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<EveningReflectionWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            EveningReflectionWorker.UNIQUE_WORK_NAME,
            policy,
            request,
        )
    }

    internal fun computeInitialDelayMillis(
        now: ZonedDateTime,
        target: LocalTime,
    ): Long {
        val todayTarget = now.with(target)
        val nextRun = if (now.isBefore(todayTarget)) todayTarget else todayTarget.plusDays(1)
        return Duration.between(now, nextRun).toMillis()
    }

    private const val WORK_TAG = "evening_reflection"
}
