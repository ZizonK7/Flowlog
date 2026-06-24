package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.AutoButtonSkipDateEntity
import com.example.flowlog.data.local.mapper.toEntity
import com.example.flowlog.data.local.mapper.toModel
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class AutoButtonScheduleRepository(context: Context) {
    private val db = FlowlogDatabase.getInstance(context)
    private val dao = db.autoButtonScheduleDao()
    private val activityDao = db.activityDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    fun observeSchedules(dateKey: Long = todayDateKey()): Flow<List<AutoButtonSchedule>> {
        return combine(
            dao.observeSchedules(userId),
            dao.observeSkipDates(dateKey)
        ) { schedules, skipDates ->
            val skippedIds = skipDates.map { it.scheduleId }.toSet()
            schedules
                .map { it.toModel(isSkippedToday = it.scheduleId in skippedIds) }
                .filter { it.source != SOURCE_CALENDAR || it.sourceDateKey == null || it.sourceDateKey == dateKey }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayBlocks(): Flow<List<ScheduledAutoButtonBlock>> {
        return minuteTicker().flatMapLatest { now ->
            val dateKey = dateKey(now)
            val endOfDay = dateKey + DAY_MILLIS
            val dayOfWeek = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
            combine(
                observeSchedules(dateKey),
                activityDao.observeTodayActivities(userId, dateKey, endOfDay)
            ) { schedules, activities ->
                val completedAutoScheduleIds = activities
                    .filter { it.sourceType == ActivitySourceType.AUTO_BUTTON }
                    .mapNotNull { it.sourceId }
                    .toSet()
                val routineBlocks = schedules.mapNotNull { schedule ->
                    val startTime = dateKey + schedule.startMinuteOfDay * MILLIS_PER_MINUTE
                    val endTime = dateKey + schedule.endMinuteOfDay * MILLIS_PER_MINUTE
                    if (!schedule.isEnabled ||
                        schedule.isSkippedToday ||
                        dayOfWeek !in schedule.repeatDays ||
                        !schedule.isValidForDate(dateKey) ||
                        endTime <= now ||
                        schedule.scheduleId in completedAutoScheduleIds
                    ) {
                        return@mapNotNull null
                    }
                    ScheduledAutoButtonBlock(
                        scheduleId = schedule.scheduleId,
                        title = schedule.title,
                        category = schedule.category,
                        startTime = startTime,
                        endTime = endTime,
                        isSkippedToday = false
                    )
                }
                routineBlocks
            }
        }
    }

    suspend fun upsertSchedule(schedule: AutoButtonSchedule): String {
        val entity = schedule.toEntity(userId)
        val existing = dao.getSchedule(entity.scheduleId)
        if (existing == null) {
            dao.insertSchedule(entity)
        } else {
            dao.updateSchedule(
                existing.copy(
                    title = entity.title,
                    category = entity.category,
                    repeatDaysMask = entity.repeatDaysMask,
                    startMinuteOfDay = entity.startMinuteOfDay,
                    endMinuteOfDay = entity.endMinuteOfDay,
                    isEnabled = entity.isEnabled,
                    notifyOnStart = entity.notifyOnStart,
                    notifyOnEnd = entity.notifyOnEnd,
                    source = entity.source,
                    sourceDateKey = entity.sourceDateKey,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        return entity.scheduleId
    }

    suspend fun setEnabled(scheduleId: String, isEnabled: Boolean) {
        dao.setScheduleEnabled(scheduleId, isEnabled, System.currentTimeMillis())
    }

    suspend fun skipToday(scheduleId: String, dateKey: Long = todayDateKey()) {
        dao.insertSkipDate(
            AutoButtonSkipDateEntity(
                id = "$scheduleId:$dateKey",
                scheduleId = scheduleId,
                dateKey = dateKey
            )
        )
    }

    suspend fun unskipToday(scheduleId: String, dateKey: Long = todayDateKey()) {
        dao.deleteSkipDate(scheduleId, dateKey)
    }

    suspend fun deleteSchedule(scheduleId: String) {
        dao.softDeleteSchedule(scheduleId, System.currentTimeMillis())
        dao.deleteSkipDatesForSchedule(scheduleId)
    }

    suspend fun updateScheduleTime(scheduleId: String, startMinuteOfDay: Int, endMinuteOfDay: Int) {
        dao.updateScheduleTime(scheduleId, startMinuteOfDay, endMinuteOfDay, System.currentTimeMillis())
    }

    suspend fun getSchedule(scheduleId: String): AutoButtonSchedule? {
        return dao.getSchedule(scheduleId)?.toModel()
    }

    private fun AutoButtonSchedule.isValidForDate(dateKey: Long): Boolean {
        return source != SOURCE_CALENDAR || sourceDateKey == null || sourceDateKey == dateKey
    }

    companion object {
        const val SOURCE_CALENDAR = "CALENDAR"
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

        private fun minuteTicker(): Flow<Long> = flow {
            while (true) {
                val now = System.currentTimeMillis()
                emit(now)
                delay(MILLIS_PER_MINUTE - (now % MILLIS_PER_MINUTE))
            }
        }

        fun todayDateKey(): Long = dateKey(System.currentTimeMillis())

        fun time24ToMillis(time24: String, dateKey: Long): Long {
            val parts = time24.split(":")
            val hours = parts.getOrNull(0)?.toIntOrNull() ?: return dateKey
            val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return dateKey
            return dateKey + (hours * 60 + minutes) * MILLIS_PER_MINUTE
        }

        fun dateKey(timestamp: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = timestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
