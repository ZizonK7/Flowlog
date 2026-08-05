package com.example.flowlog

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.BurdenLevel
import com.example.flowlog.data.recommendation.TodoBurdenCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TodoBurdenCalculatorTest {

    @Test
    fun estimateTopicGroupRecognizesDevelopmentKeywords() {
        assertEquals("개발", TodoBurdenCalculator.estimateTopicGroup("Flowlog 버그 고치기"))
        assertEquals("개발", TodoBurdenCalculator.estimateTopicGroup("사이트 코딩 이어하기"))
    }

    @Test
    fun estimateTopicGroupRecognizesStudyKeywords() {
        assertEquals("공부", TodoBurdenCalculator.estimateTopicGroup("파이썬 강의 듣기"))
    }

    @Test
    fun estimateTopicGroupStripsTrailingWeekOrSessionNumbers() {
        assertEquals("자료구조", TodoBurdenCalculator.estimateTopicGroup("자료구조 3주차"))
        assertEquals("영어", TodoBurdenCalculator.estimateTopicGroup("영어 2강"))
    }

    @Test
    fun estimateTopicGroupSkipsGenericTaskTypeWordsForCoreToken() {
        // "과제"(과제 유형 단어)는 건너뛰고 실제 과목명을 핵심 토큰으로 잡아야 함
        assertEquals("확률과통계", TodoBurdenCalculator.estimateTopicGroup("확률과통계 과제"))
    }

    @Test
    fun estimateTopicGroupReturnsUnknownForBlankTitle() {
        assertEquals("unknown", TodoBurdenCalculator.estimateTopicGroup("   "))
    }

    @Test
    fun analyzeIncludesLinkedActivityMinutesWhenTodoHasNoAccumulatedSeconds() {
        val now = System.currentTimeMillis()
        val todo = TodoItem(
            id = 1L,
            title = "선형대수 과제",
            createdAt = now - TimeUnit.DAYS.toMillis(2),
            accumulatedSeconds = 0L
        )
        val linkedSession = ActivitySession(
            category = "STUDY",
            title = "선형대수 문제풀이",
            startTime = now - TimeUnit.MINUTES.toMillis(30),
            endTime = now,
            durationMillis = TimeUnit.MINUTES.toMillis(30),
            linkedTodoId = 1L
        )

        val result = TodoBurdenCalculator.analyze(
            todos = listOf(todo),
            activities = listOf(linkedSession),
            nowMillis = now
        )

        assertEquals(1, result.size)
        val reason = result.first()
        assertTrue(reason.burdenReasonJson.contains("\"totalWorkMinutes\":30.0"))
        assertEquals(BurdenLevel.MEDIUM.name, reason.burdenLevel)
    }

    @Test
    fun analyzePrefersAccumulatedSecondsOverLinkedActivityMinutes() {
        val now = System.currentTimeMillis()
        val todo = TodoItem(
            id = 2L,
            title = "복습",
            createdAt = now,
            accumulatedSeconds = 600L // 10분, 타이머 기반 누적값
        )
        val linkedSession = ActivitySession(
            category = "STUDY",
            title = "복습",
            startTime = now - TimeUnit.MINUTES.toMillis(30),
            endTime = now,
            durationMillis = TimeUnit.MINUTES.toMillis(30),
            linkedTodoId = 2L
        )

        val result = TodoBurdenCalculator.analyze(
            todos = listOf(todo),
            activities = listOf(linkedSession),
            nowMillis = now
        )

        assertTrue(result.first().burdenReasonJson.contains("\"totalWorkMinutes\":10.0"))
    }

    @Test
    fun analyzeUsesCompletedAtForAgeDaysWhenTodoIsCompleted() {
        val createdAt = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)
        val completedAt = createdAt + TimeUnit.DAYS.toMillis(3)
        val todo = TodoItem(
            id = 3L,
            title = "완료된 과제",
            createdAt = createdAt,
            isCompleted = true,
            completedAt = completedAt
        )

        val result = TodoBurdenCalculator.analyze(
            todos = listOf(todo),
            activities = emptyList(),
            nowMillis = completedAt + TimeUnit.DAYS.toMillis(5)
        )

        // 완료 후 5일이 더 지나도 ageDays는 완료 시점(생성 후 3일) 기준으로 고정돼야 함
        assertTrue(result.first().burdenReasonJson.contains("\"ageDays\":3.0"))
    }

    @Test
    fun burdenScoreGrowsWithAgeAndWorkMinutesButIsCapped() {
        val now = System.currentTimeMillis()
        val youngTodo = TodoItem(id = 4L, title = "새 과제", createdAt = now)
        val oldTodo = TodoItem(
            id = 5L,
            title = "오래된 과제",
            createdAt = now - TimeUnit.DAYS.toMillis(60)
        )

        val result = TodoBurdenCalculator.analyze(
            todos = listOf(youngTodo, oldTodo),
            activities = emptyList(),
            nowMillis = now
        )

        val youngScore = result.first { it.todo.id == 4L }.burdenScore
        val oldScore = result.first { it.todo.id == 5L }.burdenScore

        assertTrue(oldScore > youngScore)
        // ageDays는 0..30으로 clamp되므로 60일 지난 항목도 30일 지난 것과 동일한 상한을 받음
        assertEquals(20 + 30, oldScore)
    }
}
