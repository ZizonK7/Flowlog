package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_summaries",
    indices = [
        Index("userId"),
        Index("dateKey"),
        Index(value = ["userId", "dateKey"], unique = true)
    ]
)
data class DailySummaryEntity(
    @PrimaryKey val summaryId: String,
    val userId: String,
    val dateKey: String,
    val totalActivityMillis: Long = 0L,
    val categorySummaryJson: String? = null,
    val todoSummaryJson: String? = null,
    val focusMillis: Long? = null,
    val restMillis: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
