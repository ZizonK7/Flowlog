package com.example.flowlog.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemotePullCursorTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 1_800_000_000_000L

    @Test
    fun firstPullIsFull() {
        assertNull(RemotePullCursor.queryStart(cursor = null, lastFullPull = null, now = now))
        assertNull(RemotePullCursor.queryStart(cursor = now - day, lastFullPull = null, now = now))
    }

    @Test
    fun incrementalPullStartsOneOverlapBeforeCursor() {
        val cursor = now - 60_000L
        assertEquals(
            cursor - RemotePullCursor.OVERLAP_MILLIS,
            RemotePullCursor.queryStart(cursor = cursor, lastFullPull = now - day, now = now)
        )
    }

    @Test
    fun weeklySafetyNetForcesFullPull() {
        assertNull(
            RemotePullCursor.queryStart(
                cursor = now - 60_000L,
                lastFullPull = now - RemotePullCursor.FULL_PULL_INTERVAL_MILLIS,
                now = now
            )
        )
    }

    @Test
    fun cursorInTheFutureForcesFullPull() {
        assertNull(RemotePullCursor.queryStart(cursor = now + day, lastFullPull = now - day, now = now))
    }

    @Test
    fun queryStartNeverNegative() {
        assertEquals(0L, RemotePullCursor.queryStart(cursor = 1_000L, lastFullPull = 0L, now = 2_000L))
    }
}
