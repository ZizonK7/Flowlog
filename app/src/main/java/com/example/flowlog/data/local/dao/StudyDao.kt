package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.entity.StudyDecisionEntity
import com.example.flowlog.data.local.entity.StudyLinkEntity
import com.example.flowlog.data.local.entity.StudySegment
import com.example.flowlog.data.study.StudyDecisionKind
import com.example.flowlog.data.study.StudyDecisionOutcome
import com.example.flowlog.data.study.StudyIds
import com.example.flowlog.data.study.StudyPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

enum class StudyLinkSaveResult { SAVED, INVALID, PARENT_MISSING, OUT_OF_BOUNDS, OVERLAP }

/** activity_study_links / study_decisions — 모든 쿼리는 userId 로 scope. */
@Dao
abstract class StudyDao {

    // ── Links ────────────────────────────────────────────────────────────

    @Query("SELECT * FROM activity_study_links WHERE userId = :userId AND linkId = :linkId LIMIT 1")
    abstract suspend fun getLink(userId: String, linkId: String): StudyLinkEntity?

    @Query("SELECT * FROM activity_study_links WHERE userId = :userId AND activityId = :activityDocId AND deletedAt IS NULL")
    abstract suspend fun getLiveLinksForActivity(userId: String, activityDocId: String): List<StudyLinkEntity>

    @Query("SELECT * FROM activity_study_links WHERE userId = :userId AND activityId = :activityDocId AND deletedAt IS NULL ORDER BY confirmedAt")
    abstract fun observeLinksForActivity(userId: String, activityDocId: String): Flow<List<StudyLinkEntity>>

    @Query("SELECT * FROM activity_study_links WHERE userId = :userId AND lessonId = :lessonId AND deletedAt IS NULL ORDER BY lessonDate, phase")
    abstract fun observeLinksForLesson(userId: String, lessonId: String): Flow<List<StudyLinkEntity>>

    @Query("SELECT * FROM activity_study_links WHERE userId = :userId AND syncStatus = 'PENDING'")
    abstract suspend fun getPendingLinks(userId: String): List<StudyLinkEntity>

    /** docId = legacyId.toString() 또는 Room activityId (StudyIds.activityDocId). */
    @Query("SELECT * FROM activities WHERE userId = :userId AND isDeleted = 0 AND (activityId = :docId OR (legacyId IS NOT NULL AND CAST(legacyId AS TEXT) = :docId)) LIMIT 1")
    abstract suspend fun findLiveParent(userId: String, docId: String): ActivityEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertLinkIfAbsent(link: StudyLinkEntity): Long

    @Update
    abstract suspend fun updateLink(link: StudyLinkEntity): Int

    @Transaction
    open suspend fun restoreLink(link: StudyLinkEntity) {
        val old = getLink(link.userId, link.linkId)
        if (old == null) insertLinkIfAbsent(link)
        else if (old.syncStatus == SyncStatus.SYNCED && old.remoteRevision < link.remoteRevision) updateLink(link)
    }

    /**
     * 사용자가 확인한 링크 저장. 결정적 linkId, 부모 소유/실제 시간 범위,
     * 같은 부모의 다른 live 링크와 비중첩을 검증한다.
     */
    @Transaction
    open suspend fun saveLink(link: StudyLinkEntity): StudyLinkSaveResult {
        if (link.phase !in StudyPhase.ALL) return StudyLinkSaveResult.INVALID
        if (link.linkId != StudyIds.linkId(link.activityId, link.lessonId, link.phase).value) return StudyLinkSaveResult.INVALID
        val segments = decodeSegments(link.segmentsJson) ?: return StudyLinkSaveResult.INVALID
        if (segments.isEmpty() || segments.size > 8 || segments.any { it.endTime <= it.startTime }) return StudyLinkSaveResult.INVALID
        val parent = findLiveParent(link.userId, link.activityId) ?: return StudyLinkSaveResult.PARENT_MISSING
        val parentEnd = parent.endTime ?: return StudyLinkSaveResult.PARENT_MISSING
        if (parent.category == "MOVE" && link.phase == StudyPhase.COURSE_SESSION) return StudyLinkSaveResult.INVALID
        if (segments.any { it.startTime < parent.startTime || it.endTime > parentEnd }) return StudyLinkSaveResult.OUT_OF_BOUNDS
        val others = getLiveLinksForActivity(link.userId, link.activityId)
            .filter { it.linkId != link.linkId }
            .flatMap { decodeSegments(it.segmentsJson).orEmpty() }
        if (overlaps(segments + others)) return StudyLinkSaveResult.OVERLAP

        val existing = getLink(link.userId, link.linkId)
        val row = link.copy(
            remoteRevision = existing?.remoteRevision ?: 0L,
            updatedAt = maxOf(link.updatedAt, (existing?.updatedAt ?: 0L) + 1),
            deletedAt = null,
            syncStatus = SyncStatus.PENDING
        )
        if (existing == null) insertLinkIfAbsent(row) else updateLink(row)
        return StudyLinkSaveResult.SAVED
    }

