package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.ButtonRecommendationEngine
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.AutoButtonScheduleRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.data.sync.FirebaseSyncCoordinator
import com.example.flowlog.notification.ActivityTimerNotifier
import com.example.flowlog.notification.AutoButtonScheduler
import com.example.flowlog.notification.ReminderScheduler
import com.example.flowlog.widget.FlowStatusWidgetProvider
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class DailyReport(
    val sessionCount: Int = 0,
    val totalMillis: Long = 0L,
    val sleepMillis: Long = 0L,
    val mealCount: Int = 0,
    val snackCount: Int = 0,
    val topCategory: String = "NONE",
    val topCategoryMillis: Long = 0L
)

data class CategoryStat(
    val category: String,
    val totalMillis: Long,
    val count: Int,
    val averageMillis: Long
)

data class TrendPoint(
    val label: String,
    val categoryMillis: Map<String, Long>
) {
    val totalMillis: Long = categoryMillis.values.sum()
}

data class AnalyticsState(
    val todayCategoryStats: List<CategoryStat> = emptyList(),
    val yesterdayCategoryStats: List<CategoryStat> = emptyList(),
    val weeklyDailyAverageStats: List<CategoryStat> = emptyList(),
    val weeklyTrend: List<TrendPoint> = emptyList()
)

data class ActivityUiState(
    val isRunning: Boolean = false,
    val currentCategory: String = "",
    val elapsedTime: Long = 0L,
    val timerGoalMillis: Long = TimerStateStore.DEFAULT_GOAL_MILLIS,
    val todayActivities: List<ActivitySession> = emptyList(),
    val allActivities: List<ActivitySession> = emptyList(),
    val dailyReport: DailyReport = DailyReport(),
    val favoriteActivities: List<ActivitySession> = emptyList(),
    val lastTimedActivity: ActivitySession? = null,
    val analytics: AnalyticsState = AnalyticsState(),
    val startTime: Long = 0L,
    val linkedTodoId: Long? = null,
    val sourceType: String = ActivitySourceType.MANUAL,
    val sourceId: String? = null,
    val pendingTitle: String? = null,
    val pendingNote: String? = null,
    val dailyCueId: Long? = null,
    val completedDailyCueGoalIds: Set<Long> = emptySet(),
    val pendingSavedActivity: ActivitySession? = null,
    val lastAddedActivity: ActivitySession? = null,
    val editingActivity: ActivitySession? = null,
    val selectedCategory: String? = null,
    val statusMessage: String? = null,
    val isBrushTimerRunning: Boolean = false,
    val brushDoneEndsAtMillis: Long = 0L,
    val snackButtonEndsAtMillis: Long = 0L,
    val autoButtonSchedules: List<AutoButtonSchedule> = emptyList(),
    val scheduledAutoButtonBlocks: List<ScheduledAutoButtonBlock> = emptyList(),
    val recommendedTodoBlocks: List<RecommendedTodoBlock> = emptyList(),
    val incompleteTodos: List<TodoItem> = emptyList(),
    val promotedButtons: List<String> = emptyList()
)

