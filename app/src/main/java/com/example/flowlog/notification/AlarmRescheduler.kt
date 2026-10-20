package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AlarmManager 예약을 다시 잡는 공통 묶음.
 * 부팅·앱 업데이트 수신기와 정확 알람 권한 변경 수신기가 같이 쓴다.
 * 각 묶음은 하나가 실패해도 나머지를 계속 잡는다.
 */
object AlarmRescheduler {
    private const val TAG = "AlarmRescheduler"

    /** 자동 버튼, 공부 계획 자동 시작, 양치·식사 알림. */
    suspend fun rescheduleAutoButtonGroup(context: Context) {
        runCatching { AutoButtonScheduler(context).rescheduleAll() }
            .onFailure { Log.e(TAG, "Failed to reschedule auto button alarms", it) }
        runCatching { StudyPlanAutoStartScheduler(context).rescheduleAll() }
            .onFailure { Log.e(TAG, "Failed to reschedule study plan alarms", it) }
        runCatching { ReminderScheduler(context).rescheduleAll() }
            .onFailure { Log.e(TAG, "Failed to reschedule toothbrush reminders", it) }
    }

    /** 타임테이블에 놓인 할 일 알림. */
    suspend fun reschedulePlannedTodoReminders(context: Context) {
        runCatching { PlannedTodoReminderScheduler(context).rescheduleAll() }
            .onFailure { Log.e(TAG, "Failed to reschedule planned todo reminders", it) }
    }

    suspend fun rescheduleAll(context: Context) {
        rescheduleAutoButtonGroup(context)
        reschedulePlannedTodoReminders(context)
    }
}

/**
 * 수신기에서 [block]을 백그라운드로 돌리고 끝나면 방송 처리를 마친다(goAsync).
 */
internal fun BroadcastReceiver.launchAlarmReschedule(
    context: Context,
    block: suspend (Context) -> Unit
) {
    val appContext = context.applicationContext
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block(appContext)
        } finally {
            pendingResult.finish()
        }
    }
}
