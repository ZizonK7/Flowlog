package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.agent.AiDecisionProviderFactory
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.agent.OrganizerRoutine
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.agent.TodayExamOrganizer
import com.example.flowlog.data.agent.TodayOrganizerRules
import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.TodoBurdenAnalysis
import com.example.flowlog.data.recommendation.TodoBurdenCalculator
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.GoalItem
import com.example.flowlog.data.repository.OrganizedPetiteRepository
import com.example.flowlog.data.repository.TodoRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit


data class DailyCueItem(
    val id: Long,
    val label: String,
    val title: String,
    val isCompleted: Boolean = false,
    val timerDurationMillis: Long? = null,
    val timerCategory: String = "TODO",
    val order: Int = 0
)

data class YesterdayFlowSuggestion(
    val message: String,
    val actionLabel: String,
    val actionCategory: String
)

data class OrganizedPetiteUndoEvent(
    val item: OrganizedPetite,
    val previousCompletedState: Boolean,
    val wasHidden: Boolean
)

class TodoViewModel(
    private val repository: TodoRepository,
    context: Context
) : ViewModel() {

    private val focusPrefs = context.getSharedPreferences("todo_focus", Context.MODE_PRIVATE)
    private val cuePrefs = context.getSharedPreferences("daily_cues", Context.MODE_PRIVATE)
    private val normalTodoOrderPrefs = context.getSharedPreferences("todo_normal_order", Context.MODE_PRIVATE)
    private val dailyGoalRepository = DailyGoalRepository(context.applicationContext)
    private val activityRepository = ActivityRepository(context.applicationContext)
    private val eventLogRepository = EventLogRepository(context.applicationContext)
    private val organizedPetiteRepository = OrganizedPetiteRepository(context.applicationContext)
    private val todayOrganizer = TodayExamOrganizer(AiDecisionProviderFactory.create())

    private val _todos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todos: StateFlow<List<TodoItem>> = _todos.asStateFlow()

    private val _normalTodoOrder = MutableStateFlow<List<Long>>(loadNormalTodoOrder())
    val normalTodosOrdered: StateFlow<List<TodoItem>> = combine(_todos, _normalTodoOrder) { todos, order ->
        val todayTodos = todos.filter { !it.isCompleted && it.category == TodoCategory.TODAY }
        if (order.isEmpty()) todayTodos
        else {
            val indexed = todayTodos.associateBy { it.id }
            val inOrder = order.mapNotNull { indexed[it] }
            val rest = todayTodos.filter { it.id !in order }
            inOrder + rest
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _organizedPetiteUndoEvents = MutableSharedFlow<OrganizedPetiteUndoEvent>(extraBufferCapacity = 4)
    val organizedPetiteUndoEvents: SharedFlow<OrganizedPetiteUndoEvent> = _organizedPetiteUndoEvents.asSharedFlow()

    private val _focusIds = MutableStateFlow<List<Long>>(emptyList())
    private val _dailyCues = MutableStateFlow<List<DailyCueItem>>(loadDailyCues())
    val dailyCues: StateFlow<List<DailyCueItem>> = _dailyCues.asStateFlow()
    private val _organizedPetites = MutableStateFlow<List<OrganizedPetite>>(emptyList())
    val organizedPetites: StateFlow<List<OrganizedPetite>> = _organizedPetites.asStateFlow()
    private val _isTodayOrganizerRunning = MutableStateFlow(false)
    val isTodayOrganizerRunning: StateFlow<Boolean> = _isTodayOrganizerRunning.asStateFlow()
    private val _yesterdaySuggestion = MutableStateFlow<YesterdayFlowSuggestion?>(null)
    val yesterdaySuggestion: StateFlow<YesterdayFlowSuggestion?> = _yesterdaySuggestion.asStateFlow()
    private var latestActivities: List<ActivitySession> = emptyList()
    private var isRefreshing = false
    private var lastRecommendationDateKey = 0L
    private val hiddenAiSourceKeys = mutableSetOf<String>()
    private var isTodayOrganizerAllowed = false

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
                val todayIncomplete = todos.filter { it.category == TodoCategory.TODAY && !it.isCompleted }
                // _todos가 업데이트된 시점에 모든 TODAY todo의 PETITE를 등록.
                // addTodo()보다 늦게 실행되므로 새 todo도 이 시점엔 _todos.value에 포함됨.
                if (todayIncomplete.isNotEmpty()) {
                    registerAllTodayTodoPetitesIfAbsent()
                }
                _yesterdaySuggestion.value = buildYesterdaySuggestion(todos, latestActivities)
                if (_focusIds.value.isEmpty() && todos.isNotEmpty() &&
                    lastRecommendationDateKey != startOfDay(System.currentTimeMillis())) {
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
        viewModelScope.launch {
            hiddenAiSourceKeys.clear()
            hiddenAiSourceKeys += organizedPetiteRepository.loadDismissedSourceKeys()
            organizedPetiteRepository.observeActivePetites().collect { saved ->
                _organizedPetites.value = saved
            }
        }
    }

    private suspend fun initFocusIds(allTodos: List<TodoItem>) {
        val todayKey = startOfDay(System.currentTimeMillis())
        lastRecommendationDateKey = todayKey
        val storedKey = focusPrefs.getLong("date_key", 0L)
        val storedOrdered = focusPrefs.getString("focus_ids_ordered", null)
            ?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: focusPrefs.getStringSet("focus_ids", emptySet())
                ?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()

        val hasUrgentMissed = storedKey == todayKey && storedOrdered.isNotEmpty() && run {
            allTodos.any { todo ->
                !storedOrdered.contains(todo.id) &&
                    !todo.isCompleted &&
                    todo.category == TodoCategory.ASSIGNMENT &&
                    todo.selectedDate != null &&
                    daysDiff(todayKey, startOfDay(todo.selectedDate)) in 0L..1L
            }
        }
        val existingRecForToday = dailyGoalRepository.getTodayRecommendation(dailyGoalRepository.todayDateKey())
        if (storedKey == todayKey && !hasUrgentMissed && (storedOrdered.isNotEmpty() || existingRecForToday != null)) {
            _focusIds.value = storedOrdered
            return
        }

        dailyGoalRepository.reconcilePastRecommendations(allTodos, latestActivities)
        val burdenAnalyses = TodoBurdenCalculator.analyze(allTodos, latestActivities)
        repository.updateBurdenCaches(burdenAnalyses)
        val calendarPetites = activeCalendarPetitesForRecommendation()
        val selectionResult = selectTodayFocus(allTodos, calendarPetites, burdenAnalyses)
        val newIds = selectionResult.mapNotNull { it.todo?.id }
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
            .filter { it.todo?.category == TodoCategory.REVIEW && it.todo?.reviewStage == 1 && it.todo?.isCompleted == true }
            .forEach { goalItem ->
                goalItem.todo?.let { todo ->
                    repository.updateTodo(todo.copy(isCompleted = false, updatedAt = System.currentTimeMillis()))
                }
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
            // insertTodo 완료 후 _todos flow가 먼저 업데이트되어야 새 todo가 포함됨.
            // todos flow 업데이트는 DB observer를 통해 async하게 오므로, 여기서 즉시 호출하면
            // _todos.value에 새 todo가 없을 수 있다. 대신 _todos collect에서 처리한다.
            if (category == TodoCategory.TODAY) {
                registerAllTodayTodoPetitesIfAbsent()
            }
        }
    }

    // 새 TODAY todo 추가 시 기존 TODAY todos도 함께 등록 — hasNonCalendarOrganizedPetites 전환 시
    // 화면에서 다른 TODAY todo가 사라지지 않도록 보장한다.
    private fun registerAllTodayTodoPetitesIfAbsent() {
        val todayTodos = _todos.value.filter { it.category == TodoCategory.TODAY && !it.isCompleted }
        viewModelScope.launch {
            todayTodos.forEach { todo ->
                runCatching {
                    organizedPetiteRepository.addLocalTodoPetiteIfAbsent(
                        OrganizedPetite(
                            id = "petite_${todo.id}",
                            title = todo.title,
                            sourceType = PetiteSourceType.PETITE,
                            sourceId = todo.id.toString(),
                            priorityScore = 100
                        )
                    )
                }
            }
        }
    }

    fun completeTodo(todo: TodoItem) {
        if (todo.category == TodoCategory.UNIVERSITY_EXAM) return
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                repository.completeReviewTodo(todo)
            } else {
                repository.updateCompleted(todo.id, true, System.currentTimeMillis())
            }
        }
    }

    fun completeFocusTodo(todo: TodoItem) {
        if (todo.category == TodoCategory.UNIVERSITY_EXAM) return
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
            // _todos collect에서 registerAllTodayTodoPetitesIfAbsent()가 이미 트리거되지만,
            // 즉시 반영을 위해 여기서도 호출.
            if (todo.category == TodoCategory.TODAY && !todo.isCompleted) {
                registerAllTodayTodoPetitesIfAbsent()
            }
        }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch {
            dailyGoalRepository.dismissItemsByTodoId(todo.id)
            repository.deleteTodo(todo)
        }
    }

    fun refreshSort() {
        if (isRefreshing) return
        val currentTodos = _todos.value
        if (currentTodos.isEmpty()) return

        val burdenAnalyses = TodoBurdenCalculator.analyze(currentTodos, latestActivities)
        val calendarPetites = activeCalendarPetitesForRecommendation()
        val selectionResult = selectTodayFocus(currentTodos, calendarPetites, burdenAnalyses)
        val newIds = selectionResult.mapNotNull { it.todo?.id }
        val todayKey = startOfDay(System.currentTimeMillis())
        lastRecommendationDateKey = todayKey
        _focusIds.value = newIds
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
                    .filter { it.todo?.category == TodoCategory.REVIEW && it.todo?.reviewStage == 1 && it.todo?.isCompleted == true }
                    .forEach { goalItem ->
                        goalItem.todo?.let { todo ->
                            repository.updateTodo(todo.copy(isCompleted = false, updatedAt = System.currentTimeMillis()))
                        }
                    }
            } finally {
                isRefreshing = false
            }
        }
    }

    fun runTodayOrganizer() {
        if (!isTodayOrganizerAllowed) return
        if (_isTodayOrganizerRunning.value) return
        viewModelScope.launch {
            _isTodayOrganizerRunning.value = true
            try {
                hiddenAiSourceKeys.clear()
                val organized = todayOrganizer.organize(
                    todayMillis = System.currentTimeMillis(),
                    todos = _todos.value,
                    routines = _dailyCues.value.map {
                        OrganizerRoutine(
                            id = it.id,
                            label = it.label,
                            title = it.title,
                            isCompleted = it.isCompleted,
                            timerDurationMillis = it.timerDurationMillis,
                            timerCategory = it.timerCategory
                        )
                    },
                    activities = latestActivities,
                    hiddenAiSourceKeys = hiddenAiSourceKeys
                )
                // CALENDAR items (from calendar pull) are preserved in the immediate UI update.
                // replaceWith → replaceNonCalendarForUser preserves them in DB as well.
                val calendarPetites = _organizedPetites.value
                    .filter { it.sourceType == PetiteSourceType.CALENDAR }
                _organizedPetites.value = organized + calendarPetites
                organizedPetiteRepository.replaceWith(organized)
            } finally {
                _isTodayOrganizerRunning.value = false
            }
        }
    }

    fun resetTodayOrganizer() {
        if (!isTodayOrganizerAllowed) return
        hiddenAiSourceKeys.clear()
        // CALENDAR items from calendar pull are kept — only organizer-generated items are cleared.
        _organizedPetites.value = _organizedPetites.value
            .filter { it.sourceType == PetiteSourceType.CALENDAR }
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.replaceWith(emptyList()) }
        }
    }

    fun setTodayOrganizerAllowed(allowed: Boolean) {
        isTodayOrganizerAllowed = allowed
    }

    fun dismissPetiteLinkedToTodo(todoId: Long) {
        val sourceIdStr = todoId.toString()
        // TODO·PETITE 두 sourceType이 동일 todoId로 공존할 수 있으므로 메모리·DB 모두 한꺼번에 제거.
        _organizedPetites.value = _organizedPetites.value.filterNot { p ->
            (p.sourceType == PetiteSourceType.TODO || p.sourceType == PetiteSourceType.PETITE) &&
                p.sourceId == sourceIdStr
        }
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.dismissTodoPetitesBySourceId(sourceIdStr) }
        }
    }

    fun completePetiteById(petiteId: String) {
        _organizedPetites.value = _organizedPetites.value.filterNot { it.id == petiteId }
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.completeById(petiteId) }
        }
    }

    fun completeOrganizedPetite(item: OrganizedPetite) {
        val previousCompletedState = when (item.sourceType) {
            PetiteSourceType.PETITE,
            PetiteSourceType.TODO -> item.sourceId
                ?.toLongOrNull()
                ?.let { id -> _todos.value.firstOrNull { it.id == id } }
                ?.isCompleted ?: item.isCompleted
            PetiteSourceType.ROUTINE -> item.sourceId
                ?.toLongOrNull()
                ?.let { id -> _dailyCues.value.firstOrNull { it.id == id } }
                ?.isCompleted ?: item.isCompleted
            PetiteSourceType.EXAM,
            PetiteSourceType.CALENDAR -> false
        }
        var wasHidden = false
        when (item.sourceType) {
            PetiteSourceType.PETITE,
            PetiteSourceType.TODO -> {
                val todo = item.sourceId?.toLongOrNull()?.let { id -> _todos.value.firstOrNull { it.id == id } }
                if (todo != null) {
                    if (item.sourceType == PetiteSourceType.PETITE) completeTodo(todo) else completeFocusTodo(todo)
                }
            }
            PetiteSourceType.ROUTINE -> {
                item.sourceId?.toLongOrNull()?.let { completeDailyCue(it) }
            }
            PetiteSourceType.EXAM -> {
                hiddenAiSourceKeys += item.sourceKey()
                wasHidden = true
            }
            PetiteSourceType.CALENDAR -> {
                val now = System.currentTimeMillis()
                viewModelScope.launch {
                    runCatching {
                        repository.insertTodo(
                            TodoItem(title = item.title, category = TodoCategory.TODAY, isCompleted = true, createdAt = now, updatedAt = now)
                        )
                    }
                }
            }
        }
        _organizedPetites.value = _organizedPetites.value.filterNot { it.sourceKey() == item.sourceKey() }
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.dismiss(item) }
        }
        _organizedPetiteUndoEvents.tryEmit(
            OrganizedPetiteUndoEvent(
                item = item,
                previousCompletedState = previousCompletedState,
                wasHidden = wasHidden
            )
        )
    }

    fun undoOrganizedPetiteCompletion(event: OrganizedPetiteUndoEvent) {
        val item = event.item
        when (item.sourceType) {
            PetiteSourceType.PETITE,
            PetiteSourceType.TODO -> {
                if (!event.previousCompletedState) {
                    item.sourceId
                        ?.toLongOrNull()
                        ?.let { id -> _todos.value.firstOrNull { it.id == id } }
                        ?.let { uncompleteTodo(it) }
                }
            }
            PetiteSourceType.ROUTINE -> {
                if (!event.previousCompletedState) {
                    item.sourceId?.toLongOrNull()?.let { toggleDailyCue(it) }
                }
            }
            PetiteSourceType.EXAM -> {
                if (event.wasHidden) {
                    hiddenAiSourceKeys -= item.sourceKey()
                }
            }
            PetiteSourceType.CALENDAR -> {}
        }
        if (_organizedPetites.value.none { it.sourceKey() == item.sourceKey() }) {
            val restored = TodayOrganizerRules.sort(_organizedPetites.value + item)
            _organizedPetites.value = restored
            viewModelScope.launch {
                runCatching { organizedPetiteRepository.restore(item) }
            }
        }
    }

    private fun OrganizedPetite.sourceKey(): String = "$sourceType:$sourceId"

    fun addDailyCue(title: String, label: String, timerDurationMillis: Long?, timerCategory: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanLabel = if (label == "Memo") "Memo" else "Routine"
        val cleanTimerDurationMillis = timerDurationMillis?.takeIf { cleanLabel == "Routine" && it > 0L }
        val cleanTimerCategory = timerCategory.takeIf { cleanLabel == "Routine" && it.isNotBlank() } ?: "TODO"
        val newOrder = (_dailyCues.value.maxOfOrNull { it.order } ?: -1) + 1
        val updated = sortDailyCues(_dailyCues.value + DailyCueItem(
            id = System.currentTimeMillis(),
            label = cleanLabel,
            title = cleanTitle,
            timerDurationMillis = cleanTimerDurationMillis,
            timerCategory = cleanTimerCategory,
            order = newOrder
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
                    timerCategory = item.optString("timerCategory", "TODO").ifBlank { "TODO" },
                    order = item.optInt("order", index)
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
                put("order", cue.order)
            })
        }
        cuePrefs.edit()
            .putLong("date_key", dateKey)
            .putString("items_json", array.toString())
            .apply()
    }

    private fun defaultDailyCues(): List<DailyCueItem> = listOf(
        DailyCueItem(1L, "Routine", "물 마시기", order = 0),
        DailyCueItem(2L, "Routine", "스트레칭", order = 1),
        DailyCueItem(3L, "Memo", "신청서 확인", order = 2),
        DailyCueItem(4L, "Memo", "우산 챙기기", order = 3)
    )

    private fun sortDailyCues(cues: List<DailyCueItem>): List<DailyCueItem> {
        return cues.sortedBy { it.order }
    }

    fun swapDailyCue(cueId: Long, direction: Int) {
        val sorted = _dailyCues.value.sortedBy { it.order }
        val idx = sorted.indexOfFirst { it.id == cueId }
        if (idx < 0) return
        val targetIdx = idx + direction
        if (targetIdx < 0 || targetIdx >= sorted.size) return
        val mutable = sorted.toMutableList()
        val tempOrder = mutable[idx].order
        mutable[idx] = mutable[idx].copy(order = mutable[targetIdx].order)
        mutable[targetIdx] = mutable[targetIdx].copy(order = tempOrder)
        val updated = sortDailyCues(mutable)
        _dailyCues.value = updated
        saveDailyCues(updated)
    }

    fun swapOrganizedPetite(itemId: String, direction: Int) {
        val current = _organizedPetites.value.toMutableList()
        val idx = current.indexOfFirst { it.id == itemId }
        if (idx < 0) return
        val targetIdx = idx + direction
        if (targetIdx < 0 || targetIdx >= current.size) return
        val temp = current[idx]
        current[idx] = current[targetIdx]
        current[targetIdx] = temp
        _organizedPetites.value = current.toList()
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.replaceWith(current.toList()) }
        }
    }

    fun reorderOrganizedPetites(from: Int, to: Int) {
        val current = _organizedPetites.value.toMutableList()
        if (from < 0 || to < 0 || from >= current.size || to >= current.size || from == to) return
        val item = current.removeAt(from)
        current.add(to, item)
        _organizedPetites.value = current.toList()
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.replaceWith(current.toList()) }
        }
    }

    fun reorderDailyCues(from: Int, to: Int) {
        val sorted = _dailyCues.value.sortedBy { it.order }.toMutableList()
        if (from < 0 || to < 0 || from >= sorted.size || to >= sorted.size || from == to) return
        val item = sorted.removeAt(from)
        sorted.add(to, item)
        val reordered = sorted.mapIndexed { i, cue -> cue.copy(order = i) }
        _dailyCues.value = reordered
        saveDailyCues(reordered)
    }

    fun reorderNormalTodos(from: Int, to: Int) {
        val current = normalTodosOrdered.value.toMutableList()
        if (from < 0 || to < 0 || from >= current.size || to >= current.size || from == to) return
        val item = current.removeAt(from)
        current.add(to, item)
        val newOrder = current.map { it.id }
        _normalTodoOrder.value = newOrder
        saveNormalTodoOrder(newOrder)
    }

    private fun loadNormalTodoOrder(): List<Long> {
        return normalTodoOrderPrefs.getString("order", null)
            ?.split(",")?.mapNotNull { it.toLongOrNull() }
            ?: emptyList()
    }

    private fun saveNormalTodoOrder(ids: List<Long>) {
        normalTodoOrderPrefs.edit().putString("order", ids.joinToString(",")).apply()
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
        calendarPetites: List<OrganizedPetite> = emptyList(),
        burdenAnalyses: List<TodoBurdenAnalysis> = TodoBurdenCalculator.analyze(allTodos, latestActivities)
    ): List<GoalItem> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val active = allTodos.filter {
            it.category != TodoCategory.UNIVERSITY_EXAM &&
            (!it.isCompleted || (it.category == TodoCategory.REVIEW && it.reviewStage == 1))
        }

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
                TodoCategory.NORMAL, TodoCategory.TODAY -> return@mapNotNull null
                TodoCategory.UNIVERSITY_EXAM -> return@mapNotNull null
            }
            Candidate(todo, priority, reason)
        }
        val candidatesById = active.associate { todo ->
            val priorityCandidate = priorityCandidates.firstOrNull { it.todo.id == todo.id }
            todo.id to (priorityCandidate ?: Candidate(todo, 99, RecommendationReason.EMPTY_GOAL_FILL))
        }

        val petiteCandidates = calendarPetites
            .filter { isCalendarPetiteEligibleForRecommendation(it) }
            .map { CalendarPetiteCandidate(it) }

        val available = candidatesById.values.toList() + petiteCandidates
        if (available.isEmpty()) return emptyList()
        val selected = selectBurdenCombination(available)
        return sortForTodayFocusDisplay(selected).map { it.toGoalItem() }
    }

    // 미래 필터링 훅: 나중에 calendarTaskType == "ACADEMIC"인 것만 허용하도록 수정 예정
    private fun isCalendarPetiteEligibleForRecommendation(petite: OrganizedPetite): Boolean = true

    private fun activeCalendarPetitesForRecommendation(): List<OrganizedPetite> =
        _organizedPetites.value.filter { it.sourceType == PetiteSourceType.CALENDAR }

    private inner class CalendarPetiteCandidate(val petite: OrganizedPetite) : TodayFocusCandidateLike {
        override val priority: Int = 99
        override val burdenLevel: String = "MEDIUM"
        override val burdenScore: Int = petite.burdenScore ?: 0
        override val createdAt: Long = 0L
        override fun toGoalItem(): GoalItem = GoalItem(
            petite = petite,
            reason = RecommendationReason.CALENDAR_TODAY,
            burdenLevel = "MEDIUM"
        )
    }

    private fun selectBurdenCombination(candidates: List<TodayFocusCandidateLike>): List<TodayFocusCandidateLike> {
        if (candidates.size <= 1) return candidates

        // priority <= 1 (D-0, D-1)은 부담도와 무관하게 무조건 포함
        val mandatory = candidates.filter { it.priority <= 1 }.sortedBy { it.priority }
        if (mandatory.size >= 2) return mandatory.take(3)

        val remaining = candidates.filter { it.priority > 1 }
        val slotsLeft = 2 - mandatory.size
        val fill = pickByBurdenCombination(remaining, slotsLeft)
        return mandatory + fill
    }

    private fun pickByBurdenCombination(candidates: List<TodayFocusCandidateLike>, count: Int): List<TodayFocusCandidateLike> {
        if (count <= 0 || candidates.isEmpty()) return emptyList()
        if (candidates.size <= count) return candidates

        val byBurden = candidates.groupBy { it.burdenLevel }
        fun lightPool() = byBurden["LIGHT"].orEmpty().sortedWith(lightCandidateComparator())
        fun mediumPool() = byBurden["MEDIUM"].orEmpty().sortedWith(mediumCandidateComparator())
        fun heavyPool() = byBurden["HEAVY"].orEmpty().sortedWith(heavyCandidateComparator())

        val light = lightPool()
        val medium = mediumPool()
        val heavy = heavyPool()

        if (count == 1) return listOf(candidates.sortedWith(fallbackCandidateComparator()).first())

        return when {
            light.isNotEmpty() && heavy.isNotEmpty() -> listOf(light.first(), heavy.first())
            medium.size >= 2 -> medium.take(2)
            light.isNotEmpty() && medium.isNotEmpty() -> listOf(light.first(), medium.first())
            light.size >= 2 -> light.take(2)
            else -> candidates.sortedWith(fallbackCandidateComparator()).take(count)
        }
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
