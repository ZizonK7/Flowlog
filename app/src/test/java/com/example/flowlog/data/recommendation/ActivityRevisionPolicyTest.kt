package com.example.flowlog.data.recommendation

import com.example.flowlog.data.remote.ActivityRevisionPolicy
import com.example.flowlog.data.remote.ActivityRevisionConflictException
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityRevisionPolicyTest {
    @Test fun legacyRevisionStartsAtOne() { assertEquals(1L, ActivityRevisionPolicy.next("a",0,0,"m",null)) }
    @Test fun lostAckRetryDoesNotAdvanceAgain() { assertEquals(4L, ActivityRevisionPolicy.next("a",3,4,"m","m")) }
    @Test(expected = ActivityRevisionConflictException::class)
    fun editedAfterLostAckRequiresExplicitResolution() { ActivityRevisionPolicy.next("a",3,4,"new","m") }
    @Test fun explicitKeepLocalUsesDisplayedRemoteRevision() { assertEquals(5L, ActivityRevisionPolicy.next("a",4,4,"new","m")) }
    @Test(expected = ActivityRevisionConflictException::class)
    fun changedAgainAfterUserDecisionStillConflicts() { ActivityRevisionPolicy.next("a",4,5,"new","other") }
}
