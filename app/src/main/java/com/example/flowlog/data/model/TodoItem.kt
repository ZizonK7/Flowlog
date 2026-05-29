package com.example.flowlog.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class TodoCategory { NORMAL, REVIEW, ASSIGNMENT }

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
    val updatedAt: Long = System.currentTimeMillis(),
    val reviewStage: Int = 0,                      // 0=미시작, 1=D+1완료(D+7대기), 2=D+7완료(최종)
    val reviewStage1CompletedAt: Long? = null       // D+1 복습 완료 시각
)
