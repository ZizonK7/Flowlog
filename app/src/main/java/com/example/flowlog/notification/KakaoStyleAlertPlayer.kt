package com.example.flowlog.notification

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import com.example.flowlog.R
import com.example.flowlog.data.local.FocusModeStore

object KakaoStyleAlertPlayer {
    fun soundUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.flowlog_ding}")

    fun audioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

    fun play(context: Context) {
        if (!FocusModeStore.shouldPlayRegularSound(context)) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

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
}
