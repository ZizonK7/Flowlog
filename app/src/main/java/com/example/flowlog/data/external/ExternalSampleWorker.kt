package com.example.flowlog.data.external

import android.content.Context
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.flowlog.data.local.ExternalDataSettingsStore
import java.util.concurrent.TimeUnit

/**
 * Runs one Health Connect fetch. Started every 6 hours while the switch is on, right after
 * it is turned on, and on app foreground when the last try is 6 hours old
 * ([ExternalSampleScheduler]). Does nothing when the switch is off (a periodic request that
 * outlived the switch, e.g. after an account wipe, ends here). A failed fetch is recorded as
 * an event and not retried: the next period or foreground tries again.
 */
class ExternalSampleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!ExternalDataSettingsStore.isHealthConnectEnabled(applicationContext)) return Result.success()
        // Inline check (not HealthConnectAccess.isSupported) so lint sees the API 28 guard.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return Result.success()
        val trigger = inputData.getString(KEY_TRIGGER) ?: ExternalSampleScheduler.TRIGGER_PERIODIC
        runCatching { HealthConnectCollector(applicationContext).collect(trigger) }
        return Result.success()
    }

    companion object {
        const val KEY_TRIGGER = "trigger"
    }
}

/** Starts and stops [ExternalSampleWorker]. WorkManager uses its default initializer (no custom Application). */
object ExternalSampleScheduler {
    private const val PERIODIC_WORK_NAME = "flowlog-external-samples-periodic"
    private const val ONE_TIME_WORK_NAME = "flowlog-external-samples-now"
    const val TRIGGER_PERIODIC = "periodic"
    const val TRIGGER_ENABLED = "enabled"
    const val TRIGGER_FOREGROUND = "foreground"

    /** Registers the 6-hour work; an existing one is kept (its schedule is not reset). */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExternalSampleWorker>(6, TimeUnit.HOURS)
            .setInputData(workDataOf(ExternalSampleWorker.KEY_TRIGGER to TRIGGER_PERIODIC))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(ONE_TIME_WORK_NAME)
    }

    /** One fetch as soon as possible; a fetch already queued is kept instead of doubled. */
    fun runNow(context: Context, trigger: String) {
        val request = OneTimeWorkRequestBuilder<ExternalSampleWorker>()
            .setInputData(workDataOf(ExternalSampleWorker.KEY_TRIGGER to trigger))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(ONE_TIME_WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** App came to the foreground: keep the periodic work and fetch once if 6 hours have passed. */
    fun onForeground(context: Context, now: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        if (!ExternalDataSettingsStore.isHealthConnectEnabled(appContext)) return
        if (!HealthConnectAccess.isSupported()) return
        ensurePeriodic(appContext)
        val lastAttempt = ExternalDataSettingsStore.lastAttemptAt(appContext, ExternalSampleProvider.HEALTH_CONNECT)
        if (ExternalSamples.isFetchDue(lastAttempt, now)) runNow(appContext, TRIGGER_FOREGROUND)
    }
}
