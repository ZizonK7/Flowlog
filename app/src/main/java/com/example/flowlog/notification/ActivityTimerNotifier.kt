package com.example.flowlog.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.R

class ActivityTimerNotifier(private val context: Context) {
    fun showRunningTimer(category: String, startedAtMillis: Long) {
        if (!canPostNotifications()) return

        ensureNotificationChannel()

        val openPendingIntent = PendingIntent.getActivity(
            context,
            TIMER_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("Flowlog timer")
            .setContentText(runningStatusText(category))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(startedAtMillis)
            .setUsesChronometer(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_ID, notification)
    }

    fun clearRunningTimer() {
        NotificationManagerCompat.from(context).cancel(TIMER_NOTIFICATION_ID)
    }

    fun showSnackTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = SNACK_NOTIFICATION_ID,
            title = "\uAC04\uC2DD \uD0C0\uC774\uBA38",
            text = "\uC591\uCE58 \uC54C\uB9BC\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis
        )
    }

    fun showMealTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = MEAL_NOTIFICATION_ID,
            title = "\uC2DD\uC0AC \uD0C0\uC774\uBA38",
            text = "\uC591\uCE58 \uC54C\uB9BC\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis
        )
    }

    fun showBrushDoneTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = BRUSH_DONE_NOTIFICATION_ID,
            title = "\uC591\uCE58 3\uBD84 \uD0C0\uC774\uBA38",
            text = "\uC591\uCE58 \uB9C8\uBB34\uB9AC\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis
        )
    }

    fun showBrushEatTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = BRUSH_EAT_NOTIFICATION_ID,
            title = "\uC591\uCE58 30\uBD84 \uD0C0\uC774\uBA38",
            text = "\uBA39\uC5B4\uB3C4 \uB418\uB294 \uC2DC\uAC04\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis
        )
    }

    fun showBrushStartNotification(
        isExperiment: Boolean = false,
        experimentText: String? = null
    ) {
        if (!canPostNotifications()) return

        ensureNotificationChannel()

        val openPendingIntent = PendingIntent.getActivity(
            context,
            BRUSH_START_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("\uC591\uCE58 \uD0C0\uC774\uBA38\uB97C \uC2DC\uC791\uD560\uAC8C\uC694")
            .setContentText(
                experimentText ?: if (isExperiment) {
                    "1\uBC88 \uC2E4\uD5D8\uC6A9 5\uCD08 \uD0C774\uBA38\uB97C \uC124\uC815\uD588\uC5B4\uC694."
                } else {
                    "3\uBD84 \uD0C774\uBA38\uC640 30\uBD84 \uD0C774\uBA38\uB97C \uD568\uAED8 \uC124\uC815\uD588\uC5B4\uC694."
                }
            )
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        NotificationManagerCompat.from(context).notify(BRUSH_START_NOTIFICATION_ID, notification)
    }

    fun clearSnackTimer() {
        NotificationManagerCompat.from(context).cancel(SNACK_NOTIFICATION_ID)
    }

    fun clearMealTimer() {
        NotificationManagerCompat.from(context).cancel(MEAL_NOTIFICATION_ID)
    }

    fun clearBrushDoneTimer() {
        NotificationManagerCompat.from(context).cancel(BRUSH_DONE_NOTIFICATION_ID)
    }

    fun clearBrushEatTimer() {
        NotificationManagerCompat.from(context).cancel(BRUSH_EAT_NOTIFICATION_ID)
    }

    private fun showCountdownTimer(
        notificationId: Int,
        title: String,
        text: String,
        endsAtMillis: Long
    ) {
        if (!canPostNotifications()) return

        ensureNotificationChannel()

        val remainingMillis = (endsAtMillis - System.currentTimeMillis()).coerceAtLeast(1L)
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(endsAtMillis)
            .setTimeoutAfter(remainingMillis)
            .setUsesChronometer(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Flowlog timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Running activity timer"
            setShowBadge(false)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun ensureDingNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            ToothbrushReminderReceiver.DING_CHANNEL_ID,
            "Flowlog ding alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Snack, meal, toothbrush, and experiment ding alerts"
            setSound(
                KakaoStyleAlertPlayer.soundUri(context),
                KakaoStyleAlertPlayer.audioAttributes()
            )
            enableVibration(true)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun runningStatusText(category: String): String {
        return when (category) {
            "STUDY" -> "\uACF5\uBD80\uB97C \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "MEAL" -> "\uC2DD\uC0AC\uB97C \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "EXERCISE" -> "\uC6B4\uB3D9\uC744 \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "SLEEP" -> "\uC218\uBA74 \uC911\uC785\uB2C8\uB2E4!"
            "REST" -> "\uD734\uC2DD \uC911\uC785\uB2C8\uB2E4!"
            "SCHOOL" -> "\uD559\uAD50 \uD65C\uB3D9 \uC911\uC785\uB2C8\uB2E4!"
            "TODO" -> "\uD560\uC77C\uC744 \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            else -> "\uD65C\uB3D9 \uC911\uC785\uB2C8\uB2E4!"
        }
    }

    companion object {
        private const val CHANNEL_ID = "flowlog_activity_timer"
        private const val TIMER_NOTIFICATION_ID = 2001
        private const val SNACK_NOTIFICATION_ID = 2002
        private const val BRUSH_DONE_NOTIFICATION_ID = 2003
        private const val BRUSH_EAT_NOTIFICATION_ID = 2004
        private const val MEAL_NOTIFICATION_ID = 2005
        private const val BRUSH_START_NOTIFICATION_ID = 2006
    }
}
