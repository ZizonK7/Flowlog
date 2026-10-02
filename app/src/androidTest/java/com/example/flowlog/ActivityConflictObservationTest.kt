package com.example.flowlog

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.ActivityConflictEntity
import com.example.flowlog.data.local.entity.ActivityEntity
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityConflictObservationTest {
    private lateinit var db: FlowlogDatabase
    private fun activity() = ActivityEntity(
        activityId = "a", userId = "u", title = "test", category = "REST",
        startTime = 1, endTime = 2, durationMillis = 1, updatedAt = 100
    )
    private fun conflict() = ActivityConflictEntity("u", "a", 2, 100, "local", "remote", "test")

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext, FlowlogDatabase::class.java
        ).build()
    }
    @After fun tearDown() = db.close()

    @Test fun syncedDeletedRecordDoesNotReshowPersistedFailure() = runBlocking {
        db.activityDao().insertActivity(activity().copy(syncStatus = "SYNCED", isDeleted = true, updatedAt = 200, remoteRevision = 2))
        db.activityRevisionDao().saveConflict(conflict())
        assertTrue(db.activityRevisionDao().observeConflicts("u").first().isEmpty())
        // SYNCED alone must invalidate a failure even when its timestamp still matches.
        db.activityDao().updateActivity(activity().copy(syncStatus = "SYNCED"))
        assertTrue(db.activityRevisionDao().observeConflicts("u").first().isEmpty())
    }

    @Test fun newerEditAndMissingOrOtherOwnerRowsDoNotShowOldFailure() = runBlocking {
        val dao = db.activityRevisionDao()
        dao.saveConflict(conflict())
        assertTrue(dao.observeConflicts("u").first().isEmpty())
        db.activityDao().insertActivity(activity().copy(userId = "other"))
        assertTrue(dao.observeConflicts("u").first().isEmpty())
        db.activityDao().insertActivity(activity().copy(updatedAt = 101))
        assertTrue(dao.observeConflicts("u").first().isEmpty())
    }

    @Test fun currentPendingDeletesAndSameRevisionValidationFailuresRemainVisible() = runBlocking {
        val dao = db.activityRevisionDao()
        db.activityDao().insertActivity(activity().copy(isDeleted = true, remoteRevision = 2))
        dao.saveConflict(conflict())
        assertEquals(listOf(conflict()), dao.observeConflicts("u").first())
        assertTrue(dao.observeConflicts("other").first().isEmpty())
    }

    @Test fun observingBothTablesRemovesConflictImmediatelyAfterAck() = runBlocking {
        val dao = db.activityRevisionDao()
        db.activityDao().insertActivity(activity())
        dao.saveConflict(conflict())
        assertEquals(1, dao.observeConflicts("u").first().size)
        val removed = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5000) { dao.observeConflicts("u").first { it.isEmpty() } }
        }
        // Leave the conflict row intact, reproducing a delayed failure save/clear race.
        dao.ackActivityUpload("u", "a", 100, 2)
        assertTrue(removed.await().isEmpty())
    }
}
