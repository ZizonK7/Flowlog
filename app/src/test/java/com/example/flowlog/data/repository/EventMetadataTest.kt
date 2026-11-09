package com.example.flowlog.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventMetadataTest {

    @Test
    fun jsonKeepsOrderAndSkipsNulls() {
        val json = EventMetadata.json("a" to 1, "b" to null, "c" to true, "d" to 5L, "e" to "x")

        assertEquals("""{"a":1,"c":true,"d":5,"e":"x"}""", json)
        assertEquals("{}", EventMetadata.json("only" to null))
    }

    @Test
    fun stringsAreEscaped() {
        val json = EventMetadata.json("t" to "say \"hi\"\\ \n\t\u0001")

        assertEquals("""{"t":"say \"hi\"\\ \n\t\u0001"}""", json)
    }

    @Test
    fun activityStoppedLeavesOutUnknownCategoryAndElapsed() {
        assertEquals("""{"reason":"discarded"}""", EventMetadata.activityStopped("discarded"))
        assertEquals("""{"reason":"discarded"}""", EventMetadata.activityStopped("discarded", category = " "))
        assertEquals(
            """{"reason":"paused","category":"STUDY","elapsedMs":60000}""",
            EventMetadata.activityStopped("paused", "STUDY", 60_000L)
        )
    }

    @Test
    fun settingAndSyncShapes() {
        assertEquals(
            """{"key":"inactivity_reminder_enabled","value":false}""",
            EventMetadata.settingChanged(EventMetadata.SettingKey.INACTIVITY_REMINDER, false)
        )
        assertEquals(
            """{"mode":"eligible","attempted":0,"success":0,"failure":0,"deferred":true}""",
            EventMetadata.syncFinished("eligible", 0, 0, 0, deferred = true)
        )
        assertEquals(
            """{"mode":"all","attempted":0,"success":0,"failure":0,"deferred":false,"error":"IOException"}""",
            EventMetadata.syncFinished("all", 0, 0, 0, deferred = false, error = "IOException")
        )
        assertTrue(EventMetadata.isSyncFailure(failureCount = 1, threw = false))
        assertTrue(EventMetadata.isSyncFailure(failureCount = 0, threw = true))
        assertFalse(EventMetadata.isSyncFailure(failureCount = 0, threw = false))
    }

    @Test
    fun notificationShownAppendsExtraAfterBaseFields() {
        assertEquals(
            """{"type":"TOOTHBRUSH_REMINDER","notificationId":"7","shownAt":100,"silenced":true}""",
            EventMetadata.notificationShown(
                type = EventMetadata.NotificationType.TOOTHBRUSH_REMINDER,
                notificationId = "7",
                shownAt = 100L,
                extra = linkedMapOf("reminderType" to null, "silenced" to true)
            )
        )
        assertEquals(
            """{"type":"PLANNED_TODO_REMINDER","notificationId":"i1","shownAt":100,"dismissedAt":250}""",
            EventMetadata.notificationDismissed("PLANNED_TODO_REMINDER", "i1", dismissedAt = 250L, shownAt = 100L)
        )
    }

    @Test
    fun migrationIsRecordedOnlyWhenAStoredVersionDiffers() {
        assertNull(EventMetadata.migrationFromVersion(storedVersion = null, openedVersion = 24))
        assertNull(EventMetadata.migrationFromVersion(storedVersion = 24, openedVersion = 24))
        assertEquals(23, EventMetadata.migrationFromVersion(storedVersion = 23, openedVersion = 24))
        assertEquals("""{"from":23,"to":24}""", EventMetadata.migrationCompleted(23, 24))
    }
}
