package com.example.flowlog.data.sync

import android.content.Context

/**
 * Incremental pull cursor for remote collections that are re-read on every sync
 * (activitySessions, activityStudyLinks, interactionDecisions).
 *
 * Without a cursor every sync downloaded the whole collection, which grows with every
 * recorded activity. With a cursor only documents whose client-written timestamp
 * (`modifiedTime`, `updatedAt`, `createdAt`, epoch millis) is at or after
 * `cursor - OVERLAP_MILLIS` are fetched. The overlap absorbs clock skew between the
 * phone and the browser that edited a record; a full pull still runs on first use and
 * at least every [FULL_PULL_INTERVAL_MILLIS] as a safety net.
 */
object RemotePullCursor {
    const val OVERLAP_MILLIS = 24L * 60 * 60 * 1000
    const val FULL_PULL_INTERVAL_MILLIS = 7L * 24 * 60 * 60 * 1000

    const val ACTIVITIES = "activities"
    const val STUDY_LINKS = "studyLinks"
    const val STUDY_DECISIONS = "studyDecisions"

    private const val PREFS = "flowlog_remote_pull_cursor"

    /**
     * Lower bound for the next query, or null when a full pull is required.
     * Pure function so the policy can be unit tested.
     */
    fun queryStart(cursor: Long?, lastFullPull: Long?, now: Long): Long? {
        if (cursor == null || lastFullPull == null) return null
        if (now - lastFullPull >= FULL_PULL_INTERVAL_MILLIS) return null
        if (cursor > now) return null // clock moved backwards; re-read everything
        return (cursor - OVERLAP_MILLIS).coerceAtLeast(0L)
    }

    fun since(context: Context, uid: String, key: String, now: Long = System.currentTimeMillis()): Long? {
        val prefs = prefs(context)
        val cursor = prefs.getLong(cursorKey(uid, key), -1L).takeIf { it >= 0L }
        val lastFull = prefs.getLong(fullKey(uid, key), -1L).takeIf { it >= 0L }
        return queryStart(cursor, lastFull, now)
    }

    /** Call only after every fetched document was applied, so failures are retried. */
    fun recordSuccess(context: Context, uid: String, key: String, pullStartedAt: Long, wasFullPull: Boolean) {
        prefs(context).edit().apply {
            putLong(cursorKey(uid, key), pullStartedAt)
            if (wasFullPull) putLong(fullKey(uid, key), pullStartedAt)
        }.apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun cursorKey(uid: String, key: String) = "$uid:$key:cursor"
    private fun fullKey(uid: String, key: String) = "$uid:$key:full"
}
