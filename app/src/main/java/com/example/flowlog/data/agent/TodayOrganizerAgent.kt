package com.example.flowlog.data.agent

import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import java.util.Calendar

enum class PetiteSourceType {
    PETITE,
    TODO,
    ROUTINE,
    EXAM,
    STUDY_PLAN
}

data class OrganizerRoutine(
    val id: Long,
    val label: String,
    val title: String,
    val isCompleted: Boolean,
    val timerDurationMillis: Long?,
    val timerCategory: String
)

data class OrganizedPetite(
    val id: String,
    val title: String,
    val sourceType: PetiteSourceType,
    val sourceId: String?,
    val category: TodoCategory? = null,
    val dateMillis: Long? = null,
    val linkedActivityName: String? = null,
    val activityCategory: String? = null,
    val isCompleted: Boolean = false,
    val priorityScore: Int,
    val burdenScore: Int? = null,
    val isSeverelyBehind: Boolean? = null,
    val totalStudyMinutesSinceD7: Int? = null,
    val studiedDaysSinceD7: Int? = null,
    val missedDaysSinceD7: Int? = null,
    val aiComment: String? = null,
    val estimatedMinutes: Int? = null,
    val steps: List<String> = emptyList(),
    val examDValue: Int? = null,
    val routineTimerDurationMillis: Long? = null,
    val routineTimerCategory: String? = null
)

data class ExamStudyMetrics(
    val totalStudyMinutes: Int,
    val studiedDays: Int,
    val missedDays: Int,
    val isSeverelyBehind: Boolean
)

data class AiRecommendationReason(
    val aiComment: String?,
    val estimatedMinutes: Int?,
    val steps: List<String> = emptyList()
)

data class TodayOrganizerContext(
    val todayMillis: Long,
    val recoveryMode: Boolean
)

interface AiDecisionProvider {
    suspend fun rankAmbiguousItems(
        candidates: List<OrganizedPetite>,
        context: TodayOrganizerContext
    ): List<OrganizedPetite>

    suspend fun generateRecommendationReason(
        candidate: OrganizedPetite,
        context: TodayOrganizerContext
    ): AiRecommendationReason?
}

class MockAiDecisionProvider : AiDecisionProvider {
    override suspend fun rankAmbiguousItems(
        candidates: List<OrganizedPetite>,
        context: TodayOrganizerContext
    ): List<OrganizedPetite> {
        return TodayOrganizerRules.sort(candidates)
    }

    override suspend fun generateRecommendationReason(
        candidate: OrganizedPetite,
        context: TodayOrganizerContext
    ): AiRecommendationReason? {
        if (candidate.sourceType != PetiteSourceType.EXAM) return null
        val dValue = candidate.examDValue ?: return null
        val severelyBehind = candidate.isSeverelyBehind == true
        return AiRecommendationReason(
            aiComment = examComment(dValue, severelyBehind),
            estimatedMinutes = estimatedMinutes(dValue, severelyBehind),
            steps = examSteps(dValue)
        )
    }

    private fun examComment(daysUntil: Int, severelyBehind: Boolean): String {
        return when {
            daysUntil == 2 && severelyBehind ->
                "최근 공부 기록이 부족해서 밀린 계획을 모두 따라가기보다는, 오늘은 60분 정도 실전 문제와 약점 복구를 합쳐서 진행하는 게 좋아요."
            daysUntil == 1 ->
                "시험 전날이라 새 내용을 많이 늘리기보다, 오답·공식·대표 문제 흐름을 확인하는 쪽이 좋아요."
            daysUntil == 0 ->
                "시험 당일이라 새 공부보다 예열만 하는 게 좋아요. 공식과 실수 패턴만 짧게 확인해요."
            daysUntil in 4..7 ->
                "아직 복구할 시간이 있으니 전체 범위 확인과 핵심 개념 회상부터 시작하는 게 좋아요."
            severelyBehind ->
                "최근 공부 기록이 부족해요. 오늘은 문제 풀이보다 약점 하나를 확실히 복구하는 쪽으로 잡을게요."
            else ->
                "시험까지 남은 시간을 기준으로 오늘 할 공부를 작게 잡았어요."
        }
    }

