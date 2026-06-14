package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lecture_calendar_infos",
    indices = [Index("classDate"), Index("type")]
)
data class LectureCalendarInfoEntity(
    @PrimaryKey val eventId: String,
    val type: String,
    val courseTitle: String? = null,
    val lectureTitle: String? = null,
    val classDate: Long? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val week: Int? = null,
    val syllabusText: String? = null,
    val previewCardId: String? = null,
    val location: String? = null,
    val description: String? = null,
    val sourceId: String? = null,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
