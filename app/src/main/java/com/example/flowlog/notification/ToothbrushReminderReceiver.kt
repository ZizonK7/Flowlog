package com.example.flowlog.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
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
            reminderType == TYPE_BRUSH_DONE -> activityTimerNotifier.clearBrushDoneTimer()
            reminderType == TYPE_EAT_ALLOWED -> activityTimerNotifier.clearBrushEatTimer()
        }

        if (reminderType == TYPE_BRUSH_DONE) {
            runCatching {
                openBrushAlarmScreen(context)
            }
            return
        }

        if (!canPostNotifications(context)) return

        val title = when (reminderType) {
            TYPE_EAT_ALLOWED -> "\uBA39\uC5B4\uB3C4 \uB418\uB294 \uC2DC\uAC04"
            else -> if (category == "SNACK") {
                "\uAC04\uC2DD \uD6C4 \uC591\uCE58 \uC54C\uB9BC"
            } else {
                "\uC2DD\uC0AC \uD6C4 \uC591\uCE58 \uC54C\uB9BC"
            }
        }
        val message = when (reminderType) {
            TYPE_EAT_ALLOWED -> "\uC591\uCE58\uD55C \uC9C0 30\uBD84\uC774 \uC9C0\uB0AC\uC5B4\uC694. \uC774\uC81C \uBA39\uC5B4\uB3C4 \uB3FC\uC694."
            else -> "\uC591\uCE58\uD560 \uC2DC\uAC04\uC774\uC5D0\uC694."
        }
        val openIntent = Intent(context, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = intent.getLongExtra(
            EXTRA_ACTIVITY_ID,
            System.currentTimeMillis()
        ).toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun openBrushAlarmScreen(context: Context) {
        val screenIntent = Intent(context, BrushAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(screenIntent)
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "flowlog_timer_alerts"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_ACTIVITY_ID = "extra_activity_id"
        const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
        const val TYPE_TOOTHBRUSH = "type_toothbrush"
        const val TYPE_BRUSH_DONE = "type_brush_done"
        const val TYPE_EAT_ALLOWED = "type_eat_allowed"
    }
}
