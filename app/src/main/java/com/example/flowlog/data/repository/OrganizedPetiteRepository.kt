package com.example.flowlog.data.repository

import android.content.Context
import android.util.Log
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.local.dao.OrganizedPetiteDao
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.model.TodoCategory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import org.json.JSONArray

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizedPetiteRepository(context: Context) {

    private val dao: OrganizedPetiteDao = FlowlogDatabase.getInstance(context).organizedPetiteDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    private fun userIdFlow() = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.uid ?: "anonymous")
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid ?: "anonymous")
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun observeActivePetites(): Flow<List<OrganizedPetite>> {
        return userIdFlow().flatMapLatest { uid ->
            dao.observeActive(uid).map { rows -> rows.mapNotNull { it.toModel() } }
        }
    }

    suspend fun replaceWith(items: List<OrganizedPetite>) {
        val now = System.currentTimeMillis()
        // replaceNonCalendarForUser: CALENDAR sourceType은 삭제하지 않음.
        // calendar pull이 독립적으로 upsert/dismiss를 담당한다.
        dao.replaceNonCalendarForUser(
            userId = userId,
            items = items.mapIndexed { index, item -> item.toEntity(userId, index, now) }
        )
    }

    suspend fun dismiss(item: OrganizedPetite) {
        dao.dismissBySource(
            userId = userId,
            sourceType = item.sourceType.name,
            sourceId = item.sourceId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun dismissTodoPetitesBySourceId(sourceId: String) {
        dao.dismissTodoPetitesBySourceId(
            userId = userId,
            sourceId = sourceId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun restore(item: OrganizedPetite) {
        dao.restoreBySource(
            userId = userId,
            sourceType = item.sourceType.name,
            sourceId = item.sourceId,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun complete(item: OrganizedPetite) {
        dao.markCompletedById(item.id, System.currentTimeMillis())
    }

    suspend fun addLocalTodoPetiteIfAbsent(item: OrganizedPetite) {
        val now = System.currentTimeMillis()
        dao.insertPetiteIfAbsent(item.toEntity(userId, rank = 0, now = now))
    }

    suspend fun loadDismissedSourceKeys(): Set<String> {
        return dao.getDismissed(userId).map { "${it.sourceType}:${it.sourceId}" }.toSet()
    }

    suspend fun completeById(id: String) {
        dao.markCompletedById(id, System.currentTimeMillis())
    }

    fun observeTodayCalendarAutoStartPetites(todayDateKey: Long): Flow<List<OrganizedPetiteEntity>> {
        return dao.observeTodayCalendarAutoStartPetites(userId, todayDateKey)
    }

    suspend fun updateCalendarPetiteAutoStartTimes(petiteId: String, startTime24: String, endTime24: String): String? {
        dao.updateCalendarPetiteAutoStartTimes(petiteId, startTime24, endTime24, System.currentTimeMillis())
        return dao.getById(petiteId)?.sourceId
    }

    suspend fun dismissCalendarPetiteById(petiteId: String): String? {
        val petite = dao.getById(petiteId) ?: return null
        dao.dismissBySource(
            userId = userId,
            sourceType = "CALENDAR",
            sourceId = petite.sourceId,
            updatedAt = System.currentTimeMillis()
        )
        return petite.sourceId
    }

    private fun OrganizedPetite.toEntity(uid: String, rank: Int, now: Long): OrganizedPetiteEntity {
        return OrganizedPetiteEntity(
            id = id,
            userId = uid,
            title = title,
            sourceType = sourceType.name,
            sourceId = sourceId,
            category = category?.name,
            dateMillis = dateMillis,
            linkedActivityName = linkedActivityName,
            activityCategory = activityCategory,
            isCompleted = isCompleted,
            priorityScore = priorityScore,
            burdenScore = burdenScore,
            isSeverelyBehind = isSeverelyBehind,
            totalStudyMinutesSinceD7 = totalStudyMinutesSinceD7,
            studiedDaysSinceD7 = studiedDaysSinceD7,
            missedDaysSinceD7 = missedDaysSinceD7,
            aiComment = aiComment,
            estimatedMinutes = estimatedMinutes,
            stepsJson = JSONArray().apply { steps.forEach { put(it) } }.toString(),
            examDValue = examDValue,
            routineTimerDurationMillis = routineTimerDurationMillis,
            routineTimerCategory = routineTimerCategory,
            rank = rank,
            isDismissed = false,
            createdAt = now,
            updatedAt = now,
            calendarTaskType = calendarTaskType
        )
    }

    private fun OrganizedPetiteEntity.toModel(): OrganizedPetite? {
        val parsedSourceType = runCatching { PetiteSourceType.valueOf(sourceType) }.getOrNull() ?: return null
        val parsedCategory = category?.let { runCatching { TodoCategory.valueOf(it) }.getOrNull() }
        return OrganizedPetite(
            id = id,
            title = title,
            sourceType = parsedSourceType,
            sourceId = sourceId,
            category = parsedCategory,
            dateMillis = dateMillis,
            linkedActivityName = linkedActivityName,
            activityCategory = activityCategory,
            isCompleted = isCompleted,
            priorityScore = priorityScore,
            burdenScore = burdenScore,
            isSeverelyBehind = isSeverelyBehind,
            totalStudyMinutesSinceD7 = totalStudyMinutesSinceD7,
            studiedDaysSinceD7 = studiedDaysSinceD7,
            missedDaysSinceD7 = missedDaysSinceD7,
            aiComment = aiComment,
            estimatedMinutes = estimatedMinutes,
            steps = parseSteps(stepsJson),
            examDValue = examDValue,
            routineTimerDurationMillis = routineTimerDurationMillis,
            routineTimerCategory = routineTimerCategory,
            calendarTaskType = calendarTaskType
        )
    }

    private fun parseSteps(json: String): List<String> {
        return runCatching {
            val array = JSONArray(json)
            List(array.length()) { index -> array.optString(index) }
                .filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }
}
