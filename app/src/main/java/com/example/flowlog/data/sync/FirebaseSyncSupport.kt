package com.example.flowlog.data.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.notification.FirebaseSyncReceiver
import java.util.Calendar

data class SyncOutcome(
    val attemptedCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val deferred: Boolean = false
)

object FirebaseSyncAlarmScheduler {
    private const val REQUEST_CODE_MIDNIGHT_SYNC = 9401

    fun scheduleNextMidnightSync(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = midnightSyncIntent(appContext)
        val triggerAtMillis = nextMidnightMillis()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelMidnightSync(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = midnightSyncIntent(appContext)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun midnightSyncIntent(context: Context): PendingIntent {
        val intent = Intent(context, FirebaseSyncReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_SYNC
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextMidnightMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private const val ACTION_MIDNIGHT_SYNC = "com.example.flowlog.action.MIDNIGHT_SYNC"
}

class FirebaseSyncCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val dataSource = FirebaseSyncDataSource(appContext)

    suspend fun syncAll(userId: String): SyncOutcome {
        return dataSource.syncAll(userId)
    }

    suspend fun syncEligible(userId: String): SyncOutcome {
        val activeTimer = TimerStateStore.getActiveTimer(appContext)
        if (activeTimer?.status == TimerStatus.RUNNING) {
            return SyncOutcome(deferred = true)
        }
        return dataSource.syncEligible(userId)
    }
}
