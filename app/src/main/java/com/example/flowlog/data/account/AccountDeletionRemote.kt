package com.example.flowlog.data.account

import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * The deletion reservation on the server: `users/{uid}/flowlog/account`.
 * Both rule sets let the owner write it (`users/{userId}/flowlog/{document=**}` in the
 * original rules, `users/{userId}/flowlog/{documentId}` in the proposed rules).
 * The server job (`tools/account-deletion`) finds it with a collection-group query on
 * `flowlog` and permanently deletes the account after [AccountDeletionPolicy.GRACE_DAYS].
 */
object AccountDeletionRemote {
    const val FIELD_REQUESTED_AT = "deletionRequestedAt"
    const val FIELD_REQUESTED_BY = "deletionRequestedBy"

    private fun accountDoc(uid: String) =
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .collection("flowlog").document("account")

    /** Marks the account for deletion with the server's clock. */
    suspend fun requestDeletion(uid: String) {
        accountDoc(uid).set(
            mapOf(
                FIELD_REQUESTED_AT to FieldValue.serverTimestamp(),
                FIELD_REQUESTED_BY to "app"
            ),
            SetOptions.merge()
        ).awaitResult()
    }

    /** Request time in epoch millis, or null when no deletion is pending. Throws when unreadable. */
    suspend fun readRequestedAt(uid: String): Long? {
        val snapshot = accountDoc(uid).get().awaitResult()
        return snapshot.getTimestamp(FIELD_REQUESTED_AT)?.toDate()?.time
    }

    /** Cancels the reservation (signed in again within the grace period). */
    suspend fun cancelDeletion(uid: String) {
        accountDoc(uid).set(
            mapOf(
                FIELD_REQUESTED_AT to FieldValue.delete(),
                FIELD_REQUESTED_BY to FieldValue.delete()
            ),
            SetOptions.merge()
        ).awaitResult()
    }
}
