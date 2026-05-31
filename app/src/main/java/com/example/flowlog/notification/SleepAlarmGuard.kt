package com.example.flowlog.notification

import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus

object SleepAlarmGuard {
    const val SILENT_CHANNEL_ID = "flowlog_sleep_silent_alerts"

    fun shouldSilenceAlerts(context: Context): Boolean {
        val activeTimer = TimerStateStore.getActiveTimer(context) ?: return false
        return activeTimer.category == "SLEEP" && activeTimer.status == TimerStatus.RUNNING
    }

    fun ensureSilentNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            SILENT_CHANNEL_ID,
            "Flowlog sleep silent alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts delivered silently while sleep tracking is active"
            setSound(null, null)
            enableVibration(false)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
