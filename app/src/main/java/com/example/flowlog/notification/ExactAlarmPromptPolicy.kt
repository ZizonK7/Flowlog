package com.example.flowlog.notification

/**
 * 정확 알람(알람 및 리마인더) 권한의 상태.
 * Android 12 미만에는 이 권한 개념이 없어 [NOT_REQUIRED]다.
 */
enum class ExactAlarmStatus { NOT_REQUIRED, ALLOWED, DENIED }

/**
 * 정확 알람 권한을 사용자에게 물을지 정하는 순수 규칙.
 * 권한이 꺼져 있고 아직 한 번도 묻지 않았을 때만 설명 다이얼로그를 띄운다.
 * 한 번 답하면(설정으로 가든 나중에를 누르든) 다시 묻지 않고, 설정 창에서 언제든 켤 수 있다.
 */
object ExactAlarmPromptPolicy {
    /** Android 12 (API 31, S)부터 정확 알람에 사용자 허용이 필요하다. */
    const val FIRST_SDK_REQUIRING_PERMISSION = 31

    fun status(sdkInt: Int, canScheduleExactAlarms: Boolean): ExactAlarmStatus = when {
        sdkInt < FIRST_SDK_REQUIRING_PERMISSION -> ExactAlarmStatus.NOT_REQUIRED
        canScheduleExactAlarms -> ExactAlarmStatus.ALLOWED
        else -> ExactAlarmStatus.DENIED
    }

    fun shouldPrompt(status: ExactAlarmStatus, alreadyAsked: Boolean): Boolean =
        status == ExactAlarmStatus.DENIED && !alreadyAsked
}