    private fun estimatedMinutes(daysUntil: Int, severelyBehind: Boolean): Int {
        return when {
            daysUntil == 0 -> 20
            daysUntil == 1 -> 45
            severelyBehind -> 60
            daysUntil <= 3 -> 50
            else -> 40
        }
    }

    private fun examSteps(daysUntil: Int): List<String> {
        return when (daysUntil) {
            0 -> listOf("공식과 실수 패턴 확인", "대표 문제 1개 예열")
            1 -> listOf("오답 확인", "헷갈리는 개념 비교", "대표 문제 흐름 보기")
            2 -> listOf("시간 재고 문제 풀기", "오답을 개념/실수/시간 부족으로 분류", "약점 1개 복구")
            3 -> listOf("섞인 문제 풀이", "어떤 개념을 쓸지 판단", "약점 정리")
            else -> listOf("범위 훑기", "핵심 개념 백지 회상", "기본 문제 몇 개 풀이")
        }
    }
}

object TodayOrganizerRules {
    const val EXAM_WINDOW_DAYS = 7
    const val HIGH_PRIORITY_BURDEN_SCORE = 70
    const val MIN_D7_STUDY_MINUTES = 60
    const val MAX_SEVERE_STUDIED_DAYS = 1
    const val MIN_SEVERE_MISSED_DAYS = 4

    fun isSeverelyBehind(totalStudyMinutes: Int, studiedDays: Int, missedDays: Int): Boolean {
        return totalStudyMinutes < MIN_D7_STUDY_MINUTES ||
            studiedDays <= MAX_SEVERE_STUDIED_DAYS ||
            missedDays >= MIN_SEVERE_MISSED_DAYS
    }

    fun sort(items: List<OrganizedPetite>): List<OrganizedPetite> {
        return items.sortedWith(
            compareBy<OrganizedPetite> { if (it.isCompleted) 1 else 0 }
                .thenBy { it.priorityScore }
                .thenBy { it.title }
                .thenBy { it.sourceType.name }
                .thenBy { it.sourceId ?: it.id }
        )
    }

    fun hasAmbiguousPriority(candidates: List<OrganizedPetite>): Boolean {
        return candidates
            .filterNot { it.isCompleted }
            .groupBy { it.priorityScore }
            .any { (_, samePriority) -> samePriority.size > 1 }
    }

    fun needsAiRanking(candidates: List<OrganizedPetite>): Boolean {
        return hasAmbiguousPriority(candidates)
    }

    fun needsAiReason(candidate: OrganizedPetite): Boolean {
        return candidate.sourceType == PetiteSourceType.EXAM &&
            (candidate.aiComment == null || candidate.estimatedMinutes == null || candidate.steps.isEmpty())
    }
}

