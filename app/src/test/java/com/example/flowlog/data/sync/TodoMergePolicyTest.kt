package com.example.flowlog.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoMergePolicyTest {
    private val t0 = 1_800_000_000_000L

    private fun local(
        updatedAt: Long = t0,
        completed: Boolean = false,
        completedAt: Long? = null,
        deleted: Boolean = false,
        pending: Boolean = false,
        todoId: String = "legacy_todo_1",
        calendarOnly: Boolean = false
    ) = LocalTodo(
        todoId = todoId,
        docId = todoId.removePrefix(TodoMergePolicy.LEGACY_PREFIX),
        updatedAt = updatedAt,
        isCompleted = completed,
        completedAt = completedAt,
        isDeleted = deleted,
        pending = pending,
        calendarOnly = calendarOnly
    )

    private fun remote(updatedAt: Long? = t0, completed: Boolean = false, completedAt: Long? = null) =
        RemoteTodo(docId = "1", updatedAt = updatedAt, isCompleted = completed, completedAt = completedAt)

    @Test
    fun newRemoteTodoIsInserted() {
        assertEquals(TodoMergeAction.InsertRemote, TodoMergePolicy.merge(null, remote(), null))
    }

    @Test
    fun remoteCompletionReachesASyncedPhoneRow() {
        val action = TodoMergePolicy.merge(local(), remote(updatedAt = t0 + 10, completed = true, completedAt = t0 + 10), null)
        assertEquals(TodoMergeAction.ApplyRemote(keepLocalCompletion = false), action)
    }

    @Test
    fun syncedRowIgnoresAnOlderOrEqualServerCopy() {
        assertEquals(TodoMergeAction.Ignore, TodoMergePolicy.merge(local(), remote(updatedAt = t0), null))
        assertEquals(TodoMergeAction.Ignore, TodoMergePolicy.merge(local(), remote(updatedAt = t0 - 1), null))
    }

    @Test
    fun remoteDeletionRemovesOnlySyncedRows() {
        val synced = local(todoId = "legacy_todo_1")
        val unsentNew = local(todoId = "legacy_todo_2", pending = true)
        val calendar = local(todoId = "calendar_petite_x", calendarOnly = true)
        val stillThere = local(todoId = "legacy_todo_3")
        val candidates = TodoMergePolicy.deletionCandidates(
            locals = listOf(synced, unsentNew, calendar, stillThere),
            presentIds = setOf("3"),
            full = true,
            pullStartedAt = t0 + 1_000
        )
        assertEquals(listOf(synced), candidates)
    }

    @Test
    fun incrementalPullJudgesOnlyOpenRows() {
        val open = local(todoId = "legacy_todo_1")
        val done = local(todoId = "legacy_todo_2", completed = true, completedAt = t0)
        val candidates = TodoMergePolicy.deletionCandidates(listOf(open, done), emptySet(), full = false, pullStartedAt = t0 + 1)
        assertEquals(listOf(open), candidates)
    }

    @Test
    fun newerPendingPhoneChangeWins() {
        val action = TodoMergePolicy.merge(local(updatedAt = t0 + 20, pending = true), remote(updatedAt = t0 + 10), null)
        assertEquals(TodoMergeAction.KeepLocal(adoptRemoteCompletion = false), action)
    }

    @Test
    fun newerServerChangeWinsOverAnOlderPendingChange() {
        val action = TodoMergePolicy.merge(local(updatedAt = t0 + 10, pending = true), remote(updatedAt = t0 + 20), null)
        assertEquals(TodoMergeAction.ApplyRemote(keepLocalCompletion = false), action)
    }

    @Test
    fun simultaneousEditGoesToTheServer() {
        val action = TodoMergePolicy.merge(local(updatedAt = t0 + 10, pending = true), remote(updatedAt = t0 + 10), null)
        assertEquals(TodoMergeAction.ApplyRemote(keepLocalCompletion = false), action)
    }

    @Test
    fun webCompletionIsNotUndoneByLaterPhoneTimerTime() {
        // Web completed at t0+10; the phone (which never saw it) added timer time at t0+20.
        val action = TodoMergePolicy.merge(
            local(updatedAt = t0 + 20, pending = true),
            remote(updatedAt = t0 + 10, completed = true, completedAt = t0 + 10),
            completionOnServer = null
        )
        assertEquals(TodoMergeAction.KeepLocal(adoptRemoteCompletion = true), action)
    }

    @Test
    fun deliberatePhoneUndoOfASeenCompletionStands() {
        val action = TodoMergePolicy.merge(
            local(updatedAt = t0 + 20, pending = true),
            remote(updatedAt = t0 + 10, completed = true, completedAt = t0 + 10),
            completionOnServer = t0 + 10
        )
        assertEquals(TodoMergeAction.KeepLocal(adoptRemoteCompletion = false), action)
    }

    @Test
    fun unsentPhoneCompletionSurvivesANewerWebEdit() {
        val phoneDone = local(updatedAt = t0 + 10, completed = true, completedAt = t0 + 10, pending = true)
        assertEquals(
            TodoMergeAction.ApplyRemote(keepLocalCompletion = true),
            TodoMergePolicy.merge(phoneDone, remote(updatedAt = t0 + 20), completionOnServer = null)
        )
        // Once the server held that completion, a newer web "incomplete" is a deliberate undo.
        assertEquals(
            TodoMergeAction.ApplyRemote(keepLocalCompletion = false),
            TodoMergePolicy.merge(phoneDone, remote(updatedAt = t0 + 20), completionOnServer = t0 + 10)
        )
    }

    @Test
    fun phoneDeleteNotYetUploadedLosesOnlyToANewerServerEdit() {
        val deleted = local(updatedAt = t0 + 10, deleted = true, pending = true)
        assertEquals(TodoMergeAction.KeepLocal(adoptRemoteCompletion = false), TodoMergePolicy.merge(deleted, remote(updatedAt = t0 + 5), null))
        assertEquals(TodoMergeAction.ApplyRemote(keepLocalCompletion = false), TodoMergePolicy.merge(deleted, remote(updatedAt = t0 + 15), null))
    }

    @Test
    fun syncedDeletedRowComesBackWhenTheDocumentDoes() {
        // Deleted on the phone after a pull found the document missing; the document is back.
        val deleted = local(updatedAt = t0, deleted = true)
        assertEquals(TodoMergeAction.ApplyRemote(keepLocalCompletion = false), TodoMergePolicy.merge(deleted, remote(updatedAt = t0), null))
        assertEquals(TodoMergeAction.ReuploadLocal, TodoMergePolicy.merge(deleted, remote(updatedAt = t0 - 1), null))
    }

    @Test
    fun cursorBoundaryMatchesTheQuery() {
        assertTrue(TodoMergePolicy.changedSince(updatedAt = 100L, since = 100L))
        assertFalse(TodoMergePolicy.changedSince(updatedAt = 99L, since = 100L))
        assertFalse(TodoMergePolicy.changedSince(updatedAt = null, since = 100L))
        assertTrue(TodoMergePolicy.changedSince(updatedAt = null, since = null))
        // A row changed at the moment the pull started is left for the next pull.
        val justChanged = local(updatedAt = t0)
        assertEquals(emptyList<LocalTodo>(), TodoMergePolicy.deletionCandidates(listOf(justChanged), emptySet(), true, pullStartedAt = t0))
    }

    @Test
    fun webMadeDocumentsAreAdoptedButPhoneAndCalendarOnesAreNot() {
        assertTrue(TodoMergePolicy.needsAdoption("aB3xYz", 0L))
        assertTrue(TodoMergePolicy.needsAdoption("aB3xYz", null))
        assertFalse(TodoMergePolicy.needsAdoption("1760000000000", 1760000000000L))
        assertFalse(TodoMergePolicy.needsAdoption("calendar_petite_e1", 0L))
        assertEquals("legacy_todo_42", TodoMergePolicy.todoIdFor("42", 42L))
        assertEquals("aB3xYz", TodoMergePolicy.todoIdFor("aB3xYz", 0L))
        assertEquals("42", TodoMergePolicy.docIdFor("legacy_todo_42", 42L))
        assertEquals("aB3xYz", TodoMergePolicy.docIdFor("aB3xYz", null))
    }
}
