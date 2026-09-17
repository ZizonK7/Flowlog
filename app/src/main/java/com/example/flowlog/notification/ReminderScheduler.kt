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
import android.util.Log
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
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(legacyChannel)
            FlowlogAlertChannel.ensure(context)
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

    /**
     * 부팅·앱 업데이트로 AlarmManager 예약이 통째로 지워진 뒤, 아직 시각이 남은
     * 양치·식사 알람을 다시 건다. 이미 지난 기록은 정리한다.
     */
    fun rescheduleAll() {
        ensureNotificationChannel()

        val now = System.currentTimeMillis()
        pendingReminderPrefs().all.forEach { (key, value) ->
            val requestCode = key.toIntOrNull() ?: return@forEach
            val parts = (value as? String)?.split(RECORD_SEPARATOR).orEmpty()
            if (parts.size != RECORD_FIELD_COUNT) {
                forgetPendingReminder(requestCode)
                return@forEach
            }
            val activityId = parts[2].toLongOrNull()
            val triggerAtMillis = parts[3].toLongOrNull()
            if (activityId == null || triggerAtMillis == null || triggerAtMillis <= now) {
                forgetPendingReminder(requestCode)
                return@forEach
            }
            armReminder(
                category = parts[0],
                reminderType = parts[1],
                requestCode = requestCode,
                activityId = activityId,
                triggerAtMillis = triggerAtMillis
            )
        }
    }

    private fun scheduleBrushDoneTimer(
        requestCode: Int,
        delayMillis: Long
    ): Long {
        ensureNotificationChannel()

        val triggerAtMillis = System.currentTimeMillis() + delayMillis
        armReminder(
            category = "TOOTHBRUSH",
            reminderType = ToothbrushReminderReceiver.TYPE_BRUSH_DONE,
            requestCode = requestCode,
            activityId = triggerAtMillis,
            triggerAtMillis = triggerAtMillis
        )
        return triggerAtMillis
    }

    private fun cancelReminder(requestCode: Int) {
        forgetPendingReminder(requestCode)

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
        armReminder(category, reminderType, requestCode, activityId, triggerAtMillis)
        return triggerAtMillis
    }

    private fun armReminder(
        category: String,
        reminderType: String,
        requestCode: Int,
        activityId: Long,
        triggerAtMillis: Long
    ) {
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

        // 사용자가 기다리는 알람이므로 Doze/앱 대기 버킷에서 완전히 면제되는
        // setAlarmClock 을 쓴다. setExactAndAllowWhileIdle 은 권한이 없으면
        // 부정확 알람으로 강등돼 유지보수 창까지 밀린다.
        scheduleAlarmClock(triggerAtMillis, pendingIntent)
        rememberPendingReminder(requestCode, category, reminderType, activityId, triggerAtMillis)
    }

    private fun pendingReminderPrefs() = context.applicationContext
        .getSharedPreferences(PREFS_PENDING_REMINDERS, Context.MODE_PRIVATE)

    private fun rememberPendingReminder(
        requestCode: Int,
        category: String,
        reminderType: String,
        activityId: Long,
        triggerAtMillis: Long
    ) {
        val record = listOf(category, reminderType, activityId, triggerAtMillis)
            .joinToString(RECORD_SEPARATOR)
        pendingReminderPrefs().edit().putString(requestCode.toString(), record).apply()
    }

    private fun forgetPendingReminder(requestCode: Int) {
        pendingReminderPrefs().edit().remove(requestCode.toString()).apply()
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
            Log.w(TAG, "Falling back to an inexact alarm; it may be deferred in Doze")
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
            Log.w(TAG, "setAlarmClock denied; falling back to a weaker alarm", it)
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
        private const val TAG = "ReminderScheduler"
        private const val REQUEST_MEAL_TIMER = 3000
        private const val REQUEST_SNACK_TIMER = 3001
        private const val REQUEST_BRUSH_DONE_TIMER = 3002
        private const val REQUEST_BRUSH_EAT_TIMER = 3003
        private const val REQUEST_OPEN_APP_FROM_ALARM_INFO = 3004
        private const val REQUEST_BRUSH_DONE_EXPERIMENT = 3012
        private const val REQUEST_BRUSH_EAT_EXPERIMENT = 3013
        private const val BRUSH_DONE_DELAY_MILLIS = 3L * 60L * 1000L
        private const val EXPERIMENT_DELAY_MILLIS = 5L * 1000L
        private const val PREFS_PENDING_REMINDERS = "flowlog_pending_reminders"
        private const val RECORD_SEPARATOR = "|"
        private const val RECORD_FIELD_COUNT = 4
    }
}
