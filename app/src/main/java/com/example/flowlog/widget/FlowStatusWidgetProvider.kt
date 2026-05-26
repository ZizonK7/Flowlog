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
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, views)
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
            setImageViewBitmap(
                R.id.flow_status_widget_image,
                FlowStatusWidgetRenderer.render(activeTimer)
            )
            bindTimerText(activeTimer)
            setOnClickPendingIntent(R.id.flow_status_widget_root, openIntent)
            setOnClickPendingIntent(R.id.flow_status_widget_image, openIntent)
        }
    }

    private fun RemoteViews.bindTimerText(activeTimer: ActiveTimerState?) {
        if (activeTimer == null) {
            setTextViewText(
                R.id.flow_status_widget_label,
                "\uC624\uB298\uC758 \uD750\uB984\uC744 \uC2DC\uC791\uD574\uBCF4\uC138\uC694"
            )
            setViewVisibility(R.id.flow_status_widget_chronometer, View.GONE)
            setTextColor(R.id.flow_status_widget_label, Color.rgb(95, 87, 120))
            return
        }

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
                    isPaused -> " paused... "
                    isOverGoal -> " overflowing... "
                    else -> " flowing... "
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

    private fun displayCategory(category: String): String {
        return when (category) {
            "TOOTHBRUSH" -> "\uC591\uCE58"
            "SNACK" -> "\uAC04\uC2DD"
            "MEAL" -> "\uC2DD\uC0AC"
            "STUDY" -> "\uACF5\uBD80"
            "WORK" -> "\uC5C5\uBB34"
            "DEVELOPMENT" -> "\uAC1C\uBC1C"
            "WASH" -> "\uC53B\uAE30"
            "SCHOOL" -> "\uD559\uAD50"
            "EXERCISE" -> "\uC6B4\uB3D9"
            "SLEEP" -> "\uC218\uBA74"
            "REST" -> "\uD734\uC2DD"
            "TODO" -> "\uD560\uC77C"
            else -> "\uD65C\uB3D9"
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.example.flowlog.widget.FLOW_STATUS_REFRESH"
        private const val REQUEST_OPEN_APP = 7201
        private const val REQUEST_REFRESH = 7202
        private val REFRESH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(15)

        fun updateAll(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(ComponentName(appContext, FlowStatusWidgetProvider::class.java))
            FlowStatusWidgetProvider().updateWidgets(appContext, manager, ids)
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

    fun render(activeTimer: ActiveTimerState?): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(8f, 8f, WIDTH - 8f, HEIGHT - 8f)
        val radius = HEIGHT * 0.42f

        drawBase(canvas, bounds, radius)
        if (activeTimer == null) {
            drawEmptyState(canvas, bounds)
        } else {
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
            radius,
            radius,
            shadowPaint
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
        val wavePhase = if (isPaused) {
            0f
        } else {
            (SystemClock.elapsedRealtime() % WAVE_LOOP_MILLIS).toFloat() / WAVE_LOOP_MILLIS.toFloat()
        }
        val fillRight = bounds.left + (bounds.width() * progress).coerceAtLeast(bounds.height() * 0.42f)
        val fillBounds = RectF(bounds.left, bounds.top, fillRight, bounds.bottom)
        val clip = Path().apply {
            addRoundRect(bounds, radius, radius, Path.Direction.CW)
        }

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
                fillBounds.left,
                fillBounds.top,
                fillBounds.right,
                fillBounds.bottom,
                startColor,
                endColor,
                Shader.TileMode.CLAMP
            )
        }

        val waveOffset = if (isPaused) 0f else (wavePhase * 74f)
        val crestOffset = sin(wavePhase * Math.PI.toFloat() * 2f) * 16f
        val wave = Path().apply {
            moveTo(fillBounds.left, fillBounds.top)
            lineTo(fillBounds.right - 58f + waveOffset, fillBounds.top)
            cubicTo(
                fillBounds.right + 4f + waveOffset,
                fillBounds.top + 24f + crestOffset,
                fillBounds.right - 18f + waveOffset,
                fillBounds.bottom - 24f - crestOffset,
                fillBounds.right + 48f + waveOffset,
                fillBounds.bottom
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
            26f,
            sheenPaint
        )
    }

    private fun drawEmptyState(canvas: Canvas, bounds: RectF) {
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(58, 132, 100, 255)
        }
        canvas.drawCircle(bounds.left + 40f, bounds.centerY(), 16f, accentPaint)
    }

    private const val WAVE_LOOP_MILLIS = 6_000L
}
