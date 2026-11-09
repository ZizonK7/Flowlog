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
    @Test
    fun nestedMapsAndListsKeepNulls() {
        assertEquals(
            """{"a":[1,null,"x"],"m":{"k":null,"j":2}}""",
            EventMetadata.json("a" to listOf(1, null, "x"), "m" to linkedMapOf("k" to null, "j" to 2), "skip" to null)
        )
    }

    @Test
    fun activityStartedCarriesViaAndMergesInactivityReminder() {
        assertEquals(
            """{"category":"STUDY","sourceType":"MANUAL","startTime":1000,"via":"manual_button"}""",
            EventMetadata.activityStarted(EventMetadata.StartVia.MANUAL_BUTTON, "STUDY", 1000L, sourceType = "MANUAL")
        )
        assertEquals(
            """{"category":"TODO","sourceType":"MANUAL","linkedTodoId":42,"pendingTitle":"Read","startTime":1000,""" +
                """"via":"todo","afterInactivityReminder":true,"reminderNotificationId":"n1","reminderClickedAt":900}""",
            EventMetadata.activityStarted(
                via = EventMetadata.StartVia.TODO,
                category = "TODO",
                startTime = 1000L,
                sourceType = "MANUAL",
                linkedTodoId = 42L,
                pendingTitle = "Read",
                reminderNotificationId = "n1",
                reminderClickedAt = 900L
            )
        )
        // pinned only when true; a click time without a notification id is not a reminder start;
        // linkedTodoId 0 and a blank title are "none".
        assertEquals(
            """{"category":"MEAL","startTime":5,"via":"overlapping","pinned":true}""",
            EventMetadata.activityStarted(
                via = EventMetadata.StartVia.OVERLAPPING,
                category = "MEAL",
                startTime = 5L,
                linkedTodoId = 0L,
                pendingTitle = " ",
                pinned = true,
                reminderClickedAt = 3L
            )
        )
    }

    @Test
    fun titleSuggestionShownKeepsAtMostFiveSlots() {
        assertEquals(
            """{"category":"STUDY","slots":["a","b","c","d","e"],"algorithm":"title_ranker_v1"}""",
            EventMetadata.titleSuggestionShown("STUDY", listOf("a", "b", "c", "d", "e", "f"))
        )
        assertEquals(
            """{"category":"STUDY","slot":0,"title":"Math \"1\""}""",
            EventMetadata.titleSuggestionPicked("STUDY", 0, "Math \"1\"")
        )
    }

    @Test
    fun titleSourceCountsTheRecordedChoiceOnlyForTheSameTitle() {
        assertEquals("suggestion", EventMetadata.titleSource("Math", "Math", "suggestion", defaultTitle = "Study"))
        // Edited after the pick (or changed elsewhere) without a recorded source.
        assertEquals("unknown", EventMetadata.titleSource("Math 2", "Math", "suggestion", defaultTitle = "Study"))
        assertEquals("default", EventMetadata.titleSource("Study", null, null, defaultTitle = "Study"))
        // Typing the default label by hand is still typed.
        assertEquals("typed", EventMetadata.titleSource("Study", "Study", "typed", defaultTitle = "Study"))
        assertEquals("preset", EventMetadata.titleSource("Read ch.3", "Read ch.3", "preset", defaultTitle = "Study"))
    }

    @Test
    fun activitySavedWritesSlotOnlyForAKeptSuggestion() {
        assertEquals(
            """{"via":"timer_stop","titleSource":"suggestion","slot":2,"shownSlots":["a","b"]}""",
            EventMetadata.activitySaved("timer_stop", "suggestion", 2, listOf("a", "b"))
        )
        assertEquals(
            """{"via":"timer_stop","titleSource":"typed"}""",
            EventMetadata.activitySaved("timer_stop", "typed", 2, emptyList())
        )
        assertEquals("""{"via":"import"}""", EventMetadata.activitySaved(EventMetadata.SaveVia.IMPORT))
    }

    @Test
    fun activityChangedListsOnlyChangedFieldsAndNoteLengthsOnly() {
        val before = EventMetadata.ActivityFields(
            title = "A",
            category = "STUDY",
            note = "secret memo",
            startTime = 1L,
            endTime = 2L,
            isFavorite = false,
            linkedTodoId = null
        )
        val after = before.copy(title = "B", note = "longer secret memo!", isFavorite = true)

        val json = EventMetadata.activityChanged(before, after)

        assertEquals(
            """{"changed":{"title":{"from":"A","to":"B"},"note":{"fromLen":11,"toLen":19},""" +
                """"isFavorite":{"from":false,"to":true}}}""",
            json
        )
        assertFalse("note text must not be written", json.contains("secret"))
    }

    @Test
    fun activityChangedEdgeCases() {
        val before = EventMetadata.ActivityFields("A", "STUDY", null, 1L, 2L, false, null)
        assertEquals("""{"changed":{}}""", EventMetadata.activityChanged(before, before))
        // null and empty note are the same (both length 0).
        assertEquals("""{"changed":{}}""", EventMetadata.activityChanged(before, before.copy(note = "")))
        // Same length, different text: still changed, still only lengths.
        assertEquals(
            """{"changed":{"note":{"fromLen":2,"toLen":2}}}""",
            EventMetadata.activityChanged(before.copy(note = "ab"), before.copy(note = "cd"))
        )
        assertEquals(
            """{"changed":{"startTime":{"from":1,"to":5},"linkedTodoId":{"from":null,"to":7}}}""",
            EventMetadata.activityChanged(before, before.copy(startTime = 5L, linkedTodoId = 7L))
        )
        assertEquals("""{"beforeFound":false}""", EventMetadata.activityChanged(null, before))
    }

    @Test
    fun buttonRecommendationShape() {
        assertEquals(
            """{"category":"SCHOOL","messageId":"m1"}""",
            EventMetadata.buttonRecommendation("SCHOOL", "m1")
        )
    }
}
