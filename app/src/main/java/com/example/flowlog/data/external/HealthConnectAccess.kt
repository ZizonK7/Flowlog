package com.example.flowlog.data.external

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.flowlog.data.local.ExternalDataSettingsStore
import com.example.flowlog.data.repository.ExternalSampleRepository

/** What the Settings row needs: state, last successful fetch, samples in the last 7 days. */
data class HealthConnectStatus(
    val state: HealthConnectState = HealthConnectState.NOT_SUPPORTED,
    val lastFetchedAt: Long? = null,
    val recentSampleCount: Int = 0
)

/**
 * Availability, permissions, and the install link for Health Connect. Read-only access:
 * sleep, steps, exercise. Nothing here writes to Health Connect.
 * Every Health Connect call is behind Android 9 (API 28); below that the row says
 * "not supported" and no Health Connect class is touched.
 */
object HealthConnectAccess {
    const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

    // Same strings as HealthPermission.getReadPermission(SleepSessionRecord::class) etc.;
    // written out so building the request never loads Health Connect classes on old phones.
    const val READ_SLEEP = "android.permission.health.READ_SLEEP"
    const val READ_STEPS = "android.permission.health.READ_STEPS"
    const val READ_EXERCISE = "android.permission.health.READ_EXERCISE"
    // Lets the 6-hour background work read (Health Connect reads only in the foreground
    // otherwise). 미확인: HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND in 1.1.0;
    // optional — the foreground fetch works without it.
    const val READ_IN_BACKGROUND = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    val READ_PERMISSIONS: Set<String> = setOf(READ_SLEEP, READ_STEPS, READ_EXERCISE)
    val REQUESTED_PERMISSIONS: Set<String> = READ_PERMISSIONS + READ_IN_BACKGROUND

    private const val RECENT_DAYS = 7
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun availability(context: Context): HealthConnectAvailability {
        if (!isSupported()) return HealthConnectAvailability.NOT_SUPPORTED
        val status = runCatching { HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE) }.getOrNull()
        return when (status) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.NOT_INSTALLED
        }
    }

    /** Read permissions granted now (the background one is not counted). Empty when unavailable. */
    suspend fun grantedReadPermissions(context: Context): Set<String> {
        if (availability(context) != HealthConnectAvailability.AVAILABLE) return emptySet()
        return runCatching {
            HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions()
        }.getOrDefault(emptySet()).intersect(READ_PERMISSIONS)
    }

    suspend fun status(context: Context, now: Long = System.currentTimeMillis()): HealthConnectStatus {
        val appContext = context.applicationContext
        val availability = availability(appContext)
        val enabled = ExternalDataSettingsStore.isHealthConnectEnabled(appContext)
        val granted = if (availability == HealthConnectAvailability.AVAILABLE) grantedReadPermissions(appContext) else emptySet()
        val state = ExternalSamples.healthConnectState(availability, enabled, granted.size, READ_PERMISSIONS.size)
        val recent = runCatching {
            ExternalSampleRepository(appContext).countSince(ExternalSampleProvider.HEALTH_CONNECT, now - RECENT_DAYS * DAY_MILLIS)
        }.getOrDefault(0)
        return HealthConnectStatus(
            state = state,
            lastFetchedAt = ExternalDataSettingsStore.lastFetchedAt(appContext, ExternalSampleProvider.HEALTH_CONNECT),
            recentSampleCount = recent
        )
    }

    /** Health Connect's permission screen; the delegate is made only when first launched. */
    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> = PermissionContract()

    /** Play Store page for Health Connect (install or update, Android 13 and below). */
    fun openInstallPage(context: Context): Boolean {
        val market = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding")
        ).apply {
            setPackage("com.android.vending")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(market) }.isSuccess) return true
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PROVIDER_PACKAGE")).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(web) }.isSuccess
    }

    private class PermissionContract : ActivityResultContract<Set<String>, Set<String>>() {
        // 미확인: PermissionController.createRequestPermissionResultContract() (1.1.0) returns
        // ActivityResultContract<Set<String>, Set<String>>.
        private val delegate by lazy { PermissionController.createRequestPermissionResultContract() }

        override fun createIntent(context: Context, input: Set<String>): Intent =
            delegate.createIntent(context, input)

        override fun parseResult(resultCode: Int, intent: Intent?): Set<String> =
            delegate.parseResult(resultCode, intent)
    }
}
