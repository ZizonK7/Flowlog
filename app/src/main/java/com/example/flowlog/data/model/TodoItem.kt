package com.example.flowlog.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Long = 0L,
    val title: String,
    val isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val dueDate: Long? = null,
    val accumulatedMillis: Long = 0L
)
