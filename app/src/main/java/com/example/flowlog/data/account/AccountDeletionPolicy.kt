package com.example.flowlog.data.account

import java.util.concurrent.TimeUnit

/** Where an account stands after "delete account and data". */
sealed interface AccountDeletionState {
    /** No deletion was requested. */
    object None : AccountDeletionState

    /** Requested; signing in again within the grace period can still cancel it. */
    data class Pending(val daysLeft: Int) : AccountDeletionState

    /** The grace period is over; the server job deletes the data and the sign-in account. */
    object Expired : AccountDeletionState
}

/**
 * Pure rules for the 14-day grace period of account deletion. No Android or Firebase APIs.
 * The server job (`tools/account-deletion/selection.js`) uses the same 14 days.
 */
object AccountDeletionPolicy {
    const val GRACE_DAYS = 14
    val GRACE_MILLIS: Long = TimeUnit.DAYS.toMillis(GRACE_DAYS.toLong())
    private val DAY_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

    /** The word the user types to enable the final delete button. */
    const val CONFIRMATION_WORD = "삭제"

    fun state(requestedAtMillis: Long?, nowMillis: Long): AccountDeletionState {
        if (requestedAtMillis == null) return AccountDeletionState.None
        val deadline = requestedAtMillis + GRACE_MILLIS
        if (nowMillis >= deadline) return AccountDeletionState.Expired
        return AccountDeletionState.Pending(daysLeft(requestedAtMillis, nowMillis))
    }

    /**
     * Whole days left, rounded up so the last partial day still reads "1 day".
     * A request time ahead of the device clock counts as just requested (at most [GRACE_DAYS]).
     */
    fun daysLeft(requestedAtMillis: Long, nowMillis: Long): Int {
        val remaining = (requestedAtMillis + GRACE_MILLIS - nowMillis).coerceIn(0L, GRACE_MILLIS)
        return ((remaining + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
    }

    fun canCancel(requestedAtMillis: Long?, nowMillis: Long): Boolean =
        state(requestedAtMillis, nowMillis) is AccountDeletionState.Pending

    fun isExpired(requestedAtMillis: Long?, nowMillis: Long): Boolean =
        state(requestedAtMillis, nowMillis) == AccountDeletionState.Expired

    fun isConfirmationTyped(input: String): Boolean = input.trim() == CONFIRMATION_WORD
}
