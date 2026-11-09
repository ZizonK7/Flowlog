package com.example.flowlog.data.repository

import android.content.Context
import com.example.flowlog.data.constants.SyncStatus
import com.example.flowlog.data.external.ExternalSampleRow
import com.example.flowlog.data.external.ExternalSamples
import com.example.flowlog.data.external.UpsertAction
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.ExternalSampleEntity
import com.google.firebase.auth.FirebaseAuth

data class ExternalUpsertCounts(val inserted: Int, val updated: Int, val unchanged: Int)

/** Writes external samples into Room: new ids inserted, changed ones overwritten, same ones left. */
class ExternalSampleRepository(context: Context) {
    private val dao = FlowlogDatabase.getInstance(context).externalSampleDao()
    private val auth = FirebaseAuth.getInstance()

    private val userId: String
        get() = auth.currentUser?.uid ?: "anonymous"

    suspend fun upsert(rows: List<ExternalSampleRow>, fetchedAt: Long): ExternalUpsertCounts {
        val owner = userId
        val unique = rows.distinctBy { it.sampleId }
        // SQLite caps bound variables (999 on older versions), so look up in chunks.
        val existing = unique.map { it.sampleId }
            .chunked(LOOKUP_CHUNK)
            .flatMap { dao.getByIds(it) }
            .associateBy { it.sampleId }
        var inserted = 0
        var updated = 0
        var unchanged = 0
        val writes = mutableListOf<ExternalSampleEntity>()
        for (row in unique) {
            val old = existing[row.sampleId]
            when (ExternalSamples.upsertAction(old?.toRow(), row, sameOwner = old == null || old.userId == owner)) {
                UpsertAction.INSERT -> {
                    writes += row.toEntity(owner, fetchedAt, createdAt = fetchedAt)
                    inserted++
                }
                UpsertAction.UPDATE -> {
                    writes += row.toEntity(owner, fetchedAt, createdAt = old?.createdAt ?: fetchedAt)
                    updated++
                }
                UpsertAction.UNCHANGED -> unchanged++
            }
        }
        if (writes.isNotEmpty()) dao.upsertAll(writes)
        return ExternalUpsertCounts(inserted = inserted, updated = updated, unchanged = unchanged)
    }

    suspend fun countSince(provider: String, sinceMillis: Long): Int =
        dao.countSince(userId, provider, sinceMillis)

    private fun ExternalSampleEntity.toRow() = ExternalSampleRow(
        sampleId = sampleId,
        provider = provider,
        kind = kind,
        startTime = startTime,
        endTime = endTime,
        value = value,
        unit = unit,
        sourcePackage = sourcePackage,
        payloadJson = payloadJson
    )

    private fun ExternalSampleRow.toEntity(owner: String, fetchedAt: Long, createdAt: Long) = ExternalSampleEntity(
        sampleId = sampleId,
        userId = owner,
        provider = provider,
        kind = kind,
        startTime = startTime,
        endTime = endTime,
        value = value,
        unit = unit,
        sourcePackage = sourcePackage,
        payloadJson = payloadJson,
        fetchedAt = fetchedAt,
        createdAt = createdAt,
        syncStatus = SyncStatus.PENDING
    )

    private companion object {
        const val LOOKUP_CHUNK = 500
    }
}
