package com.example.flowlog.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 부팅과 앱 업데이트 시 AlarmManager 예약이 전부 지워지므로 다시 등록한다.
 * 시스템이 보내는 브로드캐스트라 매니페스트에서 `exported="true"` 여야 전달된다.
 */
class AutoButtonBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in HANDLED_ACTIONS) return
        launchAlarmReschedule(context) { AlarmRescheduler.rescheduleAutoButtonGroup(it) }
    }

    private companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}
