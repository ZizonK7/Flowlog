package com.example.flowlog.data.sync

/**
 * How a Todo on the phone and the same Todo in `users/{uid}/flowlog/data/todos` are merged
 * when the PC web app (or another device) changed it. Pure, so it can be unit tested;
 * [TodoRemotePull] and the Todo upload in [FirebaseSyncDataSource] apply the result.
 *
 * Rules
 * 1. The later `updatedAt` wins the whole Todo (both sides write client epoch millis). A tie
 *    goes to the server. A phone row that is already SYNCED only takes newer server copies.
 * 2. A completion is not undone by a side that never saw it. When the winner is incomplete
 *    and the loser completed the Todo, the completion stays, unless that completion
 *    (its completedAt) is already known to be on the server ([completionOnServer]): then the
 *    winner's "incomplete" is a deliberate undo. Without this, adding timer time to a Todo
 *    on the phone after the web completed it would bring the Todo back.
 * 3. A Todo the web deleted (the web and the phone both delete the document) is soft-deleted
 *    on the phone only when the phone has nothing left to upload for it: rows still PENDING,
 *    such as new Todos not uploaded yet, are never touched.
 */
object TodoMergePolicy {

    fun merge(local: LocalTodo?, remote: RemoteTodo, completionOnServer: Long?): TodoMergeAction {
        if (local == null) return TodoMergeAction.InsertRemote
        if (local.calendarOnly) return TodoMergeAction.Ignore
        val remoteUpdatedAt = remote.updatedAt ?: 0L

        if (!local.pending) {
            return when {
                local.isDeleted && remoteUpdatedAt < local.updatedAt -> TodoMergeAction.ReuploadLocal
                local.isDeleted -> TodoMergeAction.ApplyRemote(keepLocalCompletion = false)
                remoteUpdatedAt > local.updatedAt -> TodoMergeAction.ApplyRemote(keepLocalCompletion = false)
                else -> TodoMergeAction.Ignore
            }
        }

        // The phone has an unsent change.
        return if (local.updatedAt > remoteUpdatedAt) {
            val adoptRemoteCompletion = !local.isDeleted &&
                remote.isCompleted && !local.isCompleted &&
                remote.completedAt != null && remote.completedAt != completionOnServer
            TodoMergeAction.KeepLocal(adoptRemoteCompletion)
        } else {
            val keepLocalCompletion = !local.isDeleted &&
                local.isCompleted && !remote.isCompleted &&
                local.completedAt != null && local.completedAt != completionOnServer
            TodoMergeAction.ApplyRemote(keepLocalCompletion)
        }
    }

    /**
     * Phone rows whose server document may have been deleted.
     *
     * @param full true when [presentIds] lists every document (a full pull); false for an
     *   incremental pull, where [presentIds] holds only the open (incomplete) documents and
     *   the changed ones, so only open phone rows can be judged and each one must still be
     *   confirmed with a direct read.
     * @param pullStartedAt rows changed at or after the pull started are left for next time.
     */
    fun deletionCandidates(
        locals: List<LocalTodo>,
        presentIds: Set<String>,
        full: Boolean,
        pullStartedAt: Long
    ): List<LocalTodo> = locals.filter { local ->
        !local.pending && !local.isDeleted && !local.calendarOnly &&
            local.updatedAt < pullStartedAt &&
            (full || !local.isCompleted) &&
            local.docId !in presentIds
    }

    /** Same boundary as the incremental query (`updatedAt >= since`). */
    fun changedSince(updatedAt: Long?, since: Long?): Boolean =
        since == null || (updatedAt != null && updatedAt >= since)

    /** Document id of a phone row: the numeric legacy id, otherwise the row id (as the upload). */
    fun docIdFor(todoId: String, legacyId: Long?): String = legacyId?.toString() ?: todoId

    /** Phone row id of a document: "legacy_todo_<id>" for numbered Todos (as the full restore). */
    fun todoIdFor(docId: String, numericId: Long?): String =
        if (numericId != null && numericId != 0L) "$LEGACY_PREFIX$numericId" else docId

    /**
     * A document made outside the phone's numbering (the web writes `id: 0` with a random
     * document id). The phone keys every Todo action on a numeric id, so such a document is
     * given one ("adopted") before it is stored on the phone.
     */
    fun needsAdoption(docId: String, numericId: Long?): Boolean =
        (numericId == null || numericId == 0L) && docId.toLongOrNull() == null && !isCalendarDocument(docId)

    /** Rows of the web calendar ("calendar_petite_…", "calendar_todo_…") the phone may have uploaded. */
    fun isCalendarDocument(docId: String): Boolean = docId.startsWith("calendar_")

    const val LEGACY_PREFIX = "legacy_todo_"
}

/** The phone's row, reduced to what the merge needs. */
data class LocalTodo(
    val todoId: String,
    val docId: String,
    val updatedAt: Long,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val isDeleted: Boolean,
    /** syncStatus PENDING: the phone still has to upload this row. */
    val pending: Boolean,
    /** Rows from the web calendar only (calendarSourceId, "calendar_" ids): not in todos. */
    val calendarOnly: Boolean = false
)

/** The server document, reduced to what the merge needs. */
data class RemoteTodo(
    val docId: String,
    val updatedAt: Long?,
    val isCompleted: Boolean,
    val completedAt: Long?
)

sealed interface TodoMergeAction {
    data object InsertRemote : TodoMergeAction

    /** Take the server copy; [keepLocalCompletion] = but keep the phone's completion and upload it. */
    data class ApplyRemote(val keepLocalCompletion: Boolean) : TodoMergeAction

    /** Keep the phone's unsent change; [adoptRemoteCompletion] = but take the server's completion. */
    data class KeepLocal(val adoptRemoteCompletion: Boolean) : TodoMergeAction

    /** The phone deleted it, but the delete did not reach the server: upload it again. */
    data object ReuploadLocal : TodoMergeAction

    data object Ignore : TodoMergeAction
}
