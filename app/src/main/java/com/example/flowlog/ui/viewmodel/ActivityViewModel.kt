package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.notification.ActivityTimerNotifier
import com.example.flowlog.notification.ReminderScheduler
import com.example.flowlog.widget.FlowlogWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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
    val weeklyCategoryStats: List<CategoryStat> = emptyList(),
    val monthlyCategoryStats: List<CategoryStat> = emptyList(),
    val weeklyTrend: List<TrendPoint> = emptyList(),
    val monthlyTrend: List<TrendPoint> = emptyList()
)

data class ActivityUiState(
    val isRunning: Boolean = false,
    val currentCategory: String = "",
    val elapsedTime: Long = 0L,
    val todayActivities: List<ActivitySession> = emptyList(),
    val allActivities: List<ActivitySession> = emptyList(),
    val dailyReport: DailyReport = DailyReport(),
    val favoriteActivities: List<ActivitySession> = emptyList(),
    val lastTimedActivity: ActivitySession? = null,
    val analytics: AnalyticsState = AnalyticsState(),
    val startTime: Long = 0L,
    val linkedTodoId: Long? = null,
    val pendingTitle: String? = null,
    val editingActivity: ActivitySession? = null,
    val selectedCategory: String? = null,
    val statusMessage: String? = null
)

class ActivityViewModel(
    private val repository: ActivityRepository,
    private val todoRepository: TodoRepository,
    private val reminderScheduler: ReminderScheduler,
    private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val activityTimerNotifier = ActivityTimerNotifier(appContext)

    init {
        restoreActiveSession()
        observeAllActivities()
        observeTodayActivities()
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
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = category,
                elapsedTime = 0L,
                startTime = startTime,
                linkedTodoId = null,
                pendingTitle = null,
                statusMessage = null
            )
        }
        FlowlogWidgetProvider.setActiveSession(appContext, category, startTime)
        updateWidgetSafely()
        activityTimerNotifier.showRunningTimer(category, startTime)
        startTimer()
    }

    fun restartActivity(activity: ActivitySession) {
        startActivity(activity.category)
    }

    fun startTodoActivity(todoId: Long, title: String) {
        if (_uiState.value.isRunning) return

        val startTime = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "TODO",
                elapsedTime = 0L,
                startTime = startTime,
                linkedTodoId = todoId,
                pendingTitle = title,
                statusMessage = null
            )
        }
        FlowlogWidgetProvider.setActiveSession(
            context = appContext,
            category = "TODO",
            startTime = startTime,
            linkedTodoId = todoId,
            linkedTodoTitle = title
        )
        updateWidgetSafely()
        activityTimerNotifier.showRunningTimer("TODO", startTime)
        startTimer()
    }

    fun scheduleSnackReminder() {
        val scheduled = runCatching {
            reminderScheduler.scheduleSnackReminder()
        }.isSuccess
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) {
                    "간식 양치 알림을 30분 뒤로 설정했어요."
                } else {
                    "알림 설정에 실패했어요."
                }
            )
        }
    }

    fun scheduleBrushTimers() {
        val scheduled = runCatching {
            reminderScheduler.scheduleBrushTimers()
        }.isSuccess
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) {
                    "양치 타이머를 시작했어요. 3분과 30분 뒤에 알려드릴게요."
                } else {
                    "양치 타이머 설정에 실패했어요."
                }
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
        FlowlogWidgetProvider.clearActiveSession(appContext)
        activityTimerNotifier.clearRunningTimer()
        updateWidgetSafely()
        return elapsedTime
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
            note = note?.takeIf { it.isNotBlank() },
            tags = emptyList(),
            linkedTodoId = state.linkedTodoId
        )

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            state.linkedTodoId?.let { todoId ->
                todoRepository.addAccumulatedMillis(todoId, durationMillis)
            }
            runCatching {
                reminderScheduler.scheduleToothbrushReminder(activity.copy(id = newId))
            }
            updateWidgetSafely()
            clearPendingActivity()
        }
    }

    fun cancelPendingActivity() {
        clearPendingActivity()
    }

    fun deleteActivity(activity: ActivitySession) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
            updateWidgetSafely()
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
            updateWidgetSafely()
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
            updateWidgetSafely()
            _uiState.update { it.copy(editingActivity = null) }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                _uiState.update {
                    it.copy(elapsedTime = System.currentTimeMillis() - it.startTime)
                }
                delay(1_000L)
            }
        }
    }

    private fun restoreActiveSession() {
        val activeSession = FlowlogWidgetProvider.getActiveSessionDetails(appContext) ?: return
        val elapsedTime = (System.currentTimeMillis() - activeSession.startTime).coerceAtLeast(0L)

        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = activeSession.category,
                elapsedTime = elapsedTime,
                startTime = activeSession.startTime,
                linkedTodoId = activeSession.linkedTodoId,
                pendingTitle = activeSession.linkedTodoTitle,
                statusMessage = null
            )
        }
        activityTimerNotifier.showRunningTimer(activeSession.category, activeSession.startTime)
        startTimer()
    }

    private fun observeAllActivities() {
        viewModelScope.launch {
            repository.getAllActivities().collect { activities ->
                val timedActivities = activities.filter { isTimedCategory(it.category) }
                val analytics = withContext(Dispatchers.Default) {
                    buildAnalytics(timedActivities)
                }
                _uiState.update {
                    it.copy(
                        allActivities = activities,
                        favoriteActivities = timedActivities.filter { activity -> activity.isFavorite },
                        lastTimedActivity = timedActivities.maxByOrNull { activity -> activity.startTime },
                        analytics = analytics
                    )
                }
            }
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
        val weekStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -6)
        }).timeInMillis
        val monthStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -29)
        }).timeInMillis
        val weekActivities = activities.filter { it.startTime >= weekStart }
        val monthActivities = activities.filter { it.startTime >= monthStart }

        return AnalyticsState(
            weeklyCategoryStats = buildCategoryStats(weekActivities),
            monthlyCategoryStats = buildCategoryStats(monthActivities),
            weeklyTrend = buildTrend(weekActivities, weekStart, 7),
            monthlyTrend = buildTrend(monthActivities, monthStart, 30)
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
                    averageMillis = total / sessions.size.coerceAtLeast(1)
                )
            }
            .sortedByDescending { it.totalMillis }
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

    private fun dayLabel(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun clearPendingActivity() {
        activityTimerNotifier.clearRunningTimer()
        FlowlogWidgetProvider.clearActiveSession(appContext)
        _uiState.update {
            it.copy(
                isRunning = false,
                currentCategory = "",
                elapsedTime = 0L,
                startTime = 0L,
                linkedTodoId = null,
                pendingTitle = null,
                statusMessage = null
            )
        }
    }

    private fun updateWidgetSafely() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                FlowlogWidgetProvider.updateAll(appContext)
            }
        }
    }

    private fun isTimedCategory(category: String): Boolean {
        return category != "SNACK" && category != "TOOTHBRUSH"
    }

    private fun defaultTitle(category: String): String {
        return when (category) {
            "MEAL" -> "식사"
            "EXERCISE" -> "운동"
            "SLEEP" -> "수면"
            "STUDY" -> "공부"
            "REST" -> "휴식"
            "SCHOOL" -> "학교"
            else -> "활동"
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
