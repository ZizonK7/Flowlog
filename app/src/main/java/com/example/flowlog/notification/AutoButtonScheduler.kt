package com.example.flowlog.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.AutoButtonScheduleEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class AutoButtonScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val dao = FlowlogDatabase.getInstance(appContext).autoButtonScheduleDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    suspend fun rescheduleAll() {
        withContext(Dispatchers.IO) {
            dao.getActiveSchedules(userId).forEach { schedule ->
                cancel(schedule.scheduleId)
                scheduleSchedule(schedule)
            }
        }
    }

    suspend fun reschedule(scheduleId: String) {
        withContext(Dispatchers.IO) {
            cancel(scheduleId)
            val schedule = dao.getSchedule(scheduleId) ?: return@withContext
            if (!schedule.isEnabled || schedule.isDeleted) return@withContext
            scheduleSchedule(schedule)
        }
    }

    fun cancel(scheduleId: String) {
        cancelPendingIntent(scheduleId, AutoButtonAlarmReceiver.ACTION_START)
        cancelPendingIntent(scheduleId, AutoButtonAlarmReceiver.ACTION_END)
    }

    private suspend fun scheduleSchedule(schedule: AutoButtonScheduleEntity) {
        val now = System.currentTimeMillis()
        val nextStart = nextOccurrence(schedule, now, schedule.startMinuteOfDay, skipToday = true)
        if (nextStart != null) {
            scheduleAlarm(schedule, AutoButtonAlarmReceiver.ACTION_START, nextStart)
        }

        val todayEnd = occurrenceToday(schedule, now, schedule.endMinuteOfDay)
        if (todayEnd != null && todayEnd > now && dao.getSkipDate(schedule.scheduleId, dateKey(todayEnd)) == null) {
            scheduleAlarm(schedule, AutoButtonAlarmReceiver.ACTION_END, todayEnd)
        } else {
            val nextEnd = nextOccurrence(schedule, now, schedule.endMinuteOfDay, skipToday = true)
            if (nextEnd != null) {
                scheduleAlarm(schedule, AutoButtonAlarmReceiver.ACTION_END, nextEnd)
            }
        }
    }

    private suspend fun nextOccurrence(
        schedule: AutoButtonScheduleEntity,
        now: Long,
        minuteOfDay: Int,
        skipToday: Boolean
    ): Long? {
        repeat(8) { offset ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
                set(Calendar.MINUTE, minuteOfDay % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val triggerAt = calendar.timeInMillis
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val scheduledToday = offset == 0
            val shouldSkipToday = skipToday && scheduledToday && triggerAt <= now
            if (!shouldSkipToday &&
                schedule.isValidForDate(dateKey(triggerAt)) &&
                schedule.repeatDaysMask and (1 shl dayOfWeek) != 0 &&
                triggerAt > now &&
                dao.getSkipDate(schedule.scheduleId, dateKey(triggerAt)) == null
            ) {
                return triggerAt
            }
        }
        return null
    }

    private fun occurrenceToday(
        schedule: AutoButtonScheduleEntity,
        now: Long,
        minuteOfDay: Int
    ): Long? {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (!schedule.isValidForDate(dateKey(calendar.timeInMillis))) return null
        if (schedule.repeatDaysMask and (1 shl dayOfWeek) == 0) return null
        return calendar.timeInMillis
    }

    private fun AutoButtonScheduleEntity.isValidForDate(dateKey: Long): Boolean {
        if (source != SOURCE_CALENDAR) return true
        val dateKeys = sourceDateKeysCsv.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
        if (dateKeys.isNotEmpty()) return dateKey in dateKeys
        return sourceDateKey == null || sourceDateKey == dateKey
    }

    private fun scheduleAlarm(schedule: AutoButtonScheduleEntity, action: String, triggerAt: Long) {
        val pendingIntent = buildPendingIntent(schedule.scheduleId, action, triggerAt, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                throw SecurityException("Exact alarms are not allowed")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.recoverCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }.recoverCatching {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelPendingIntent(scheduleId: String, action: String) {
        val pendingIntent = buildPendingIntent(scheduleId, action, scheduledAt = 0L, flags = PendingIntent.FLAG_NO_CREATE)
            ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun buildPendingIntent(
        scheduleId: String,
        action: String,
        scheduledAt: Long,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(appContext, AutoButtonAlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AutoButtonAlarmReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(AutoButtonAlarmReceiver.EXTRA_SCHEDULED_AT, scheduledAt)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(scheduleId, action),
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(scheduleId: String, action: String): Int {
        val type = if (action == AutoButtonAlarmReceiver.ACTION_START) 1 else 2
        return 31 * scheduleId.hashCode() + type
    }

    companion object {
        private const val SOURCE_CALENDAR = "CALENDAR"

        fun dateKey(timestamp: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
