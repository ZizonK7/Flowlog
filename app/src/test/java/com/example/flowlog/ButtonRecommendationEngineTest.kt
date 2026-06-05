package com.example.flowlog

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

    private fun session(category: String, startTime: Long): ActivitySession {
        return ActivitySession(
            category = category,
            title = category,
            startTime = startTime,
            endTime = startTime + 10 * 60 * 1000L,
            durationMillis = 10 * 60 * 1000L
        )
    }
}
