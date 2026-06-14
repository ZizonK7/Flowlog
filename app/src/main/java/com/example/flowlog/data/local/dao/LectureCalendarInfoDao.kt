package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.flowlog.data.local.entity.LectureCalendarInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LectureCalendarInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(infos: List<LectureCalendarInfoEntity>)

    @Query("SELECT * FROM lecture_calendar_infos WHERE startTime >= :dayStart AND startTime < :dayEnd ORDER BY startTime ASC")
    fun observeTodayLectureInfos(dayStart: Long, dayEnd: Long): Flow<List<LectureCalendarInfoEntity>>

    @Query("DELETE FROM lecture_calendar_infos WHERE startTime >= :dayStart AND startTime < :dayEnd")
    suspend fun deleteLectureInfosForDay(dayStart: Long, dayEnd: Long)

    @Transaction
    suspend fun replaceTodayLectureInfos(dayStart: Long, dayEnd: Long, infos: List<LectureCalendarInfoEntity>) {
        deleteLectureInfosForDay(dayStart, dayEnd)
        if (infos.isNotEmpty()) upsertAll(infos)
    }

    @Query("DELETE FROM lecture_calendar_infos WHERE classDate = :classDate AND type = 'COURSE_LESSON'")
    suspend fun deleteCourseLessonsForDay(classDate: Long)

    @Transaction
    suspend fun replaceCourseLessonsForDay(classDate: Long, infos: List<LectureCalendarInfoEntity>) {
        deleteCourseLessonsForDay(classDate)
        if (infos.isNotEmpty()) upsertAll(infos)
    }
}
