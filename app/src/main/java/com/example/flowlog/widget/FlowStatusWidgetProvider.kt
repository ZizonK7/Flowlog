package com.example.flowlog.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.data.local.ActiveTimerState
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.sin

class FlowStatusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            updateAll(context)
        }
    }

    override fun onEnabled(context: Context) {
        updateAll(context)
    }

    override fun onDisabled(context: Context) {
        cancelNextRefresh(context)
    }

    private fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val activeTimer = TimerStateStore.getActiveTimer(context)
        val views = buildRemoteViews(context, activeTimer)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, views)
        }

        if (activeTimer == null || activeTimer.status == TimerStatus.PAUSED || appWidgetIds.isEmpty()) {
            cancelNextRefresh(context)
        } else {
            scheduleNextRefresh(context)
        }
    }

    private fun buildRemoteViews(
        context: Context,
        activeTimer: ActiveTimerState?
    ): RemoteViews {
        val openIntent = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_OPEN_SCREEN, MainActivity.SCREEN_HOME)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return RemoteViews(context.packageName, R.layout.flow_status_widget).apply {
            setOnClickPendingIntent(R.id.flow_status_widget_root, openIntent)

            if (activeTimer != null) {
                resetEmptyTracker(context)
                setImageViewBitmap(
                    R.id.flow_status_widget_image,
                    FlowStatusWidgetRenderer.render(activeTimer)
                )
                setViewVisibility(R.id.flow_status_empty_container, View.GONE)
                setViewVisibility(R.id.flow_status_widget_text_row, View.VISIBLE)
                bindTimerText(activeTimer)
            } else {
                val frame = getEmptyFrame(context)
                setImageViewBitmap(
                    R.id.flow_status_widget_image,
                    FlowStatusWidgetRenderer.render(null)
                )
                setViewVisibility(R.id.flow_status_empty_container, View.VISIBLE)
                setViewVisibility(R.id.flow_status_widget_text_row, View.GONE)
                setImageViewResource(R.id.flow_status_character_image, CHARACTER_RES_IDS[frame])
                setTextViewText(R.id.flow_status_empty_label, EMPTY_LABELS[frame])
                setTextViewText(R.id.flow_status_empty_sublabel, "· 탭해서 시작")
            }
        }
    }

    private fun RemoteViews.bindTimerText(activeTimer: ActiveTimerState) {
        val elapsedMillis = activeTimer.elapsedMillis
        val isOverGoal = elapsedMillis >= FlowStatusWidgetRenderer.GOAL_MILLIS
        val isPaused = activeTimer.status == TimerStatus.PAUSED
        val textColor = when {
            isPaused -> Color.rgb(51, 47, 62)
            isOverGoal -> Color.WHITE
            else -> Color.rgb(37, 28, 64)
        }
        val label = buildString {
            append(displayCategory(activeTimer.category))
            append(
                when {
                    isPaused -> " · 멈춤 "
                    isOverGoal -> " · 넘치는 중 "
                    else -> " · flowing "
                }
            )
        }

        setTextViewText(R.id.flow_status_widget_label, label)
        setTextColor(R.id.flow_status_widget_label, textColor)
        setTextColor(R.id.flow_status_widget_chronometer, textColor)
        setViewVisibility(R.id.flow_status_widget_chronometer, View.VISIBLE)
        setChronometer(
            R.id.flow_status_widget_chronometer,
            activeTimer.chronometerBaseElapsedRealtime,
            null,
            !isPaused
        )
    }

    private fun displayCategory(category: String): String = when (category) {
        "TOOTHBRUSH" -> "양치"
        "SNACK" -> "간식"
        "MEAL" -> "식사"
        "STUDY" -> "공부"
        "WORK" -> "업무"
        "COMPANY" -> "회사"
        "DEVELOPMENT" -> "개발"
        "WASH" -> "씻기"
        "SCHOOL" -> "학교"
        "EXERCISE" -> "운동"
        "SLEEP" -> "수면"
        "REST" -> "휴식"
        "TODO" -> "할일"
        else -> "활동"
    }

    companion object {
        private const val ACTION_REFRESH = "com.example.flowlog.widget.FLOW_STATUS_REFRESH"
        private const val REQUEST_OPEN_APP = 7201
        private const val REQUEST_REFRESH = 7202
        private val REFRESH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(15)
        private const val PREFS_WIDGET = "flowlog_widget_state"
        private const val KEY_EMPTY_SINCE = "empty_since_millis"

        // 기록 없는 시간이 길어질수록 더 쓸쓸한 캐릭터 표시
        private val CHARACTER_RES_IDS = intArrayOf(
            R.drawable.widget_char_empty_1,
            R.drawable.widget_char_empty_2,
            R.drawable.widget_char_empty_3,
            R.drawable.widget_char_empty_4
        )

        private val EMPTY_LABELS = arrayOf(
            "탭해서 오늘의 흐름을 시작해요",  // 0–10분
            "고요한 하루가 흘러가고 있어요",  // 10–30분
            "아직 흐르지 않는 하루예요",      // 30–60분
            "빈 흐름이 기다리고 있어요"        // 60분 이상
        )

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(appContext, FlowStatusWidgetProvider::class.java)
            )
            FlowStatusWidgetProvider().updateWidgets(appContext, manager, ids)
        }

        // 기록 없는 시간에 따라 0~3 프레임 반환, 최초 진입 시점을 기록
        private fun getEmptyFrame(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
            var emptySince = prefs.getLong(KEY_EMPTY_SINCE, 0L)
            if (emptySince == 0L) {
                emptySince = System.currentTimeMillis()
                prefs.edit().putLong(KEY_EMPTY_SINCE, emptySince).apply()
            }
            val emptyMinutes = (System.currentTimeMillis() - emptySince) / 60_000L
            return when {
                emptyMinutes < 10L -> 0
                emptyMinutes < 30L -> 1
                emptyMinutes < 60L -> 2
                else -> 3
            }
        }

        // 활동 시작 시 empty 타이머 리셋
        private fun resetEmptyTracker(context: Context) {
            context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
                .edit().remove(KEY_EMPTY_SINCE).apply()
        }

        private fun scheduleNextRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + REFRESH_INTERVAL_MILLIS,
                refreshIntent(context)
            )
        }

        private fun cancelNextRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(refreshIntent(context))
        }

        private fun refreshIntent(context: Context): PendingIntent {
            return PendingIntent.getBroadcast(
                context,
                REQUEST_REFRESH,
                Intent(context, FlowStatusWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

private object FlowStatusWidgetRenderer {
    private const val WIDTH = 900
    private const val HEIGHT = 180
    const val GOAL_MILLIS = 2L * 60L * 60L * 1000L
    private const val WAVE_LOOP_MILLIS = 6_000L

    fun render(activeTimer: ActiveTimerState?): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(8f, 8f, WIDTH - 8f, HEIGHT - 8f)
        val radius = HEIGHT * 0.42f

        drawBase(canvas, bounds, radius)
        if (activeTimer != null) {
            drawActiveState(canvas, bounds, radius, activeTimer)
        }

        return bitmap
    }

    private fun drawBase(canvas: Canvas, bounds: RectF, radius: Float) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(34, 82, 64, 160)
        }
        canvas.drawRoundRect(
            RectF(bounds.left + 2f, bounds.top + 6f, bounds.right - 2f, bounds.bottom + 1f),
            radius, radius, shadowPaint
        )
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(249, 246, 255)
        }
        canvas.drawRoundRect(bounds, radius, radius, basePaint)
    }

    private fun drawActiveState(
        canvas: Canvas,
        bounds: RectF,
        radius: Float,
        activeTimer: ActiveTimerState
    ) {
        val elapsedMillis = activeTimer.elapsedMillis
        val progress = min(elapsedMillis.toFloat() / GOAL_MILLIS.toFloat(), 1f)
        val isOverGoal = elapsedMillis >= GOAL_MILLIS
        val isPaused = activeTimer.status == TimerStatus.PAUSED
        val wavePhase = if (isPaused) 0f
        else (SystemClock.elapsedRealtime() % WAVE_LOOP_MILLIS).toFloat() / WAVE_LOOP_MILLIS

        val fillRight = bounds.left + (bounds.width() * progress).coerceAtLeast(bounds.height() * 0.42f)
        val fillBounds = RectF(bounds.left, bounds.top, fillRight, bounds.bottom)
        val clip = Path().apply { addRoundRect(bounds, radius, radius, Path.Direction.CW) }

        canvas.save()
        canvas.clipPath(clip)
        drawLiquidFill(canvas, fillBounds, isOverGoal, isPaused, wavePhase)
        canvas.restore()
    }

    private fun drawLiquidFill(
        canvas: Canvas,
        fillBounds: RectF,
        isOverGoal: Boolean,
        isPaused: Boolean,
        wavePhase: Float
    ) {
        val startColor = when {
            isPaused -> Color.rgb(158, 151, 177)
            isOverGoal -> Color.rgb(255, 82, 122)
            else -> Color.rgb(132, 100, 255)
        }
        val endColor = when {
            isPaused -> Color.rgb(203, 198, 216)
            isOverGoal -> Color.rgb(255, 132, 74)
            else -> Color.rgb(179, 139, 255)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                fillBounds.left, fillBounds.top,
                fillBounds.right, fillBounds.bottom,
                startColor, endColor, Shader.TileMode.CLAMP
            )
        }

        val waveOffset = if (isPaused) 0f else wavePhase * 74f
        val crestOffset = sin(wavePhase * Math.PI.toFloat() * 2f) * 16f
        val wave = Path().apply {
            moveTo(fillBounds.left, fillBounds.top)
            lineTo(fillBounds.right - 58f + waveOffset, fillBounds.top)
            cubicTo(
                fillBounds.right + 4f + waveOffset, fillBounds.top + 24f + crestOffset,
                fillBounds.right - 18f + waveOffset, fillBounds.bottom - 24f - crestOffset,
                fillBounds.right + 48f + waveOffset, fillBounds.bottom
            )
            lineTo(fillBounds.left, fillBounds.bottom)
            close()
        }
        canvas.drawPath(wave, paint)

        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(52, 255, 255, 255)
        }
        canvas.drawOval(
            RectF(
                fillBounds.left + fillBounds.width() * (0.08f + wavePhase * 0.18f),
                fillBounds.top + 22f,
                fillBounds.left + fillBounds.width() * (0.42f + wavePhase * 0.18f),
                fillBounds.top + 68f
            ),
            sheenPaint
        )
        canvas.drawCircle(
            fillBounds.right - 86f + waveOffset * 0.55f,
            fillBounds.centerY() + 34f - crestOffset * 0.45f,
            26f, sheenPaint
        )
    }
}
