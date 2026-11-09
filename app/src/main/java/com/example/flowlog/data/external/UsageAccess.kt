package com.example.flowlog.data.external

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.example.flowlog.data.local.ExternalDataSettingsStore
import com.example.flowlog.data.repository.ExternalSampleRepository

/** What the "앱 사용 통계" Settings row needs. */
data class UsageStatsStatus(
    val state: UsageStatsState = UsageStatsState.OFF,
    val lastFetchedAt: Long? = null,
    val recentSampleCount: Int = 0
)

/**
 * "Usage access" (PACKAGE_USAGE_STATS) is a special access the user turns on in system
 * settings; the app cannot ask for it with a dialog. Whether it is on is an app-op, not a
 * runtime permission, so it is read from [AppOpsManager].
 */
object UsageAccess {
    // AppOpsManager.MODE_ALLOWED / MODE_DEFAULT, written out for [isAllowedMode] (pure).
    const val MODE_ALLOWED = 0
    const val MODE_DEFAULT = 3

    private const val RECENT_DAYS = 7
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun isGranted(context: Context): Boolean = runCatching {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        isAllowedMode(
            mode,
            permissionGranted = context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }.getOrDefault(false)

    /** MODE_DEFAULT means "follow the manifest permission" (some builds report that after a grant). */
    fun isAllowedMode(mode: Int, permissionGranted: Boolean): Boolean =
        mode == MODE_ALLOWED || (mode == MODE_DEFAULT && permissionGranted)

    /** The system "사용 정보 접근" list; the user finds Flowlog there and turns it on. */
    fun openSettings(context: Context): Boolean = runCatching {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.isSuccess

    suspend fun status(context: Context, now: Long = System.currentTimeMillis()): UsageStatsStatus {
        val appContext = context.applicationContext
        val state = ExternalSamples.usageStatsState(
            enabled = ExternalDataSettingsStore.isUsageStatsEnabled(appContext),
            granted = isGranted(appContext)
        )
        val recent = runCatching {
            ExternalSampleRepository(appContext).countSince(ExternalSampleProvider.USAGE_STATS, now - RECENT_DAYS * DAY_MILLIS)
        }.getOrDefault(0)
        return UsageStatsStatus(
            state = state,
            lastFetchedAt = ExternalDataSettingsStore.lastFetchedAt(appContext, ExternalSampleProvider.USAGE_STATS),
            recentSampleCount = recent
        )
    }
}
