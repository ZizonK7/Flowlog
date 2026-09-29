package com.example.flowlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.example.flowlog.data.constants.SyncStatus

/**
 * users/{uid}/interactionDecisions 로컬 사본 (v24). 한 번 기록되면 불변 (wire revision 은 항상 1).
 * decisionId = StudyIds.decisionId(mutationId, kind).value — 같은 mutation 재시도는 중복 없이 무시.
 */
@Entity(
    tableName = "study_decisions",
    primaryKeys = ["userId", "decisionId"],
    indices = [
        Index(value = ["userId", "createdAt"]),
        Index(value = ["userId", "proposalId"]),
        Index("syncStatus")
    ]
)
data class StudyDecisionEntity(
    val decisionId: String,
    val userId: String,
    val proposalId: String,
    val kind: String,              // StudyDecisionKind
    val activityId: String? = null,
    val category: String? = null,
    val localDate: String,         // YYYY-MM-DD
    val minuteOfDay: Int,
    val weekday: Int,              // Sun = 0
    val outcome: String,           // StudyDecisionOutcome
    val createdAt: Long,
    val payloadJson: String,       // {fromCategory,toCategory,linkIds,...}
    @ColumnInfo(defaultValue = "'PENDING'")
    val syncStatus: String = SyncStatus.PENDING
)
