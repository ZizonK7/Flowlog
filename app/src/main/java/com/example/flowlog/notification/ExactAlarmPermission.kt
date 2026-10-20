package com.example.flowlog.notification

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * 정확 알람 권한의 현재 상태, "이미 물었음" 기록, 시스템 설정 화면 열기.
 * 물을지 말지의 판단은 [ExactAlarmPromptPolicy]에 있다.
 */
object ExactAlarmPermission {
    private const val PREFS = "exact_alarm_permission"
    private const val KEY_PROMPTED = "prompted"

    fun status(context: Context): ExactAlarmStatus {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        return ExactAlarmPromptPolicy.status(Build.VERSION.SDK_INT, canExact)
    }

    fun shouldPrompt(context: Context): Boolean =
        ExactAlarmPromptPolicy.shouldPrompt(status(context), wasPrompted(context))

    fun wasPrompted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) {
        prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    /** 이 앱의 "알람 및 리마인더" 설정 화면을 연다. Android 12 미만이거나 열 수 없으면 false. */
    fun openSettings(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.isSuccess
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
