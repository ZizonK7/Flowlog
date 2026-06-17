package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_button_schedules",
    indices = [
        Index("userId"),
        Index("isEnabled"),
        Index(value = ["userId", "isDeleted"])
    ]
)
data class AutoButtonScheduleEntity(
    @PrimaryKey val scheduleId: String,
    val userId: String,
    val title: String,
    val category: String,
    val repeatDaysMask: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val isEnabled: Boolean = true,
    val notifyOnStart: Boolean = true,
    val notifyOnEnd: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val source: String = "MANUAL"
)
