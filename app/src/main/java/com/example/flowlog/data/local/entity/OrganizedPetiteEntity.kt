package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "organized_petites",
    indices = [
        Index("userId"),
        Index("isDismissed"),
        Index(value = ["userId", "sourceType", "sourceId"])
    ]
)
data class OrganizedPetiteEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val sourceType: String,
    val sourceId: String?,
    val category: String?,
    val dateMillis: Long?,
    val linkedActivityName: String?,
    val activityCategory: String?,
    val isCompleted: Boolean,
    val priorityScore: Int,
    val burdenScore: Int?,
    val isSeverelyBehind: Boolean?,
    val totalStudyMinutesSinceD7: Int?,
    val studiedDaysSinceD7: Int?,
    val missedDaysSinceD7: Int?,
    val aiComment: String?,
    val estimatedMinutes: Int?,
    val stepsJson: String,
    val examDValue: Int?,
    val routineTimerDurationMillis: Long?,
    val routineTimerCategory: String?,
    val rank: Int,
    val isDismissed: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    // 미래 필터링용: "ACADEMIC"(공부/과제) / "DAILY"(일상) / null(미분류)
    val calendarTaskType: String? = null
)