class TodayExamOrganizer(
    private val decisionProvider: AiDecisionProvider = MockAiDecisionProvider()
) {
    suspend fun organize(
        todayMillis: Long,
        todos: List<TodoItem>,
        routines: List<OrganizerRoutine>,
        activities: List<ActivitySession>,
        hiddenAiSourceKeys: Set<String> = emptySet()
    ): List<OrganizedPetite> {
        val todayStart = startOfDay(todayMillis)
        val exams = collectExamCandidates(todayStart, todos, activities)
        val recoveryMode = exams.any { it.metrics.isSeverelyBehind }
        val context = TodayOrganizerContext(todayMillis = todayMillis, recoveryMode = recoveryMode)

        val candidates = collectCandidates(
            todayStart = todayStart,
            exams = exams,
            todos = todos,
            routines = routines,
            recoveryMode = recoveryMode
        )
            .distinctBy { it.sourceKey() }
            .filterNot { it.sourceKey() in hiddenAiSourceKeys }

        val locallySorted = TodayOrganizerRules.sort(candidates)
        val ranked = if (TodayOrganizerRules.needsAiRanking(locallySorted)) {
            decisionProvider.rankAmbiguousItems(locallySorted, context)
        } else {
            locallySorted
        }

        return ranked.map { candidate ->
            if (TodayOrganizerRules.needsAiReason(candidate)) {
                val reason = decisionProvider.generateRecommendationReason(candidate, context)
                candidate.copy(
                    aiComment = reason?.aiComment ?: candidate.aiComment,
                    estimatedMinutes = reason?.estimatedMinutes ?: candidate.estimatedMinutes,
                    steps = reason?.steps?.takeIf { it.isNotEmpty() } ?: candidate.steps
                )
            } else {
                candidate
            }
        }
    }

    fun calculateExamStudyMetrics(
        todayStart: Long,
        exam: TodoItem,
        activities: List<ActivitySession>
    ): ExamStudyMetrics {
        val examDate = startOfDay(exam.selectedDate ?: todayStart)
        val windowStart = examDate - TodayOrganizerRules.EXAM_WINDOW_DAYS * DAY_MILLIS
        val todayEnd = todayStart + DAY_MILLIS
        val studyActivities = activities.filter { activity ->
            activity.durationMillis > 0L &&
                activity.startTime in windowStart until todayEnd &&
                isStudyForExam(activity, exam)
        }
        val studiedDayKeys = studyActivities
            .map { startOfDay(it.startTime) }
            .toSet()
        val elapsedWindowDays = (daysDiff(windowStart, todayStart) + 1).coerceAtLeast(1).toInt()
        val studiedDays = studiedDayKeys.size
        val missedDays = (elapsedWindowDays - studiedDays).coerceAtLeast(0)
        val totalMinutes = (studyActivities.sumOf { it.durationMillis } / 60_000L).toInt()

        return ExamStudyMetrics(
            totalStudyMinutes = totalMinutes,
            studiedDays = studiedDays,
            missedDays = missedDays,
            isSeverelyBehind = TodayOrganizerRules.isSeverelyBehind(
                totalStudyMinutes = totalMinutes,
                studiedDays = studiedDays,
                missedDays = missedDays
            )
        )
    }

    private fun collectExamCandidates(
        todayStart: Long,
        todos: List<TodoItem>,
        activities: List<ActivitySession>
    ): List<ExamCandidate> {
        return todos
            .filter { it.category == TodoCategory.UNIVERSITY_EXAM && !it.isCompleted && it.selectedDate != null }
            .mapNotNull { exam ->
                val daysUntil = daysDiff(todayStart, startOfDay(exam.selectedDate ?: return@mapNotNull null)).toInt()
                if (daysUntil in 0..TodayOrganizerRules.EXAM_WINDOW_DAYS) {
                    ExamCandidate(exam, daysUntil, calculateExamStudyMetrics(todayStart, exam, activities))
                } else {
                    null
                }
            }
    }

    private fun collectCandidates(
        todayStart: Long,
        exams: List<ExamCandidate>,
        todos: List<TodoItem>,
        routines: List<OrganizerRoutine>,
        recoveryMode: Boolean
    ): List<OrganizedPetite> {
        return buildList {
            addAll(exams.map { it.toOrganizedPetite(recoveryMode) })
            addAll(todos.mapNotNull { it.toTodoPetite(todayStart, recoveryMode) })
            addAll(routines.mapNotNull { it.toRoutinePetite(recoveryMode) })
            addAll(todos.mapNotNull { it.toExistingPetite(recoveryMode) })
        }
    }

    private data class ExamCandidate(
        val exam: TodoItem,
        val daysUntil: Int,
        val metrics: ExamStudyMetrics
    )

    private fun ExamCandidate.toOrganizedPetite(recoveryMode: Boolean): OrganizedPetite {
        val needsRecovery = metrics.isSeverelyBehind
        val titleSuffix = if (needsRecovery) "복구 공부" else "시험 공부"
        return OrganizedPetite(
            id = "ai_exam_${exam.id}",
            title = "${exam.title} $titleSuffix",
            sourceType = PetiteSourceType.EXAM,
            sourceId = exam.id.toString(),
            category = TodoCategory.UNIVERSITY_EXAM,
            dateMillis = exam.selectedDate,
            linkedActivityName = exam.title,
            activityCategory = "STUDY",
            priorityScore = examPriority(daysUntil, recoveryMode),
            isSeverelyBehind = needsRecovery,
            totalStudyMinutesSinceD7 = metrics.totalStudyMinutes,
            studiedDaysSinceD7 = metrics.studiedDays,
            missedDaysSinceD7 = metrics.missedDays,
            aiComment = null,
            estimatedMinutes = null,
            steps = emptyList(),
            examDValue = daysUntil
        )
    }

    private fun TodoItem.toTodoPetite(todayStart: Long, recoveryMode: Boolean): OrganizedPetite? {
        if (
            isCompleted ||
            category == TodoCategory.TODAY ||
            category == TodoCategory.UNIVERSITY_EXAM
        ) return null

        val priority = when (category) {
            TodoCategory.ASSIGNMENT -> when {
                selectedDate == null -> return null
                daysUntilDue(todayStart) < 0 -> if (recoveryMode) 20 else 5
                daysUntilDue(todayStart) == 0 -> if (recoveryMode) 20 else 10
                daysUntilDue(todayStart) == 1 -> if (recoveryMode) 50 else 30
                else -> return null
            }
            TodoCategory.NORMAL -> when {
                selectedDate == null -> return null
                daysUntilDue(todayStart) < 0 -> if (recoveryMode) 75 else 65
                daysUntilDue(todayStart) == 0 -> if (recoveryMode) 80 else 80
                daysUntilDue(todayStart) == 1 && shouldPullTomorrowTodoEarly() -> if (recoveryMode) 85 else 85
                else -> return null
            }
            else -> return null
        }

        return OrganizedPetite(
            id = "ai_todo_$id",
            title = title,
            sourceType = PetiteSourceType.TODO,
            sourceId = id.toString(),
            category = category,
            dateMillis = selectedDate,
            priorityScore = priority,
            burdenScore = burdenScore,
            aiComment = if (category == TodoCategory.ASSIGNMENT) "마감이 가까워서 오늘 큐에 올렸어요." else null
        )
    }

    private fun TodoItem.daysUntilDue(todayStart: Long): Int {
        val due = selectedDate ?: return Int.MAX_VALUE
        return daysDiff(todayStart, startOfDay(due)).toInt()
    }

    private fun TodoItem.shouldPullTomorrowTodoEarly(): Boolean {
        return burdenLevel == "HEAVY" || burdenScore >= TodayOrganizerRules.HIGH_PRIORITY_BURDEN_SCORE
    }

    private fun OrganizerRoutine.toRoutinePetite(recoveryMode: Boolean): OrganizedPetite? {
        if (label != "Routine" || isCompleted) return null
        return OrganizedPetite(
            id = "ai_routine_$id",
            title = title,
            sourceType = PetiteSourceType.ROUTINE,
            sourceId = id.toString(),
            priorityScore = if (recoveryMode) 90 else 90,
            routineTimerDurationMillis = timerDurationMillis,
            routineTimerCategory = timerCategory
        )
    }

    private fun TodoItem.toExistingPetite(recoveryMode: Boolean): OrganizedPetite? {
        if (category != TodoCategory.TODAY) return null
        return OrganizedPetite(
            id = "petite_$id",
            title = title,
            sourceType = PetiteSourceType.PETITE,
            sourceId = id.toString(),
            category = category,
            dateMillis = selectedDate,
            isCompleted = isCompleted,
            priorityScore = if (recoveryMode) 100 else 100
        )
    }

    private fun examPriority(daysUntil: Int, recoveryMode: Boolean): Int {
        return if (recoveryMode) {
            when (daysUntil) {
                0 -> 10
                1 -> 30
                2 -> 40
                3 -> 60
                in 4..7 -> 70
                else -> 95
            }
        } else {
            when (daysUntil) {
                2 -> 20
                3 -> 40
                1 -> 50
                0 -> 60
                in 4..7 -> 70
                else -> 95
            }
        }
    }

    private fun isStudyForExam(activity: ActivitySession, exam: TodoItem): Boolean {
        if (activity.linkedTodoId == exam.id) return true
        if (!activity.category.equals("STUDY", ignoreCase = true)) return false
        val activityText = normalize(activity.title)
        val examTokens = normalize(exam.title)
            .split(" ")
            .filter { it.length >= 2 && it !in EXAM_STOP_WORDS }
        return examTokens.isNotEmpty() && examTokens.any { activityText.contains(it) }
    }

    private fun normalize(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^0-9a-z가-힣]+"), " ")
            .trim()
    }

    private fun OrganizedPetite.sourceKey(): String = "$sourceType:$sourceId"

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun daysDiff(fromMs: Long, toMs: Long): Long = (toMs - fromMs) / DAY_MILLIS

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        val EXAM_STOP_WORDS = setOf("시험", "공부", "중간", "기말", "대학", "복구")
    }
}
