package com.example.flowlog.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSyncPolicyTest {
    private val me = "device-me"
    private val local = LocalTimer(category = "STUDY", title = "수학", startTime = 1_000_000L)

    private fun web(
        status: String = TimerSyncPolicy.STATUS_RUNNING,
        startTime: Long = 2_000_000L,
        updatedAt: Long? = startTime + 5L,
        category: String = "EXERCISE",
        title: String = "달리기",
        updatedBy: String = TimerSyncPolicy.BY_WEB,
        deviceId: String = "browser-1",
        pending: Boolean = false,
        cache: Boolean = false
    ) = RemoteTimer(
        status = status,
        category = category,
        title = title,
        startTime = startTime,
        updatedAtMillis = updatedAt,
        updatedBy = updatedBy,
        deviceId = deviceId,
        hasPendingWrites = pending,
        isFromCache = cache
    )

    private fun decide(local: LocalTimer?, remote: RemoteTimer?, lastEnded: Long? = null) =
        TimerSyncPolicy.decide(local, remote, me, lastEnded)

    @Test
    fun ownEchoThatMatchesLocalIsIgnored() {
        val echo = web(
            startTime = local.startTime, category = local.category, title = local.title,
            updatedBy = TimerSyncPolicy.BY_APP, deviceId = me
        )
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.ECHO), decide(local, echo))
    }

    @Test
    fun ownDocumentThatNoLongerMatchesIsRepublished() {
        // The phone stopped its timer but the "stopped" write never reached the server.
        val staleOwn = web(
            startTime = local.startTime, category = local.category, title = local.title,
            updatedBy = TimerSyncPolicy.BY_APP, deviceId = me
        )
        assertEquals(TimerSyncAction.Republish, decide(null, staleOwn))
    }

    @Test
    fun pendingWriteAndCachedSnapshotsAreIgnored() {
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.PENDING_WRITE), decide(null, web(pending = true)))
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.FROM_CACHE), decide(null, web(cache = true)))
    }

    @Test
    fun webWriteCarryingMyDeviceIdIsTreatedAsEcho() {
        // Never followed; it does not match the idle phone, so the phone's state is written again.
        val odd = web(deviceId = me)
        assertEquals(TimerSyncAction.Republish, decide(null, odd))
        assertFalse(TimerSyncPolicy.mirrors(odd, null))
        assertTrue(TimerSyncPolicy.mirrors(odd.copy(status = TimerSyncPolicy.STATUS_STOPPED), null))
    }

    @Test
    fun anotherPhoneIsNotFollowed() {
        val otherPhone = web(updatedBy = TimerSyncPolicy.BY_APP, deviceId = "device-other")
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.OTHER_APP_DEVICE), decide(null, otherPhone))
    }

    @Test
    fun idlePhoneStartsTheWebTimer() {
        val remote = web()
        assertEquals(TimerSyncAction.StartRemote(remote, replacesLocal = false), decide(null, remote))
    }

    @Test
    fun pausedWebTimerIsFollowedLikeRunning() {
        val remote = web(status = TimerSyncPolicy.STATUS_PAUSED)
        assertEquals(TimerSyncAction.StartRemote(remote, replacesLocal = false), decide(null, remote))
    }

    @Test
    fun webStopOfTheSameTimerCancelsLocallyWithoutRecord() {
        val stop = web(status = TimerSyncPolicy.STATUS_STOPPED, startTime = local.startTime, updatedAt = local.startTime + 60_000L)
        assertEquals(TimerSyncAction.CancelLocal, decide(local, stop))
    }

    @Test
    fun webStopOfAnotherTimerIsIgnored() {
        val stop = web(status = TimerSyncPolicy.STATUS_STOPPED, startTime = local.startTime - 1L)
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.OTHER_TIMER_STOPPED), decide(local, stop))
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.OTHER_TIMER_STOPPED), decide(null, stop))
    }

    @Test
    fun laterWebStartReplacesTheLocalTimer() {
        val remote = web(startTime = local.startTime + 10_000L, updatedAt = local.startTime + 10_000L)
        assertEquals(TimerSyncAction.StartRemote(remote, replacesLocal = true), decide(local, remote))
    }

    @Test
    fun earlierWebStartLosesAndPhoneStateIsRepublished() {
        val remote = web(startTime = local.startTime - 10_000L, updatedAt = local.startTime - 5_000L)
        assertEquals(TimerSyncAction.Republish, decide(local, remote))
    }

    @Test
    fun staleWebTimerThePhoneAlreadyEndedIsIgnored() {
        val remote = web(startTime = 3_000_000L)
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.STALE), decide(null, remote, lastEnded = 3_000_000L))
    }

    @Test
    fun sameTimerFromWebUpdatesTitleOrIsIgnored() {
        val renamed = web(startTime = local.startTime, category = local.category, title = "물리")
        assertEquals(TimerSyncAction.UpdateFromRemote(renamed), decide(local, renamed))
        val same = web(startTime = local.startTime, category = local.category, title = local.title)
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.SAME_TIMER), decide(local, same))
    }

    @Test
    fun invalidOrUntimedWebDocumentsAreIgnored() {
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.INVALID), decide(null, web(status = "done")))
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.INVALID), decide(null, web(category = "")))
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.INVALID), decide(null, web(updatedAt = null)))
    }

    @Test
    fun missingDocumentRepublishesOnlyARunningTimer() {
        assertEquals(TimerSyncAction.Republish, decide(local, null))
        assertEquals(TimerSyncAction.Ignore(IgnoreReason.NO_DOCUMENT), decide(null, null))
    }

    @Test
    fun documentFieldsUseTheAgreedShape() {
        val fields = TimerSyncPolicy.documentFields(local.copy(linkedTodoId = "42"), stopped = false, deviceId = me)
        assertEquals(
            setOf("status", "category", "title", "startTime", "pausedElapsedMs", "linkedTodoId", "updatedBy", "deviceId"),
            fields.keys
        )
        assertEquals("running", fields["status"])
        assertEquals("app", fields["updatedBy"])
        assertEquals("42", fields["linkedTodoId"])
        val stopped = TimerSyncPolicy.documentFields(local.copy(paused = true, pausedElapsedMs = 500L), stopped = true, deviceId = me)
        assertEquals("stopped", stopped["status"])
        assertEquals(0L, stopped["pausedElapsedMs"])
        assertEquals(local.startTime, stopped["startTime"])
    }

    @Test
    fun remoteFieldsAreReadDefensively() {
        val remote = TimerSyncPolicy.remoteFrom(
            mapOf("status" to "running", "category" to "STUDY", "startTime" to 5.0, "linkedTodoId" to 7L, "updatedBy" to "web"),
            updatedAtMillis = 9L,
            hasPendingWrites = false,
            isFromCache = false
        )
        assertEquals(5L, remote.startTime)
        assertEquals("7", remote.linkedTodoId)
        assertEquals("", remote.title)
        assertEquals(null, remote.deviceId)
    }
}