class ActivityViewModel(
    private val repository: ActivityRepository,
    private val todoRepository: TodoRepository,
    private val reminderScheduler: ReminderScheduler,
    private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()
    data class DailyCueGoalReachedEvent(val cueId: Long, val category: String, val title: String)
    private val _dailyCueGoalReachedEvents = MutableSharedFlow<DailyCueGoalReachedEvent>(extraBufferCapacity = 8)
    val dailyCueGoalReachedEvents: SharedFlow<DailyCueGoalReachedEvent> = _dailyCueGoalReachedEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var activeSessionWatcherJob: Job? = null
    private var brushTimerJob: Job? = null
    private var snackTimerJob: Job? = null
    private val activityTimerNotifier = ActivityTimerNotifier(appContext)
    private val autoButtonScheduleRepository = AutoButtonScheduleRepository(appContext)
    private val dailyGoalRepository = DailyGoalRepository(appContext)
    private val buttonRecommendationEngine = ButtonRecommendationEngine()
    private val autoButtonScheduler = AutoButtonScheduler(appContext)
    private val undoPreferences = appContext.getSharedPreferences(
        PREFS_ACTIVITY_UNDO,
        Context.MODE_PRIVATE
    )
    private val timerPreferences = appContext.getSharedPreferences(
        PREFS_TIMER_STATE,
        Context.MODE_PRIVATE
    )
    init {
        _uiState.update { it.copy(lastAddedActivity = loadLastAddedActivity()) }
        restoreBrushTimerState()
        restoreSnackButtonTimerState()
        restoreActiveSession()
        seedMissingSleepRecord()
        observeAllActivities()
        observeTodayActivities()
        observeAutoButtonSchedules()
        observeScheduledAutoButtonBlocks()
        observeRecommendedTodoBlocks()
        observeIncompleteTodos()
        watchActiveSessionStore()
    }

    fun startActivity(category: String) {
        when (category) {
            "SNACK" -> {
                scheduleSnackReminder()
                return
            }
            "TOOTHBRUSH" -> {
                scheduleBrushTimers()
                return
            }
        }
        if (_uiState.value.isRunning) return

        val startTime = System.currentTimeMillis()
        val goalMillis = goalMillisForTimer(category, startTime)
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = category,
                elapsedTime = 0L,
                timerGoalMillis = goalMillis,
                startTime = startTime,
                linkedTodoId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = null,
                pendingNote = null,
                dailyCueId = null,
                statusMessage = null
            )
        }
        saveActiveSession(category = category, startTime = startTime, goalMillis = goalMillis)
        if (category != "SLEEP") {
            activityTimerNotifier.showRunningTimer(category, startTime)
        }
        startTimer()
    }

    fun restartActivity(activity: ActivitySession) {
        startActivity(activity.category)
    }

    fun setRunningActivityTitle(title: String) {
        if (!_uiState.value.isRunning) return
        val cleanTitle = title.trim().takeIf { cleanTitle -> cleanTitle.isNotBlank() }
        _uiState.update {
            it.copy(pendingTitle = cleanTitle)
        }
    }

    fun startTodoActivity(todoId: Long, title: String) {
        if (_uiState.value.isRunning) return

        val startTime = System.currentTimeMillis()
        val goalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "TODO",
                elapsedTime = 0L,
                timerGoalMillis = goalMillis,
                startTime = startTime,
                linkedTodoId = todoId,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = title,
                pendingNote = null,
                dailyCueId = null,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = "TODO",
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = todoId,
            linkedTodoTitle = title
        )
        activityTimerNotifier.showRunningTimer("TODO", startTime)
        startTimer()
    }

    fun startDailyCueRoutineActivity(cueId: Long, title: String, goalMillis: Long, category: String) {
        if (_uiState.value.isRunning) return
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanCategory = category.takeIf { isTimedCategory(it) } ?: "TODO"

        val startTime = System.currentTimeMillis()
        val cleanGoalMillis = goalMillis.coerceAtLeast(1L)
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = cleanCategory,
                elapsedTime = 0L,
                timerGoalMillis = cleanGoalMillis,
                startTime = startTime,
                linkedTodoId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = cleanTitle,
                pendingNote = null,
                dailyCueId = cueId,
                completedDailyCueGoalIds = it.completedDailyCueGoalIds - cueId,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = cleanCategory,
            startTime = startTime,
            goalMillis = cleanGoalMillis,
            linkedTodoTitle = cleanTitle,
            dailyCueId = cueId
        )
        activityTimerNotifier.showRunningTimer(cleanCategory, startTime)
        startTimer()
    }

    fun startRecommendedTodoActivity(block: RecommendedTodoBlock) {
        if (_uiState.value.isRunning) return

        val startTime = System.currentTimeMillis()
        val goalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "TODO",
                elapsedTime = 0L,
                timerGoalMillis = goalMillis,
                startTime = startTime,
                linkedTodoId = block.todoId,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = block.itemId,
                pendingTitle = block.title,
                pendingNote = null,
                dailyCueId = null,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = "TODO",
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = block.todoId,
            linkedTodoTitle = block.title,
            sourceType = ActivitySourceType.MANUAL,
            sourceId = block.itemId
        )
        activityTimerNotifier.showRunningTimer("TODO", startTime)
        startTimer()
        viewModelScope.launch {
            dailyGoalRepository.markPlannedItemStarted(block.itemId)
        }
    }

    fun scheduleSnackReminder() {
        val triggerAtMillis = runCatching {
            reminderScheduler.scheduleSnackReminder()
        }.getOrNull()
        val scheduled = triggerAtMillis != null
        if (triggerAtMillis != null) {
            clearBrushTimerState()
            rememberSnackButtonTimerState(triggerAtMillis)
        }
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) {
                    "간식 양치 알림을 30분 뒤로 설정했어요."
                } else {
                    "알림 설정에 실패했어요."
                },
                snackButtonEndsAtMillis = triggerAtMillis ?: it.snackButtonEndsAtMillis
            )
        }
    }

    fun scheduleBrushTimers() {
        val result = runCatching {
            reminderScheduler.scheduleBrushTimers()
        }.getOrNull()
        val scheduled = result != null
        if (result != null) {
            val (brushDoneAtMillis, eatAllowedAtMillis) = result
            rememberBrushTimerState(brushDoneAtMillis)
            rememberSnackButtonTimerState(eatAllowedAtMillis)
            _uiState.update {
                it.copy(
                    brushDoneEndsAtMillis = brushDoneAtMillis,
                    snackButtonEndsAtMillis = eatAllowedAtMillis
                )
            }
        }
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) null else "양치 타이머 설정에 실패했어요.",
                isBrushTimerRunning = scheduled || it.isBrushTimerRunning
            )
        }
    }

    fun scheduleBrushDoneExperiment() {
        val scheduled = runCatching {
            reminderScheduler.scheduleBrushDoneExperiment()
        }.isSuccess
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) {
                    "1번 실험: 양치 3분 타이머를 5초로 설정했어요."
                } else {
                    "1번 실험 설정에 실패했어요."
                }
            )
        }
    }

    fun scheduleEatAllowedExperiment() {
        val scheduled = runCatching {
            reminderScheduler.scheduleEatAllowedExperiment()
        }.isSuccess
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) {
                    "2번 실험: 양치 30분 타이머를 5초로 설정했어요."
                } else {
                    "2번 실험 설정에 실패했어요."
                }
            )
        }
    }

    fun stopActivity(): Long {
        if (!_uiState.value.isRunning) return -1L

        timerJob?.cancel()
        val elapsedTime = _uiState.value.elapsedTime
        _uiState.update { it.copy(isRunning = false) }
        TimerStateStore.pauseActiveTimer(appContext, elapsedTime)
        FlowStatusWidgetProvider.updateAll(appContext)
        activityTimerNotifier.clearRunningTimer()
        return elapsedTime
    }

    fun stopActivityAndSave(titleOverride: String? = null) {
        val state = _uiState.value
        if (!state.isRunning || state.currentCategory.isEmpty() || state.startTime == 0L) return

        timerJob?.cancel()
        val endTime = System.currentTimeMillis()
        val durationMillis = if (state.elapsedTime > 0L) state.elapsedTime else endTime - state.startTime
        val cleanCategory = state.currentCategory
        val cleanTitle = titleOverride?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: state.pendingTitle
            ?: defaultTitle(cleanCategory)
        val activity = ActivitySession(
            category = cleanCategory,
            title = cleanTitle,
            startTime = state.startTime,
            endTime = endTime,
            durationMillis = durationMillis,
            tags = emptyList(),
            linkedTodoId = state.linkedTodoId,
            sourceType = state.sourceType,
            sourceId = state.sourceId,
            note = state.pendingNote?.takeIf { it.isNotBlank() }
        )

        clearActiveSession()
        activityTimerNotifier.clearRunningTimer()

        // UI를 즉시 종료 상태로 전환 — Firebase sync 완료를 기다리지 않음
        _uiState.update {
            it.copy(
                isRunning = false,
                currentCategory = "",
                elapsedTime = 0L,
                timerGoalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS,
                startTime = 0L,
                linkedTodoId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = null,
                pendingNote = null,
                dailyCueId = null
            )
        }

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            state.linkedTodoId?.let { todoId ->
                todoRepository.addAccumulatedSeconds(todoId, durationMillis / 1000L)
                if (state.sourceId == null) {
                    dailyGoalRepository.markOpenPlannedItemCompleted(todoId, newId)
                }
            }
            dailyGoalRepository.ensureTodayTimePlan(
                activities = _uiState.value.allActivities + savedActivity,
                forceRefresh = false
            )
            val mealTimerEndsAt = runCatching {
                reminderScheduler.scheduleToothbrushReminder(savedActivity)
            }.getOrNull()
            if (mealTimerEndsAt != null) {
                rememberSnackButtonTimerState(mealTimerEndsAt)
                _uiState.update { it.copy(snackButtonEndsAtMillis = mealTimerEndsAt) }
            }
            rememberLastAddedActivity(savedActivity)
            attemptDeferredSync()
            _uiState.update {
                it.copy(
                    pendingSavedActivity = null,
                    statusMessage = "활동이 저장되었습니다."
                )
            }
        }
    }

    fun saveActivity(category: String, title: String, note: String? = null) {
        val state = _uiState.value
        if (state.currentCategory.isEmpty() || state.startTime == 0L) return

        val endTime = System.currentTimeMillis()
        val durationMillis = if (state.elapsedTime > 0L) state.elapsedTime else endTime - state.startTime
        val cleanCategory = category.ifBlank { state.currentCategory }
        val cleanTitle = title.trim().ifBlank { state.pendingTitle ?: defaultTitle(cleanCategory) }
        val activity = ActivitySession(
            category = cleanCategory,
            title = cleanTitle,
            startTime = state.startTime,
            endTime = endTime,
            durationMillis = durationMillis,
            note = note?.takeIf { it.isNotBlank() } ?: state.pendingNote?.takeIf { it.isNotBlank() },
            tags = emptyList(),
            linkedTodoId = state.linkedTodoId,
            sourceType = state.sourceType,
            sourceId = state.sourceId
        )

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            state.linkedTodoId?.let { todoId ->
                todoRepository.addAccumulatedSeconds(todoId, durationMillis / 1000L)
                if (state.sourceId != null) {
                    dailyGoalRepository.markPlannedItemCompleted(state.sourceId, todoId, newId)
                } else {
                    dailyGoalRepository.markOpenPlannedItemCompleted(todoId, newId)
                }
            }
            dailyGoalRepository.ensureTodayTimePlan(
                activities = _uiState.value.allActivities + savedActivity,
                forceRefresh = false
            )
            val mealTimerEndsAt = runCatching {
                reminderScheduler.scheduleToothbrushReminder(savedActivity)
            }.getOrNull()
            if (mealTimerEndsAt != null) {
                rememberSnackButtonTimerState(mealTimerEndsAt)
                _uiState.update { it.copy(snackButtonEndsAtMillis = mealTimerEndsAt) }
            }
            rememberLastAddedActivity(savedActivity)
            if (cleanCategory != "ETC") {
                attemptDeferredSync()
            }
            clearPendingActivity()
        }
    }

    fun cancelPendingActivity() {
        clearPendingActivity()
    }

    fun dismissPendingSavedActivity() {
        _uiState.update { it.copy(pendingSavedActivity = null) }
    }

    fun updatePendingSavedActivity(category: String, title: String, note: String?) {
        viewModelScope.launch {
            val savedActivity = _uiState.value.pendingSavedActivity ?: return@launch
            val cleanCategory = category.ifBlank { savedActivity.category }
            val updatedActivity = savedActivity.copy(
                category = cleanCategory,
                title = title.trim().ifBlank { defaultTitle(cleanCategory) },
                note = note?.takeIf { it.isNotBlank() },
                tags = emptyList(),
                modifiedTime = System.currentTimeMillis()
            )
            repository.updateActivity(updatedActivity)
            rememberLastAddedActivity(updatedActivity)
            attemptDeferredSync()
            _uiState.update {
                it.copy(
                    pendingSavedActivity = null,
                    statusMessage = "활동 내용이 업데이트되었습니다."
                )
            }
        }
    }

    fun deleteActivity(activity: ActivitySession) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
        }
    }

    fun undoLastAddedActivity() {
        val lastAddedActivity = _uiState.value.lastAddedActivity ?: return

        viewModelScope.launch {
            val existingActivity = repository.getActivityById(lastAddedActivity.id)
            if (existingActivity != null) {
                repository.deleteActivity(existingActivity)
                existingActivity.linkedTodoId?.let { todoId ->
                    todoRepository.addAccumulatedSeconds(todoId, -existingActivity.durationMillis / 1000L)
                }
                _uiState.update {
                    it.copy(
                        pendingSavedActivity = null,
                        statusMessage = "최근 추가한 활동을 삭제했습니다."
                    )
                }
            } else {
                val restoreDraft = lastAddedActivity.copy(
                    id = 0L,
                    modifiedTime = System.currentTimeMillis()
                )
                val restoredId = repository.insertActivity(restoreDraft)
                val restoredActivity = restoreDraft.copy(id = restoredId)
                restoredActivity.linkedTodoId?.let { todoId ->
                    todoRepository.addAccumulatedSeconds(todoId, restoredActivity.durationMillis / 1000L)
                }
                rememberLastAddedActivity(restoredActivity)
                _uiState.update {
                    it.copy(statusMessage = "삭제된 최근 활동을 다시 불러왔습니다.")
                }
            }
        }
    }

    fun toggleFavorite(activity: ActivitySession) {
        viewModelScope.launch {
            repository.updateActivity(
                activity.copy(
                    isFavorite = !activity.isFavorite,
                    modifiedTime = System.currentTimeMillis()
                )
            )
        }
    }

    fun filterByCategory(category: String) {
        _uiState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun clearFilter() {
        _uiState.update { it.copy(selectedCategory = null) }
    }

    fun startEditActivity(activityId: Long) {
        viewModelScope.launch {
            val activity = repository.getActivityById(activityId)
            if (activity != null) {
                _uiState.update { it.copy(editingActivity = activity) }
            }
        }
    }

    fun cancelEditActivity() {
        _uiState.update { it.copy(editingActivity = null) }
    }

    fun saveEditedActivity(category: String, title: String, note: String?) {
        viewModelScope.launch {
            val currentEditing = _uiState.value.editingActivity ?: return@launch
            val cleanCategory = category.ifBlank { currentEditing.category }
            val updatedActivity = currentEditing.copy(
                category = cleanCategory,
                title = title.trim().ifBlank { defaultTitle(cleanCategory) },
                tags = emptyList(),
                note = note?.takeIf { it.isNotBlank() },
                modifiedTime = System.currentTimeMillis()
            )
            repository.updateActivity(updatedActivity)
            _uiState.update { it.copy(editingActivity = null) }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                _uiState.update {
                    val elapsedTime = System.currentTimeMillis() - it.startTime
                    val cueId = it.dailyCueId
                    val shouldMarkCue = cueId != null &&
                        it.timerGoalMillis > 0L &&
                        elapsedTime >= it.timerGoalMillis &&
                        cueId !in it.completedDailyCueGoalIds
                    if (shouldMarkCue) {
                        _dailyCueGoalReachedEvents.tryEmit(
                            DailyCueGoalReachedEvent(cueId!!, it.currentCategory, it.pendingTitle ?: "")
                        )
                    }
                    it.copy(
                        elapsedTime = elapsedTime,
                        completedDailyCueGoalIds = if (shouldMarkCue) {
                            it.completedDailyCueGoalIds + cueId
                        } else {
                            it.completedDailyCueGoalIds
                        }
                    )
                }
                delay(1_000L)
            }
        }
    }

    private fun restoreActiveSession() {
        val activeTimer = TimerStateStore.getActiveTimer(appContext) ?: return
        if (activeTimer.status != TimerStatus.RUNNING) return
        val category = activeTimer.category
        val startTime = activeTimer.startTime
        val elapsedTime = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
        val goalMillis = activeTimer.goalMillis

        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = category,
                elapsedTime = elapsedTime,
                timerGoalMillis = goalMillis,
                startTime = startTime,
                linkedTodoId = activeTimer.linkedTodoId,
                sourceType = activeTimer.sourceType,
                sourceId = activeTimer.sourceId,
                pendingTitle = activeTimer.linkedTodoTitle,
                pendingNote = activeTimer.pendingNote,
                dailyCueId = activeTimer.dailyCueId,
                statusMessage = null
            )
        }
        if (category != "SLEEP") {
            activityTimerNotifier.showRunningTimer(category, startTime)
        }
        startTimer()
    }

    fun saveAutoButtonSchedule(schedule: AutoButtonSchedule) {
        viewModelScope.launch {
            val scheduleId = autoButtonScheduleRepository.upsertSchedule(schedule)
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    private fun watchActiveSessionStore() {
        activeSessionWatcherJob?.cancel()
        activeSessionWatcherJob = viewModelScope.launch {
            while (true) {
                syncActiveSessionFromStore()
                delay(1_000L)
            }
        }
    }

    private fun syncActiveSessionFromStore() {
        val activeTimer = TimerStateStore.getActiveTimer(appContext)
        val state = _uiState.value
        if (activeTimer == null) {
            if (state.isRunning) {
                timerJob?.cancel()
                timerJob = null
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        currentCategory = "",
                        elapsedTime = 0L,
                        timerGoalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS,
                        startTime = 0L,
                        linkedTodoId = null,
                        sourceType = ActivitySourceType.MANUAL,
                        sourceId = null,
                        pendingTitle = null,
                        pendingNote = null,
                        dailyCueId = null
                    )
                }
            }
            return
        }

        val shouldUpdate = !state.isRunning ||
            state.currentCategory != activeTimer.category ||
            state.startTime != activeTimer.startTime ||
            state.linkedTodoId != activeTimer.linkedTodoId ||
            state.sourceType != activeTimer.sourceType ||
            state.sourceId != activeTimer.sourceId

        if (shouldUpdate) {
            _uiState.update {
                it.copy(
                    isRunning = true,
                    currentCategory = activeTimer.category,
                    elapsedTime = activeTimer.elapsedMillis,
                    timerGoalMillis = activeTimer.goalMillis,
                    startTime = activeTimer.startTime,
                    linkedTodoId = activeTimer.linkedTodoId,
                    sourceType = activeTimer.sourceType,
                    sourceId = activeTimer.sourceId,
                    pendingTitle = activeTimer.linkedTodoTitle,
                    pendingNote = activeTimer.pendingNote,
                    dailyCueId = activeTimer.dailyCueId,
                    statusMessage = null
                )
            }
            if (timerJob == null || timerJob?.isActive != true) {
                startTimer()
            }
        }
    }

    fun setAutoButtonEnabled(scheduleId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            autoButtonScheduleRepository.setEnabled(scheduleId, isEnabled)
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun skipAutoButtonToday(scheduleId: String) {
        viewModelScope.launch {
            autoButtonScheduleRepository.skipToday(scheduleId)
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun unskipAutoButtonToday(scheduleId: String) {
        viewModelScope.launch {
            autoButtonScheduleRepository.unskipToday(scheduleId)
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun deleteAutoButtonSchedule(scheduleId: String) {
        viewModelScope.launch {
            autoButtonScheduler.cancel(scheduleId)
            autoButtonScheduleRepository.deleteSchedule(scheduleId)
        }
    }

    fun regenerateTodayRecommendedTimePlan() {
        viewModelScope.launch {
            val regenerated = dailyGoalRepository.ensureTodayTimePlan(
                activities = _uiState.value.allActivities,
                forceRefresh = true,
                recommendationModeOverride = DailyGoalRepository.MODE_MANUAL_REFRESH
            )
            _uiState.update {
                it.copy(
                    statusMessage = if (regenerated) {
                        "추천 시간 계획을 다시 만들었습니다."
                    } else {
                        "오늘의 목표가 아직 없어 추천 시간 계획을 만들 수 없습니다."
                    }
                )
            }
        }
    }

    fun setRecommendedTodoTime(block: RecommendedTodoBlock, startHourOfDay: Int) {
        Log.d(TAG, "user setRecommendedTodoTime: itemId=${block.itemId} todoId=${block.todoId} oldPlannedStart=${block.plannedStartMillis} newHour=$startHourOfDay")
        viewModelScope.launch {
            dailyGoalRepository.setPlannedItemTime(block.itemId, startHourOfDay)
        }
    }

    fun replaceRecommendedTodoItem(block: RecommendedTodoBlock, newTodo: TodoItem) {
        Log.d(TAG, "user replaceRecommendedTodoItem: oldItemId=${block.itemId} oldTodoId=${block.todoId} oldPlannedStart=${block.plannedStartMillis} newTodoId=${newTodo.id} newTodoTitle=${newTodo.title}")
        viewModelScope.launch {
            dailyGoalRepository.replacePlannedItemTodo(
                block = block,
                newTodo = newTodo,
                activities = _uiState.value.allActivities
            )
        }
    }

    private fun observeIncompleteTodos() {
        viewModelScope.launch {
            todoRepository.getIncompleteTodos().collect { todos ->
                _uiState.update {
                    it.copy(incompleteTodos = todos.filter { t -> t.category != com.example.flowlog.data.model.TodoCategory.UNIVERSITY_EXAM })
                }
            }
        }
    }

    fun startExamStudyActivity(todoId: Long, subjectTitle: String, dValue: Int) {
        if (_uiState.value.isRunning) return
        val dLabel = if (dValue == 0) "D-Day" else "D-$dValue"
        val title = "$subjectTitle 시험 공부 $dLabel"
        val startTime = System.currentTimeMillis()
        val goalMillis = com.example.flowlog.data.local.TimerStateStore.DEFAULT_GOAL_MILLIS
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "STUDY",
                elapsedTime = 0L,
                timerGoalMillis = goalMillis,
                startTime = startTime,
                linkedTodoId = todoId,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = title,
                pendingNote = null,
                dailyCueId = null,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = "STUDY",
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = todoId,
            linkedTodoTitle = title
        )
        activityTimerNotifier.showRunningTimer("STUDY", startTime)
        startTimer()
    }

    private fun restoreBrushTimerState() {
        val endsAtMillis = timerPreferences.getLong(KEY_BRUSH_TIMER_ENDS_AT, 0L)
        if (endsAtMillis <= System.currentTimeMillis()) {
            clearBrushTimerState()
            return
        }
        _uiState.update { it.copy(isBrushTimerRunning = true, brushDoneEndsAtMillis = endsAtMillis) }
        scheduleBrushTimerStateClear(endsAtMillis)
    }

    private fun rememberBrushTimerState(endsAtMillis: Long) {
        timerPreferences.edit()
            .putLong(KEY_BRUSH_TIMER_ENDS_AT, endsAtMillis)
            .apply()
        _uiState.update { it.copy(brushDoneEndsAtMillis = endsAtMillis) }
        scheduleBrushTimerStateClear(endsAtMillis)
    }

    private fun scheduleBrushTimerStateClear(endsAtMillis: Long) {
        brushTimerJob?.cancel()
        brushTimerJob = viewModelScope.launch {
            delay((endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            clearBrushTimerState()
        }
    }

    private fun clearBrushTimerState() {
        brushTimerJob?.cancel()
        brushTimerJob = null
        timerPreferences.edit()
            .remove(KEY_BRUSH_TIMER_ENDS_AT)
            .apply()
        _uiState.update { it.copy(isBrushTimerRunning = false, brushDoneEndsAtMillis = 0L) }
    }

    private fun restoreSnackButtonTimerState() {
        val endsAtMillis = timerPreferences.getLong(KEY_SNACK_BUTTON_TIMER_ENDS_AT, 0L)
        if (endsAtMillis <= System.currentTimeMillis()) {
            clearSnackButtonTimerState()
            return
        }
        _uiState.update { it.copy(snackButtonEndsAtMillis = endsAtMillis) }
        scheduleSnackButtonTimerStateClear(endsAtMillis)
    }

    private fun rememberSnackButtonTimerState(endsAtMillis: Long) {
        timerPreferences.edit()
            .putLong(KEY_SNACK_BUTTON_TIMER_ENDS_AT, endsAtMillis)
            .apply()
        scheduleSnackButtonTimerStateClear(endsAtMillis)
    }

    private fun scheduleSnackButtonTimerStateClear(endsAtMillis: Long) {
        snackTimerJob?.cancel()
        snackTimerJob = viewModelScope.launch {
            delay((endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            clearSnackButtonTimerState()
        }
    }

    private fun clearSnackButtonTimerState() {
        snackTimerJob?.cancel()
        snackTimerJob = null
        timerPreferences.edit()
            .remove(KEY_SNACK_BUTTON_TIMER_ENDS_AT)
            .apply()
        _uiState.update { it.copy(snackButtonEndsAtMillis = 0L) }
    }

    private fun observeAllActivities() {
        viewModelScope.launch {
            repository.getAllActivities().collect { activities ->
                val timedActivities = activities.filter { isTimedCategory(it.category) }
                val analytics = withContext(Dispatchers.Default) {
                    buildAnalytics(timedActivities)
                }
                val promotedButtons = withContext(Dispatchers.Default) {
                    buttonRecommendationEngine.computePromotedCategories(activities)
                }
                _uiState.update {
                    it.copy(
                        allActivities = activities,
                        favoriteActivities = timedActivities.filter { activity -> activity.isFavorite },
                        lastTimedActivity = timedActivities.maxByOrNull { activity -> activity.startTime },
                        analytics = analytics,
                        promotedButtons = promotedButtons
                    )
                }
            }
        }
    }

    private fun seedMissingSleepRecord() {
        val migrationPreferences = appContext.getSharedPreferences(
            PREFS_MIGRATIONS,
            Context.MODE_PRIVATE
        )
        if (migrationPreferences.getBoolean(KEY_SLEEP_RECORD_2026_05_19, false)) return

        viewModelScope.launch {
            val allActivities = repository.getAllActivities().first()

            // 신규 설치(빈 DB)면 삽입 없이 플래그만 세우고 종료.
            // 이 마이그레이션은 기존 데이터가 있는 기기에서만 적용한다.
            if (allActivities.isEmpty()) {
                migrationPreferences.edit()
                    .putBoolean(KEY_SLEEP_RECORD_2026_05_19, true)
                    .apply()
                return@launch
            }

            val startTime = koreaTimeMillis(2026, Calendar.MAY, 19, 21, 29, 57)
            val endTime = koreaTimeMillis(2026, Calendar.MAY, 20, 1, 18, 10)
            val alreadyExists = allActivities.any { activity ->
                activity.category == "SLEEP" &&
                    activity.startTime == startTime &&
                    activity.endTime == endTime
            }

            if (!alreadyExists) {
                repository.insertActivity(
                    ActivitySession(
                        category = "SLEEP",
                        title = "수면",
                        startTime = startTime,
                        endTime = endTime,
                        durationMillis = endTime - startTime,
                        modifiedTime = endTime
                    )
                )
            }

            migrationPreferences.edit()
                .putBoolean(KEY_SLEEP_RECORD_2026_05_19, true)
                .apply()
        }
    }

    private fun observeTodayActivities() {
        val today = startOfDay(Calendar.getInstance()).timeInMillis

        viewModelScope.launch {
            repository.getTodayActivities(today).collect { activities ->
                val timedActivities = activities.filter { isTimedCategory(it.category) }
                _uiState.update {
                    it.copy(
                        todayActivities = timedActivities,
                        dailyReport = buildDailyReport(timedActivities)
                    )
                }
            }
        }
    }

    private fun buildDailyReport(activities: List<ActivitySession>): DailyReport {
        val totalsByCategory = activities.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.durationMillis } }
        val topCategory = totalsByCategory.maxByOrNull { it.value }

        return DailyReport(
            sessionCount = activities.size,
            totalMillis = activities.sumOf { it.durationMillis },
            sleepMillis = totalsByCategory["SLEEP"] ?: 0L,
            mealCount = activities.count { it.category == "MEAL" },
            snackCount = 0,
            topCategory = topCategory?.key ?: "NONE",
            topCategoryMillis = topCategory?.value ?: 0L
        )
    }

    private fun buildAnalytics(activities: List<ActivitySession>): AnalyticsState {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = now
        }).timeInMillis
        val weekStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -7)
        }).timeInMillis
        val weekActivities = activities.filter { activity ->
            activity.startTime >= weekStart && activity.startTime < todayStart
        }
        val tomorrowStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }).timeInMillis
        val yesterdayStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }).timeInMillis
        val todayActivities = activities.filter { activity ->
            activity.startTime >= todayStart && activity.startTime < tomorrowStart
        }
        val yesterdayActivities = activities.filter { activity ->
            activity.startTime >= yesterdayStart && activity.startTime < todayStart
        }

        return AnalyticsState(
            todayCategoryStats = buildCategoryStats(todayActivities),
            yesterdayCategoryStats = buildCategoryStats(yesterdayActivities),
            weeklyDailyAverageStats = buildDailyAverageCategoryStats(weekActivities, days = 7),
            weeklyTrend = buildTrend(weekActivities, weekStart, 7)
        )
    }

    private fun buildCategoryStats(activities: List<ActivitySession>): List<CategoryStat> {
        return activities.groupBy { it.category }
            .map { (category, sessions) ->
                val total = sessions.sumOf { it.durationMillis }
                CategoryStat(
                    category = category,
                    totalMillis = total,
                    count = sessions.size,
                    averageMillis = total
                )
            }
            .sortedByDescending { it.totalMillis }
    }

    private fun buildDailyAverageCategoryStats(
        activities: List<ActivitySession>,
        days: Int
    ): List<CategoryStat> {
        return activities.groupBy { it.category }
            .map { (category, sessions) ->
                val total = sessions.sumOf { it.durationMillis }
                CategoryStat(
                    category = category,
                    totalMillis = total,
                    count = sessions.size,
                    averageMillis = total / days.coerceAtLeast(1)
                )
            }
            .sortedByDescending { it.averageMillis }
    }

    private fun buildTrend(
        activities: List<ActivitySession>,
        startMillis: Long,
        days: Int
    ): List<TrendPoint> {
        val dayMillis = 24L * 60L * 60L * 1000L
        return (0 until days).map { index ->
            val dayStart = startMillis + index * dayMillis
            val dayEnd = dayStart + dayMillis
            val dayActivities = activities.filter { it.startTime in dayStart until dayEnd }
            TrendPoint(
                label = dayLabel(dayStart),
                categoryMillis = dayActivities
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.durationMillis } }
            )
        }
    }

    private fun startOfDay(calendar: Calendar): Calendar {
        return calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun koreaTimeMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun dayLabel(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun clearPendingActivity() {
        activityTimerNotifier.clearRunningTimer()
        clearActiveSession()
        _uiState.update {
            it.copy(
                isRunning = false,
                currentCategory = "",
                elapsedTime = 0L,
                timerGoalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS,
                startTime = 0L,
                linkedTodoId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = null,
                pendingNote = null,
                dailyCueId = null,
                statusMessage = null
            )
        }
    }

    private suspend fun attemptDeferredSync() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseSyncCoordinator(appContext).syncEligible(uid)
    }

    private fun saveActiveSession(
        category: String,
        startTime: Long,
        goalMillis: Long,
        linkedTodoId: Long? = null,
        linkedTodoTitle: String? = null,
        pendingNote: String? = null,
        dailyCueId: Long? = null,
        sourceType: String = ActivitySourceType.MANUAL,
        sourceId: String? = null
    ) {
        TimerStateStore.saveActiveTimer(
            context = appContext,
            category = category,
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = linkedTodoId,
            linkedTodoTitle = linkedTodoTitle,
            pendingNote = pendingNote,
            dailyCueId = dailyCueId,
            sourceType = sourceType,
            sourceId = sourceId
        )
        FlowStatusWidgetProvider.updateAll(appContext)
    }

    private fun clearActiveSession() {
        TimerStateStore.clearActiveTimer(appContext)
        FlowStatusWidgetProvider.updateAll(appContext)
    }

    private fun rememberLastAddedActivity(activity: ActivitySession) {
        undoPreferences.edit()
            .putString(KEY_LAST_ADDED_ACTIVITY, undoJson.encodeToString(activity))
            .apply()
        _uiState.update { it.copy(lastAddedActivity = activity) }
    }

    private fun loadLastAddedActivity(): ActivitySession? {
        val data = undoPreferences.getString(KEY_LAST_ADDED_ACTIVITY, null) ?: return null
        return runCatching {
            undoJson.decodeFromString<ActivitySession>(data)
        }.getOrNull()
    }

    private fun isTimedCategory(category: String): Boolean {
        return category != "SNACK" && category != "TOOTHBRUSH"
    }

    private fun observeAutoButtonSchedules() {
        viewModelScope.launch {
            autoButtonScheduleRepository.observeSchedules().collect { schedules ->
                _uiState.update { it.copy(autoButtonSchedules = schedules) }
            }
        }
    }

    private fun observeScheduledAutoButtonBlocks() {
        viewModelScope.launch {
            autoButtonScheduleRepository.observeTodayBlocks().collect { blocks ->
                _uiState.update { it.copy(scheduledAutoButtonBlocks = blocks) }
            }
        }
    }

    private fun observeRecommendedTodoBlocks() {
        viewModelScope.launch {
            dailyGoalRepository.observeTodayRecommendedBlocks().collect { blocks ->
                blocks.forEach { block ->
                    Log.d(TAG, "uiBlock: itemId=${block.itemId} todoId=${block.todoId} plannedStart=${block.plannedStartMillis} plannedEnd=${block.plannedEndMillis} notifAt=${block.notificationScheduledAtMillis} userActionStatus=${block.userActionStatus} isBubbleOnly=${block.isBubbleOnly}")
                }
                _uiState.update { it.copy(recommendedTodoBlocks = blocks) }
            }
        }
    }

    private fun goalMillisForTimer(category: String, startTime: Long): Long {
        if (category != "SCHOOL" && category != "COMPANY") {
            return TimerStateStore.DEFAULT_GOAL_MILLIS
        }

        val lastWeekStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = startTime
            add(Calendar.DAY_OF_YEAR, -7)
        }).timeInMillis
        val lastWeekEnd = startOfDay(Calendar.getInstance().apply {
            timeInMillis = lastWeekStart
            add(Calendar.DAY_OF_YEAR, 1)
        }).timeInMillis
        val lastWeekSameDaySessions = _uiState.value.allActivities.filter { activity ->
            activity.category == category &&
                activity.durationMillis > 0L &&
                activity.startTime in lastWeekStart until lastWeekEnd
        }

        return if (lastWeekSameDaySessions.isEmpty()) {
            DEFAULT_SCHOOL_COMPANY_GOAL_MILLIS
        } else {
            lastWeekSameDaySessions.sumOf { it.durationMillis } / lastWeekSameDaySessions.size
        }.coerceAtLeast(1L)
    }

    private fun defaultTitle(category: String): String {
        return when (category) {
            "MEAL" -> "\uC2DD\uC0AC"
            "EXERCISE" -> "\uC6B4\uB3D9"
            "SLEEP" -> "\uC218\uBA74"
            "STUDY" -> "\uACF5\uBD80"
            "WORK" -> "\uC5C5\uBB34"
            "COMPANY" -> "\uD68C\uC0AC"
            "DEVELOPMENT" -> "\uAC1C\uBC1C"
            "READING" -> "\uB3C5\uC11C"
            "WASH" -> "\uC53B\uAE30"
            "REST" -> "\uD734\uC2DD"
            "SCHOOL" -> "\uD559\uAD50"
            else -> "\uD65C\uB3D9"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        activeSessionWatcherJob?.cancel()
        brushTimerJob?.cancel()
        snackTimerJob?.cancel()
    }

    companion object {
        private const val TAG = "ActivityViewModel"
        private const val PREFS_MIGRATIONS = "flowlog_migrations"
        private const val PREFS_ACTIVITY_UNDO = "activity_undo"
        private const val PREFS_TIMER_STATE = "timer_state"
        private const val KEY_LAST_ADDED_ACTIVITY = "last_added_activity"
        private const val KEY_BRUSH_TIMER_ENDS_AT = "brush_timer_ends_at"
        private const val KEY_SNACK_BUTTON_TIMER_ENDS_AT = "snack_button_timer_ends_at"
        private val undoJson = Json { ignoreUnknownKeys = true }
        private const val KEY_SLEEP_RECORD_2026_05_19 = "sleep_record_2026_05_19_212957"
        private val DEFAULT_SCHOOL_COMPANY_GOAL_MILLIS = TimeUnit.HOURS.toMillis(10)
    }
}
