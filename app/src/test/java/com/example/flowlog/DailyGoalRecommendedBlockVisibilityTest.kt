package com.example.flowlog

import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.repository.shouldShowRecommendedBlock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalRecommendedBlockVisibilityTest {
    @Test
    fun futureBlockForSameCompletedTodoStaysVisibleWhenPlanHasMultipleBlocks() {
        val completedTodoIds = setOf("legacy_todo_10")
        val plannedItemCountsByTodoId = mapOf("legacy_todo_10" to 2)

        assertFalse(
            shouldShowRecommendedBlock(
                item = item("first", wasCompleted = true),
                completedTodoIds = completedTodoIds,
                plannedItemCountsByTodoId = plannedItemCountsByTodoId
            )
        )
        assertTrue(
            shouldShowRecommendedBlock(
                item = item("second"),
                completedTodoIds = completedTodoIds,
                plannedItemCountsByTodoId = plannedItemCountsByTodoId
            )
        )
    }

    @Test
    fun singleBlockForCompletedTodoIsHidden() {
        assertFalse(
            shouldShowRecommendedBlock(
                item = item("single"),
                completedTodoIds = setOf("legacy_todo_10"),
                plannedItemCountsByTodoId = mapOf("legacy_todo_10" to 1)
            )
        )
    }

    private fun item(
        itemId: String,
        wasCompleted: Boolean = false,
        userActionStatus: String = "PLANNED"
    ) = DailyGoalItemEntity(
        itemId = itemId,
        recommendationId = "recommendation",
        userId = "user",
        todoId = "legacy_todo_10",
        rank = 1,
        plannedStartMillis = 1_000L,
        plannedEndMillis = 2_000L,
        userActionStatus = userActionStatus,
        wasCompleted = wasCompleted
    )
}
