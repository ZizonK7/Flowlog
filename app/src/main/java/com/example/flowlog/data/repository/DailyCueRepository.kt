package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.local.dao.DailyCueDao
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.DailyCueEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class DailyCueRecord(
    val id: Long,
    val label: String,
    val title: String,
    val timerDurationMillis: Long?,
    val timerCategory: String,
    val order: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null
)

class DailyCueRepository(context: Context) {
    private val dao: DailyCueDao = FlowlogDatabase.getInstance(context).dailyCueDao()

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    private fun userIdFlow(): Flow<String> = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.uid ?: "anonymous")
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid ?: "anonymous")
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun observeCueRecords(): Flow<List<DailyCueRecord>> {
        return userIdFlow().flatMapLatest { uid ->
            dao.observeCues(uid)
        }.map { entities -> entities.map { it.toRecord() } }
            .distinctUntilChanged()
    }

    suspend fun backfillExistingCuesIfNeeded(cues: List<DailyCueRecord>, fallbackCreatedAt: Long) {
        if (cues.isEmpty()) return
        val uid = userId
        if (dao.countCues(uid) > 0) return
        dao.upsertCues(cues.map { cue ->
            cue.toEntity(
                userId = uid,
                createdAt = cue.createdAt.takeIf { it > 0L } ?: fallbackCreatedAt,
                updatedAt = cue.updatedAt.takeIf { it > 0L } ?: fallbackCreatedAt
            )
        })
    }

    suspend fun upsertCue(cue: DailyCueRecord) {
        val uid = userId
        val existing = dao.getCue(uid, cue.id)
        val now = System.currentTimeMillis()
        dao.upsertCue(
            cue.toEntity(
                userId = uid,
                createdAt = existing?.createdAt ?: cue.createdAt.takeIf { it > 0L } ?: now,
                updatedAt = now,
                archivedAt = existing?.archivedAt
            )
        )
    }

    suspend fun archiveCue(cueId: Long) {
        val now = System.currentTimeMillis()
        dao.archiveCue(userId, cueId, archivedAt = now, updatedAt = now)
    }

    private fun DailyCueRecord.toEntity(
        userId: String,
        createdAt: Long,
        updatedAt: Long,
        archivedAt: Long? = this.archivedAt
    ): DailyCueEntity {
        return DailyCueEntity(
            userId = userId,
            cueId = id,
            label = label,
            title = title,
            timerDurationMillis = timerDurationMillis,
            timerCategory = timerCategory,
            sortOrder = order,
            createdAt = createdAt,
            updatedAt = updatedAt,
            archivedAt = archivedAt
        )
    }

    private fun DailyCueEntity.toRecord(): DailyCueRecord {
        return DailyCueRecord(
            id = cueId,
            label = label,
            title = title,
            timerDurationMillis = timerDurationMillis,
            timerCategory = timerCategory,
            order = sortOrder,
            createdAt = createdAt,
            updatedAt = updatedAt,
            archivedAt = archivedAt
        )
    }
}
