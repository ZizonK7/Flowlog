package com.example.flowlog.data.model

data class ExamStrategyCard(
    val examTodoId: Long,
    val examTitle: String,
    val examDateMillis: Long,
    val examCreatedAt: Long,
    val dValue: Int,           // 0=D-Day, 1=D-1, ..., 7=D-7
    val daysUntilExam: Int,    // 오늘 기준 실제 남은 날
    val strategyLabel: String,
    val strategyUrl: String,
    val isChecked: Boolean = false,
    val checkId: String? = null
)
