package com.example.flowlog.ui.viewmodel

import com.example.flowlog.data.model.ActivitySession
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** Counts shown before an import is confirmed. Period covers only the new records. */
data class ActivityImportSummary(
    val total: Int,
    val newCount: Int,
    val duplicateCount: Int,
    val invalidCount: Int,
    val firstStartMillis: Long?,
    val lastEndMillis: Long?
)

sealed interface ActivityImportResult {
    /** The file is not JSON, or its top level is not an array. */
    object NotActivityJson : ActivityImportResult

    /** [toInsert] keeps each record's file `id` and `localActivityId`, with `remoteRevision` reset to 0. */
    data class Ready(
        val toInsert: List<ActivitySession>,
        val summary: ActivityImportSummary
    ) : ActivityImportResult
}

/**
 * Pure reading of a file written by "export as JSON" ([ActivityExportFormatter.toJson]).
 * No Android APIs, so it is unit tested directly.
 *
 * - Broken items (missing required fields, wrong types, blank category, end before
 *   start, longer than 24 hours, in the future, impossible duration) are skipped and counted.
 *   A zero-length record is valid only with a zero duration (the app saves check-type
 *   records that way).
 * - Duplicates are skipped and counted: same record identity as a row already on this
 *   device (any account, deleted or not, because Room would replace that row), or the
 *   same category + start + end as a saved record of the current account, or a repeat
 *   inside the file.
 */
object ActivityImportParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * @param existingActivityIds every Room `activityId` on this device, deleted rows included,
     *   plus `legacy_activity_<legacyId>` for every row that has a legacy id (the remote
     *   document id comes from it, so two rows must never share one).
     * @param existing the current account's saved (non-deleted) records.
     */
    fun parse(
        text: String,
        existingActivityIds: Set<String>,
        existing: List<ActivitySession>,
        nowMillis: Long
    ): ActivityImportResult {
        val root: JsonElement = runCatching { json.parseToJsonElement(text.removePrefix("﻿")) }
            .getOrNull() ?: return ActivityImportResult.NotActivityJson
        val items = root as? JsonArray ?: return ActivityImportResult.NotActivityJson

        val seenIds = existingActivityIds.toMutableSet()
        val seenRanges = existing.map { rangeKey(it) }.toMutableSet()
        val toInsert = mutableListOf<ActivitySession>()
        var duplicates = 0
        var invalid = 0

        items.forEach { element ->
            val session = runCatching { json.decodeFromJsonElement(ActivitySession.serializer(), element) }
                .getOrNull()
            if (session == null || !isValid(session, nowMillis)) {
                invalid++
                return@forEach
            }
            val keys = identityKeys(session)
            val range = rangeKey(session)
            if (keys.any { it in seenIds } || range in seenRanges) {
                duplicates++
                return@forEach
            }
            seenIds += keys
            seenRanges += range
            toInsert += session.copy(remoteRevision = 0L)
        }

        val sorted = toInsert.sortedWith(compareBy<ActivitySession> { it.startTime }.thenBy { it.id })
        return ActivityImportResult.Ready(
            toInsert = sorted,
            summary = ActivityImportSummary(
                total = items.size,
                newCount = sorted.size,
                duplicateCount = duplicates,
                invalidCount = invalid,
                firstStartMillis = sorted.minOfOrNull { it.startTime },
                lastEndMillis = sorted.maxOfOrNull { it.endTime }
            )
        )
    }

    /**
     * The Room `activityId` this record will be stored under, the same rule as
     * `toActivityEntity`: the file's `localActivityId`, else `legacy_activity_<id>`.
     * Null only when both are missing (a new id is generated on insert).
     */
    fun recordIdentity(session: ActivitySession): String? =
        session.localActivityId ?: if (session.id != 0L) legacyKey(session.id) else null

    /** Every key that must not already exist: the stored identity and the legacy-id form. */
    fun identityKeys(session: ActivitySession): List<String> = listOfNotNull(
        session.localActivityId,
        if (session.id != 0L) legacyKey(session.id) else null
    ).distinct()

    fun legacyKey(legacyId: Long): String = "legacy_activity_$legacyId"

    fun isValid(session: ActivitySession, nowMillis: Long): Boolean {
        if (session.category.isBlank()) return false
        val length = session.endTime - session.startTime
        val rangeOk = when {
            length == 0L -> session.durationMillis == 0L
            else -> ManualActivityValidator.validate(
                startMillis = session.startTime,
                endMillis = session.endTime,
                nowMillis = nowMillis,
                existing = emptyList()
            ).canSave
        }
        return rangeOk &&
            session.endTime <= nowMillis &&
            session.startTime > 0L &&
            session.durationMillis in 0L..length
    }

    private fun rangeKey(session: ActivitySession): Triple<String, Long, Long> =
        Triple(session.category, session.startTime, session.endTime)
}
