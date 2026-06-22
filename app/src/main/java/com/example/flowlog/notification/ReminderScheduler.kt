package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.flowlog.MainActivity
import com.example.flowlog.data.model.ActivitySession

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val activityTimerNotifier = ActivityTimerNotifier(context)

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) runCatching {
            val legacyChannel = NotificationChannel(
                ToothbrushReminderReceiver.CHANNEL_ID,
                "Flowlog timer alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Snack and toothbrush timer alerts"
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
            }
            val dingChannel = NotificationChannel(
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
            notificationManager.createNotificationChannel(legacyChannel)
            notificationManager.createNotificationChannel(dingChannel)
            notificationManager.deleteNotificationChannel(LEGACY_BRUSH_ALARM_CHANNEL_ID)
        }
    }

    fun scheduleToothbrushReminder(activity: ActivitySession): Long? {
        if (activity.category != "MEAL") return null

        cancelSnackReminder()
        cancelMealReminder()

        val triggerAtMillis = scheduleReminder(
            category = activity.category,
            reminderType = ToothbrushReminderReceiver.TYPE_TOOTHBRUSH,
            reminderDelayMillis = 30L * 60L * 1000L,
            requestCode = REQUEST_MEAL_TIMER,
            activityId = activity.id
        )
        activityTimerNotifier.showMealTimer(triggerAtMillis)
        return triggerAtMillis
    }

    fun scheduleSnackReminder(): Long {
        cancelBrushTimers()
        cancelSnackReminder()
        cancelMealReminder()

        val now = System.currentTimeMillis()
        val triggerAtMillis = scheduleReminder(
            category = "SNACK",
            reminderType = ToothbrushReminderReceiver.TYPE_TOOTHBRUSH,
            reminderDelayMillis = 30L * 60L * 1000L,
            requestCode = REQUEST_SNACK_TIMER,
            activityId = now
        )
        activityTimerNotifier.showSnackTimer(triggerAtMillis)
        return triggerAtMillis
    }

    fun scheduleBrushTimers(): Pair<Long, Long> {
        cancelSnackReminder()
        cancelMealReminder()
        cancelBrushTimers()

        val brushDoneAtMillis = scheduleBrushDoneTimer(
            requestCode = REQUEST_BRUSH_DONE_TIMER,
            delayMillis = BRUSH_DONE_DELAY_MILLIS
        )
        val eatAllowedAtMillis = scheduleReminder(
            category = "TOOTHBRUSH",
            reminderType = ToothbrushReminderReceiver.TYPE_EAT_ALLOWED,
            reminderDelayMillis = 30L * 60L * 1000L,
            requestCode = REQUEST_BRUSH_EAT_TIMER
        )
        activityTimerNotifier.showBrushDoneTimer(brushDoneAtMillis)
        activityTimerNotifier.showBrushEatTimer(eatAllowedAtMillis)
        return Pair(brushDoneAtMillis, eatAllowedAtMillis)
    }

    fun scheduleBrushDoneExperiment() {
        cancelReminder(REQUEST_BRUSH_DONE_EXPERIMENT)

        val brushDoneAtMillis = scheduleBrushDoneTimer(
            requestCode = REQUEST_BRUSH_DONE_EXPERIMENT,
            delayMillis = EXPERIMENT_DELAY_MILLIS
        )
        activityTimerNotifier.showBrushDoneTimer(brushDoneAtMillis)
        activityTimerNotifier.showBrushStartNotification(isExperiment = true)
    }

    fun scheduleEatAllowedExperiment() {
        cancelReminder(REQUEST_BRUSH_EAT_EXPERIMENT)

        val now = System.currentTimeMillis()
        val eatAllowedAtMillis = scheduleReminder(
            category = "TOOTHBRUSH",
            reminderType = ToothbrushReminderReceiver.TYPE_EAT_ALLOWED,
            reminderDelayMillis = EXPERIMENT_DELAY_MILLIS,
            requestCode = REQUEST_BRUSH_EAT_EXPERIMENT,
            activityId = now
        )
        activityTimerNotifier.showBrushEatTimer(eatAllowedAtMillis)
        activityTimerNotifier.showBrushStartNotification(
            isExperiment = true,
            experimentText = "2\uBC88 \uC2E4\uD5D8\uC6A9 5\uCD08 \uD0C0\uC774\uBA38\uB97C \uC124\uC815\uD588\uC5B4\uC694."
        )
    }

    private fun scheduleBrushDoneTimer(
        requestCode: Int,
        delayMillis: Long
    ): Long {
        ensureNotificationChannel()

        val triggerAtMillis = System.currentTimeMillis() + delayMillis
        val intent = Intent(context, ToothbrushReminderReceiver::class.java).apply {
            putExtra(ToothbrushReminderReceiver.EXTRA_CATEGORY, "TOOTHBRUSH")
            putExtra(ToothbrushReminderReceiver.EXTRA_REMINDER_TYPE, ToothbrushReminderReceiver.TYPE_BRUSH_DONE)
            putExtra(ToothbrushReminderReceiver.EXTRA_ACTIVITY_ID, triggerAtMillis)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarmClock(triggerAtMillis, pendingIntent)
        return triggerAtMillis
    }

    private fun cancelReminder(requestCode: Int) {
        val intent = Intent(context, ToothbrushReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleReminder(
        category: String,
        reminderType: String,
        reminderDelayMillis: Long,
        requestCode: Int,
        activityId: Long = System.currentTimeMillis()
    ): Long {
        ensureNotificationChannel()

        val triggerAtMillis = System.currentTimeMillis() + reminderDelayMillis
        val intent = Intent(context, ToothbrushReminderReceiver::class.java).apply {
            putExtra(ToothbrushReminderReceiver.EXTRA_CATEGORY, category)
            putExtra(ToothbrushReminderReceiver.EXTRA_REMINDER_TYPE, reminderType)
            putExtra(ToothbrushReminderReceiver.EXTRA_ACTIVITY_ID, activityId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerAtMillis, pendingIntent)
        return triggerAtMillis
    }

    fun cancelMealReminder() {
        cancelReminder(REQUEST_MEAL_TIMER)
        activityTimerNotifier.clearMealTimer()
    }

    fun cancelSnackReminder() {
        cancelReminder(REQUEST_SNACK_TIMER)
        activityTimerNotifier.clearSnackTimer()
    }

    fun cancelBrushEatTimer() {
        cancelReminder(REQUEST_BRUSH_EAT_TIMER)
        activityTimerNotifier.clearBrushEatTimer()
    }

    fun cancelBrushTimers() {
        cancelReminder(REQUEST_BRUSH_DONE_TIMER)
        cancelReminder(REQUEST_BRUSH_EAT_TIMER)
        activityTimerNotifier.clearBrushDoneTimer()
        activityTimerNotifier.clearBrushEatTimer()
    }

    private fun scheduleAlarm(
        triggerAtMillis: Long,
        alarmPendingIntent: PendingIntent
    ) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmPendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmPendingIntent
                )
            }
        }.recoverCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmPendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    alarmPendingIntent
                )
            }
        }.recoverCatching {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                alarmPendingIntent
            )
        }.getOrThrow()
    }

    private fun scheduleAlarmClock(
        triggerAtMillis: Long,
        alarmPendingIntent: PendingIntent
    ) {
        runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, alarmClockInfoPendingIntent()),
                alarmPendingIntent
            )
        }.recoverCatching {
            scheduleAlarm(triggerAtMillis, alarmPendingIntent)
        }.getOrThrow()
    }

    private fun alarmClockInfoPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP_FROM_ALARM_INFO,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_MEAL_TIMER = 3000
        private const val REQUEST_SNACK_TIMER = 3001
        private const val REQUEST_BRUSH_DONE_TIMER = 3002
        private const val REQUEST_BRUSH_EAT_TIMER = 3003
        private const val REQUEST_OPEN_APP_FROM_ALARM_INFO = 3004
        private const val REQUEST_BRUSH_DONE_EXPERIMENT = 3012
        private const val REQUEST_BRUSH_EAT_EXPERIMENT = 3013
        private const val BRUSH_DONE_DELAY_MILLIS = 3L * 60L * 1000L
        private const val EXPERIMENT_DELAY_MILLIS = 5L * 1000L
        private const val LEGACY_BRUSH_ALARM_CHANNEL_ID = "flowlog_brush_alarm"
    }
}
