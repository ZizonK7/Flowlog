package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flowlog.data.local.entity.AutoButtonScheduleEntity
import com.example.flowlog.data.local.entity.AutoButtonSkipDateEntity
import com.example.flowlog.data.local.entity.AutoButtonUndoSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoButtonScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: AutoButtonScheduleEntity)

    @Update
    suspend fun updateSchedule(schedule: AutoButtonScheduleEntity)

    @Query("""
        SELECT * FROM auto_button_schedules
        WHERE userId = :userId
          AND isDeleted = 0
        ORDER BY startMinuteOfDay ASC, title ASC
    """)
    fun observeSchedules(userId: String): Flow<List<AutoButtonScheduleEntity>>

    @Query("""
        SELECT * FROM auto_button_schedules
        WHERE userId = :userId
          AND isDeleted = 0
          AND isEnabled = 1
        ORDER BY startMinuteOfDay ASC
    """)
    suspend fun getActiveSchedules(userId: String): List<AutoButtonScheduleEntity>

    @Query("""
        SELECT * FROM auto_button_schedules
        WHERE scheduleId = :scheduleId
          AND isDeleted = 0
        LIMIT 1
    """)
    suspend fun getSchedule(scheduleId: String): AutoButtonScheduleEntity?

    @Query("""
        UPDATE auto_button_schedules
        SET isEnabled = :isEnabled,
            updatedAt = :updatedAt
        WHERE scheduleId = :scheduleId
    """)
    suspend fun setScheduleEnabled(scheduleId: String, isEnabled: Boolean, updatedAt: Long)

    @Query("""
        UPDATE auto_button_schedules
        SET isDeleted = 1,
            updatedAt = :updatedAt
        WHERE scheduleId = :scheduleId
    """)
    suspend fun softDeleteSchedule(scheduleId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkipDate(skipDate: AutoButtonSkipDateEntity)

    @Query("""
        DELETE FROM auto_button_skip_dates
        WHERE scheduleId = :scheduleId
          AND dateKey = :dateKey
    """)
    suspend fun deleteSkipDate(scheduleId: String, dateKey: Long)

    @Query("""
        SELECT * FROM auto_button_skip_dates
        WHERE dateKey = :dateKey
    """)
    fun observeSkipDates(dateKey: Long): Flow<List<AutoButtonSkipDateEntity>>

    @Query("""
        SELECT * FROM auto_button_skip_dates
        WHERE scheduleId = :scheduleId
          AND dateKey = :dateKey
        LIMIT 1
    """)
    suspend fun getSkipDate(scheduleId: String, dateKey: Long): AutoButtonSkipDateEntity?

    @Query("DELETE FROM auto_button_skip_dates WHERE scheduleId = :scheduleId")
    suspend fun deleteSkipDatesForSchedule(scheduleId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUndoSnapshot(snapshot: AutoButtonUndoSnapshotEntity)

    @Query("SELECT * FROM auto_button_undo_snapshots WHERE id = :snapshotId LIMIT 1")
    suspend fun getUndoSnapshot(snapshotId: String): AutoButtonUndoSnapshotEntity?

    @Query("UPDATE auto_button_undo_snapshots SET isUsed = 1 WHERE id = :snapshotId")
    suspend fun markUndoSnapshotUsed(snapshotId: String)

    @Query("""
        UPDATE auto_button_undo_snapshots
        SET isUsed = 1
        WHERE isUsed = 0
          AND expiresAt < :now
    """)
    suspend fun markExpiredUndoSnapshotsUsed(now: Long)

    @Query("UPDATE auto_button_undo_snapshots SET isUsed = 1 WHERE isUsed = 0")
    suspend fun markOpenUndoSnapshotsUsed()

    @Query("UPDATE auto_button_schedules SET userId = :newUserId WHERE userId = 'anonymous'")
    suspend fun reassignAnonymousUser(newUserId: String): Int
}
