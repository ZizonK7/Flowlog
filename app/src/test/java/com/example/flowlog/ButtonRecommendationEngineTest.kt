package com.example.flowlog

import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.recommendation.ButtonRecommendationEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class ButtonRecommendationEngineTest {

    private val engine = ButtonRecommendationEngine()

    @Test
    fun computePromotedCategories_returnsSingleStableCandidate() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val activities = listOf(
            session("DEVELOPMENT", now - oneDay),
            session("DEVELOPMENT", now - oneDay * 2),
            session("DEVELOPMENT", now - oneDay * 3)
        )

        assertEquals(listOf("DEVELOPMENT"), engine.computePromotedCategories(activities))
    }

    @Test
    fun computePromotedCategories_dropsUnstableSecondCandidateButKeepsStableFirst() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val activities = listOf(
            session("DEVELOPMENT", now - oneDay),
            session("DEVELOPMENT", now - oneDay * 2),
            session("DEVELOPMENT", now - oneDay * 3),
            session("READING", now - 60 * 60 * 1000L)
        )

        assertEquals(listOf("DEVELOPMENT"), engine.computePromotedCategories(activities))
    }

    @Test
    fun computePromotedCategories_excludesDailyCueRoutineSessions() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val activities = listOf(
            session("DEVELOPMENT", now - oneDay, ActivitySourceType.DAILY_CUE_ROUTINE),
            session("READING", now - oneDay * 2, ActivitySourceType.DAILY_CUE_ROUTINE),
            session("SCHOOL", now - oneDay * 3, ActivitySourceType.DAILY_CUE_ROUTINE),
            session("COMPANY", now - oneDay * 4, ActivitySourceType.DAILY_CUE_ROUTINE),
            session("DEVELOPMENT", now - oneDay * 5, ActivitySourceType.DAILY_CUE_ROUTINE),
            session("READING", now - oneDay * 6, ActivitySourceType.DAILY_CUE_ROUTINE)
        )

        assertEquals(emptyList<String>(), engine.computePromotedCategories(activities))
    }

    @Test
    fun computePromotedCategories_keepsManualSessionsForSameCategories() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val activities = listOf(
            session("DEVELOPMENT", now - oneDay, ActivitySourceType.MANUAL),
            session("DEVELOPMENT", now - oneDay * 2, ActivitySourceType.MANUAL),
            session("DEVELOPMENT", now - oneDay * 3, ActivitySourceType.MANUAL)
        )

        assertEquals(listOf("DEVELOPMENT"), engine.computePromotedCategories(activities))
    }

    @Test
    fun computePromotedCategories_promotesMoveCandidate() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val activities = listOf(
            session("MOVE", now - oneDay),
            session("MOVE", now - oneDay * 2),
            session("MOVE", now - oneDay * 3)
        )

        assertEquals(listOf("MOVE"), engine.computePromotedCategories(activities))
    }

    @Test
    fun computePromotedCategories_infersMoveFromText() {
        val now = System.currentTimeMillis()
        val oneDay = 24L * 60 * 60 * 1000
        val activities = listOf(
            session("ETC", now - oneDay, title = "이동"),
            session("ETC", now - oneDay * 2, title = "버스"),
            session("ETC", now - oneDay * 3, title = "지하철")
        )

        assertEquals(listOf("MOVE"), engine.computePromotedCategories(activities))
    }

    private fun session(
        category: String,
        startTime: Long,
        sourceType: String = ActivitySourceType.MANUAL,
        title: String = category
    ): ActivitySession {
        return ActivitySession(
            category = category,
            title = title,
            startTime = startTime,
            endTime = startTime + 10 * 60 * 1000L,
            durationMillis = 10 * 60 * 1000L,
            sourceType = sourceType
        )
    }
}
