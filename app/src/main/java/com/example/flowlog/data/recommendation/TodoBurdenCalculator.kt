package com.example.flowlog.data.recommendation

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import kotlin.math.max

object TodoBurdenCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val HEAVY_RATIO = 1.5
    private const val LIGHT_RATIO = 0.6

    private val json = Json { encodeDefaults = true }

    private val topicTypeWords = setOf(
        "과제", "숙제", "복습", "예습", "공부", "학습", "퀴즈", "시험",
        "정리", "연습", "실습", "수업", "강의", "발표", "준비", "제출",
        "todo", "study", "review", "assignment", "homework", "quiz", "test"
    )

    private val topicDevelopmentKeywords = listOf(
        "개발", "코딩", "프로그래밍", "앱", "사이트", "아이콘", "버그", "고치",
        "수정", "로직", "추천", "에이전트", "기록", "버튼", "firestore", "firebase"
    )

    private val topicStudyKeywords = listOf(
        "파이썬", "python", "강의", "인강", "수업", "공부", "학습", "문제", "기출"
    )

    private val defaultBurdenByCategory = mapOf(
        TodoCategory.ASSIGNMENT to BurdenLevel.MEDIUM,
        TodoCategory.REVIEW to BurdenLevel.LIGHT,
        TodoCategory.NORMAL to BurdenLevel.MEDIUM,
        TodoCategory.TODAY to BurdenLevel.MEDIUM
    )

    fun analyze(
        todos: List<TodoItem>,
        activities: List<ActivitySession>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TodoBurdenAnalysis> {
        val linkedWorkMinutes = linkedWorkMinutesByTodo(activities)
        val rows = todos.map { todo ->
            val topicGroup = estimateTopicGroup(todo.title)
            val groupKey = "${todo.category.name}:$topicGroup"
            val totalWorkMinutes = totalWorkMinutes(todo, linkedWorkMinutes[todo.id] ?: 0.0)
            BurdenRow(
                todo = todo,
                topicGroup = topicGroup,
                burdenGroupKey = groupKey,
                ageDays = ageDays(todo, nowMillis),
                totalWorkMinutes = totalWorkMinutes
            )
        }

        val groupStats = rows.groupBy { it.burdenGroupKey }.mapValues { summarize(it.value) }
        val categoryStats = rows.groupBy { it.todo.category }.mapValues { summarize(it.value) }
        val globalStats = summarize(rows)

        return rows.map { row ->
            val result = estimateBurdenLevel(
                row = row,
                groupStats = groupStats[row.burdenGroupKey],
                categoryStats = categoryStats[row.todo.category],
                globalStats = globalStats
            )
            val reason = BurdenReason(
                method = result.method,
                topicGroup = row.topicGroup,
                burdenGroupKey = row.burdenGroupKey,
                ageDays = row.ageDays,
                totalWorkMinutes = row.totalWorkMinutes,
                baselineDays = result.baseline?.averageDays,
                baselineWorkMinutes = result.baseline?.averageWorkMinutes,
                dayRatio = result.dayRatio,
                workRatio = result.workRatio
            )
            TodoBurdenAnalysis(
                todo = row.todo,
                burdenLevel = result.level.name,
                burdenGroupKey = row.burdenGroupKey,
                burdenScore = burdenScore(result.level, row.ageDays, row.totalWorkMinutes),
                burdenReasonJson = json.encodeToString(reason)
            )
        }
    }

    fun estimateTopicGroup(title: String): String {
        var text = title.trim()
        if (text.isEmpty()) return "unknown"

        text = text
            .replace(Regex("""[~_#\-\s]*(\d+)(주차|차|회|번|강|장|단원|th|st|nd|rd)?$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[\s#\-]*(part|chapter|ch|week)\s*\d+$""", RegexOption.IGNORE_CASE), "")
            .trim()

        val compact = text.lowercase(Locale.getDefault()).replace(Regex("""\s+"""), " ")
        val noSpace = compact.replace(Regex("""\s+"""), "")
        if ("과외준비" in noSpace) return "과외_준비"
        if (topicStudyKeywords.any { it in compact }) return "공부"
        if (topicDevelopmentKeywords.any { it in compact }) return "개발"

        val tokens = text
            .split(Regex("""[\s\-/.:·_|,]+"""))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val core = tokens.firstOrNull { token ->
            token.lowercase(Locale.getDefault()) !in topicTypeWords
        } ?: tokens.firstOrNull() ?: text

        return core.take(24).ifBlank { "unknown" }
    }

    private fun estimateBurdenLevel(
        row: BurdenRow,
        groupStats: BurdenStats?,
        categoryStats: BurdenStats?,
        globalStats: BurdenStats
    ): BurdenEstimate {
        val baselineWithMethod = when {
            groupStats != null && groupStats.sampleSize >= 2 -> groupStats to "GROUP_AVERAGE"
            categoryStats != null && categoryStats.sampleSize >= 2 -> categoryStats to "CATEGORY_AVERAGE"
            globalStats.sampleSize >= 2 -> globalStats to "GLOBAL_AVERAGE"
            else -> null
        }

        if (baselineWithMethod == null) {
            return BurdenEstimate(defaultBurden(row.todo.category), "DEFAULT_BY_CATEGORY")
        }

        val (baseline, method) = baselineWithMethod
        if (row.ageDays <= 0.0 && row.totalWorkMinutes <= 0.0) {
            return BurdenEstimate(defaultBurden(row.todo.category), "TODO_VALUE_ONLY", baseline)
        }

        val dayRatio = baseline.averageDays
            .takeIf { it > 0.0 }
            ?.let { row.ageDays / it }
        val workRatio = baseline.averageWorkMinutes
            .takeIf { it > 0.0 && row.totalWorkMinutes > 0.0 }
            ?.let { row.totalWorkMinutes / it }
        val ratios = listOfNotNull(dayRatio, workRatio)

        if (ratios.isEmpty()) {
            return BurdenEstimate(defaultBurden(row.todo.category), "DEFAULT_BY_CATEGORY", baseline)
        }

        val level = when {
            ratios.any { it > HEAVY_RATIO } -> BurdenLevel.HEAVY
            ratios.all { it < LIGHT_RATIO } -> BurdenLevel.LIGHT
            else -> BurdenLevel.MEDIUM
        }
        return BurdenEstimate(level, method, baseline, dayRatio, workRatio)
    }

    private fun summarize(rows: List<BurdenRow>): BurdenStats {
        val completedWithDays = rows.filter { it.todo.isCompleted && it.ageDays >= 0.0 }
        val withWork = rows.filter { it.totalWorkMinutes > 0.0 }
        return BurdenStats(
            sampleSize = rows.size,
            averageDays = completedWithDays
                .takeIf { it.isNotEmpty() }
                ?.map { it.ageDays }
                ?.average() ?: 0.0,
            averageWorkMinutes = withWork
                .takeIf { it.isNotEmpty() }
                ?.map { it.totalWorkMinutes }
                ?.average() ?: 0.0
        )
    }

    private fun ageDays(todo: TodoItem, nowMillis: Long): Double {
        val end = if (todo.isCompleted) todo.completedAt ?: nowMillis else nowMillis
        return max(0.0, (end - todo.createdAt).toDouble() / DAY_MILLIS.toDouble())
    }

    private fun totalWorkMinutes(todo: TodoItem, linkedMinutes: Double): Double {
        val accumulatedMinutes = todo.accumulatedSeconds.toDouble() / 60.0
        return if (accumulatedMinutes > 0.0) accumulatedMinutes else linkedMinutes
    }

    private fun linkedWorkMinutesByTodo(activities: List<ActivitySession>): Map<Long, Double> {
        return activities
            .filter { it.linkedTodoId != null && it.durationMillis > 0L }
            .groupBy { it.linkedTodoId ?: 0L }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMillis }.toDouble() / 60_000.0 }
    }

    private fun defaultBurden(category: TodoCategory): BurdenLevel {
        return defaultBurdenByCategory[category] ?: BurdenLevel.MEDIUM
    }

    private fun burdenScore(level: BurdenLevel, ageDays: Double, workMinutes: Double): Int {
        val base = when (level) {
            BurdenLevel.HEAVY -> 30
            BurdenLevel.MEDIUM -> 20
            BurdenLevel.LIGHT -> 10
        }
        return base + ageDays.toInt().coerceIn(0, 30) + (workMinutes / 60.0).toInt().coerceIn(0, 20)
    }

    private data class BurdenRow(
        val todo: TodoItem,
        val topicGroup: String,
        val burdenGroupKey: String,
        val ageDays: Double,
        val totalWorkMinutes: Double
    )

    private data class BurdenStats(
        val sampleSize: Int,
        val averageDays: Double,
        val averageWorkMinutes: Double
    )

    private data class BurdenEstimate(
        val level: BurdenLevel,
        val method: String,
        val baseline: BurdenStats? = null,
        val dayRatio: Double? = null,
        val workRatio: Double? = null
    )
}

data class TodoBurdenAnalysis(
    val todo: TodoItem,
    val burdenLevel: String,
    val burdenGroupKey: String,
    val burdenScore: Int,
    val burdenReasonJson: String
)

@Serializable
data class BurdenReason(
    val method: String,
    val topicGroup: String,
    val burdenGroupKey: String,
    val ageDays: Double,
    val totalWorkMinutes: Double,
    val baselineDays: Double? = null,
    val baselineWorkMinutes: Double? = null,
    val dayRatio: Double? = null,
    val workRatio: Double? = null
)

enum class BurdenLevel {
    LIGHT,
    MEDIUM,
    HEAVY
}
