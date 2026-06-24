package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.constants.RecommendationReason
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.DailyCueRecommendationTiming
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.TodoBurdenAnalysis
import com.example.flowlog.data.recommendation.TodoBurdenCalculator
import com.example.flowlog.data.recommendation.ReviewRecommendationPolicy
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.DailyCueRecord
import com.example.flowlog.data.repository.DailyCueRepository
import com.example.flowlog.data.remote.FirestoreSyncRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.GoalItem
import com.example.flowlog.data.repository.OrganizedPetiteRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.data.sync.FirebaseSyncCoordinator
import com.google.firebase.auth.FirebaseAuth
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
    val recommendationTiming: DailyCueRecommendationTiming = DailyCueRecommendationTiming.default,
    val note: String = "",
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

    private val appContext = context.applicationContext
    private val focusPrefs = context.getSharedPreferences("todo_focus", Context.MODE_PRIVATE)
    private val cuePrefs = context.getSharedPreferences("daily_cues", Context.MODE_PRIVATE)
    private val normalTodoOrderPrefs = context.getSharedPreferences("todo_normal_order", Context.MODE_PRIVATE)
    private val dailyGoalRepository = DailyGoalRepository(context.applicationContext)
    private val activityRepository = ActivityRepository(context.applicationContext)
    private val dailyCueRepository = DailyCueRepository(context.applicationContext)
    private val firestoreSyncRepository = FirestoreSyncRepository()
    private val eventLogRepository = EventLogRepository(context.applicationContext)
    private val organizedPetiteRepository = OrganizedPetiteRepository(context.applicationContext)

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
    private val _dailyCueTodayMillis = MutableStateFlow<Map<Long, Long>>(emptyMap())
    val dailyCueTodayMillis: StateFlow<Map<Long, Long>> = _dailyCueTodayMillis.asStateFlow()
    private val _petiteTodayMillis = MutableStateFlow<Map<String, Long>>(emptyMap())
    val petiteTodayMillis: StateFlow<Map<String, Long>> = _petiteTodayMillis.asStateFlow()
    private val _yesterdaySuggestion = MutableStateFlow<YesterdayFlowSuggestion?>(null)
    val yesterdaySuggestion: StateFlow<YesterdayFlowSuggestion?> = _yesterdaySuggestion.asStateFlow()
    private var latestActivities: List<ActivitySession> = emptyList()
    private var isRefreshing = false
    private var lastRecommendationDateKey = 0L

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
            backfillDailyCueDefinitions()
            syncDailyCueDefinitions(_dailyCues.value)
        }
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
                _dailyCueTodayMillis.value = computeCueTodayMillis(activities)
                _petiteTodayMillis.value = computePetiteTodayMillis(activities)
                _yesterdaySuggestion.value = buildYesterdaySuggestion(_todos.value, activities)
            }
        }
        viewModelScope.launch {
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
        repository.updateBurdenCaches(burdenAnalyses.filter { it.todo.id >= 0L })
        val selectionResult = selectTodayFocus(allTodos, burdenAnalyses)
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
                repository.updateCompleted(todo, true, System.currentTimeMillis())
            }
            syncAllPendingChanges()
        }
    }

    fun completeFocusTodo(todo: TodoItem) {
        if (todo.category == TodoCategory.UNIVERSITY_EXAM) return
        viewModelScope.launch {
            if (todo.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                repository.completeReviewTodo(todo)
            } else {
                repository.updateCompleted(todo, true, System.currentTimeMillis())
            }
            // 오늘의 목표에서 완료된 항목으로 Room에 기록
            runCatching {
                dailyGoalRepository.markItemCompleted(
                    dateKey = dailyGoalRepository.todayDateKey(),
                    todoLegacyId = todo.id
                )
            }
            syncAllPendingChanges()
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
                repository.updateCompleted(todo, false, null)
            }
            syncAllPendingChanges()
        }
    }

    private suspend fun syncAllPendingChanges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            FirebaseSyncCoordinator(appContext).syncAll(uid)
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
        val selectionResult = selectTodayFocus(currentTodos, burdenAnalyses)
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
                    repository.updateBurdenCaches(burdenAnalyses.filter { it.todo.id >= 0L })
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

    fun dismissOrganizedPetite(item: OrganizedPetite) {
        _organizedPetites.value = _organizedPetites.value.filterNot { it.id == item.id }
        viewModelScope.launch {
            runCatching { organizedPetiteRepository.dismiss(item) }
            if (item.sourceType == PetiteSourceType.PETITE) {
                item.sourceId?.toLongOrNull()?.let { todoId ->
                    val todo = _todos.value.firstOrNull { it.id == todoId }
                    if (todo != null) deleteTodo(todo)
                }
            }
        }
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

    fun updateOrganizedPetiteTitle(item: OrganizedPetite, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            when (item.sourceType) {
                PetiteSourceType.PETITE, PetiteSourceType.TODO -> {
                    item.sourceId?.toLongOrNull()?.let { todoId ->
                        val todo = _todos.value.firstOrNull { it.id == todoId }
                        if (todo != null) {
                            repository.updateTodo(todo.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
                            if (todo.category == TodoCategory.TODAY && !todo.isCompleted) {
                                registerAllTodayTodoPetitesIfAbsent()
                            }
                        }
                    }
                    organizedPetiteRepository.updateTitle(item.id, newTitle)
                    syncAllPendingChanges()
                }
                else -> organizedPetiteRepository.updateTitle(item.id, newTitle)
            }
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
            else -> false
        }
        val wasHidden = false
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
            else -> {}
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
            else -> {}
        }
        if (_organizedPetites.value.none { it.sourceKey() == item.sourceKey() }) {
            _organizedPetites.value = _organizedPetites.value + item
            viewModelScope.launch {
                runCatching { organizedPetiteRepository.restore(item) }
            }
        }
    }

    private fun OrganizedPetite.sourceKey(): String = "$sourceType:$sourceId"

    fun addDailyCue(
        title: String,
        label: String,
        timerDurationMillis: Long?,
        timerCategory: String,
        recommendationTiming: DailyCueRecommendationTiming,
        note: String
    ) {
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
            recommendationTiming = recommendationTiming,
            note = note.trim(),
            order = newOrder
        ))
        _dailyCues.value = updated
        saveDailyCues(updated)
        syncDailyCueDefinitions(updated)
    }

    fun toggleDailyCue(cueId: Long) {
        val currentCue = _dailyCues.value.firstOrNull { it.id == cueId }
        val willComplete = currentCue?.isCompleted != true
        val updated = sortDailyCues(_dailyCues.value.map { cue ->
            if (cue.id == cueId) cue.copy(isCompleted = willComplete) else cue
        })
        _dailyCues.value = updated
        saveDailyCues(updated)
        syncDailyCueDefinitions(updated)
        currentCue?.takeIf { it.label == "Routine" }?.let { cue ->
            viewModelScope.launch {
                if (willComplete) {
                    recordDailyCueCheck(cue)
                } else {
                    activityRepository.deleteActivitiesBySourceToday(
                        sourceType = ActivitySourceType.DAILY_CUE_CHECK,
                        sourceId = cue.id.toString()
                    )
                }
            }
        }
    }

    fun completeDailyCue(cueId: Long) {
        val cue = _dailyCues.value.firstOrNull { it.id == cueId }
        val alreadyCompleted = cue?.isCompleted == true
        val updated = sortDailyCues(_dailyCues.value.map { c ->
            if (c.id == cueId) c.copy(isCompleted = true) else c
        })
        _dailyCues.value = updated
        saveDailyCues(updated)
        syncDailyCueDefinitions(updated)
        if (!alreadyCompleted && cue != null) {
            viewModelScope.launch { recordDailyCueCheck(cue) }
        }
    }

    private suspend fun recordDailyCueCheck(cue: DailyCueItem) {
        val sourceId = cue.id.toString()
        if (activityRepository.hasActivityBySourceToday(ActivitySourceType.DAILY_CUE_CHECK, sourceId)) return
        val now = System.currentTimeMillis()
        activityRepository.insertActivity(
            ActivitySession(
                category = cue.timerCategory.ifBlank { "TODO" },
                title = cue.title,
                startTime = now,
                endTime = now,
                durationMillis = 0L,
                sourceType = ActivitySourceType.DAILY_CUE_CHECK,
                sourceId = sourceId
            )
        )
    }

    fun updateDailyCue(
        cueId: Long,
        title: String,
        label: String,
        timerDurationMillis: Long?,
        timerCategory: String,
        recommendationTiming: DailyCueRecommendationTiming,
        note: String
    ) {
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
                    timerCategory = cleanTimerCategory,
                    recommendationTiming = recommendationTiming,
                    note = note.trim()
                )
            } else {
                cue
            }
        })
        _dailyCues.value = updated
        saveDailyCues(updated)
        syncDailyCueDefinitions(updated)
    }

    fun deleteDailyCue(cueId: Long) {
        val deletedCue = _dailyCues.value.firstOrNull { it.id == cueId }
        val updated = _dailyCues.value.filter { it.id != cueId }
        _dailyCues.value = updated
        saveDailyCues(updated)
        viewModelScope.launch {
            val archivedAt = System.currentTimeMillis()
            dailyCueRepository.archiveCue(cueId)
            deletedCue?.let { cue ->
                val fallbackCreatedAt = cuePrefs.getLong("date_key", 0L)
                    .takeIf { it > 0L }
                    ?: archivedAt
                val record = cue.toDailyCueRecord(fallbackCreatedAt)
                runCatching {
                    firestoreSyncRepository.syncDailyCue(
                        cueId = record.id,
                        label = record.label,
                        title = record.title,
                        timerDurationMillis = record.timerDurationMillis,
                        timerCategory = record.timerCategory,
                        recommendationTiming = record.recommendationTiming,
                        note = record.note,
                        sortOrder = record.order,
                        createdAt = record.createdAt,
                        updatedAt = archivedAt,
                        archivedAt = archivedAt,
                        isCompletedToday = cue.isCompleted,
                        completionDateKey = startOfDay(archivedAt)
                    )
                }
            }
        }
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
                    recommendationTiming = DailyCueRecommendationTiming.fromStorage(
                        item.optString("recommendationTiming", "")
                    ),
                    note = item.optString("note", ""),
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
                put("recommendationTiming", cue.recommendationTiming.name)
                put("note", cue.note)
                put("order", cue.order)
            })
        }
        cuePrefs.edit()
            .putLong("date_key", dateKey)
            .putString("items_json", array.toString())
            .apply()
    }

    private suspend fun backfillDailyCueDefinitions() {
        val fallbackCreatedAt = cuePrefs.getLong("date_key", 0L)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        dailyCueRepository.backfillExistingCuesIfNeeded(
            cues = _dailyCues.value.map { it.toDailyCueRecord(fallbackCreatedAt) },
            fallbackCreatedAt = fallbackCreatedAt
        )
    }

    private fun syncDailyCueDefinitions(cues: List<DailyCueItem>) {
        val fallbackCreatedAt = cuePrefs.getLong("date_key", 0L)
            .takeIf { it > 0L }
            ?: System.currentTimeMillis()
        viewModelScope.launch {
            cues.forEach { cue ->
                val record = cue.toDailyCueRecord(fallbackCreatedAt)
                dailyCueRepository.upsertCue(record)
                runCatching {
                    firestoreSyncRepository.syncDailyCue(
                        cueId = record.id,
                        label = record.label,
                        title = record.title,
                        timerDurationMillis = record.timerDurationMillis,
                        timerCategory = record.timerCategory,
                        recommendationTiming = record.recommendationTiming,
                        note = record.note,
                        sortOrder = record.order,
                        createdAt = record.createdAt,
                        updatedAt = System.currentTimeMillis(),
                        archivedAt = record.archivedAt,
                        isCompletedToday = cue.isCompleted,
                        completionDateKey = startOfDay(System.currentTimeMillis())
                    )
                }
            }
        }
    }

    private fun DailyCueItem.toDailyCueRecord(fallbackCreatedAt: Long): DailyCueRecord {
        val inferredCreatedAt = id.takeIf { it >= MIN_REASONABLE_CUE_TIMESTAMP } ?: fallbackCreatedAt
        return DailyCueRecord(
            id = id,
            label = label,
            title = title,
            timerDurationMillis = timerDurationMillis,
            timerCategory = timerCategory,
            recommendationTiming = recommendationTiming.name,
            note = note,
            order = order,
            createdAt = inferredCreatedAt,
            updatedAt = inferredCreatedAt
        )
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
        syncDailyCueDefinitions(updated)
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
        syncDailyCueDefinitions(reordered)
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
    private fun selectTodayFocus(
        allTodos: List<TodoItem>,
        burdenAnalyses: List<TodoBurdenAnalysis> = TodoBurdenCalculator.analyze(allTodos, latestActivities)
    ): List<GoalItem> {
        val todayStart = startOfDay(System.currentTimeMillis())

        // Step 1: TODAY 카테고리 제외
        val active = allTodos.filter {
            it.category != TodoCategory.UNIVERSITY_EXAM &&
            it.category != TodoCategory.TODAY &&
            (!it.isCompleted || (it.category == TodoCategory.REVIEW && it.reviewStage == 1))
        }

        val analysisById = burdenAnalyses.associateBy { it.todo.id }

        data class Candidate(
            val todo: TodoItem,
            override val priority: Int,
            val reason: String
        ) : TodayFocusCandidateLike {
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

        fun makeCandidate(todo: TodoItem, priority: Int, reason: String) = Candidate(todo, priority, reason)

        val selected = mutableListOf<TodayFocusCandidateLike>()
        val selectedIds = mutableSetOf<Long>()

        fun addOne(c: TodayFocusCandidateLike) {
            val id = (c as? Candidate)?.todo?.id ?: return
            if (id !in selectedIds) { selected.add(c); selectedIds.add(id) }
        }

        fun applyBurdenRules(candidates: List<TodayFocusCandidateLike>) {
            val available = candidates.filter { (it as? Candidate)?.todo?.id !in selectedIds }
            if (selected.size >= 2 || available.isEmpty()) return
            val toAdd = if (selected.size == 1) {
                listOfNotNull(pickCompatiblePartner(selected.first(), available))
            } else {
                pickByBurdenCombination(available, 2)
            }
            toAdd.forEach(::addOne)
        }

        // Step 2: D-0 과제 - 개수 제한 없이 전부
        active.filter {
            it.category == TodoCategory.ASSIGNMENT &&
            it.selectedDate?.let { d -> daysDiff(todayStart, startOfDay(d)) == 0L } == true
        }.forEach { addOne(makeCandidate(it, 0, RecommendationReason.ASSIGNMENT_TODAY)) }

        // Step 3: D-1 과제 - 총합 2개 이하인 선에서
        active.filter {
            it.category == TodoCategory.ASSIGNMENT &&
            it.selectedDate?.let { d -> daysDiff(todayStart, startOfDay(d)) == 1L } == true
        }.forEach { todo ->
            if (selected.size < 2) addOne(makeCandidate(todo, 1, RecommendationReason.ASSIGNMENT_D_MINUS_1))
        }

        // Step 4: D+1 복습 - 과중도 규칙
        val d1Reviews = active.mapNotNull { todo ->
            val e = ReviewRecommendationPolicy.eligibility(todo, todayStart) ?: return@mapNotNull null
            if (e.reason != RecommendationReason.REVIEW_D_PLUS_1 &&
                e.reason != RecommendationReason.REVIEW_D_PLUS_1_LATE) return@mapNotNull null
            makeCandidate(todo, e.priority, e.reason)
        }
        applyBurdenRules(d1Reviews)

        // Step 5: 오늘 날짜 NORMAL - 총합 3개 이하인 선에서
        active.filter {
            it.category == TodoCategory.NORMAL &&
            it.selectedDate?.let { d -> startOfDay(d) == todayStart } == true
        }.forEach { todo ->
            if (selected.size < 3) addOne(makeCandidate(todo, 99, RecommendationReason.EMPTY_GOAL_FILL))
        }

        // Step 6: D-7 과제 - 과중도 규칙
        val d7 = active.filter {
            it.category == TodoCategory.ASSIGNMENT &&
            it.selectedDate?.let { d -> daysDiff(todayStart, startOfDay(d)) == 7L } == true
        }.map { makeCandidate(it, 4, RecommendationReason.ASSIGNMENT_D_MINUS_7) }
        applyBurdenRules(d7)

        // Step 7: D+7 복습 - 과중도 규칙
        val d7Reviews = active.mapNotNull { todo ->
            val e = ReviewRecommendationPolicy.eligibility(todo, todayStart) ?: return@mapNotNull null
            if (e.reason != RecommendationReason.REVIEW_D_PLUS_7 &&
                e.reason != RecommendationReason.REVIEW_D_PLUS_7_LATE) return@mapNotNull null
            makeCandidate(todo, e.priority, e.reason)
        }
        applyBurdenRules(d7Reviews)

        // Step 8: 미완료 NORMAL (오늘 날짜 제외) - 과중도 규칙
        val remainingNormals = active.filter {
            it.category == TodoCategory.NORMAL &&
            it.selectedDate?.let { d -> startOfDay(d) == todayStart } != true
        }.map { makeCandidate(it, 99, RecommendationReason.EMPTY_GOAL_FILL) }
        applyBurdenRules(remainingNormals)

        return sortForTodayFocusDisplay(selected).map { it.toGoalItem() }
    }

    private fun pickCompatiblePartner(
        mandatory: TodayFocusCandidateLike,
        candidates: List<TodayFocusCandidateLike>
    ): TodayFocusCandidateLike? {
        val byBurden = candidates.groupBy { it.burdenLevel }
        val light = byBurden["LIGHT"].orEmpty().sortedWith(lightCandidateComparator())
        val medium = byBurden["MEDIUM"].orEmpty().sortedWith(mediumCandidateComparator())
        val heavy = byBurden["HEAVY"].orEmpty().sortedWith(heavyCandidateComparator())

        return when (mandatory.burdenLevel) {
            "HEAVY" -> light.firstOrNull()
            "MEDIUM" -> medium.firstOrNull() ?: light.firstOrNull()
            "LIGHT" -> heavy.firstOrNull() ?: medium.firstOrNull() ?: light.firstOrNull()
            else -> null
        }
    }

    private fun pickByBurdenCombination(candidates: List<TodayFocusCandidateLike>, count: Int): List<TodayFocusCandidateLike> {
        if (count <= 0 || candidates.isEmpty()) return emptyList()

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
            // 허용된 2개 조합이 없으면 하나만 추천한다.
            else -> listOf(
                heavy.firstOrNull()
                    ?: medium.firstOrNull()
                    ?: light.firstOrNull()
                    ?: candidates.sortedWith(fallbackCandidateComparator()).first()
            )
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

    private fun computeCueTodayMillis(activities: List<ActivitySession>): Map<Long, Long> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val todayEnd = todayStart + DAY_MILLIS
        return activities
            .filter { it.startTime in todayStart until todayEnd && it.sourceType == ActivitySourceType.DAILY_CUE_ROUTINE }
            .groupBy { it.sourceId?.toLongOrNull() }
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMillis } }
    }

    private fun computePetiteTodayMillis(activities: List<ActivitySession>): Map<String, Long> {
        val todayStart = startOfDay(System.currentTimeMillis())
        val todayEnd = todayStart + DAY_MILLIS
        return activities
            .filter { it.startTime in todayStart until todayEnd }
            .mapNotNull { activity ->
                val petiteId = activity.linkedPetiteId
                    ?: activity.sourceId?.takeIf { activity.sourceType == ActivitySourceType.MANUAL }
                petiteId?.let { it to activity.durationMillis }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, durations) -> durations.sum() }
    }

    private fun daysDiff(fromMs: Long, toMs: Long): Long =
        (toMs - fromMs) / (24L * 60 * 60 * 1000)

    private val Int.minutes: Long get() = this * 60L * 1000L
    private val Int.hours: Long get() = this * 60L * 60L * 1000L

    companion object {
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
        private const val MIN_REASONABLE_CUE_TIMESTAMP = 1_577_836_800_000L
    }
}
