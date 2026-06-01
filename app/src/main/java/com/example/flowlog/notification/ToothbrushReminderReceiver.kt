package com.example.flowlog.notification

import android.annotation.SuppressLint
import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.R

class ToothbrushReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra(EXTRA_REMINDER_TYPE).orEmpty()
        val category = intent.getStringExtra(EXTRA_CATEGORY).orEmpty()
        val activityTimerNotifier = ActivityTimerNotifier(context)

        when {
            category == "SNACK" -> activityTimerNotifier.clearSnackTimer()
            category == "MEAL" -> activityTimerNotifier.clearMealTimer()
            reminderType == TYPE_BRUSH_DONE -> {
                activityTimerNotifier.clearBrushDoneTimer()
                activityTimerNotifier.clearBrushStartNotification()
            }
            reminderType == TYPE_EAT_ALLOWED -> {
                activityTimerNotifier.clearBrushEatTimer()
                activityTimerNotifier.clearBrushStartNotification()
            }
        }

        if (!canPostNotifications(context)) return
        val shouldSilence = SleepAlarmGuard.shouldSilenceAlerts(context)
        if (shouldSilence) {
            SleepAlarmGuard.ensureSilentNotificationChannel(context)
        }

        val title = when (reminderType) {
            TYPE_BRUSH_DONE -> "\uC591\uCE58 \uC644\uB8CC \uC2DC\uAC04"
            TYPE_EAT_ALLOWED -> "\uBA39\uC5B4\uB3C4 \uB418\uB294 \uC2DC\uAC04"
            else -> if (category == "SNACK") {
                "\uAC04\uC2DD \uD6C4 \uC591\uCE58 \uC54C\uB9BC"
            } else {
                "\uC2DD\uC0AC \uD6C4 \uC591\uCE58 \uC54C\uB9BC"
            }
        }
        val message = when (reminderType) {
            TYPE_BRUSH_DONE -> "\uC591\uCE58\uB97C \uB9C8\uBB34\uB9AC\uD560 \uC2DC\uAC04\uC774\uC5D0\uC694."
            TYPE_EAT_ALLOWED -> "\uC591\uCE58\uD55C \uC9C0 30\uBD84\uC774 \uC9C0\uB0AC\uC5B4\uC694. \uC774\uC81C \uBA39\uC5B4\uB3C4 \uB3FC\uC694."
            else -> "\uC591\uCE58\uD560 \uC2DC\uAC04\uC774\uC5D0\uC694."
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (shouldSilence) SleepAlarmGuard.SILENT_CHANNEL_ID else DING_CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(ALERT_NOTIFICATION_TIMEOUT_MILLIS)

        if (shouldSilence) {
            builder
                .setSilent(true)
                .setDefaults(0)
                .setVibrate(null)
                .setSound(null)
        } else {
            builder
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setSound(KakaoStyleAlertPlayer.soundUri(context))
                .setVibrate(longArrayOf(0L, 500L, 250L, 500L))
        }

        val notificationId = intent.getLongExtra(
            EXTRA_ACTIVITY_ID,
            System.currentTimeMillis()
        ).toInt()
        notifySafely(context, notificationId, builder.build())
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(context: Context, notificationId: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    companion object {
        const val CHANNEL_ID = "flowlog_timer_alerts"
        const val DING_CHANNEL_ID = "flowlog_timer_alerts_app_sound_v2"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_TOOTHBRUSH = "type_toothbrush"
        const val TYPE_BRUSH_DONE = "type_brush_done"
        const val TYPE_EAT_ALLOWED = "type_eat_allowed"
        private const val REQUEST_OPEN_APP = 5001
        private const val ALERT_NOTIFICATION_TIMEOUT_MILLIS = 3_000L
        private val NOTIFICATION_ICON_COLOR = 0xFF4F5060.toInt()
    }
}
