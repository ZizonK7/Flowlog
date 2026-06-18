package com.example.flowlog.data.recommendation

import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import java.util.Calendar

data class ReviewRecommendationEligibility(
    val priority: Int,
    val reason: String
)

object ReviewRecommendationPolicy {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun eligibility(todo: TodoItem, todayMillis: Long): ReviewRecommendationEligibility? {
        if (todo.category != TodoCategory.REVIEW) return null

        val todayStart = startOfDay(todayMillis)
        return when (todo.reviewStage) {
            0 -> {
                val firstReviewBase = todo.selectedDate ?: todo.createdAt
                when (daysSince(firstReviewBase, todayStart)) {
                    1L -> ReviewRecommendationEligibility(2, RecommendationReason.REVIEW_D_PLUS_1)
                    2L, 3L -> ReviewRecommendationEligibility(3, RecommendationReason.REVIEW_D_PLUS_1_LATE)
                    else -> null
                }
            }
            1 -> {
                val secondReviewBase = todo.reviewStage1CompletedAt
                    ?: todo.selectedDate
                    ?: todo.createdAt
                when (daysSince(secondReviewBase, todayStart)) {
                    7L -> ReviewRecommendationEligibility(5, RecommendationReason.REVIEW_D_PLUS_7)
                    8L, 9L, 10L -> ReviewRecommendationEligibility(6, RecommendationReason.REVIEW_D_PLUS_7_LATE)
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun daysSince(baseMillis: Long, todayStart: Long): Long =
        (todayStart - startOfDay(baseMillis)) / DAY_MILLIS

    private fun startOfDay(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
