package com.example.flowlog.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast

object CalendarIntentHelper {
    fun openInsertEvent(context: Context, title: String, dateMillis: Long?) {
        val beginTime = startOfDay(dateMillis ?: System.currentTimeMillis())
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime)
            .putExtra(CalendarContract.Events.ALL_DAY, 1)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "캘린더 앱을 열 수 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startOfDay(millis: Long): Long =
        java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
