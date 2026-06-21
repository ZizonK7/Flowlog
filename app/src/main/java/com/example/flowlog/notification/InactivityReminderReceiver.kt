package com.example.flowlog.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.R
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.local.InactivityReminderStore
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.repository.EventLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class InactivityReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val blockReason = blockReason(context, now)
                if (blockReason != null) {
                    if (blockReason == "outside_allowed_hours") {
                        InactivityReminderScheduler(context).rescheduleFromLastActivityIfNeeded()
                    }
                    Log.i(TAG, "blocked: reason=$blockReason")
                    return@launch
                }

                ensureNotificationChannel(context)
                val notificationId = UUID.randomUUID().toString()
                val notificationIntId = NOTIFICATION_ID
                val clickPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationIntId,
                    Intent(context, InactivityReminderClickReceiver::class.java).apply {
                        putExtra(InactivityReminderClickReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                        putExtra(InactivityReminderClickReceiver.EXTRA_NOTIFICATION_INT_ID, notificationIntId)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                notifySafely(context, notificationIntId, buildNotification(context, clickPendingIntent))
                InactivityReminderStore.markShown(context, notificationId, now)
                EventLogRepository(context).log(
                    eventType = EventType.NOTIFICATION_SHOWN,
                    source = EventSource.NOTIFICATION,
                    metadataJson = JSONObject()
                        .put("type", TYPE)
                        .put("notificationId", notificationId)
                        .put("shownAt", now)
                        .put("lastActivityEndedAt", InactivityReminderStore.getLastActivityEndedAt(context))
                        .toString()
                )
                Log.i(TAG, "shown: notificationId=$notificationId")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing inactivity reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun blockReason(context: Context, now: Long): String? {
        if (!InactivityReminderStore.isEnabled(context)) return "disabled"
        if (!canPostNotifications(context)) return "notification_permission_denied"
        if (TimerStateStore.getActiveTimer(context) != null) return "active_timer"
        if (SleepAlarmGuard.shouldSilenceAlerts(context)) return "sleep"
        if (!isAllowedHour(now)) return "outside_allowed_hours"
        val lastActivityEndedAt = InactivityReminderStore.getLastActivityEndedAt(context)
        if (lastActivityEndedAt <= 0L) return "missing_last_activity_end"
        if (now - lastActivityEndedAt < InactivityReminderScheduler.DELAY_AFTER_ACTIVITY_END_MILLIS) {
            return "too_early"
        }
        val lastShownAt = InactivityReminderStore.getLastShownAt(context)
        if (lastShownAt > 0L && now - lastShownAt < SAME_NOTIFICATION_COOLDOWN_MILLIS) {
            return "cooldown"
        }
        if (InactivityReminderStore.shownCountToday(context, now) >= MAX_PER_DAY) {
            return "daily_limit"
        }
        return null
    }

    private fun buildNotification(context: Context, clickPendingIntent: PendingIntent): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle("Flowlog")
            .setContentText(MESSAGE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(MESSAGE))
            .setContentIntent(clickPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    private fun isAllowedHour(timeMillis: Long): Boolean {
        val hour = Calendar.getInstance().apply { this.timeInMillis = timeMillis }
            .get(Calendar.HOUR_OF_DAY)
        return hour in START_HOUR until END_HOUR
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Flowlog record nudges",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Gentle reminders when no activity has been recorded for a while"
            setShowBadge(true)
        }
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, notificationId: Int, notification: Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "flowlog_inactivity_reminders"
        const val TYPE = "INACTIVITY_REMINDER"
        private const val TAG = "InactivityReminder"
        private const val NOTIFICATION_ID = 62032
        private const val START_HOUR = 8
        private const val END_HOUR = 22
        private const val MAX_PER_DAY = 2
        private const val SAME_NOTIFICATION_COOLDOWN_MILLIS = 6L * 60L * 60L * 1000L
        private const val NOTIFICATION_ICON_COLOR = 0xFF4F5060.toInt()
        private const val MESSAGE = "꼭 뭔가를 하고 있지 않아도 괜찮아요. 오늘의 흐름만 가볍게 남겨볼까요?"
    }
}
