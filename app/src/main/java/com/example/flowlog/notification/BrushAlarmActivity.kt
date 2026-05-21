package com.example.flowlog.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.flowlog.MainActivity

class BrushAlarmActivity : Activity() {
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        wakeScreenForAlarm()
        ActivityTimerNotifier(this).clearBrushDoneTimer()
        setContentView(buildContentView())
        startAlarmSound()
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Suppress("DEPRECATION")
    private fun wakeScreenForAlarm() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Flowlog:BrushAlarmWakeLock"
        ).apply {
            setReferenceCounted(false)
            acquire(ALARM_WAKE_LOCK_TIMEOUT_MILLIS)
        }
    }

    private fun buildContentView(): LinearLayout {
        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(32), dp(24), dp(32))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(0xFF073B3A.toInt(), 0xFF102027.toInt())
            )

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(24), dp(28), dp(24), dp(24))
                background = GradientDrawable().apply {
                    setColor(0x22FFFFFF)
                    cornerRadius = dp(28).toFloat()
                    setStroke(dp(1), 0x33FFFFFF)
                }

                addView(TextView(context).apply {
                    text = "\uC591\uCE58 \uC644\uB8CC \uC2DC\uAC04"
                    setTextColor(0xFFB2DFDB.toInt())
                    textSize = 16f
                    letterSpacing = 0.08f
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                })

                addView(TextView(context).apply {
                    text = "3:00"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 64f
                    gravity = Gravity.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                    includeFontPadding = false
                    setPadding(0, dp(16), 0, dp(10))
                })

                addView(TextView(context).apply {
                    text = "\uC591\uCE58\uB97C \uB9C8\uBB34\uB9AC\uD560 \uC2DC\uAC04\uC774\uC5D0\uC694."
                    setTextColor(0xFFE0F2F1.toInt())
                    textSize = 18f
                    gravity = Gravity.CENTER
                    setPadding(0, 0, 0, dp(28))
                })

                addView(Button(context).apply {
                    text = "\uB044\uAE30"
                    textSize = 20f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(0xFF083B3A.toInt())
                    background = GradientDrawable().apply {
                        setColor(0xFFFFFFFF.toInt())
                        cornerRadius = dp(18).toFloat()
                    }
                    setPadding(0, dp(12), 0, dp(12))
                    setOnClickListener {
                        stopAlarm()
                        finish()
                    }
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(58)
                ))

                addView(Button(context).apply {
                    text = "Flowlog \uC5F4\uAE30"
                    textSize = 16f
                    setTextColor(0xFFFFFFFF.toInt())
                    background = GradientDrawable().apply {
                        setColor(0x00000000)
                        cornerRadius = dp(16).toFloat()
                        setStroke(dp(1), 0x55FFFFFF)
                    }
                    setOnClickListener {
                        stopAlarm()
                        startActivity(Intent(context, MainActivity::class.java))
                        finish()
                    }
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
                ).apply {
                    topMargin = dp(12)
                })
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
                topMargin = dp(12)
                bottomMargin = dp(12)
            })

            addView(TextView(context).apply {
                text = "\uC54C\uB78C\uC740 \uB044\uAE30\uB97C \uB204\uB974\uBA74 \uBA48\uCD94\uC5B4\uC694."
                setTextColor(0x99FFFFFF.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            })
        }
    }

    private fun startAlarmSound() {
        KakaoStyleAlertPlayer.play(this)
    }

    private fun stopAlarm() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
        startService(
            Intent(this, BrushAlarmService::class.java).apply {
                action = BrushAlarmService.ACTION_STOP
            }
        )
    }

    companion object {
        private const val ALARM_WAKE_LOCK_TIMEOUT_MILLIS = 10L * 60L * 1000L
    }
}
