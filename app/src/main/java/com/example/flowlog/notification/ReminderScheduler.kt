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
import android.os.Handler
import android.os.Looper
import com.example.flowlog.MainActivity
import com.example.flowlog.data.model.ActivitySession

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val activityTimerNotifier = ActivityTimerNotifier(context)

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) runCatching {
            val channel = NotificationChannel(
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
            notificationManager.createNotificationChannel(channel)
        }

        runCatching {
            BrushAlarmService.ensureNotificationChannel(context)
        }
    }

    fun scheduleToothbrushReminder(activity: ActivitySession) {
        if (activity.category != "MEAL") return

        scheduleReminder(
            category = activity.category,
            reminderType = ToothbrushReminderReceiver.TYPE_TOOTHBRUSH,
            reminderDelayMillis = 30L * 60L * 1000L,
            requestCode = activity.id.toInt(),
            activityId = activity.id
        )
    }

    fun scheduleSnackReminder() {
        cancelBrushTimers()
        cancelSnackReminder()

        val now = System.currentTimeMillis()
        val triggerAtMillis = scheduleReminder(
            category = "SNACK",
            reminderType = ToothbrushReminderReceiver.TYPE_TOOTHBRUSH,
            reminderDelayMillis = 30L * 60L * 1000L,
            requestCode = REQUEST_SNACK_TIMER,
            activityId = now
        )
        activityTimerNotifier.showSnackTimer(triggerAtMillis)
    }

    fun scheduleBrushTimers() {
        cancelSnackReminder()
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
    }

    fun scheduleBrushDoneExperiment() {
        cancelReminder(REQUEST_BRUSH_DONE_EXPERIMENT)

        val brushDoneAtMillis = scheduleBrushDoneTimer(
            requestCode = REQUEST_BRUSH_DONE_EXPERIMENT,
            delayMillis = EXPERIMENT_DELAY_MILLIS
        )
        activityTimerNotifier.showBrushDoneTimer(brushDoneAtMillis)
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
    }

    private fun scheduleBrushDoneTimer(
        requestCode: Int,
        delayMillis: Long
    ): Long {
        ensureNotificationChannel()

        val triggerAtMillis = System.currentTimeMillis() + delayMillis
        val alarmScreenIntent = Intent(context, BrushAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val alarmScreenPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            alarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerAtMillis, alarmScreenPendingIntent, alarmScreenPendingIntent)
        scheduleInProcessBrushDoneAlarm(requestCode, triggerAtMillis)
        return triggerAtMillis
    }

    private fun scheduleInProcessBrushDoneAlarm(
        requestCode: Int,
        triggerAtMillis: Long
    ) {
        cancelInProcessAlarm(requestCode)

        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(1L)
        val appContext = context.applicationContext
        val runnable = Runnable {
            inProcessAlarms.remove(requestCode)
            cancelActivityAlarm(requestCode)
            appContext.startActivity(
                Intent(appContext, BrushAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }
        inProcessAlarms[requestCode] = runnable
        handler.postDelayed(runnable, delayMillis)
    }

    private fun cancelActivityAlarm(requestCode: Int) {
        val intent = Intent(context, BrushAlarmActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun cancelReminder(requestCode: Int) {
        cancelInProcessAlarm(requestCode)
        cancelActivityAlarm(requestCode)

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
        val openPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerAtMillis, openPendingIntent, pendingIntent)
        scheduleInProcessAlarm(requestCode, triggerAtMillis, intent)
        return triggerAtMillis
    }

    fun cancelSnackReminder() {
        cancelReminder(REQUEST_SNACK_TIMER)
        activityTimerNotifier.clearSnackTimer()
    }

    fun cancelBrushTimers() {
        cancelReminder(REQUEST_BRUSH_DONE_TIMER)
        cancelReminder(REQUEST_BRUSH_EAT_TIMER)
        activityTimerNotifier.clearBrushDoneTimer()
        activityTimerNotifier.clearBrushEatTimer()
        context.stopService(Intent(context, BrushAlarmService::class.java))
    }

    private fun scheduleAlarm(
        triggerAtMillis: Long,
        openPendingIntent: PendingIntent,
        alarmPendingIntent: PendingIntent
    ) {
        runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, openPendingIntent),
                alarmPendingIntent
            )
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

    private fun scheduleInProcessAlarm(
        requestCode: Int,
        triggerAtMillis: Long,
        intent: Intent
    ) {
        cancelInProcessAlarm(requestCode)

        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(1L)
        val appContext = context.applicationContext
        val alarmIntent = Intent(intent)
        val runnable = Runnable {
            inProcessAlarms.remove(requestCode)
            cancelSystemAlarm(requestCode)
            appContext.sendBroadcast(alarmIntent)
        }
        inProcessAlarms[requestCode] = runnable
        handler.postDelayed(runnable, delayMillis)
    }

    private fun cancelInProcessAlarm(requestCode: Int) {
        inProcessAlarms.remove(requestCode)?.let { handler.removeCallbacks(it) }
    }

    private fun cancelSystemAlarm(requestCode: Int) {
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

    companion object {
        private const val REQUEST_SNACK_TIMER = 3001
        private const val REQUEST_BRUSH_DONE_TIMER = 3002
        private const val REQUEST_BRUSH_EAT_TIMER = 3003
        private const val REQUEST_BRUSH_DONE_EXPERIMENT = 3012
        private const val REQUEST_BRUSH_EAT_EXPERIMENT = 3013
        private const val BRUSH_DONE_DELAY_MILLIS = 3L * 60L * 1000L
        private const val EXPERIMENT_DELAY_MILLIS = 5L * 1000L
        private val handler = Handler(Looper.getMainLooper())
        private val inProcessAlarms = mutableMapOf<Int, Runnable>()
    }
}
