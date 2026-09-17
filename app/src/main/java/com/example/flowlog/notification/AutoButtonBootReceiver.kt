package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 부팅과 앱 업데이트 시 AlarmManager 예약이 전부 지워지므로 다시 등록한다.
 * 시스템이 보내는 브로드캐스트라 매니페스트에서 `exported="true"` 여야 전달된다.
 */
class AutoButtonBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                runCatching { AutoButtonScheduler(appContext).rescheduleAll() }
                    .onFailure { Log.e(TAG, "Failed to reschedule auto button alarms", it) }
                runCatching { StudyPlanAutoStartScheduler(appContext).rescheduleAll() }
                    .onFailure { Log.e(TAG, "Failed to reschedule study plan alarms", it) }
                runCatching { ReminderScheduler(appContext).rescheduleAll() }
                    .onFailure { Log.e(TAG, "Failed to reschedule toothbrush reminders", it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AutoButtonBoot"
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
