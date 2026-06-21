package com.example.flowlog

import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.agent.OrganizerRoutine
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.agent.TodayExamOrganizer
import com.example.flowlog.data.agent.TodayOrganizerRules
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TodayOrganizerAgentTest {
    @Test
    fun severeBehindWhenStudyTimeOrDaysAreLow() {
        assertTrue(TodayOrganizerRules.isSeverelyBehind(totalStudyMinutes = 59, studiedDays = 3, missedDays = 1))
        assertTrue(TodayOrganizerRules.isSeverelyBehind(totalStudyMinutes = 120, studiedDays = 1, missedDays = 1))
        assertTrue(TodayOrganizerRules.isSeverelyBehind(totalStudyMinutes = 120, studiedDays = 3, missedDays = 4))
        assertFalse(TodayOrganizerRules.isSeverelyBehind(totalStudyMinutes = 60, studiedDays = 2, missedDays = 3))
    }

    @Test
    fun sortUsesPriorityBeforeSourceType() {
        val sorted = TodayOrganizerRules.sort(
            listOf(
                OrganizedPetite(
                    id = "routine",
                    title = "Routine",
                    sourceType = PetiteSourceType.ROUTINE,
                    sourceId = "1",
                    priorityScore = 90
                ),
                OrganizedPetite(
                    id = "exam",
                    title = "Exam",
                    sourceType = PetiteSourceType.EXAM,
                    sourceId = "2",
                    priorityScore = 10
                )
            )
        )

        assertEquals(PetiteSourceType.EXAM, sorted.first().sourceType)
    }

    @Test
    fun examMetricsCountsD7StudyActivitiesForSameExam() {
        val organizer = TodayExamOrganizer()
        val today = day(2026, Calendar.JUNE, 6)
        val exam = TodoItem(
            id = 42L,
            title = "선대 기말 시험",
            category = TodoCategory.UNIVERSITY_EXAM,
            selectedDate = day(2026, Calendar.JUNE, 8)
        )
        val activities = listOf(
            ActivitySession(
                category = "STUDY",
                title = "선대 기말 시험 공부",
                startTime = day(2026, Calendar.JUNE, 5) + 10 * 60 * 60 * 1000L,
                endTime = day(2026, Calendar.JUNE, 5) + 11 * 60 * 60 * 1000L,
                durationMillis = 60 * 60 * 1000L
            ),
            ActivitySession(
                category = "REST",
                title = "쉬기",
                startTime = day(2026, Calendar.JUNE, 5),
                endTime = day(2026, Calendar.JUNE, 5) + 10_000L,
                durationMillis = 10_000L
            )
        )

        val metrics = organizer.calculateExamStudyMetrics(today, exam, activities)

        assertEquals(60, metrics.totalStudyMinutes)
        assertEquals(1, metrics.studiedDays)
        assertTrue(metrics.isSeverelyBehind)
    }

    @Test
    fun organizerDoesNotShowExamRecommendationsInTodayCards() {
        val organizer = TodayExamOrganizer()
        val today = day(2026, Calendar.JUNE, 6)

        val cards = runBlocking { organizer.organize(
            todayMillis = today,
            todos = listOf(
                TodoItem(id = 1L, title = "D7 exam", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = day(2026, Calendar.JUNE, 13)),
                TodoItem(id = 2L, title = "D8 exam", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = day(2026, Calendar.JUNE, 14))
            ),
            routines = emptyList(),
            activities = emptyList()
        ) }

        val examCards = cards.filter { it.sourceType == PetiteSourceType.EXAM }
        assertTrue(examCards.isEmpty())
    }

    @Test
    fun organizerKeepsUrgentTodoSourceItemsWithoutPullingFutureTodos() {
        val organizer = TodayExamOrganizer()
        val today = day(2026, Calendar.JUNE, 6)
        val todos = listOf(
            TodoItem(id = 1L, title = "Overdue", category = TodoCategory.ASSIGNMENT, selectedDate = day(2026, Calendar.JUNE, 5)),
            TodoItem(id = 2L, title = "Today", category = TodoCategory.ASSIGNMENT, selectedDate = today),
            TodoItem(id = 3L, title = "Future", selectedDate = day(2026, Calendar.JUNE, 10)),
            TodoItem(id = 4L, title = "Tomorrow light", selectedDate = day(2026, Calendar.JUNE, 7)),
            TodoItem(id = 5L, title = "Tomorrow heavy", category = TodoCategory.ASSIGNMENT, selectedDate = day(2026, Calendar.JUNE, 7), burdenLevel = "HEAVY"),
            TodoItem(id = 6L, title = "Normal today", selectedDate = today),
            TodoItem(id = 7L, title = "Assignment in 30 days", category = TodoCategory.ASSIGNMENT, selectedDate = day(2026, Calendar.JULY, 6))
        )

        val todoCards = runBlocking { organizer.organize(
            todayMillis = today,
            todos = todos,
            routines = emptyList(),
            activities = emptyList()
        ) }.filter { it.sourceType == PetiteSourceType.TODO }

        assertEquals(4, todoCards.size)
        assertTrue(todoCards.any { it.title == "Overdue" })
        assertTrue(todoCards.any { it.title == "Today" })
        assertTrue(todoCards.any { it.title == "Tomorrow heavy" })
        assertTrue(todoCards.any { it.title == "Normal today" })
        assertTrue(todoCards.none { it.title == "Future" })
        assertTrue(todoCards.none { it.title == "Tomorrow light" })
        assertTrue(todoCards.none { it.title == "Assignment in 30 days" })
    }

    @Test
    fun rulesDetectAmbiguousPriorityForPotentialAiRanking() {
        val candidates = listOf(
            OrganizedPetite(
                id = "a",
                title = "A",
                sourceType = PetiteSourceType.TODO,
                sourceId = "1",
                priorityScore = 80
            ),
            OrganizedPetite(
                id = "b",
                title = "B",
                sourceType = PetiteSourceType.ROUTINE,
                sourceId = "2",
                priorityScore = 80
            )
        )

        assertTrue(TodayOrganizerRules.hasAmbiguousPriority(candidates))
        assertTrue(TodayOrganizerRules.needsAiRanking(candidates))
    }

    @Test
    fun organizerDoesNotRecommendReviewTodosBecauseTheyAreOutsideOrganizerOrder() {
        val organizer = TodayExamOrganizer()
        val today = day(2026, Calendar.JUNE, 6)
        val cards = runBlocking { organizer.organize(
            todayMillis = today,
            todos = listOf(
                TodoItem(
                    id = 10L,
                    title = "Old review",
                    category = TodoCategory.REVIEW,
                    selectedDate = day(2026, Calendar.MAY, 19),
                    reviewStage = 0
                ),
                TodoItem(
                    id = 11L,
                    title = "D plus one review",
                    category = TodoCategory.REVIEW,
                    selectedDate = day(2026, Calendar.JUNE, 5),
                    reviewStage = 0
                )
            ),
            routines = emptyList(),
            activities = emptyList()
        ) }

        val todoCards = cards.filter { it.sourceType == PetiteSourceType.TODO }
        assertTrue(todoCards.none { it.title == "Old review" })
        assertTrue(todoCards.none { it.title == "D plus one review" })
    }

    @Test
    fun organizerIgnoresExamItemsWhenOrderingTodayCards() {
        val organizer = TodayExamOrganizer()
        val today = day(2026, Calendar.JUNE, 6)
        val cards = runBlocking { organizer.organize(
            todayMillis = today,
            todos = listOf(
                TodoItem(id = 1L, title = "Exam today", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = today),
                TodoItem(id = 2L, title = "Assignment today", category = TodoCategory.ASSIGNMENT, selectedDate = today),
                TodoItem(id = 3L, title = "Exam d1", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = day(2026, Calendar.JUNE, 7)),
                TodoItem(id = 4L, title = "Exam d2", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = day(2026, Calendar.JUNE, 8)),
                TodoItem(id = 5L, title = "Assignment tomorrow", category = TodoCategory.ASSIGNMENT, selectedDate = day(2026, Calendar.JUNE, 7)),
                TodoItem(id = 6L, title = "Exam d3", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = day(2026, Calendar.JUNE, 9)),
                TodoItem(id = 7L, title = "Exam d4", category = TodoCategory.UNIVERSITY_EXAM, selectedDate = day(2026, Calendar.JUNE, 10)),
                TodoItem(id = 8L, title = "Normal today", category = TodoCategory.NORMAL, selectedDate = today),
                TodoItem(id = 9L, title = "Existing petite", category = TodoCategory.TODAY)
            ),
            routines = listOf(
                OrganizerRoutine(
                    id = 1L,
                    label = "Routine",
                    title = "Routine",
                    isCompleted = false,
                    timerDurationMillis = null,
                    timerCategory = "TODO"
                )
            ),
            activities = emptyList()
        ) }

        assertEquals(
            listOf(
                PetiteSourceType.TODO,
                PetiteSourceType.TODO,
                PetiteSourceType.TODO,
                PetiteSourceType.ROUTINE,
                PetiteSourceType.PETITE
            ),
            cards.map { it.sourceType }
        )
        assertEquals(listOf(10, 30, 80, 90, 100), cards.map { it.priorityScore })
    }

    @Test
    fun organizerDoesNotTreatMemoCueAsRoutine() {
        val organizer = TodayExamOrganizer()
        val today = day(2026, Calendar.JUNE, 6)

        val cards = runBlocking { organizer.organize(
            todayMillis = today,
            todos = emptyList(),
            routines = listOf(
                OrganizerRoutine(
                    id = 1L,
                    label = "Memo",
                    title = "Bring umbrella",
                    isCompleted = false,
                    timerDurationMillis = null,
                    timerCategory = "TODO"
                ),
                OrganizerRoutine(
                    id = 2L,
                    label = "Routine",
                    title = "Stretch",
                    isCompleted = false,
                    timerDurationMillis = null,
                    timerCategory = "EXERCISE"
                )
            ),
            activities = emptyList()
        ) }

        val routineCards = cards.filter { it.sourceType == PetiteSourceType.ROUTINE }
        assertEquals(1, routineCards.size)
        assertEquals("Stretch", routineCards.first().title)
    }

    private fun day(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
