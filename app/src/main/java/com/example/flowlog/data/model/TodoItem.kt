package com.example.flowlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TodoCategory { NORMAL, TODAY, REVIEW, ASSIGNMENT, UNIVERSITY_EXAM }

@Serializable
data class TodoItem(
    val id: Long = 0L,
    val title: String,
    val category: TodoCategory = TodoCategory.NORMAL,
    val createdAt: Long = System.currentTimeMillis(),
    val selectedDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val accumulatedSeconds: Long = 0L,
    val burdenLevel: String? = null,
    val burdenGroupKey: String? = null,
    val burdenScore: Int = 0,
    val burdenReasonJson: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val reviewStage: Int = 0,                      // 0=미시작, 1=D+1완료(D+7대기), 2=D+7완료(최종)
    val reviewStage1CompletedAt: Long? = null       // D+1 복습 완료 시각
)

data class RecommendedTodoBlock(
    val itemId: String,
    val recommendationId: String,
    val todoId: Long,
    val title: String,
    val category: TodoCategory? = null,
    val selectedDate: Long? = null,
    val burdenLevel: String,
    val reason: String?,
    val plannedStartMillis: Long,
    val plannedEndMillis: Long,
    val recommendedDurationMinutes: Int,
    val userActionStatus: String,
    val notificationScheduledAtMillis: Long? = null,
    val isBubbleOnly: Boolean = false
)
