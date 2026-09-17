package com.example.flowlog.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.util.Log

/**
 * Flowlog 앱 사운드로 울리는 알림 채널을 한 곳에서 정의한다.
 *
 * 채널 설정(사운드·진동·DND 우회)은 생성된 뒤에는 **변경할 수 없다**. 코드에서 설정을
 * 바꿔도 이미 만들어진 채널에는 반영되지 않으므로, 바꿀 때마다 [ID]의 버전을 올리고
 * 이전 버전을 [LEGACY_IDS]에 넣어 삭제해야 한다.
 */
object FlowlogAlertChannel {
    /** v10: 사운드 URI를 이름 기반으로 교체하고 DND 우회를 켜면서 버전을 올렸다. */
    const val ID = "flowlog_timer_alerts_app_sound_v10"

    private const val TAG = "FlowlogAlertChannel"
    private const val NAME = "Flowlog timer app sound alerts"
    private const val DESCRIPTION = "Timer alerts that play Flowlog's app sound"

    private val LEGACY_IDS = listOf(
        "flowlog_timer_alerts_app_sound_v9",
        "flowlog_brush_alarm"
    )

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(ID, NAME, NotificationManager.IMPORTANCE_HIGH).apply {
            description = DESCRIPTION
            setSound(
                KakaoStyleAlertPlayer.soundUri(context),
                KakaoStyleAlertPlayer.audioAttributes()
            )
            enableVibration(true)
            // 취침 모드/방해 금지가 켜져 있어도 울려야 하는 알람성 알림이다.
            // 알림 정책 접근 권한이 없으면 시스템이 조용히 무시한다.
            setBypassDnd(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                setVibrationEffect(
                    VibrationEffect.createWaveform(
                        FlowlogVibrationPatterns.alert(),
                        FlowlogVibrationPatterns.alertAmplitudes(),
                        -1
                    )
                )
            } else {
                setVibrationPattern(FlowlogVibrationPatterns.alert())
            }
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        LEGACY_IDS.forEach { legacyId ->
            runCatching { notificationManager.deleteNotificationChannel(legacyId) }
        }

        // setBypassDnd 는 알림 정책 접근 권한이 있을 때만 반영되고, 채널 설정은 불변이라
        // 나중에 권한을 켜도 소급 적용되지 않는다. 어긋나면 방해 금지 중 알람이 묵음이 된다.
        if (notificationManager.getNotificationChannel(ID)?.canBypassDnd() == false) {
            Log.w(
                TAG,
                "Alert channel cannot bypass DND. Grant notification policy access, " +
                    "then bump the channel version so the setting can take effect."
            )
        }
    }
}
