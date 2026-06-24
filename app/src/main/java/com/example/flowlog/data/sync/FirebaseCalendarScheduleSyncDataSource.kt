package com.example.flowlog.data.sync

import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirebaseCalendarScheduleSyncDataSource {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun upsertAutoStart(userId: String, schedule: AutoButtonSchedule) = withContext(Dispatchers.IO) {
        val eventId = schedule.calendarEventId() ?: return@withContext
        val sourceDateKey = schedule.sourceDateKey ?: return@withContext
        val now = System.currentTimeMillis()
        val data = mapOf(
            "eventId" to eventId,
            "type" to "CALENDAR_PETITE",
            "title" to schedule.title,
            "startTime" to sourceDateKey + schedule.startMinuteOfDay * MILLIS_PER_MINUTE,
            "autoStartEnabled" to schedule.isEnabled,
            "autoStartTime24" to formatMinuteOfDay(schedule.startMinuteOfDay),
            "autoStartEndTime24" to formatMinuteOfDay(schedule.endMinuteOfDay),
            "activityCategory" to schedule.category,
            "updatedAt" to now,
            "deletedAt" to null
        )
        calendarEventDoc(userId, eventId).set(data, com.google.firebase.firestore.SetOptions.merge()).awaitResult()
    }

    suspend fun disableAutoStart(userId: String, schedule: AutoButtonSchedule) = withContext(Dispatchers.IO) {
        val eventId = schedule.calendarEventId() ?: return@withContext
        val now = System.currentTimeMillis()
        val data = mapOf(
            "eventId" to eventId,
            "type" to "CALENDAR_PETITE",
            "autoStartEnabled" to false,
            "autoStartTime24" to "",
            "autoStartEndTime24" to "",
            "updatedAt" to now
        )
        calendarEventDoc(userId, eventId).set(data, com.google.firebase.firestore.SetOptions.merge()).awaitResult()
    }

    private fun calendarEventDoc(userId: String, eventId: String) = firestore
        .collection("users").document(userId)
        .collection("flowlog").document("data")
        .collection("calendarEvents").document(eventId)

    private fun AutoButtonSchedule.calendarEventId(): String? {
        if (source != SOURCE_CALENDAR) return null
        return scheduleId.removePrefix(CALENDAR_SCHEDULE_PREFIX).takeIf { it != scheduleId && it.isNotBlank() }
    }

    private fun formatMinuteOfDay(minuteOfDay: Int): String {
        val minute = minuteOfDay.coerceIn(0, 24 * 60 - 1)
        return "%02d:%02d".format(minute / 60, minute % 60)
    }

    companion object {
        private const val SOURCE_CALENDAR = "CALENDAR"
        private const val CALENDAR_SCHEDULE_PREFIX = "cal-"
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
