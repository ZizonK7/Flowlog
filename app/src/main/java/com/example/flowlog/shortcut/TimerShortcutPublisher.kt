package com.example.flowlog.shortcut

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.flowlog.MainActivity
import com.example.flowlog.R
import com.example.flowlog.ui.component.categoryColor
import com.example.flowlog.ui.component.categoryNotificationIconRes
import com.example.flowlog.ui.component.displayCategory
import kotlin.math.roundToInt

/**
 * Publishes dynamic launcher shortcuts ("Study start", ...) for the first main
 * buttons. Long-pressing the app icon shows them; each opens MainActivity with
 * [TimerShortcutRules.ACTION_START_TIMER].
 */
object TimerShortcutPublisher {
    private const val TAG = "TimerShortcuts"

    fun publish(context: Context, categories: List<String>) {
        runCatching {
            val appContext = context.applicationContext
            val limit = minOf(
                TimerShortcutRules.MAX_SHORTCUTS,
                ShortcutManagerCompat.getMaxShortcutCountPerActivity(appContext)
            )
            val shortcuts = categories.take(limit).mapIndexed { rank, category ->
                val label = appContext.getString(R.string.shortcut_start_timer_label, displayCategory(category))
                val intent = Intent(appContext, MainActivity::class.java)
                    .setAction(TimerShortcutRules.ACTION_START_TIMER)
                    .putExtra(TimerShortcutRules.EXTRA_CATEGORY, category)
                ShortcutInfoCompat.Builder(appContext, TimerShortcutRules.SHORTCUT_ID_PREFIX + category)
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(shortcutIcon(appContext, category))
                    .setIntent(intent)
                    .setRank(rank)
                    .build()
            }
            ShortcutManagerCompat.setDynamicShortcuts(appContext, shortcuts)
        }.onFailure { Log.w(TAG, "could not publish timer shortcuts", it) }
    }

    // The category glyphs are white vectors made for notifications, so they are
    // drawn on the category color to stay visible on any launcher background.
    private fun shortcutIcon(context: Context, category: String): IconCompat {
        val glyph = ContextCompat.getDrawable(context, categoryNotificationIconRes(category))?.mutate()
            ?: return IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        val density = context.resources.displayMetrics.density
        val size = (108 * density).roundToInt()
        val glyphSize = (48 * density).roundToInt()
        val inset = (size - glyphSize) / 2
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(categoryColor(category).toArgb())
        glyph.setBounds(inset, inset, inset + glyphSize, inset + glyphSize)
        glyph.setTint(android.graphics.Color.WHITE)
        glyph.draw(canvas)
        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }
}
