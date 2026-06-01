package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.AutoButtonSkipDateEntity
import com.example.flowlog.data.local.mapper.toEntity
import com.example.flowlog.data.local.mapper.toModel
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

class AutoButtonScheduleRepository(context: Context) {
    private val dao = FlowlogDatabase.getInstance(context).autoButtonScheduleDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    fun observeSchedules(dateKey: Long = todayDateKey()): Flow<List<AutoButtonSchedule>> {
        return combine(
            dao.observeSchedules(userId),
            dao.observeSkipDates(dateKey)
        ) { schedules, skipDates ->
            val skippedIds = skipDates.map { it.scheduleId }.toSet()
            schedules.map { it.toModel(isSkippedToday = it.scheduleId in skippedIds) }
        }
    }

    fun observeTodayBlocks(now: Long = System.currentTimeMillis()): Flow<List<ScheduledAutoButtonBlock>> {
        val dateKey = dateKey(now)
        val dayOfWeek = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_WEEK)
        return observeSchedules(dateKey).combine(dao.observeSkipDates(dateKey)) { schedules, _ ->
            schedules
                .filter { schedule -> schedule.isEnabled && dayOfWeek in schedule.repeatDays }
                .map { schedule ->
                    ScheduledAutoButtonBlock(
                        scheduleId = schedule.scheduleId,
                        title = schedule.title,
                        category = schedule.category,
                        startTime = dateKey + schedule.startMinuteOfDay * MILLIS_PER_MINUTE,
                        endTime = dateKey + schedule.endMinuteOfDay * MILLIS_PER_MINUTE,
                        isSkippedToday = schedule.isSkippedToday
                    )
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

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L

        fun todayDateKey(): Long = dateKey(System.currentTimeMillis())

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
