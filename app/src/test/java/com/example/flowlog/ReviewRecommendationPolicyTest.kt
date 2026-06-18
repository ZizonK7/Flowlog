package com.example.flowlog

import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.ReviewRecommendationPolicy
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewRecommendationPolicyTest {
    @Test
    fun secondReviewDoesNotEnterBeforeSevenDaysAfterFirstReviewCompletion() {
        val firstReviewCompletedAt = day(2026, Calendar.JUNE, 1)
        val todo = reviewTodo(stage = 1, firstReviewCompletedAt = firstReviewCompletedAt)

        assertNull(ReviewRecommendationPolicy.eligibility(todo, day(2026, Calendar.JUNE, 7)))
        assertEquals(
            RecommendationReason.REVIEW_D_PLUS_7,
            ReviewRecommendationPolicy.eligibility(todo, day(2026, Calendar.JUNE, 8))?.reason
        )
    }

    @Test
    fun secondReviewUsesFirstReviewCompletionDateInsteadOfOriginalStudyDate() {
        val todo = reviewTodo(
            stage = 1,
            selectedDate = day(2026, Calendar.MAY, 1),
            firstReviewCompletedAt = day(2026, Calendar.JUNE, 10)
        )

        assertNull(ReviewRecommendationPolicy.eligibility(todo, day(2026, Calendar.JUNE, 16)))
        assertEquals(
            RecommendationReason.REVIEW_D_PLUS_7,
            ReviewRecommendationPolicy.eligibility(todo, day(2026, Calendar.JUNE, 17))?.reason
        )
    }

    @Test
    fun completedSecondReviewIsNeverEligible() {
        assertNull(
            ReviewRecommendationPolicy.eligibility(
                reviewTodo(stage = 2, firstReviewCompletedAt = day(2026, Calendar.JUNE, 1)),
                day(2026, Calendar.JUNE, 8)
            )
        )
    }

    private fun reviewTodo(
        stage: Int,
        selectedDate: Long = day(2026, Calendar.MAY, 20),
        firstReviewCompletedAt: Long?
    ) = TodoItem(
        id = 1L,
        title = "복습",
        category = TodoCategory.REVIEW,
        selectedDate = selectedDate,
        reviewStage = stage,
        reviewStage1CompletedAt = firstReviewCompletedAt,
        isCompleted = stage >= 1
    )

    private fun day(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis
}
