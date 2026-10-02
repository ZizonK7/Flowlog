package com.example.flowlog.data.recommendation

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.remote.ActivityRevisionContent
import com.example.flowlog.data.remote.ActivityRevisionPolicy
import com.example.flowlog.data.remote.ActivityRevisionConflictException
import org.junit.Assert.*
import org.junit.Test

class ActivityRevisionContentTest {
    private val local = mapOf<String, Any?>(
        "id" to 123L, "title" to "휴식", "category" to "REST",
        "startTime" to 1000L, "endTime" to 6000L, "durationMillis" to 5000L,
        "note" to null, "tags" to emptyList<String>(), "exerciseSets" to emptyList<Any>(),
        "isFavorite" to false, "linkedTodoId" to null, "linkedPetiteId" to null,
        "sourceType" to "MANUAL", "sourceId" to null, "originalCategory" to null,
        "modifiedTime" to 10L, "isDeleted" to false, "deletedAt" to null
    )

    @Test fun legacyContentCatchesUpWithoutAnotherWrite() {
        val remote = local - setOf("isDeleted", "deletedAt", "linkedPetiteId", "originalCategory") +
            mapOf("modifiedTime" to 5L, "revision" to 2L)
        val matches = ActivityRevisionContent.matches(remote, local)
        assertTrue(matches)
        assertEquals(2L, ActivityRevisionPolicy.next("123", 0, 2, "new", "legacy", matches))
    }

    @Test fun differencesHiddenFromConflictSummaryAreStillConflicts() {
        for ((key, value) in mapOf(
            "note" to "웹 메모", "tags" to listOf("tag"), "isFavorite" to true,
            "linkedTodoId" to 2L, "linkedPetiteId" to "p", "sourceId" to "s",
            "sourceType" to "AUTO",
            "durationMillis" to 4000L, "isDeleted" to true, "deletedAt" to 50L,
            "title" to "다름", "category" to "STUDY", "startTime" to 999L,
            "endTime" to 6001L, "id" to 124L, "exerciseSets" to listOf(mapOf("reps" to 2))
        )) {
            assertFalse(key, ActivityRevisionContent.matches(local + (key to value), local))
        }
    }

    @Test fun originalCategoryUsesTheSamePreservationRuleAsTheUploader() {
        assertTrue(ActivityRevisionContent.matches(local + ("originalCategory" to "STUDY"), local))
        assertFalse(ActivityRevisionContent.matches(local, local + ("originalCategory" to "STUDY")))
    }

    @Test(expected = ActivityRevisionConflictException::class)
    fun genuineStaleEditIsNeverRebased() {
        ActivityRevisionPolicy.next("123", 0, 2, "new", "remote",
            ActivityRevisionContent.matches(local + ("note" to "changed"), local))
    }

    @Test fun missingDocumentAndConflictingAliasesDoNotMatch() {
        assertFalse(ActivityRevisionContent.matches(null, local))
        assertFalse(ActivityRevisionContent.matches(local + ("start_time" to 999), local))
        assertTrue(ActivityRevisionContent.matches(local + ("start_time" to 1000.0), local))
        assertFalse(ActivityRevisionContent.matches(local + ("durationMinutes" to 99.0), local))
        assertTrue(ActivityRevisionContent.matches(local + ("durationMinutes" to 5000 / 60000.0), local))
    }

    @Test fun nestedNumericRepresentationsMatchWithoutLosingPrecision() {
        val sets = listOf(mapOf("name" to "run", "reps" to 2, "durationMillis" to null))
        val remoteSets = listOf(mapOf("name" to "run", "reps" to 2L))
        assertTrue(ActivityRevisionContent.matches(local + ("exerciseSets" to remoteSets), local + ("exerciseSets" to sets)))
        assertFalse(ActivityRevisionContent.matches(local + ("id" to 9007199254740993L), local + ("id" to 9007199254740992L)))
    }

    @Test fun retryIdentityIgnoresAckMetadataButRetainsEditIdentity() {
        val a = ActivitySession(id=123, title="휴식", category="REST", startTime=1000, endTime=6000, durationMillis=5000, modifiedTime=10)
        val original = ActivityRevisionContent.mutationId("123", a, null)
        assertEquals(original, ActivityRevisionContent.mutationId("123", a.copy(remoteRevision=2, localActivityId="local"), null))
        assertNotEquals(original, ActivityRevisionContent.mutationId("123", a.copy(modifiedTime=11), null))
        assertNotEquals(original, ActivityRevisionContent.mutationId("123", a.copy(note="new"), null))
        assertNotEquals(original, ActivityRevisionContent.mutationId("123", a, 50))
    }
}
