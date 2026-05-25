package com.example.flowlog.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone

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
    val weeklyDailyAverageStats: List<CategoryStat> = emptyList(),
    val weeklyTrend: List<TrendPoint> = emptyList()
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
    val pendingSavedActivity: ActivitySession? = null,
    val lastAddedActivity: ActivitySession? = null,
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
    private val undoPreferences = appContext.getSharedPreferences(
        PREFS_ACTIVITY_UNDO,
        Context.MODE_PRIVATE
    )
    private val widgetPreferences = appContext.getSharedPreferences(
        FlowlogWidgetProvider.PREFS_WIDGET,
        Context.MODE_PRIVATE
    )
    private var isWritingWidgetState = false
    private val widgetPreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in WIDGET_SESSION_KEYS && !isWritingWidgetState) {
            viewModelScope.launch {
                syncActiveSessionFromWidget()
            }
        }
    }

    init {
        widgetPreferences.registerOnSharedPreferenceChangeListener(widgetPreferenceListener)
        _uiState.update { it.copy(lastAddedActivity = loadLastAddedActivity()) }
        restoreActiveSession()
        seedMissingSleepRecord()
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
        writeWidgetState {
            FlowlogWidgetProvider.setActiveSession(appContext, category, startTime)
        }
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
        writeWidgetState {
            FlowlogWidgetProvider.setActiveSession(
                context = appContext,
                category = "TODO",
                startTime = startTime,
                linkedTodoId = todoId,
                linkedTodoTitle = title
            )
        }
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
        writeWidgetState {
            FlowlogWidgetProvider.clearActiveSession(appContext)
        }
        activityTimerNotifier.clearRunningTimer()
        updateWidgetSafely()
        return elapsedTime
    }

    fun stopActivityAndSave() {
        val state = _uiState.value
        if (!state.isRunning || state.currentCategory.isEmpty() || state.startTime == 0L) return

        timerJob?.cancel()
        val endTime = System.currentTimeMillis()
        val durationMillis = if (state.elapsedTime > 0L) state.elapsedTime else endTime - state.startTime
        val cleanCategory = state.currentCategory
        val activity = ActivitySession(
            category = cleanCategory,
            title = state.pendingTitle ?: defaultTitle(cleanCategory),
            startTime = state.startTime,
            endTime = endTime,
            durationMillis = durationMillis,
            tags = emptyList(),
            linkedTodoId = state.linkedTodoId
        )

        writeWidgetState {
            FlowlogWidgetProvider.clearActiveSession(appContext)
        }
        activityTimerNotifier.clearRunningTimer()

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            state.linkedTodoId?.let { todoId ->
                todoRepository.addAccumulatedMillis(todoId, durationMillis)
            }
            runCatching {
                reminderScheduler.scheduleToothbrushReminder(savedActivity)
            }
            rememberLastAddedActivity(savedActivity)
            _uiState.update {
                it.copy(
                    isRunning = false,
                    currentCategory = "",
                    elapsedTime = 0L,
                    startTime = 0L,
                    linkedTodoId = null,
                    pendingTitle = null,
                    pendingSavedActivity = savedActivity,
                    statusMessage = "활동이 저장되었습니다."
                )
            }
            updateWidgetSafely()
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
            note = note?.takeIf { it.isNotBlank() },
            tags = emptyList(),
            linkedTodoId = state.linkedTodoId
        )

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            state.linkedTodoId?.let { todoId ->
                todoRepository.addAccumulatedMillis(todoId, durationMillis)
            }
            runCatching {
                reminderScheduler.scheduleToothbrushReminder(savedActivity)
            }
            rememberLastAddedActivity(savedActivity)
            updateWidgetSafely()
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
            updateWidgetSafely()
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
            updateWidgetSafely()
        }
    }

    fun undoLastAddedActivity() {
        val lastAddedActivity = _uiState.value.lastAddedActivity ?: return

        viewModelScope.launch {
            val existingActivity = repository.getActivityById(lastAddedActivity.id)
            if (existingActivity != null) {
                repository.deleteActivity(existingActivity)
                existingActivity.linkedTodoId?.let { todoId ->
                    todoRepository.addAccumulatedMillis(todoId, -existingActivity.durationMillis)
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
                    todoRepository.addAccumulatedMillis(todoId, restoredActivity.durationMillis)
                }
                rememberLastAddedActivity(restoredActivity)
                _uiState.update {
                    it.copy(statusMessage = "삭제된 최근 활동을 다시 불러왔습니다.")
                }
            }
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

    private fun syncActiveSessionFromWidget() {
        val activeSession = FlowlogWidgetProvider.getActiveSessionDetails(appContext)
        if (activeSession == null) {
            timerJob?.cancel()
            activityTimerNotifier.clearRunningTimer()
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
            return
        }

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

    private fun seedMissingSleepRecord() {
        val migrationPreferences = appContext.getSharedPreferences(
            PREFS_MIGRATIONS,
            Context.MODE_PRIVATE
        )
        if (migrationPreferences.getBoolean(KEY_SLEEP_RECORD_2026_05_19, false)) return

        viewModelScope.launch {
            val startTime = koreaTimeMillis(2026, Calendar.MAY, 19, 21, 29, 57)
            val endTime = koreaTimeMillis(2026, Calendar.MAY, 20, 1, 18, 10)
            val alreadyExists = repository.getAllActivities().first().any { activity ->
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
                updateWidgetSafely()
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
        val weekStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -6)
        }).timeInMillis
        val weekActivities = activities.filter { it.startTime >= weekStart }

        return AnalyticsState(
            weeklyDailyAverageStats = buildDailyAverageCategoryStats(weekActivities, days = 7),
            weeklyTrend = buildTrend(weekActivities, weekStart, 7)
        )
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
        writeWidgetState {
            FlowlogWidgetProvider.clearActiveSession(appContext)
        }
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

    private inline fun writeWidgetState(action: () -> Unit) {
        isWritingWidgetState = true
        try {
            action()
        } finally {
            isWritingWidgetState = false
        }
    }

    private fun updateWidgetSafely() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                FlowlogWidgetProvider.updateAll(appContext)
            }
        }
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

    private fun defaultTitle(category: String): String {
        return when (category) {
            "MEAL" -> "\uC2DD\uC0AC"
            "EXERCISE" -> "\uC6B4\uB3D9"
            "SLEEP" -> "\uC218\uBA74"
            "STUDY" -> "\uACF5\uBD80"
            "WORK" -> "\uC5C5\uBB34"
            "DEVELOPMENT" -> "\uAC1C\uBC1C"
            "REST" -> "\uD734\uC2DD"
            "SCHOOL" -> "\uD559\uAD50"
            else -> "\uD65C\uB3D9"
        }
    }

    override fun onCleared() {
        super.onCleared()
        widgetPreferences.unregisterOnSharedPreferenceChangeListener(widgetPreferenceListener)
        timerJob?.cancel()
    }

    companion object {
        private const val PREFS_MIGRATIONS = "flowlog_migrations"
        private const val PREFS_ACTIVITY_UNDO = "activity_undo"
        private const val KEY_LAST_ADDED_ACTIVITY = "last_added_activity"
        private val undoJson = Json { ignoreUnknownKeys = true }
        private const val KEY_SLEEP_RECORD_2026_05_19 = "sleep_record_2026_05_19_212957"
        private val WIDGET_SESSION_KEYS = setOf(
            FlowlogWidgetProvider.KEY_ACTIVE_CATEGORY,
            FlowlogWidgetProvider.KEY_ACTIVE_START_TIME,
            FlowlogWidgetProvider.KEY_ACTIVE_TODO_ID,
            FlowlogWidgetProvider.KEY_ACTIVE_TODO_TITLE
        )
    }
}
