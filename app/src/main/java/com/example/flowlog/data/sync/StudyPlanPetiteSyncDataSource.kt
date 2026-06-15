package com.example.flowlog.data.sync

import android.content.Context
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class StudyPlanPetiteSyncDataSource(context: Context) {
    private val dao = FlowlogDatabase.getInstance(context).organizedPetiteDao()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sync(userId: String): Int {
        val docs = firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("petites")
            .whereEqualTo("origin", "studyPlan")
            .get()
            .awaitResult()
            .documents

        val rows = docs.mapIndexedNotNull { index, doc ->
            val title = doc.getString("title")?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val date = doc.getString("selectedDate") ?: doc.getString("date") ?: return@mapIndexedNotNull null
            val autoStartEnabled = doc.getBoolean("autoStartEnabled") ?: false
            val startTime = doc.getString("autoStartTime24") ?: doc.getString("time24")
            val endTime = doc.getString("autoStartEndTime24")
            val startMillis = parseDateTime(date, startTime.takeIf { autoStartEnabled })
            val endMillis = parseDateTime(date, endTime.takeIf { autoStartEnabled })
            val durationMillis = if (autoStartEnabled && endMillis != null && endMillis > startMillis) {
                endMillis - startMillis
            } else {
                null
            }
            val sourceId = doc.getString("sourceCalendarId") ?: doc.id

            OrganizedPetiteEntity(
                id = "study-plan-$sourceId",
                userId = userId,
                title = title,
                sourceType = PetiteSourceType.STUDY_PLAN.name,
                sourceId = sourceId,
                category = null,
                dateMillis = startMillis,
                linkedActivityName = title,
                activityCategory = doc.getString("activityCategory") ?: "STUDY",
                isCompleted = doc.getBoolean("isCompleted") ?: false,
                priorityScore = (doc.getLong("rank") ?: index.toLong()).toInt(),
                burdenScore = null,
                isSeverelyBehind = null,
                totalStudyMinutesSinceD7 = null,
                studiedDaysSinceD7 = null,
                missedDaysSinceD7 = null,
                aiComment = doc.getString("note"),
                estimatedMinutes = durationMillis?.let { (it / 60_000L).toInt().coerceAtLeast(1) },
                stepsJson = "[]",
                examDValue = null,
                routineTimerDurationMillis = durationMillis,
                routineTimerCategory = if (autoStartEnabled) doc.getString("activityCategory") ?: "STUDY" else null,
                rank = index,
                isDismissed = false,
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        }

        dao.replaceAllForUserBySource(userId, PetiteSourceType.STUDY_PLAN.name, rows)
        return rows.size
    }

    private fun parseDateTime(date: String, time24: String?): Long {
        val safeTime = time24?.takeIf { it.matches(TIME_REGEX) } ?: "00:00"
        return dateTimeFormat.parse("$date $safeTime")?.time ?: System.currentTimeMillis()
    }

    companion object {
        private val TIME_REGEX = Regex("""^\d{2}:\d{2}$""")
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
    }
}
