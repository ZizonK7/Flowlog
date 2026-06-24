package com.example.flowlog.data.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.notification.StudyPlanAutoStartScheduler
import com.example.flowlog.notification.FirebaseSyncReceiver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

data class CalendarPullOutcome(
    val pulledCalendarTodoCount: Int = 0,
    val pulledLectureInfoCount: Int = 0,
    val pulledGeneralEventCount: Int = 0,
    val failed: Boolean = false
)

data class SyncOutcome(
    val attemptedCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val deferred: Boolean = false,
    val calendarPull: CalendarPullOutcome? = null
)

object FirebaseSyncAlarmScheduler {
    private const val REQUEST_CODE_MIDNIGHT_SYNC = 9401

    fun scheduleNextMidnightSync(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = midnightSyncIntent(appContext)
        val triggerAtMillis = nextMidnightMillis()

        // Android 12(S) 이상에서 SCHEDULE_EXACT_ALARM 권한이 없으면 SecurityException 발생.
        // 미드나이트 sync는 정확성보다 안정성이 우선이므로 권한 없을 때 inexact로 폴백.
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        runCatching {
            when {
                canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT -> {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
                else -> {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            }
        }
    }

    fun cancelMidnightSync(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = midnightSyncIntent(appContext)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun midnightSyncIntent(context: Context): PendingIntent {
        val intent = Intent(context, FirebaseSyncReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_SYNC
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT_SYNC,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextMidnightMillis(): Long {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private const val ACTION_MIDNIGHT_SYNC = "com.example.flowlog.action.MIDNIGHT_SYNC"
}

class FirebaseSyncCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val dataSource = FirebaseSyncDataSource(appContext)

    suspend fun syncAll(userId: String): SyncOutcome {
        return globalSyncMutex.withLock {
            dataSource.syncAll(userId)
        }
    }

    suspend fun syncEligible(userId: String): SyncOutcome {
        return globalSyncMutex.withLock {
            val activeTimer = TimerStateStore.getActiveTimer(appContext)
            if (activeTimer?.status == TimerStatus.RUNNING) {
                return@withLock SyncOutcome(deferred = true)
            }
            dataSource.syncEligible(userId)
        }
    }

    /**
     * 자정 자동 동기화용: 업로드 후 오늘 calendar pull.
     * 업로드는 타이머 실행 중이면 deferred되지만, calendar pull은 독립적으로 항상 실행한다.
     */
    suspend fun syncEligibleWithTodayCalendar(userId: String): SyncOutcome {
        val uploadResult = syncEligible(userId)
        val pullResult = runCatching {
            FirebaseCalendarPullDataSource(appContext).pullTodayCalendar(userId)
        }.getOrElse { CalendarPullOutcome(failed = true) }
        return uploadResult.copy(calendarPull = pullResult)
    }

    /**
     * 개발자 버튼용: 전체 PENDING 업로드 후 오늘 calendar pull.
     * 타이머 체크 없이 강제 실행한다.
     */
    suspend fun syncAllWithTodayCalendar(userId: String): SyncOutcome {
        val uploadResult = syncAll(userId)
        val pullResult = runCatching {
            FirebaseCalendarPullDataSource(appContext).pullTodayCalendar(userId)
        }.getOrElse { CalendarPullOutcome(failed = true) }
        return uploadResult.copy(calendarPull = pullResult)
    }

    companion object {
        private val globalSyncMutex = Mutex()
    }
}
