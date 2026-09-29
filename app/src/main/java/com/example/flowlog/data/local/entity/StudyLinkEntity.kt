package com.example.flowlog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.example.flowlog.data.constants.SyncStatus
import kotlinx.serialization.Serializable

/** segmentsJson 원소 (ms). 부모 활동 실제 시간 범위 안, 같은 부모의 다른 live 링크와 비중첩. */
@Serializable
data class StudySegment(val startTime: Long, val endTime: Long)

/**
 * users/{uid}/activityStudyLinks 로컬 사본 (owner-scoped, v24).
 * linkId = StudyIds.linkId(activityId, lessonId, phase).value
 * activityId = Firestore activity doc id (legacyId.toString() ?: Room activityId).
 * 총 시간은 부모 활동 duration 기준 — 링크는 구간만 참조.
 */
@Entity(
    tableName = "activity_study_links",
    primaryKeys = ["userId", "linkId"],
    indices = [
        Index(value = ["userId", "activityId"]),
        Index(value = ["userId", "lessonId"]),
        Index("syncStatus")
    ]
)
data class StudyLinkEntity(
    val linkId: String,
    val userId: String,
    val activityId: String,
    val courseId: String,
    val lessonId: String,
    val lessonDate: String,        // YYYY-MM-DD
    val phase: String,             // StudyPhase
    val segmentsJson: String,      // List<StudySegment>
    val snapshotJson: String,      // 확인 시점 lesson 스냅샷 {courseName,note,lessonDate,time,endTime,done,skipped}
    val sourceVersion: String,
    val confirmedAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "0")
    val remoteRevision: Long = 0L,
    val deletedAt: Long? = null,
    @ColumnInfo(defaultValue = "'PENDING'")
    val syncStatus: String = SyncStatus.PENDING
)
