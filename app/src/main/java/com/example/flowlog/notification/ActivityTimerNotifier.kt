package com.example.flowlog.notification

import android.annotation.SuppressLint
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
import com.example.flowlog.data.local.FocusModeStore
import com.example.flowlog.ui.component.categoryNotificationIconRes
import com.example.flowlog.ui.component.displayCategory

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
            .setSmallIcon(notificationIcon(category))
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle("Flowlog timer")
            .setContentText(runningStatusText(category))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setWhen(startedAtMillis)
            .setUsesChronometer(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notifySafely(TIMER_NOTIFICATION_ID, notification)
    }

    fun clearRunningTimer() {
        NotificationManagerCompat.from(context).cancel(TIMER_NOTIFICATION_ID)
    }

    fun showSnackTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = SNACK_NOTIFICATION_ID,
            title = "\uAC04\uC2DD \uD0C0\uC774\uBA38",
            text = "\uC591\uCE58 \uC54C\uB9BC\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis,
            smallIcon = R.drawable.ic_notification
        )
    }

    fun showMealTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = MEAL_NOTIFICATION_ID,
            title = "\uC2DD\uC0AC \uD0C0\uC774\uBA38",
            text = "\uC591\uCE58 \uC54C\uB9BC\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis,
            smallIcon = R.drawable.ic_notification
        )
    }

    fun showBrushDoneTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = BRUSH_DONE_NOTIFICATION_ID,
            title = "\uC591\uCE58 3\uBD84 \uD0C0\uC774\uBA38",
            text = "\uC591\uCE58 \uB9C8\uBB34\uB9AC\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis,
            smallIcon = R.drawable.ic_notification
        )
    }

    fun showBrushEatTimer(endsAtMillis: Long) {
        showCountdownTimer(
            notificationId = BRUSH_EAT_NOTIFICATION_ID,
            title = "\uC591\uCE58 30\uBD84 \uD0C0\uC774\uBA38",
            text = "\uBA39\uC5B4\uB3C4 \uB418\uB294 \uC2DC\uAC04\uAE4C\uC9C0 \uB0A8\uC740 \uC2DC\uAC04",
            endsAtMillis = endsAtMillis,
            smallIcon = R.drawable.ic_notification
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
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle("\uC591\uCE58 \uD0C0\uC774\uBA38\uB97C \uC2DC\uC791\uD588\uC5B4\uC694")
            .setContentText(
                experimentText ?: if (isExperiment) {
                    "1\uBC88 \uC2E4\uD5D8\uC6A9 5\uCD08 \uD0C0\uC774\uBA38\uB97C \uC124\uC815\uD588\uC5B4\uC694."
                } else {
                    "3\uBD84 \uD0C0\uC774\uBA38\uC640 30\uBD84 \uD0C0\uC774\uBA38\uB97C \uD568\uAED8 \uC124\uC815\uD588\uC5B4\uC694."
                }
            )
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(BRUSH_START_NOTIFICATION_TIMEOUT_MILLIS)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notifySafely(BRUSH_START_NOTIFICATION_ID, notification)
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

    fun clearBrushStartNotification() {
        NotificationManagerCompat.from(context).cancel(BRUSH_START_NOTIFICATION_ID)
    }

    fun showFocusModeEnded() {
        if (!canPostNotifications()) return
        ensureDingNotificationChannel()

        val openPendingIntent = PendingIntent.getActivity(
            context,
            FOCUS_MODE_END_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ToothbrushReminderReceiver.DING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle("집중 시간이 끝났어요!")
            .setContentText("정말 잘했어요 👏 이제 휴식을 취해볼까요?")
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(ALERT_NOTIFICATION_TIMEOUT_MILLIS * 10)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setSound(KakaoStyleAlertPlayer.soundUri(context))
            .setVibrate(longArrayOf(0L, 500L, 250L, 500L))

        notifySafely(FOCUS_MODE_END_NOTIFICATION_ID, builder.build())
    }

    fun showRoutineGoalAlert(title: String, category: String) {
        if (!canPostNotifications()) return
        ensureDingNotificationChannel()

        // SLEEP 카테고리 루틴은 수면 타이머 본인의 알람이므로 SleepAlarmGuard를 우회
        val shouldSilence = if (category == "SLEEP") false
                            else !FocusModeStore.shouldPlayRegularSound(context) ||
                                 SleepAlarmGuard.shouldSilenceAlerts(context)
        if (shouldSilence) SleepAlarmGuard.ensureSilentNotificationChannel(context)

        val openPendingIntent = PendingIntent.getActivity(
            context,
            ROUTINE_GOAL_NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (shouldSilence) SleepAlarmGuard.SILENT_CHANNEL_ID
                        else ToothbrushReminderReceiver.DING_CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(notificationIcon(category))
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle(if (title.isNotBlank()) "$title 루틴 완료" else "루틴 완료")
            .setContentText("목표 시간에 도달했어요.")
            .setContentIntent(openPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(ALERT_NOTIFICATION_TIMEOUT_MILLIS)

        if (shouldSilence) {
            builder.setSilent(true).setDefaults(0).setVibrate(null).setSound(null)
        } else {
            builder
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setSound(KakaoStyleAlertPlayer.soundUri(context))
                .setVibrate(longArrayOf(0L, 500L, 250L, 500L))
        }

        notifySafely(ROUTINE_GOAL_NOTIFICATION_ID, builder.build())
    }

    fun showAutoButtonStarted(
        title: String,
        category: String,
        switchedFromTitle: String?,
        undoSnapshotId: String?
    ) {
        val text = if (switchedFromTitle.isNullOrBlank()) {
            "${title} 기록을 시작했어요."
        } else {
            "${switchedFromTitle} 기록을 종료하고 ${title} 기록을 시작했어요."
        }
        showAutoButtonNotification(
            notificationId = AUTO_BUTTON_START_NOTIFICATION_ID,
            title = "${title} 시간이 되어 기록을 전환했어요.",
            text = text,
            category = category,
            undoSnapshotId = undoSnapshotId,
            playSound = true
        )
    }

    fun showAutoButtonEnded(title: String, category: String) {
        showAutoButtonNotification(
            notificationId = AUTO_BUTTON_END_NOTIFICATION_ID,
            title = "${title} 기록이 자동으로 종료됐어요.",
            text = "반복 루틴 일정에 맞춰 기록을 마쳤어요.",
            category = category,
            playSound = true
        )
    }

    fun showAutoButtonUndoRestored(title: String, category: String) {
        showAutoButtonNotification(
            notificationId = AUTO_BUTTON_UNDO_NOTIFICATION_ID,
            title = "자동 전환을 되돌렸어요.",
            text = "${title} 기록을 다시 진행 중으로 복원했어요.",
            category = category
        )
    }

    fun showAutoButtonUndoUnavailable() {
        showAutoButtonNotification(
            notificationId = AUTO_BUTTON_UNDO_NOTIFICATION_ID,
            title = "되돌릴 수 없어요.",
            text = "이미 다른 기록이 진행 중이거나 되돌리기 시간이 지났어요.",
            category = "ETC"
        )
    }

    private fun showCountdownTimer(
        notificationId: Int,
        title: String,
        text: String,
        endsAtMillis: Long,
        smallIcon: Int
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
            .setSmallIcon(smallIcon)
            .setColor(NOTIFICATION_ICON_COLOR)
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

        notifySafely(notificationId, builder.build())
    }

    private fun showAutoButtonNotification(
        notificationId: Int,
        title: String,
        text: String,
        category: String,
        undoSnapshotId: String? = null,
        playSound: Boolean = false
    ) {
        if (!canPostNotifications()) return
        val effectivePlaySound = playSound && FocusModeStore.shouldPlayRegularSound(context)
        if (effectivePlaySound) {
            ensureDingNotificationChannel()
        } else {
            ensureNotificationChannel()
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (effectivePlaySound) ToothbrushReminderReceiver.DING_CHANNEL_ID else CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(notificationIcon(category))
            .setColor(NOTIFICATION_ICON_COLOR)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(if (effectivePlaySound) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(if (effectivePlaySound) NotificationCompat.DEFAULT_VIBRATE else 0)
        if (effectivePlaySound && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(KakaoStyleAlertPlayer.soundUri(context))
        }
        if (!undoSnapshotId.isNullOrBlank()) {
            val undoPendingIntent = PendingIntent.getBroadcast(
                context,
                AUTO_BUTTON_UNDO_REQUEST_CODE,
                Intent(context, AutoButtonUndoReceiver::class.java).apply {
                    putExtra(AutoButtonUndoReceiver.EXTRA_SNAPSHOT_ID, undoSnapshotId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                "되돌리기",
                undoPendingIntent
            )
        }

        notifySafely(notificationId, builder.build())
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(notificationId: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun notificationIcon(@Suppress("UNUSED_PARAMETER") category: String): Int =
        R.drawable.ic_notification

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
            "Flowlog timer app sound alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Timer alerts that play Flowlog's app sound"
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
            "WORK" -> "\uC5C5\uBB34\uB97C \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "COMPANY" -> "\uD68C\uC0AC \uD65C\uB3D9 \uC911\uC785\uB2C8\uB2E4!"
            "DEVELOPMENT" -> "\uAC1C\uBC1C\uC744 \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "MEAL" -> "\uC2DD\uC0AC\uB97C \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "EXERCISE" -> "\uC6B4\uB3D9\uC744 \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            "SLEEP" -> "\uC218\uBA74 \uC911\uC785\uB2C8\uB2E4!"
            "REST" -> "\uD734\uC2DD \uC911\uC785\uB2C8\uB2E4!"
            "SCHOOL" -> "\uD559\uAD50 \uD65C\uB3D9 \uC911\uC785\uB2C8\uB2E4!"
            "TODO" -> "\uD560\uC77C\uC744 \uD558\uB294 \uC911\uC785\uB2C8\uB2E4!"
            else -> "${displayCategory(category)} \uC911\uC785\uB2C8\uB2E4!"
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
        private const val AUTO_BUTTON_START_NOTIFICATION_ID = 2007
        private const val AUTO_BUTTON_END_NOTIFICATION_ID = 2008
        private const val AUTO_BUTTON_UNDO_REQUEST_CODE = 2009
        private const val AUTO_BUTTON_UNDO_NOTIFICATION_ID = 2010
        private const val ROUTINE_GOAL_NOTIFICATION_ID = 2011
        const val FOCUS_MODE_END_NOTIFICATION_ID = 2012
        private const val ALERT_NOTIFICATION_TIMEOUT_MILLIS = 3_000L
        private const val BRUSH_START_NOTIFICATION_TIMEOUT_MILLIS = 3_000L
        private val NOTIFICATION_ICON_COLOR = 0xFF4F5060.toInt()
    }
}
