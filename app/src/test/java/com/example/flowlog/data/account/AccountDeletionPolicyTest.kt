package com.example.flowlog.data.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AccountDeletionPolicyTest {
    private val day = TimeUnit.DAYS.toMillis(1)
    private val requested = 1_800_000_000_000L

    @Test
    fun noRequestMeansNone() {
        assertEquals(AccountDeletionState.None, AccountDeletionPolicy.state(null, requested))
        assertFalse(AccountDeletionPolicy.canCancel(null, requested))
        assertFalse(AccountDeletionPolicy.isExpired(null, requested))
    }

    @Test
    fun rightAfterRequestFourteenDaysAreLeft() {
        assertEquals(AccountDeletionState.Pending(14), AccountDeletionPolicy.state(requested, requested))
        assertEquals(AccountDeletionState.Pending(14), AccountDeletionPolicy.state(requested, requested + 1))
        assertTrue(AccountDeletionPolicy.canCancel(requested, requested))
    }

    @Test
    fun partialDaysRoundUp() {
        assertEquals(13, AccountDeletionPolicy.daysLeft(requested, requested + day))
        assertEquals(1, AccountDeletionPolicy.daysLeft(requested, requested + 14 * day - 1))
    }

    @Test
    fun exactlyFourteenDaysLaterIsExpired() {
        assertEquals(AccountDeletionState.Expired, AccountDeletionPolicy.state(requested, requested + 14 * day))
        assertTrue(AccountDeletionPolicy.isExpired(requested, requested + 30 * day))
        assertFalse(AccountDeletionPolicy.canCancel(requested, requested + 14 * day))
    }

    @Test
    fun requestAheadOfDeviceClockCountsAsJustRequested() {
        assertEquals(AccountDeletionState.Pending(14), AccountDeletionPolicy.state(requested + 3 * day, requested))
    }

    @Test
    fun confirmationNeedsTheExactWord() {
        assertTrue(AccountDeletionPolicy.isConfirmationTyped("삭제"))
        assertTrue(AccountDeletionPolicy.isConfirmationTyped(" 삭제 "))
        assertFalse(AccountDeletionPolicy.isConfirmationTyped(""))
        assertFalse(AccountDeletionPolicy.isConfirmationTyped("삭제해"))
        assertFalse(AccountDeletionPolicy.isConfirmationTyped("delete"))
    }
}
