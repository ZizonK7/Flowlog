package com.example.flowlog.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.example.flowlog.data.local.FocusModeStore

object KakaoStyleAlertPlayer {
    /**
     * 리소스 **이름** 기반 URI를 쓴다. `R.raw.flowlog_ding` 숫자 ID로 만들면 리소스를
     * 추가/삭제할 때마다 값이 바뀌는데, 알림 채널은 이 URI 문자열을 OS에 영구 저장하므로
     * 앱 업데이트 후 채널이 사라진 리소스를 가리켜 무음/기본음으로 폴백한다.
     */
    fun soundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/raw/$SOUND_RESOURCE_NAME")

    fun audioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    fun play(context: Context) {
        if (!FocusModeStore.shouldPlayRegularSound(context)) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) return

        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes())
                setDataSource(context.applicationContext, soundUri(context))
                setOnCompletionListener { player ->
                    player.release()
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    true
                }
                prepare()
                start()
            }
        }
    }

    private const val SOUND_RESOURCE_NAME = "flowlog_ding"
}
