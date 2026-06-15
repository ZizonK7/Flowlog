package com.example.flowlog.data.sync

import android.content.Context
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StudyPlanPetiteSyncDataSource(context: Context) {
    private val dao = FlowlogDatabase.getInstance(context).organizedPetiteDao()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sync(userId: String): Int {
        val petiteDocs = firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("petites")
            .whereEqualTo("origin", "studyPlan")
            .get()
            .awaitResult()
            .documents

        val calendarEventDocs = firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("calendarEvents")
            .whereEqualTo("origin", "studyPlan")
            .get()
            .awaitResult()
            .documents

        val rows = (petiteDocs.mapIndexedNotNull { index, doc -> doc.toStudyPlanPetite(userId, index) } +
            calendarEventDocs.mapIndexedNotNull { index, doc -> doc.toStudyPlanPetite(userId, petiteDocs.size + index) })
            .distinctBy { it.sourceId }
            .sortedWith(compareBy<OrganizedPetiteEntity> { it.dateMillis ?: Long.MAX_VALUE }.thenBy { it.rank })
            .mapIndexed { index, row -> row.copy(rank = index) }

        dao.replaceAllForUserBySource(userId, PetiteSourceType.STUDY_PLAN.name, rows)
        return rows.size
    }

    private fun DocumentSnapshot.toStudyPlanPetite(userId: String, index: Int): OrganizedPetiteEntity? {
        val title = getString("title")?.takeIf { it.isNotBlank() } ?: return null
        val startTimeMillis = getLong("startTime")
        val date = getString("selectedDate")
            ?: getString("date")
            ?: startTimeMillis?.let { dateKeyFormat.format(Date(it)) }
            ?: return null
        val autoStartEnabled = getBoolean("autoStartEnabled") ?: false
        val startTime = getString("autoStartTime24") ?: getString("time24")
        val endTime = getString("autoStartEndTime24")
        val startMillis = startTimeMillis ?: parseDateTime(date, startTime.takeIf { autoStartEnabled })
        val endMillis = parseDateTime(date, endTime.takeIf { autoStartEnabled })
        val durationMillis = if (autoStartEnabled && endMillis != null && endMillis > startMillis) {
            endMillis - startMillis
        } else {
            null
        }
        val sourceId = getString("sourceCalendarId") ?: getString("eventId") ?: id
        val activityCategory = getString("activityCategory") ?: "STUDY"
        val now = System.currentTimeMillis()

        return OrganizedPetiteEntity(
            id = "study-plan-$sourceId",
            userId = userId,
            title = title,
            sourceType = PetiteSourceType.STUDY_PLAN.name,
            sourceId = sourceId,
            category = null,
            dateMillis = startMillis,
            linkedActivityName = title,
            activityCategory = activityCategory,
            isCompleted = getBoolean("isCompleted") ?: false,
            priorityScore = (getLong("rank") ?: index.toLong()).toInt(),
            burdenScore = null,
            isSeverelyBehind = null,
            totalStudyMinutesSinceD7 = null,
            studiedDaysSinceD7 = null,
            missedDaysSinceD7 = null,
            aiComment = getString("note") ?: getString("description"),
            estimatedMinutes = durationMillis?.let { (it / 60_000L).toInt().coerceAtLeast(1) },
            stepsJson = "[]",
            examDValue = null,
            routineTimerDurationMillis = durationMillis,
            routineTimerCategory = if (autoStartEnabled) activityCategory else null,
            rank = index,
            isDismissed = false,
            createdAt = getLong("createdAt") ?: now,
            updatedAt = getLong("updatedAt") ?: now
        )
    }

    private fun parseDateTime(date: String, time24: String?): Long {
        val safeTime = time24?.takeIf { it.matches(TIME_REGEX) } ?: "00:00"
        return dateTimeFormat.parse("$date $safeTime")?.time ?: System.currentTimeMillis()
    }

    companion object {
        private val TIME_REGEX = Regex("""^\d{2}:\d{2}$""")
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }
    }
}
