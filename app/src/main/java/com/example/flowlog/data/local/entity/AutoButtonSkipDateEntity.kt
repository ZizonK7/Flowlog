package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auto_button_skip_dates",
    indices = [
        Index(value = ["scheduleId", "dateKey"], unique = true),
        Index("dateKey")
    ]
)
data class AutoButtonSkipDateEntity(
    @PrimaryKey val id: String,
    val scheduleId: String,
    val dateKey: Long,
    val createdAt: Long = System.currentTimeMillis()
)
