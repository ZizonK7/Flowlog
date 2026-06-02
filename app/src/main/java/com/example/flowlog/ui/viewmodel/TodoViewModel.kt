package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.TodoBurdenAnalysis
import com.example.flowlog.data.recommendation.TodoBurdenCalculator
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import com.example.flowlog.data.repository.GoalItem
import com.example.flowlog.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class DailyCueItem(
    val id: Long,
    val label: String,
    val title: String,
    val isCompleted: Boolean = false,
    val timerDurationMillis: Long? = null,
    val timerCategory: String = "TODO"
)

data class YesterdayFlowSuggestion(
    val message: String,
    val actionLabel: String,
    val actionCategory: String
)

class TodoViewModel(
    private val repository: TodoRepository,
    context: Context
) : ViewModel() {

    private val focusPrefs = context.getSharedPreferences("todo_focus", Context.MODE_PRIVATE)
    private val cuePrefs = context.getSharedPreferences("daily_cues", Context.MODE_PRIVATE)
    private val dailyGoalRepository = DailyGoalRepository(context.applicationContext)
    private val activityRepository = ActivityRepository(context.applicationContext)

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _focusIds = MutableStateFlow<List<Long>>(emptyList())
    private val _dailyCues = MutableStateFlow<List<DailyCueItem>>(loadDailyCues())
    val dailyCues: StateFlow<List<DailyCueItem>> = _dailyCues.asStateFlow()
    private val _yesterdaySuggestion = MutableStateFlow<YesterdayFlowSuggestion?>(null)
    val yesterdaySuggestion: StateFlow<YesterdayFlowSuggestion?> = _yesterdaySuggestion.asStateFlow()
    private var latestActivities: List<ActivitySession> = emptyList()
    private var isRefreshing = false

    val todayFocusItems: StateFlow<List<TodoItem>> = combine(
        _todos,
        dailyGoalRepository.observeTodayActiveTodoIds()
    ) { todos, repoIds ->
        val effectiveIds = repoIds.ifEmpty { _focusIds.value }
        val idToIndex = effectiveIds.mapIndexed { i, id -> id to i }.toMap()
        todos.filter { it.id in idToIndex }
            .sortedWith(compareBy(
                { if (it.isCompleted) 1 else 0 },
                { idToIndex[it.id] ?: Int.MAX_VALUE }
            ))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repository.getAllTodos().collect { todos ->
                _todos.value = todos
                _yesterdaySuggestion.value = buildYesterdaySuggestion(todos, latestActivities)
                if (_focusIds.value.isEmpty() && todos.isNotEmpty()) {
                    initFocusIds(todos)
                }
            }
        }
        viewModelScope.launch {
            activityRepository.getAllActivities().collect { activities ->
                latestActivities = activities
                _yesterdaySuggestion.value = buildYesterdaySuggestion(_todos.value, activities)
            }
        }
    }

    private suspend fun initFocusIds(allTodos: List<TodoItem>) {
        val todayKey = startOfDay(System.currentTimeMillis())
        val storedKey = focusPrefs.getLong("date_key", 0L)
        val storedOrdered = focusPrefs.getString("focus_ids_ordered", null)
            ?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: focusPrefs.getStringSet("focus_ids", emptySet())
                ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        if (storedKey == todayKey && storedOrdered.isNotEmpty()) {
            _focusIds.value = storedOrdered
            return
        }

        dailyGoalRepository.reconcilePastRecommendations(allTodos, latestActivities)
        val burdenAnalyses = TodoBurdenCalculator.analyze(allTodos, latestActivities)
        repository.updateBurdenCaches(burdenAnalyses)
        val selectionResult = selectTodayFocus(allTodos, burdenAnalyses)
        val newIds = selectionResult.map { it.todo.id }
        _focusIds.value = newIds

        focusPrefs.edit()
            .putLong("date_key", todayKey)
            .putString("focus_ids_ordered", newIds.joinToString(","))
            .apply()

        // Room에 오늘의 목표 추천 저장 (async, 실패해도 기존 동작 유지)
        val dateKey = dailyGoalRepository.todayDateKey()
        viewModelScope.launch {
            runCatching {
                dailyGoalRepository.saveRecommendation(
                    dateKey = dateKey,
                    selectedItems = selectionResult,
                    candidateTodos = allTodos.filter { !it.isCompleted },
                    isRefresh = false
                )
                dailyGoalRepository.ensureTodayTimePlan(
                    dateKey = dateKey,
                    activities = latestActivities,
                    forceRefresh = false
                )
            }
        }

        selectionResult
            .filter { it.todo.category == TodoCategory.REVIEW && it.todo.reviewStage == 1 && it.todo.isCompleted }
            .forEach { goalItem ->
                repository.updateTodo(goalItem.todo.copy(isCompleted = false, updatedAt = System.currentTimeMillis()))
            }
    }

    fun addTodo(title: String, category: TodoCategory = TodoCategory.NORMAL, selectedDate: Long? = null) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.insertTodo(
                TodoItem(
                    title = clean,
                    category = category,
                    selectedDate = selectedDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun completeTodo(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                repository.completeReviewTodo(todo)
            } else {
                repository.updateCompleted(todo.id, true, System.currentTimeMillis())
            }
        }
    }

    fun completeFocusTodo(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                repository.completeReviewTodo(todo)
            } else {
                repository.updateCompleted(todo.id, true, System.currentTimeMillis())
            }
            // 오늘의 목표에서 완료된 항목으로 Room에 기록
            runCatching {
                dailyGoalRepository.markItemCompleted(
                    dateKey = dailyGoalRepository.todayDateKey(),
                    todoLegacyId = todo.id
                )
            }
        }
    }

    fun startFocusTodo(todo: TodoItem) {
        viewModelScope.launch {
            runCatching {
                dailyGoalRepository.markItemClicked(
                    dateKey = dailyGoalRepository.todayDateKey(),
                    todoLegacyId = todo.id
                )
            }
        }
    }

    fun uncompleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW) {
                val now = System.currentTimeMillis()
                when (todo.reviewStage) {
                    1 -> repository.updateTodo(todo.copy(
                        isCompleted = false, reviewStage = 0,
                        reviewStage1CompletedAt = null, updatedAt = now
                    ))
                    2 -> repository.updateTodo(todo.copy(
                        isCompleted = true, reviewStage = 1,
                        completedAt = null, updatedAt = now
                    ))
                    else -> repository.updateCompleted(todo.id, false, null)
                }
            } else {
                repository.updateCompleted(todo.id, false, null)
            }
        }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.updateTodo(todo.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            repository.deleteTodo(todo)
        }
    }

    fun refreshSort() {
        if (isRefreshing) return
        val currentTodos = _todos.value
        if (currentTodos.isEmpty()) return

        val burdenAnalyses = TodoBurdenCalculator.analyze(currentTodos, latestActivities)
        val selectionResult = selectTodayFocus(currentTodos, burdenAnalyses)
        val newIds = selectionResult.map { it.todo.id }
        _focusIds.value = newIds

        val todayKey = startOfDay(System.currentTimeMillis())
        focusPrefs.edit()
            .putLong("date_key", todayKey)
            .putString("focus_ids_ordered", newIds.joinToString(","))
            .apply()

        isRefreshing = true
        viewModelScope.launch {
            try {
                // Room에 새로고침 추천 저장
                runCatching {
                    dailyGoalRepository.reconcilePastRecommendations(currentTodos, latestActivities)
                    repository.updateBurdenCaches(burdenAnalyses)
                    dailyGoalRepository.saveRecommendation(
                        dateKey = dailyGoalRepository.todayDateKey(),
                        selectedItems = selectionResult,
                        candidateTodos = currentTodos.filter { !it.isCompleted },
                        isRefresh = true
                    )
                    dailyGoalRepository.ensureTodayTimePlan(
                        dateKey = dailyGoalRepository.todayDateKey(),
                        activities = latestActivities,
                        forceRefresh = true,
                        recommendationModeOverride = DailyGoalRepository.MODE_MANUAL_REFRESH
                    )
                }

                selectionResult
                    .filter { it.todo.category == TodoCategory.REVIEW && it.todo.reviewStage == 1 && it.todo.isCompleted }
                    .forEach { goalItem ->
                        repository.updateTodo(goalItem.todo.copy(isCompleted = false, updatedAt = System.currentTimeMillis()))
                    }
            } finally {
                isRefreshing = false
            }
        }
    }

    fun addDailyCue(title: String, label: String, timerDurationMillis: Long?, timerCategory: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanLabel = if (label == "Memo") "Memo" else "Routine"
        val cleanTimerDurationMillis = timerDurationMillis?.takeIf { cleanLabel == "Routine" && it > 0L }
        val cleanTimerCategory = timerCategory.takeIf { cleanLabel == "Routine" && it.isNotBlank() } ?: "TODO"
        val updated = sortDailyCues(_dailyCues.value + DailyCueItem(
            id = System.currentTimeMillis(),
            label = cleanLabel,
            title = cleanTitle,
            timerDurationMillis = cleanTimerDurationMillis,
            timerCategory = cleanTimerCategory
        ))
        _dailyCues.value = updated
        saveDailyCues(updated)
    }

    fun toggleDailyCue(cueId: Long) {
        val updated = sortDailyCues(_dailyCues.value.map { cue ->
            if (cue.id == cueId) cue.copy(isCompleted = !cue.isCompleted) else cue
        })
        _dailyCues.value = updated
        saveDailyCues(updated)
    }

    fun completeDailyCue(cueId: Long) {
        val updated = sortDailyCues(_dailyCues.value.map { cue ->
            if (cue.id == cueId) cue.copy(isCompleted = true) else cue
        })
        _dailyCues.value = updated
        saveDailyCues(updated)
    }

    fun updateDailyCue(cueId: Long, title: String, label: String, timerDurationMillis: Long?, timerCategory: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanLabel = if (label == "Memo") "Memo" else "Routine"
        val cleanTimerDurationMillis = timerDurationMillis?.takeIf { cleanLabel == "Routine" && it > 0L }
        val cleanTimerCategory = timerCategory.takeIf { cleanLabel == "Routine" && it.isNotBlank() } ?: "TODO"
        val updated = sortDailyCues(_dailyCues.value.map { cue ->
            if (cue.id == cueId) {
                cue.copy(
                    title = cleanTitle,
                    label = cleanLabel,
                    timerDurationMillis = cleanTimerDurationMillis,
                    timerCategory = cleanTimerCategory
                )
            } else {
                cue
            }
        })
        _dailyCues.value = updated
        saveDailyCues(updated)
    }

    fun deleteDailyCue(cueId: Long) {
        val updated = _dailyCues.value.filter { it.id != cueId }
        _dailyCues.value = updated
        saveDailyCues(updated)
    }

    private fun loadDailyCues(): List<DailyCueItem> {
        val todayKey = startOfDay(System.currentTimeMillis())
        val storedDay = cuePrefs.getLong("date_key", 0L)
        val storedJson = cuePrefs.getString("items_json", null)
        val parsed = runCatching {
            val array = JSONArray(storedJson ?: return@runCatching defaultDailyCues())
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                DailyCueItem(
                    id = item.optLong("id", index.toLong()),
                    label = item.optString("label", "Routine"),
                    title = item.optString("title", ""),
                    isCompleted = item.optBoolean("isCompleted", false),
                    timerDurationMillis = item.optLong("timerDurationMillis", 0L).takeIf { it > 0L },
                    timerCategory = item.optString("timerCategory", "TODO").ifBlank { "TODO" }
                )
            }.filter { it.title.isNotBlank() }
        }.getOrElse { defaultDailyCues() }

        val cues = sortDailyCues(parsed.ifEmpty { defaultDailyCues() })
        val normalized = if (storedDay == todayKey) {
            cues
        } else {
            sortDailyCues(cues.map { it.copy(isCompleted = false) })
        }
        if (storedDay != todayKey || storedJson == null) {
            saveDailyCues(normalized, todayKey)
        }
        return normalized
    }

    private fun saveDailyCues(cues: List<DailyCueItem>, dateKey: Long = startOfDay(System.currentTimeMillis())) {
        val array = JSONArray()
        sortDailyCues(cues).forEach { cue ->
            array.put(JSONObject().apply {
                put("id", cue.id)
                put("label", cue.label)
                put("title", cue.title)
                put("isCompleted", cue.isCompleted)
                cue.timerDurationMillis?.let { put("timerDurationMillis", it) }
                put("timerCategory", cue.timerCategory)
            })
        }
        cuePrefs.edit()
            .putLong("date_key", dateKey)
            .putString("items_json", array.toString())
            .apply()
    }

    private fun defaultDailyCues(): List<DailyCueItem> = listOf(
        DailyCueItem(1L, "Routine", "물 마시기"),
        DailyCueItem(2L, "Routine", "스트레칭"),
        DailyCueItem(3L, "Memo", "신청서 확인"),
        DailyCueItem(4L, "Memo", "우산 챙기기")
    )

    private fun sortDailyCues(cues: List<DailyCueItem>): List<DailyCueItem> {
        return cues.sortedWith(compareBy<DailyCueItem> { if (it.label == "Routine") 0 else 1 }.thenBy { it.id })
    }

    fun addWorkTime(todoId: Long, durationMillis: Long) {
        val seconds = durationMillis / 1000L
        if (seconds <= 0L) return
        viewModelScope.launch {
            repository.addAccumulatedSeconds(todoId, seconds)
        }
    }

    suspend fun restoreTodosFromBackup(jsonText: String): Int {
        return repository.restoreTodosFromBackup(jsonText)
    }

    // ── 오늘의 목표 선정 ──────────────────────────────────────────────────
    // 우선순위: D-0 과제는 최우선. 일반 추천은 Todo 2개를 목표로 하되,
    // D-0 과제는 개수 제한 없이 모두 포함하고, D-1 등 긴급 Todo가 많으면 최대 3개까지 허용한다.
    private fun selectTodayFocus(
        allTodos: List<TodoItem>,
        burdenAnalyses: List<TodoBurdenAnalysis> = TodoBurdenCalculator.analyze(allTodos, latestActivities)
    ): List<GoalItem> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val active = allTodos.filter {
            !it.isCompleted || (it.category == TodoCategory.REVIEW && it.reviewStage == 1)
        }
        if (active.isEmpty()) return emptyList()

        val analysisById = burdenAnalyses.associateBy { it.todo.id }

        data class Candidate(
            val todo: TodoItem,
            override val priority: Int,
            val reason: String
        ) : TodayFocusCandidateLike {
            val isUrgent: Boolean = priority <= 1
            val analysis: TodoBurdenAnalysis? get() = analysisById[todo.id]
            override val burdenLevel: String get() = analysis?.burdenLevel ?: todo.burdenLevel ?: "MEDIUM"
            override val burdenScore: Int get() = analysis?.burdenScore ?: todo.burdenScore
            override val createdAt: Long get() = todo.createdAt
            override fun toGoalItem(): GoalItem {
                return GoalItem(
                    todo = todo.copy(
                        burdenLevel = analysis?.burdenLevel ?: todo.burdenLevel,
                        burdenGroupKey = analysis?.burdenGroupKey ?: todo.burdenGroupKey,
                        burdenScore = analysis?.burdenScore ?: todo.burdenScore,
                        burdenReasonJson = analysis?.burdenReasonJson ?: todo.burdenReasonJson
                    ),
                    reason = reason,
                    burdenLevel = analysis?.burdenLevel ?: todo.burdenLevel,
                    burdenGroupKey = analysis?.burdenGroupKey ?: todo.burdenGroupKey,
                    burdenScore = analysis?.burdenScore ?: todo.burdenScore,
                    burdenReasonJson = analysis?.burdenReasonJson ?: todo.burdenReasonJson
                )
            }
        }

        val priorityCandidates = active.mapNotNull { todo ->
            val (priority, reason) = when (todo.category) {
                TodoCategory.ASSIGNMENT -> {
                    val deadline = todo.selectedDate ?: return@mapNotNull null
                    val daysUntil = daysDiff(todayStart, startOfDay(deadline))
                    when (daysUntil) {
                        0L -> 0 to RecommendationReason.ASSIGNMENT_TODAY
                        1L -> 1 to RecommendationReason.ASSIGNMENT_D_MINUS_1
                        7L -> 4 to RecommendationReason.ASSIGNMENT_D_MINUS_7
                        else -> return@mapNotNull null
                    }
                }
                TodoCategory.REVIEW -> {
                    val base = todo.selectedDate ?: todo.createdAt
                    val daysSince = daysDiff(startOfDay(base), todayStart)
                    when {
                        todo.reviewStage == 0 -> when (daysSince) {
                            1L       -> 2 to RecommendationReason.REVIEW_D_PLUS_1
                            2L, 3L   -> 3 to RecommendationReason.REVIEW_D_PLUS_1_LATE
                            else     -> return@mapNotNull null
                        }
                        todo.reviewStage == 1 -> when (daysSince) {
                            7L              -> 5 to RecommendationReason.REVIEW_D_PLUS_7
                            8L, 9L, 10L     -> 6 to RecommendationReason.REVIEW_D_PLUS_7_LATE
                            else            -> return@mapNotNull null
                        }
                        else -> return@mapNotNull null
                    }
                }
                TodoCategory.NORMAL -> return@mapNotNull null
            }
            Candidate(todo, priority, reason)
        }
        val candidatesById = active.associate { todo ->
            val priorityCandidate = priorityCandidates.firstOrNull { it.todo.id == todo.id }
            todo.id to (priorityCandidate ?: Candidate(todo, 99, RecommendationReason.EMPTY_GOAL_FILL))
        }

        val available = candidatesById.values.toList()
        val selected = selectBurdenCombination(available)
        return sortForTodayFocusDisplay(selected).map { it.toGoalItem() }
    }

    private fun selectBurdenCombination(candidates: List<TodayFocusCandidateLike>): List<TodayFocusCandidateLike> {
        if (candidates.size <= 1) return candidates

        val byBurden = candidates.groupBy { it.burdenLevel }
        fun lightPool() = byBurden["LIGHT"].orEmpty().sortedWith(lightCandidateComparator())
        fun mediumPool() = byBurden["MEDIUM"].orEmpty().sortedWith(mediumCandidateComparator())
        fun heavyPool() = byBurden["HEAVY"].orEmpty().sortedWith(heavyCandidateComparator())

        val light = lightPool()
        val medium = mediumPool()
        val heavy = heavyPool()

        val combo = when {
            light.isNotEmpty() && heavy.isNotEmpty() -> listOf(light.first(), heavy.first())
            medium.size >= 2 -> medium.take(2)
            light.isNotEmpty() && medium.isNotEmpty() -> listOf(light.first(), medium.first())
            light.size >= 2 -> light.take(2)
            else -> candidates
                .sortedWith(fallbackCandidateComparator())
                .take(2)
        }
        return combo
    }

    private interface TodayFocusCandidateLike {
        val priority: Int
        val burdenLevel: String
        val burdenScore: Int
        val createdAt: Long
        fun toGoalItem(): GoalItem
    }

    private fun sortForTodayFocusDisplay(candidates: List<TodayFocusCandidateLike>): List<TodayFocusCandidateLike> {
        return candidates.sortedWith(
            compareBy<TodayFocusCandidateLike> { burdenOrder(it.burdenLevel) }
                .thenBy { it.priority }
                .thenByDescending { it.burdenScore }
                .thenByDescending { it.createdAt }
        )
    }

    private fun lightCandidateComparator(): Comparator<TodayFocusCandidateLike> {
        return compareBy<TodayFocusCandidateLike> { it.priority }
            .thenBy { it.burdenScore }
            .thenByDescending { it.createdAt }
    }

    private fun mediumCandidateComparator(): Comparator<TodayFocusCandidateLike> {
        return compareBy<TodayFocusCandidateLike> { it.priority }
            .thenByDescending { it.burdenScore }
            .thenByDescending { it.createdAt }
    }

    private fun heavyCandidateComparator(): Comparator<TodayFocusCandidateLike> {
        return compareBy<TodayFocusCandidateLike> { it.priority }
            .thenByDescending { it.burdenScore }
            .thenByDescending { it.createdAt }
    }

    private fun fallbackCandidateComparator(): Comparator<TodayFocusCandidateLike> {
        return compareBy<TodayFocusCandidateLike> { it.priority }
            .thenByDescending { it.burdenScore }
            .thenByDescending { it.createdAt }
    }

    private fun burdenOrder(level: String): Int {
        return when (level) {
            "LIGHT" -> 0
            "MEDIUM" -> 1
            "HEAVY" -> 2
            else -> 1
        }
    }

    private fun buildYesterdaySuggestion(
        todos: List<TodoItem>,
        activities: List<ActivitySession>
    ): YesterdayFlowSuggestion? {
        val yesterdayStart = startOfDay(System.currentTimeMillis()) - DAY_MILLIS
        val yesterdayEnd = yesterdayStart + DAY_MILLIS
        val yesterday = activities.filter { it.startTime in yesterdayStart until yesterdayEnd }
        val totals = yesterday.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.durationMillis } }
        val productive = listOf("STUDY", "WORK", "COMPANY", "DEVELOPMENT").sumOf { totals[it] ?: 0L }
        val rest = totals["REST"] ?: 0L
        val sleep = totals["SLEEP"] ?: 0L
        val hasOpenTodo = todos.any { !it.isCompleted }

        return when {
            yesterday.isEmpty() -> YesterdayFlowSuggestion(
                message = "어제 기록이 비어 있어요. 오늘은 가벼운 시작 기록부터 남겨볼까요?",
                actionLabel = "휴식으로 시작",
                actionCategory = "REST"
            )
            sleep > 0L && sleep < 5.hours -> YesterdayFlowSuggestion(
                message = "어제 수면이 짧았어요. 오늘은 회복 리듬을 먼저 챙겨도 좋아요.",
                actionLabel = "수면 기록",
                actionCategory = "SLEEP"
            )
            rest < 30.minutes -> YesterdayFlowSuggestion(
                message = "어제 휴식 기록이 거의 없었어요. 짧게 쉬고 다시 들어가도 괜찮아요.",
                actionLabel = "휴식 시작",
                actionCategory = "REST"
            )
            hasOpenTodo && productive < 30.minutes -> YesterdayFlowSuggestion(
                message = "어제 Todo로 이어진 집중 시간이 적었어요. 오늘은 10분만 먼저 열어볼까요?",
                actionLabel = "공부 시작",
                actionCategory = "STUDY"
            )
            productive < 60.minutes -> YesterdayFlowSuggestion(
                message = "어제 공부/업무/개발 흐름이 낮았어요. 부담 없는 한 세션을 추천해요.",
                actionLabel = "집중 시작",
                actionCategory = "STUDY"
            )
            else -> YesterdayFlowSuggestion(
                message = "어제 흐름은 안정적이었어요. 오늘은 목표 두 개만 또렷하게 가져가요.",
                actionLabel = "휴식 시작",
                actionCategory = "REST"
            )
        }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = millis
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun daysDiff(fromMs: Long, toMs: Long): Long =
        (toMs - fromMs) / (24L * 60 * 60 * 1000)

    private val Int.minutes: Long get() = this * 60L * 1000L
    private val Int.hours: Long get() = this * 60L * 60L * 1000L

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}
