package com.example.flowlog.data.recommendation

import com.example.flowlog.data.model.ActivitySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityTitleSuggestionRankerTest {

    private val categoryWork = "WORK"
    private val defaultTitleWork = "Work"
    private val fixedNowMillis = 1_700_000_000_000L
    private val dayMillis = 24L * 60 * 60 * 1000L

    @Test
    fun weekOldThousandUsesLoseToSingleNewCompletion() {
        val history = List(1_000) { createSession("Old habit", fixedNowMillis - 7 * dayMillis) } +
            createSession("New activity", fixedNowMillis)
        val ranked = ActivityTitleSuggestionRanker.rankedScores(categoryWork, history, defaultTitleWork, fixedNowMillis)
        assertEquals("New activity", ranked.first().title)
        assertTrue(ranked.last().score <= 50.0)
    }

    @Test
    fun rankingUsesCompletionInsteadOfStartTime() {
        val history = listOf(
            createSession("Long activity", fixedNowMillis, fixedNowMillis - 2 * dayMillis),
            createSession("Short activity", fixedNowMillis - dayMillis)
        )
        assertEquals("Long activity", ActivityTitleSuggestionRanker.suggest(
            categoryWork, history, defaultTitleWork, fixedNowMillis
        ).first())
    }

    @Test
    fun reuseDoesNotRestoreAncientFrequencyAsFreshUses() {
        val ancient = List(30) { createSession("Resumed", fixedNowMillis - 140 * dayMillis) } +
            createSession("Resumed", fixedNowMillis)
        val recent = List(30) { createSession("Regular", fixedNowMillis - dayMillis) } +
            createSession("Regular", fixedNowMillis)
        val ranked = ActivityTitleSuggestionRanker.rankedScores(categoryWork, ancient + recent, defaultTitleWork, fixedNowMillis)
        assertEquals("Regular", ranked.first().title)
        assertTrue(ranked.last().weightedUses < 1.1)
    }

    @Test
    fun oldEligibleTitlesRemainAvailableWithoutExpiryCutoff() {
        val history = listOf(createSession("Occasional", fixedNowMillis - 365 * dayMillis))
        assertEquals(listOf("Occasional"), ActivityTitleSuggestionRanker.suggest(
            categoryWork, history, defaultTitleWork, fixedNowMillis
        ))
        assertTrue(ActivityTitleSuggestionRanker.suggest(categoryWork, emptyList(), defaultTitleWork, fixedNowMillis).isEmpty())
    }

    @Test
    fun representativeHistoryKeepsRecentHabitsAndReplacesStaleSlot() {
        val history = (1..4).flatMap { project ->
            (1..10).map { day -> createSession("Active $project", fixedNowMillis - day * dayMillis) }
        } + List(100) { createSession("Stale", fixedNowMillis - 30 * dayMillis) }
        val before = ActivityTitleSuggestionRanker.suggest(categoryWork, history, defaultTitleWork, fixedNowMillis)
        val after = ActivityTitleSuggestionRanker.rankedScores(
            categoryWork, history + createSession("New", fixedNowMillis), defaultTitleWork, fixedNowMillis
        )
        assertTrue(before.contains("Stale"))
        assertEquals(listOf("Active 1", "Active 2", "Active 3", "Active 4", "New"), after.take(5).map { it.title })
        after.forEach { println("recommendation example: ${it.title}, score=${it.score}, weightedUses=${it.weightedUses}") }
    }

    private fun createSession(
        title: String,
        endTime: Long,
        startTime: Long = endTime - 3_600_000L,
        category: String = categoryWork,
        durationMillis: Long = (endTime - startTime).coerceAtLeast(0L),
        id: Long = 0L
    ): ActivitySession = ActivitySession(
        id = id,
        category = category,
        title = title,
        startTime = startTime,
        endTime = endTime,
        durationMillis = durationMillis
    )

    @Test
    fun singleNewSessionScores55() {
        val sessions = listOf(
            createSession(title = "Design Architecture", endTime = fixedNowMillis)
        )

        val candidates = ActivityTitleSuggestionRanker.rankedScores(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals(1, candidates.size)
        val candidate = candidates.first()
        assertEquals("Design Architecture", candidate.title)
        assertEquals(55.0, candidate.score, 0.001)
        assertEquals(1.0, candidate.weightedUses, 0.001)
        assertEquals(fixedNowMillis, candidate.lastCompletedAt)
    }

    @Test
    fun heavyStaleHistoryLosesVsNew() {
        val staleEndTime = fixedNowMillis - 30 * dayMillis
        val staleSessions = (1..20).map { i ->
            createSession(title = "Legacy System", endTime = staleEndTime - i * 1_000L)
        }
        val newSession = createSession(title = "Modern Refactor", endTime = fixedNowMillis)
        val allSessions = staleSessions + newSession

        val suggestions = ActivityTitleSuggestionRanker.suggest(
            category = categoryWork,
            activities = allSessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals("Modern Refactor", suggestions.first())

        val ranked = ActivityTitleSuggestionRanker.rankedScores(
            category = categoryWork,
            activities = allSessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )
        val newScore = ranked.first { it.title == "Modern Refactor" }.score
        val staleScore = ranked.first { it.title == "Legacy System" }.score

        assertTrue(newScore > staleScore)
        assertTrue(staleScore < 5.2)
    }

    @Test
    fun noGuaranteedNewestInsertionWithFiveActiveEstablishedTitles() {
        val establishedTitles = (1..5).map { "Active Project $it" }
        val establishedSessions = establishedTitles.flatMap { title ->
            listOf(
                createSession(title = title, endTime = fixedNowMillis),
                createSession(title = title, endTime = fixedNowMillis - dayMillis)
            )
        }
        val newSession = createSession(title = "One-Off Inquiry", endTime = fixedNowMillis)
        val allSessions = establishedSessions + newSession

        val suggestions = ActivityTitleSuggestionRanker.suggest(
            category = categoryWork,
            activities = allSessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals(5, suggestions.size)
        assertFalse(suggestions.contains("One-Off Inquiry"))
        assertTrue(suggestions.containsAll(establishedTitles))
    }

    @Test
    fun periodicRecentUseRanksHigherThanSingleNew() {
        val periodicSessions = listOf(
            createSession(title = "Daily Standup", endTime = fixedNowMillis),
            createSession(title = "Daily Standup", endTime = fixedNowMillis - dayMillis),
            createSession(title = "Daily Standup", endTime = fixedNowMillis - 2 * dayMillis)
        )
        val singleSession = listOf(
            createSession(title = "Ad-hoc Review", endTime = fixedNowMillis)
        )

        val suggestions = ActivityTitleSuggestionRanker.suggest(
            category = categoryWork,
            activities = periodicSessions + singleSession,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals("Daily Standup", suggestions.first())
    }

    @Test
    fun rawTitleCaseAndSpacePreservation() {
        val sessions = listOf(
            createSession(title = " Coding ", endTime = fixedNowMillis),
            createSession(title = "Coding", endTime = fixedNowMillis - dayMillis),
            createSession(title = "coding", endTime = fixedNowMillis - 2 * dayMillis)
        )

        val ranked = ActivityTitleSuggestionRanker.rankedScores(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        val titles = ranked.map { it.title }
        assertEquals(3, titles.size)
        assertTrue(titles.contains(" Coding "))
        assertTrue(titles.contains("Coding"))
        assertTrue(titles.contains("coding"))
    }

    @Test
    fun sameExactTitleAggregationOnly() {
        val sessions = listOf(
            createSession(title = "Code Review", endTime = fixedNowMillis),
            createSession(title = "Code Review", endTime = fixedNowMillis - dayMillis),
            createSession(title = "code review", endTime = fixedNowMillis)
        )

        val ranked = ActivityTitleSuggestionRanker.rankedScores(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        val exactAggregated = ranked.first { it.title == "Code Review" }
        val caseDifferent = ranked.first { it.title == "code review" }

        assertTrue(exactAggregated.weightedUses > 1.9)
        assertEquals(1.0, caseDifferent.weightedUses, 0.001)
        assertTrue(exactAggregated.score > caseDifferent.score)
    }

    @Test
    fun invalidEndTimeExclusion() {
        val sessions = listOf(
            createSession(title = "Zero EndTime", endTime = 0L),
            createSession(title = "Negative EndTime", endTime = -5_000L),
            createSession(title = "Future EndTime", endTime = fixedNowMillis + 60_000L),
            createSession(
                title = "End Before Start",
                endTime = fixedNowMillis - 1_000L,
                startTime = fixedNowMillis
            ),
            createSession(title = "Valid Task", endTime = fixedNowMillis)
        )

        val ranked = ActivityTitleSuggestionRanker.rankedScores(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals(1, ranked.size)
        assertEquals("Valid Task", ranked.first().title)
    }

    @Test
    fun defaultBlankAndCategoryFilters() {
        val sessions = listOf(
            createSession(title = "Personal Chore", endTime = fixedNowMillis, category = "PERSONAL"),
            createSession(title = "", endTime = fixedNowMillis),
            createSession(title = "   ", endTime = fixedNowMillis),
            createSession(title = defaultTitleWork, endTime = fixedNowMillis),
            createSession(title = "Legitimate Work", endTime = fixedNowMillis, category = categoryWork)
        )

        val suggestions = ActivityTitleSuggestionRanker.suggest(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals(listOf("Legitimate Work"), suggestions)
    }

    @Test
    fun top5Limit() {
        val sessions = (1..8).map { i ->
            createSession(title = "Task $i", endTime = fixedNowMillis - i * 1_000L)
        }

        val suggestions = ActivityTitleSuggestionRanker.suggest(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals(5, suggestions.size)
        assertEquals(listOf("Task 1", "Task 2", "Task 3", "Task 4", "Task 5"), suggestions)
    }

    @Test
    fun tieBreakingOrder() {
        val sessions = listOf(
            createSession(title = "Beta Task", endTime = fixedNowMillis),
            createSession(title = "Alpha Task", endTime = fixedNowMillis)
        )

        val suggestions = ActivityTitleSuggestionRanker.suggest(
            category = categoryWork,
            activities = sessions,
            defaultTitle = defaultTitleWork,
            nowMillis = fixedNowMillis
        )

        assertEquals(listOf("Alpha Task", "Beta Task"), suggestions)
    }

    @Test
    fun monotoneTimeDecay() {
        val sessions = listOf(createSession(title = "Project Plan", endTime = fixedNowMillis))

        val scoreNow = ActivityTitleSuggestionRanker.rankedScores(
            categoryWork, sessions, defaultTitleWork, nowMillis = fixedNowMillis
        ).first().score

        val score1DayLater = ActivityTitleSuggestionRanker.rankedScores(
            categoryWork, sessions, defaultTitleWork, nowMillis = fixedNowMillis + dayMillis
        ).first().score

        val score7DaysLater = ActivityTitleSuggestionRanker.rankedScores(
            categoryWork, sessions, defaultTitleWork, nowMillis = fixedNowMillis + 7 * dayMillis
        ).first().score

        assertTrue(scoreNow > score1DayLater)
        assertTrue(score1DayLater > score7DaysLater)
        assertEquals(55.0, scoreNow, 0.001)
        assertTrue(score7DaysLater < 27.5)
    }
}
