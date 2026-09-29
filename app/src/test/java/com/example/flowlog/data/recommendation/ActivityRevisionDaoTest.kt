package com.example.flowlog.data.recommendation

import com.example.flowlog.data.local.dao.ActivityRevisionDao
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.entity.ActivityConflictEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.*
import org.junit.Test

/** Exercises production transaction methods with a deterministic storage adapter. */
class ActivityRevisionDaoTest {
    private class MemoryDao(var row: ActivityEntity) : ActivityRevisionDao() {
        override suspend fun getOwnedActivity(userId: String, activityId: String) = row.takeIf { it.userId == userId && it.activityId == activityId }
        override suspend fun reclassifyActivity(userId: String, activityId: String, newCategory: String, now: Long) = 0
        override suspend fun markActivityDeleted(userId: String, activityId: String, now: Long) = 0
        override suspend fun markSyncedIfUnchanged(userId: String, activityId: String, uploadedUpdatedAt: Long, revision: Long): Int {
            if (getOwnedActivity(userId,activityId) == null || row.updatedAt != uploadedUpdatedAt || row.syncStatus != "PENDING") return 0
            row = row.copy(syncStatus="SYNCED",remoteRevision=revision); return 1
        }
        override suspend fun advanceRemoteRevision(userId: String, activityId: String, revision: Long): Int {
            if (getOwnedActivity(userId,activityId) == null || row.remoteRevision >= revision) return 0
            row = row.copy(remoteRevision=revision);return 1
        }
        override suspend fun insertIfAbsent(entity: ActivityEntity) = -1L
        override suspend fun updateActivityRow(entity: ActivityEntity): Int { row=entity;return 1 }
        override suspend fun saveConflict(conflict: ActivityConflictEntity) {}
        override fun observeConflicts(userId: String) = flowOf(emptyList<ActivityConflictEntity>())
        override suspend fun clearConflict(userId: String, activityId: String) {}
        override suspend fun rebaseLocal(userId: String, activityId: String, expectedUpdatedAt: Long, revision: Long) = 0
    }
    private fun row() = ActivityEntity(activityId="a",userId="u",title="local",category="ETC",startTime=100,endTime=200,durationMillis=100,updatedAt=10,remoteRevision=1)
    @Test fun ackDoesNotLoseEditMadeWhileUploadInFlight() = runBlocking {
        val dao=MemoryDao(row().copy(updatedAt=11));assertFalse(dao.ackActivityUpload("u","a",10,2));assertEquals("PENDING",dao.row.syncStatus);assertEquals(2L,dao.row.remoteRevision)
    }
    @Test fun unchangedUploadIsAcknowledged() = runBlocking {
        val dao=MemoryDao(row());assertTrue(dao.ackActivityUpload("u","a",10,2));assertEquals("SYNCED",dao.row.syncStatus)
    }
    @Test fun pendingEditIsNeverSilentlyOverwritten() = runBlocking {
        val dao=MemoryDao(row());assertFalse(dao.applyRestoredActivity(row().copy(title="remote",remoteRevision=2,syncStatus="SYNCED")));assertEquals("local",dao.row.title)
    }
    @Test fun explicitRemoteChoiceChecksLocalRevisionTimestamp() = runBlocking {
        val dao=MemoryDao(row());val remote=row().copy(title="remote",remoteRevision=2,syncStatus="SYNCED")
        assertFalse(dao.applyRestoredActivity(remote,9));assertTrue(dao.applyRestoredActivity(remote,10));assertEquals("remote",dao.row.title)
    }
    @Test fun newerSyncedRemoteTombstoneReplacesOldSyncedRow() = runBlocking {
        val dao=MemoryDao(row().copy(syncStatus="SYNCED"));assertTrue(dao.applyRestoredActivity(row().copy(remoteRevision=2,syncStatus="SYNCED",isDeleted=true,deletedAt=100)));assertTrue(dao.row.isDeleted)
    }
    @Test fun ackCannotChangeOtherAccount() = runBlocking { val dao=MemoryDao(row());assertFalse(dao.ackActivityUpload("other","a",10,2));assertEquals(1L,dao.row.remoteRevision) }
}
