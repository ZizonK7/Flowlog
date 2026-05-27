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
    val updatedAt: Long = System.currentTimeMillis()
)
