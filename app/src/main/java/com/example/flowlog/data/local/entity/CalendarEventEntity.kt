package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_events",
    indices = [Index("startTime"), Index("type")]
)
data class CalendarEventEntity(
    @PrimaryKey val eventId: String,
    val type: String,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val location: String? = null,
    val description: String? = null,
    val source: String? = null,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
