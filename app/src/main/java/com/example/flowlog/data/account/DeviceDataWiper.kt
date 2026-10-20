package com.example.flowlog.data.account

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.sync.FirebaseSyncAlarmScheduler
import com.example.flowlog.notification.AlarmRescheduler
import com.example.flowlog.widget.FlowStatusWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Clears everything Flowlog keeps on this phone, for "delete account and data".
 * The sign-in account itself is not touched here (the caller signs out); the server
 * copy stays for the grace period so the user can still cancel.
 */
object DeviceDataWiper {
    /**
     * SharedPreferences files owned by Firebase / Google Play services / AndroidX libraries. Clearing them while
     * those SDKs are running can confuse them; they hold no Flowlog records, and signing out
     * removes the auth session.
     */
    private val sdkPrefixes = listOf("com.google.", "FirebaseHeartBeat", "firebase", "androidx.")

    /** Call while still signed in: alarm lookups read the current account's rows. */
    suspend fun wipe(context: Context) {
        val appContext = context.applicationContext
        AlarmRescheduler.cancelAll(appContext)
        FirebaseSyncAlarmScheduler.cancelMidnightSync(appContext)
        runCatching { NotificationManagerCompat.from(appContext).cancelAll() }
        withContext(Dispatchers.IO) {
            FlowlogDatabase.getInstance(appContext).clearAllTables()
            clearAppPreferences(appContext)
        }
        runCatching { FlowStatusWidgetProvider.updateAll(appContext) }
    }

    private fun clearAppPreferences(context: Context) {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        dir.listFiles { file -> file.name.endsWith(".xml") }.orEmpty()
            .map { it.name.removeSuffix(".xml") }
            .filterNot { name -> sdkPrefixes.any { name.startsWith(it, ignoreCase = true) } }
            .forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            }
    }
}
