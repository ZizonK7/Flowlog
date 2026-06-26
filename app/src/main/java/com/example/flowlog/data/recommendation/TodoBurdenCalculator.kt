package com.example.flowlog.data.recommendation

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

object TodoBurdenCalculator {
    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L

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

    fun analyze(
        todos: List<TodoItem>,
        activities: List<ActivitySession>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TodoBurdenAnalysis> {
        val linkedWorkMinutes = linkedWorkMinutesByTodo(activities)
        return todos.map { todo ->
            val topicGroup = estimateTopicGroup(todo.title)
            val groupKey = "${todo.category.name}:$topicGroup"
            val ageDays = ageDays(todo, nowMillis)
            val totalWorkMinutes = totalWorkMinutes(todo, linkedWorkMinutes[todo.id] ?: 0.0)
            val reason = BurdenReason(
                method = "FIXED_MEDIUM",
                topicGroup = topicGroup,
                burdenGroupKey = groupKey,
                ageDays = ageDays,
                totalWorkMinutes = totalWorkMinutes
            )
            TodoBurdenAnalysis(
                todo = todo,
                burdenLevel = BurdenLevel.MEDIUM.name,
                burdenGroupKey = groupKey,
                burdenScore = burdenScore(BurdenLevel.MEDIUM, ageDays, totalWorkMinutes),
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

    private fun ageDays(todo: TodoItem, nowMillis: Long): Double {
        val end = if (todo.isCompleted) todo.completedAt ?: nowMillis else nowMillis
        return ((end - todo.createdAt).toDouble() / DAY_MILLIS.toDouble()).coerceAtLeast(0.0)
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

    private fun burdenScore(level: BurdenLevel, ageDays: Double, workMinutes: Double): Int {
        val base = when (level) {
            BurdenLevel.HEAVY -> 30
            BurdenLevel.MEDIUM -> 20
            BurdenLevel.LIGHT -> 10
        }
        return base + ageDays.toInt().coerceIn(0, 30) + (workMinutes / 60.0).toInt().coerceIn(0, 20)
    }

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
    val totalWorkMinutes: Double
)

enum class BurdenLevel {
    LIGHT,
    MEDIUM,
    HEAVY
}
