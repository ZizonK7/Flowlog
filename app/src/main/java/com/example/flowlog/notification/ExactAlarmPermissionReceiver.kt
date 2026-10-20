package com.example.flowlog.notification

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 사용자가 "알람 및 리마인더"(정확 알람) 권한을 켜면 Android 12 이상이 이 방송을 보낸다.
 * 권한이 없을 때 부정확 알람으로 잡아 둔 예약을 정확 알람으로 다시 잡는다.
 * 시스템이 보내는 브로드캐스트라 매니페스트에서 `exported="true"` 여야 전달된다.
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        launchAlarmReschedule(context) { AlarmRescheduler.rescheduleAll(it) }
    }
}
