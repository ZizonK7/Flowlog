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
 * Runs one fetch for each provider that is switched on (Health Connect, usage statistics),
 * or only for the one named in the input. Started every 6 hours while any switch is on, right
 * after a switch is turned on, and on app foreground for each provider whose last try is
 * 6 hours old ([ExternalSampleScheduler]). One provider failing does not stop the other; each
 * collector records its own EXTERNAL_SAMPLES_FETCHED. A failed fetch is not retried: the next
 * period or foreground tries again. With every switch off it does nothing (a periodic request
 * that outlived the switches, e.g. after an account wipe, ends here).
 */
class ExternalSampleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val trigger = inputData.getString(KEY_TRIGGER) ?: ExternalSampleScheduler.TRIGGER_PERIODIC
        val only = inputData.getString(KEY_PROVIDER)
        fun wanted(provider: String) = only == null || only == provider

        if (wanted(ExternalSampleProvider.HEALTH_CONNECT) &&
            ExternalDataSettingsStore.isHealthConnectEnabled(applicationContext) &&
            // Inline check (not HealthConnectAccess.isSupported) so lint sees the API 28 guard.
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        ) {
            runCatching { HealthConnectCollector(applicationContext).collect(trigger) }
        }
        if (wanted(ExternalSampleProvider.USAGE_STATS) &&
            ExternalDataSettingsStore.isUsageStatsEnabled(applicationContext)
        ) {
            runCatching { UsageStatsCollector(applicationContext).collect(trigger) }
        }
        return Result.success()
    }

    companion object {
        const val KEY_TRIGGER = "trigger"
        /** Absent = every provider that is on. */
        const val KEY_PROVIDER = "provider"
    }
}

/** Starts and stops [ExternalSampleWorker]. WorkManager uses its default initializer (no custom Application). */
object ExternalSampleScheduler {
    private const val PERIODIC_WORK_NAME = "flowlog-external-samples-periodic"
    // One queue per provider, so turning one on never drops the other's queued fetch (KEEP).
    private const val ONE_TIME_WORK_PREFIX = "flowlog-external-samples-now-"
    const val TRIGGER_PERIODIC = "periodic"
    const val TRIGGER_ENABLED = "enabled"
    const val TRIGGER_FOREGROUND = "foreground"

    fun anyEnabled(context: Context): Boolean =
        ExternalDataSettingsStore.isHealthConnectEnabled(context) || ExternalDataSettingsStore.isUsageStatsEnabled(context)

    /** Registers the 6-hour work; an existing one is kept (its schedule is not reset). */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ExternalSampleWorker>(6, TimeUnit.HOURS)
            .setInputData(workDataOf(ExternalSampleWorker.KEY_TRIGGER to TRIGGER_PERIODIC))
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** A switch was turned off: drop its queued fetch, and the periodic work once both are off. */
    fun onProviderDisabled(context: Context, provider: String) {
        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        workManager.cancelUniqueWork(ONE_TIME_WORK_PREFIX + provider)
        if (!anyEnabled(appContext)) workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /** One fetch of [provider] as soon as possible; a fetch already queued for it is kept. */
    fun runNow(context: Context, trigger: String, provider: String) {
        val request = OneTimeWorkRequestBuilder<ExternalSampleWorker>()
            .setInputData(
                workDataOf(
                    ExternalSampleWorker.KEY_TRIGGER to trigger,
                    ExternalSampleWorker.KEY_PROVIDER to provider
                )
            )
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(ONE_TIME_WORK_PREFIX + provider, ExistingWorkPolicy.KEEP, request)
    }

    /**
     * App came to the foreground: keep the periodic work and fetch each provider that is on
     * and whose last try is 6 hours old. Usage statistics without access is skipped here
     * (the Settings row shows it); it is tried as soon as access is on.
     */
    fun onForeground(context: Context, now: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        if (!anyEnabled(appContext)) return
        ensurePeriodic(appContext)
        if (ExternalDataSettingsStore.isHealthConnectEnabled(appContext) && HealthConnectAccess.isSupported()) {
            val lastAttempt = ExternalDataSettingsStore.lastAttemptAt(appContext, ExternalSampleProvider.HEALTH_CONNECT)
            if (ExternalSamples.isFetchDue(lastAttempt, now)) {
                runNow(appContext, TRIGGER_FOREGROUND, ExternalSampleProvider.HEALTH_CONNECT)
            }
        }
        if (ExternalDataSettingsStore.isUsageStatsEnabled(appContext) && UsageAccess.isGranted(appContext)) {
            val lastAttempt = ExternalDataSettingsStore.lastAttemptAt(appContext, ExternalSampleProvider.USAGE_STATS)
            if (ExternalSamples.isFetchDue(lastAttempt, now)) {
                runNow(appContext, TRIGGER_FOREGROUND, ExternalSampleProvider.USAGE_STATS)
            }
        }
    }
}
