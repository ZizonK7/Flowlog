package com.example.flowlog.data.assistant

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.recommendation.FlowRecommendationEngine
import com.example.flowlog.data.repository.AutoButtonScheduleRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * 웹 비서(/assistant)가 읽는 "오늘의 시간표 스냅샷"을 만든다.
 * placed = 이미 시간이 정해진 것(반복 루틴 + 시간 배정된 할일, 캘린더 유래 항목은 제외 —
 *          agentContext.calendarEvents가 이미 커버함), unplaced = 오늘 포커스로 선정됐지만
 * 아직 시간 미정인 할일, anchors = 기상/예상 취침 시각.
 */
object AssistantSnapshotBuilder {

    fun kstDateKey(now: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply { timeInMillis = now }
        return String.format(
            Locale.US, "%04d-%02d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    suspend fun build(
        dailyGoalRepository: DailyGoalRepository,
        autoButtonScheduleRepository: AutoButtonScheduleRepository,
        activities: List<ActivitySession>,
        now: Long = System.currentTimeMillis()
    ): Map<String, Any?> {
        val routineBlocks = autoButtonScheduleRepository.getTodayActiveBlocks()
        val timetableBlocks = dailyGoalRepository.observeTodayRecommendedBlocks().first()
            .filter { it.petiteId == null }
        val unplacedItems = dailyGoalRepository.getTodayUnplacedFocusItems()

        val placed = buildList<Map<String, Any?>> {
            routineBlocks.forEach { block ->
                add(
                    mapOf(
                        "source" to "ROUTINE",
                        "title" to block.title,
                        "category" to block.category,
                        "startTime" to block.startTime,
                        "endTime" to block.endTime
                    )
                )
            }
            timetableBlocks.forEach { block ->
                add(
                    mapOf(
                        "source" to "TIMETABLE_TODO",
                        "title" to block.title,
                        "category" to block.category?.name,
                        "startTime" to block.plannedStartMillis,
                        "endTime" to block.plannedEndMillis
                    )
                )
            }
        }

        val unplaced = unplacedItems.map { item ->
            mapOf(
                "todoId" to item.todoId,
                "title" to item.title,
                "category" to item.category,
                "reason" to item.reason,
                "burdenLevel" to item.burdenLevel
            )
        }

        // FlowRecommendationEngine.MIN_LONG_SLEEP_MILLIS와 같은 기준(90분) — 그 상수는 private companion이라 재사용 불가.
        val minLongSleepMillis = TimeUnit.MINUTES.toMillis(90)
        val lastWakeTime = activities
            .filter { it.category == "SLEEP" && it.durationMillis >= minLongSleepMillis }
            .maxByOrNull { it.endTime }
            ?.endTime
        val predictedBedtime = FlowRecommendationEngine().estimateNextLongSleepStart(now, activities)

        return mapOf(
            "updatedAt" to now,
            "placed" to placed,
            "unplaced" to unplaced,
            "anchors" to mapOf(
                "lastWakeTime" to lastWakeTime,
                "predictedBedtime" to predictedBedtime
            )
        )
    }
}
