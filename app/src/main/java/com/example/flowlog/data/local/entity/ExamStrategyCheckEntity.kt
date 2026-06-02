package com.example.flowlog.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.flowlog.data.constants.SyncStatus

@Entity(
    tableName = "exam_strategy_checks",
    indices = [
        Index("userId"),
        Index("syncStatus"),
        Index(value = ["examTodoLegacyId", "strategyDValue"])
    ]
)
data class ExamStrategyCheckEntity(
    @PrimaryKey val checkId: String,
    val userId: String,
    val examTodoLegacyId: Long,
    val subjectTitleSnapshot: String,
    val examDateMillis: Long,
    val strategyDValue: Int,
    val strategyLabelSnapshot: String,
    val checkedAtMillis: Long,
    val checkedOnDateKey: Long,
    val checkedOnDaysUntilExam: Int,
    val undoneAtMillis: Long? = null,   // null = 유효한 체크, non-null = undo됨
    val syncStatus: String = SyncStatus.PENDING
)