    @Query("UPDATE activity_study_links SET deletedAt = :now, updatedAt = MAX(:now, updatedAt + 1), syncStatus = 'PENDING' WHERE userId = :userId AND linkId = :linkId AND deletedAt IS NULL")
    abstract suspend fun markLinkDeleted(userId: String, linkId: String, now: Long): Int

    @Query("UPDATE activity_study_links SET syncStatus = 'SYNCED', remoteRevision = :revision WHERE userId = :userId AND linkId = :linkId AND updatedAt = :uploadedUpdatedAt AND syncStatus = 'PENDING'")
    abstract suspend fun markLinkSyncedIfUnchanged(userId: String, linkId: String, uploadedUpdatedAt: Long, revision: Long): Int

    @Query("UPDATE activity_study_links SET remoteRevision = :revision WHERE userId = :userId AND linkId = :linkId AND remoteRevision < :revision")
    abstract suspend fun advanceLinkRemoteRevision(userId: String, linkId: String, revision: Long): Int

    @Transaction
    open suspend fun ackLinkUpload(userId: String, linkId: String, uploadedUpdatedAt: Long, revision: Long): Boolean {
        if (markLinkSyncedIfUnchanged(userId, linkId, uploadedUpdatedAt, revision) > 0) return true
        advanceLinkRemoteRevision(userId, linkId, revision)
        return false
    }

    // ── Decisions (불변) ─────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertDecisionIfAbsent(decision: StudyDecisionEntity): Long

    /** 같은 mutation 재시도는 무시되고 false. */
    suspend fun recordDecision(decision: StudyDecisionEntity): Boolean {
        require(decision.kind in StudyDecisionKind.ALL) { "Unknown decision kind: ${decision.kind}" }
        require(decision.outcome in StudyDecisionOutcome.ALL) { "Unknown outcome: ${decision.outcome}" }
        require(decision.decisionId.endsWith("~" + decision.kind)) { "decisionId must be mutationId~kind" }
        require(decision.weekday in 0..6 && decision.minuteOfDay in 0 until 1440)
        return insertDecisionIfAbsent(decision) != -1L
    }

    @Query("SELECT * FROM study_decisions WHERE userId = :userId AND createdAt >= :sinceMillis ORDER BY createdAt")
    abstract fun observeDecisionsSince(userId: String, sinceMillis: Long): Flow<List<StudyDecisionEntity>>

    @Query("SELECT * FROM study_decisions WHERE userId = :userId AND syncStatus = 'PENDING' ORDER BY createdAt")
    abstract suspend fun getPendingDecisions(userId: String): List<StudyDecisionEntity>

    @Query("UPDATE study_decisions SET syncStatus = 'SYNCED' WHERE userId = :userId AND decisionId = :decisionId")
    abstract suspend fun markDecisionSynced(userId: String, decisionId: String): Int

    private fun decodeSegments(raw: String): List<StudySegment>? =
        runCatching { segmentJson.decodeFromString<List<StudySegment>>(raw) }.getOrNull()

    private fun overlaps(segments: List<StudySegment>): Boolean =
        segments.sortedBy { it.startTime }.zipWithNext().any { (a, b) -> b.startTime < a.endTime }

    companion object {
        private val segmentJson = Json { ignoreUnknownKeys = true }
    }
}
