package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.flowlog.data.local.entity.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("SELECT * FROM calendar_events WHERE startTime >= :dayStart AND startTime < :dayEnd ORDER BY startTime ASC")
    fun observeEventsForDay(dayStart: Long, dayEnd: Long): Flow<List<CalendarEventEntity>>

    @Query("DELETE FROM calendar_events WHERE startTime >= :dayStart AND startTime < :dayEnd")
    suspend fun deleteEventsForDay(dayStart: Long, dayEnd: Long)

    @Transaction
    suspend fun replaceEventsForDay(dayStart: Long, dayEnd: Long, events: List<CalendarEventEntity>) {
        deleteEventsForDay(dayStart, dayEnd)
        if (events.isNotEmpty()) upsertAll(events)
    }
}
