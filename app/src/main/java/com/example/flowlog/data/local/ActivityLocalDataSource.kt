package com.example.flowlog.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import com.example.flowlog.data.model.ActivitySession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ActivityLocalDataSource(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_ACTIVITY,
        Context.MODE_PRIVATE
    )

    init {
        ensureLoaded(sharedPreferences)
    }

    fun getAllActivities(): Flow<List<ActivitySession>> {
        return activities
    }

    fun getTodayActivities(timestamp: Long): Flow<List<ActivitySession>> {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val dayStartMillis = cal.timeInMillis

        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val dayEndMillis = cal.timeInMillis

        return activities.map { allActivities ->
            allActivities.filter { activity ->
                activity.startTime in dayStartMillis until dayEndMillis
            }.sortedByDescending { it.startTime }
        }
    }

    suspend fun insert(activity: ActivitySession): Long {
        val allActivities = activities.value.toMutableList()

        val newActivity = activity.copy(id = (allActivities.maxOfOrNull { it.id } ?: 0) + 1)
        allActivities.add(0, newActivity)

        saveActivities(allActivities)
        return newActivity.id
    }

    suspend fun update(activity: ActivitySession) {
        val allActivities = activities.value.toMutableList()

        val index = allActivities.indexOfFirst { it.id == activity.id }
        if (index >= 0) {
            allActivities[index] = activity
            saveActivities(allActivities)
        }
    }

    suspend fun delete(activity: ActivitySession) {
        deleteById(activity.id)
    }

    suspend fun deleteById(id: Long) {
        val allActivities = activities.value.toMutableList()

        allActivities.removeAll { it.id == id }
        saveActivities(allActivities)
    }

    suspend fun searchActivities(query: String): List<ActivitySession> {
        val lowerQuery = query.trim().lowercase()
        if (lowerQuery.isEmpty()) return emptyList()

        return activities.value.filter { activity ->
            activity.title.lowercase().contains(lowerQuery) ||
                activity.category.lowercase().contains(lowerQuery) ||
                activity.tags.any { it.lowercase().contains(lowerQuery) } ||
                activity.note.orEmpty().lowercase().contains(lowerQuery)
        }
    }

    suspend fun filterByCategory(category: String): List<ActivitySession> {
        return activities.value.filter { it.category == category }
    }

    suspend fun filterByTag(tag: String): List<ActivitySession> {
        return activities.value.filter { activity ->
            activity.tags.any { it.equals(tag, ignoreCase = true) }
        }
    }

    suspend fun getActivityById(id: Long): ActivitySession? {
        return activities.value.find { it.id  == id }
    }

    private fun loadActivities(): List<ActivitySession> {
        val data = sharedPreferences.getString(KEY_ALL_ACTIVITIES, "[]") ?: "[]"
        return try {
            snapshotJson.decodeFromString<List<ActivitySession>>(data)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveActivities(newActivities: List<ActivitySession>) {
        val sortedActivities = withContext(Dispatchers.Default) {
            newActivities.sortedByDescending { it.startTime }
        }
        activities.value = sortedActivities
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putString(KEY_ALL_ACTIVITIES, snapshotJson.encodeToString(sortedActivities))
                .apply()
            writeCsvSnapshot(sortedActivities)
        }
    }

    private fun writeCsvSnapshot(allActivities: List<ActivitySession>) {
        val exportDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val csvFile = File(exportDir, "flowlog_activities.csv")
        val exportActivities = allActivities
            .filter { it.category != "SNACK" && it.category != "TOOTHBRUSH" }
            .sortedBy { it.startTime }
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val csv = buildString {
            append("\uFEFF")
            appendLine("id,category,title,start_time,end_time,duration_minutes,duration_seconds,note,is_favorite,modified_time")
            exportActivities.forEach { activity ->
                appendLine(
                    listOf(
                        activity.id.toString(),
                        activity.category,
                        activity.title,
                        formatCsvTime(activity.startTime, timeFormat),
                        formatCsvTime(activity.endTime, timeFormat),
                        (activity.durationMillis / 60000L).toString(),
                        (activity.durationMillis / 1000L).toString(),
                        activity.note.orEmpty(),
                        activity.isFavorite.toString(),
                        formatCsvTime(activity.modifiedTime, timeFormat)
                    ).joinToString(",") { csvEscape(it) }
                )
            }
        }

        runCatching {
            csvFile.writeText(csv, Charsets.UTF_8)
        }
    }

    private fun formatCsvTime(timestamp: Long, format: SimpleDateFormat): String {
        return format.format(Date(timestamp))
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    companion object {
        private const val PREFS_ACTIVITY = "activity_data"
        private const val KEY_ALL_ACTIVITIES = "all_activities"
        private val snapshotJson = Json { ignoreUnknownKeys = true }
        private val activities = MutableStateFlow<List<ActivitySession>>(emptyList())
        @Volatile
        private var isLoaded = false

        private fun ensureLoaded(sharedPreferences: SharedPreferences) {
            if (isLoaded) return
            synchronized(this) {
                if (isLoaded) return
                activities.value = loadSnapshot(sharedPreferences)
                isLoaded = true
            }
        }

        private fun loadSnapshot(sharedPreferences: SharedPreferences): List<ActivitySession> {
            val data = sharedPreferences.getString(KEY_ALL_ACTIVITIES, "[]") ?: "[]"
            return try {
                snapshotJson.decodeFromString<List<ActivitySession>>(data)
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun loadSnapshot(context: Context): List<ActivitySession> {
            val sharedPreferences = context.getSharedPreferences(PREFS_ACTIVITY, Context.MODE_PRIVATE)
            return loadSnapshot(sharedPreferences)
        }
    }
}
