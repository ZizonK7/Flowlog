package com.example.flowlog.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.FocusModeStore
import com.example.flowlog.data.local.DailyCueCompletionStore
import com.example.flowlog.data.local.FlowRecommendationWidgetStore
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.data.local.TimerStatus
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.ExerciseSetRecord
import com.example.flowlog.data.model.AiMessage
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.MainButtonItem
import com.example.flowlog.data.model.MainButtonSource
import com.example.flowlog.data.model.RecommendationStatus
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.data.recommendation.FlowRecommendationDecision
import com.example.flowlog.data.recommendation.FlowRecommendationEngine
import com.example.flowlog.data.recommendation.FlowRecommendationSource
import com.example.flowlog.data.recommendation.ReviewRecommendationPolicy
import com.example.flowlog.data.recommendation.TimetableProgress
import com.example.flowlog.data.constants.EntityType
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.local.InactivityReminderStore
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.repository.ActivityRepository
import com.example.flowlog.data.repository.AutoButtonScheduleRepository
import com.example.flowlog.data.repository.DailyGoalRepository
import com.example.flowlog.data.repository.DailyCueRecord
import com.example.flowlog.data.repository.DailyCueRepository
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.OrganizedPetiteRepository
import com.example.flowlog.data.repository.TodoRepository
import com.example.flowlog.data.remote.FirestoreSyncRepository
import com.example.flowlog.data.sync.FirebaseCalendarScheduleSyncDataSource
import com.example.flowlog.data.sync.FirebaseSyncCoordinator
import com.example.flowlog.notification.ActivityTimerNotifier
import com.example.flowlog.notification.AutoButtonScheduler
import com.example.flowlog.notification.FocusDndController
import com.example.flowlog.notification.FocusModeScheduler
import com.example.flowlog.notification.InactivityReminderReceiver
import com.example.flowlog.notification.InactivityReminderScheduler
import com.example.flowlog.notification.ReminderScheduler
import com.example.flowlog.notification.RoutineGoalAlarmScheduler
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import org.json.JSONObject

data class CategoryStat(
    val category: String,
    val totalMillis: Long,
    val count: Int,
    val averageMillis: Long
)

data class AnalyticsState(
    val todayCategoryStats: List<CategoryStat> = emptyList(),
    val yesterdayCategoryStats: List<CategoryStat> = emptyList()
)

data class RecommendedTodoCompletionEvent(
    val block: RecommendedTodoBlock,
    val previousTodo: TodoItem?
)

data class AiMessengerUiState(
    val messages: List<AiMessage> = emptyList(),
    val showSheet: Boolean = false,
    val hasUnread: Boolean = false
)

enum class FlowRecommendationStage {
    SINGLE
}

data class FlowActivityRecommendation(
    val petite: OrganizedPetite? = null,
    val routine: DailyCueRecord? = null,
    val timetableTodo: RecommendedTodoBlock? = null,
    val todayTodo: TodoItem? = null,
    val source: FlowRecommendationSource = FlowRecommendationSource.PETITE,
    val stage: FlowRecommendationStage,
    val title: String,
    val category: String,
    val recommendationId: String = UUID.randomUUID().toString(),
    val algorithmStep: Int = 0,
    val reasonCode: String = "LEGACY_PETITE",
    val isCompleted: Boolean = false
)

private data class FlowRecommendationInputs(
    val routines: List<DailyCueRecord>,
    val completedRoutineIds: Set<Long>,
    val timetableTodos: List<RecommendedTodoBlock>,
    val timetableProgress: TimetableProgress,
    val todayTodos: List<TodoItem>,
    val activities: List<ActivitySession>,
    val petites: List<OrganizedPetite>,
    val now: Long
)


data class ActivityUiState(
    val isRunning: Boolean = false,
    val currentCategory: String = "",
    val todayActivities: List<ActivitySession> = emptyList(),
    val allActivities: List<ActivitySession> = emptyList(),
    val analytics: AnalyticsState = AnalyticsState(),
    val startTime: Long = 0L,
    val linkedTodoId: Long? = null,
    val linkedTodoCalendarSourceId: String? = null,
    val linkedPetiteId: String? = null,
    val sourceType: String = ActivitySourceType.MANUAL,
    val sourceId: String? = null,
    val pendingTitle: String? = null,
    val pendingNote: String? = null,
    val dailyCueId: Long? = null,
    val dailyCueTargetDateKey: Long? = null,
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
    val weekSkipDatesByDateKey: Map<Long, Set<String>> = emptyMap(),
    val scheduledAutoButtonBlocks: List<ScheduledAutoButtonBlock> = emptyList(),
    val todayCalendarPetites: List<OrganizedPetiteEntity> = emptyList(),
    val recommendedTodoBlocks: List<RecommendedTodoBlock> = emptyList(),
    val incompleteTodos: List<TodoItem> = emptyList(),
    val recommendedTodoCandidates: List<TodoItem> = emptyList(),
    val isFocusModeActive: Boolean = false,
    val focusModeEndsAtMillis: Long = 0L,
    val isRoutineActive: Boolean = false,
    val routineGoalMillis: Long = 0L,
    val activeAutoButtonCategory: String? = null,
    val activeAutoButtonStartedAt: Long = 0L,
    val exerciseSets: List<ExerciseSetRecord> = emptyList(),
    val exerciseMemo: String = "",
    val mainButtonConfig: MainButtonConfig = MainButtonConfig.EMPTY,
    val showMainButtonSetup: Boolean = false,
    val showMainButtonConflict: Boolean = false,
    val pendingRemoteMainButtonConfig: MainButtonConfig? = null,
    val mainButtonSetupTarget: String? = null,
    val isMainButtonReorderMode: Boolean = false,
    val selectedMainButtonForSwapId: String? = null,
    val temporaryMainButtons: List<MainButtonItem>? = null,
    val activePetites: List<OrganizedPetite> = emptyList(),
    val flowRecommendations: List<FlowActivityRecommendation> = emptyList()
)

data class TimerDisplayState(
    val elapsedTime: Long = 0L,
    val timerGoalMillis: Long = TimerStateStore.DEFAULT_GOAL_MILLIS
)

class ActivityViewModel(
    private val repository: ActivityRepository,
    private val todoRepository: TodoRepository,
    private val reminderScheduler: ReminderScheduler,
    private val appContext: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()
    private val _timerDisplayState = MutableStateFlow(TimerDisplayState())
    val timerDisplayState: StateFlow<TimerDisplayState> = _timerDisplayState.asStateFlow()
    private val _promotedButtons = MutableStateFlow<List<String>>(emptyList())
    val promotedButtons: StateFlow<List<String>> = _promotedButtons.asStateFlow()
    private val _isNotificationSoundEnabled = MutableStateFlow(true)
    val isNotificationSoundEnabled: StateFlow<Boolean> = _isNotificationSoundEnabled.asStateFlow()
    private val _isInactivityReminderEnabled = MutableStateFlow(true)
    val isInactivityReminderEnabled: StateFlow<Boolean> = _isInactivityReminderEnabled.asStateFlow()
    data class DailyCueGoalReachedEvent(val cueId: Long, val category: String, val title: String)
    private val _dailyCueGoalReachedEvents = MutableSharedFlow<DailyCueGoalReachedEvent>(extraBufferCapacity = 8)
    val dailyCueGoalReachedEvents: SharedFlow<DailyCueGoalReachedEvent> = _dailyCueGoalReachedEvents.asSharedFlow()
    private val _recommendedTodoCompletionEvents = MutableSharedFlow<RecommendedTodoCompletionEvent>(extraBufferCapacity = 4)
    val recommendedTodoCompletionEvents: SharedFlow<RecommendedTodoCompletionEvent> = _recommendedTodoCompletionEvents.asSharedFlow()
    private val _aiMessengerUiState = MutableStateFlow(AiMessengerUiState())
    val aiMessengerUiState: StateFlow<AiMessengerUiState> = _aiMessengerUiState.asStateFlow()

    private var timerJob: Job? = null
    private var activeSessionWatcherJob: Job? = null
    private var brushTimerJob: Job? = null
    private var snackTimerJob: Job? = null
    private var focusModeJob: Job? = null
    private val activityTimerNotifier = ActivityTimerNotifier(appContext)
    private val focusModeScheduler = FocusModeScheduler(appContext)
    private val routineGoalAlarmScheduler = RoutineGoalAlarmScheduler(appContext)
    private val inactivityReminderScheduler = InactivityReminderScheduler(appContext)
    private val eventLogRepository = EventLogRepository(appContext)
    private val autoButtonScheduleRepository = AutoButtonScheduleRepository(appContext)
    private val calendarScheduleSyncDataSource = FirebaseCalendarScheduleSyncDataSource()
    private val dailyGoalRepository = DailyGoalRepository(appContext)
    private val dailyCueRepository = DailyCueRepository(appContext)
    private val flowRecommendationEngine = FlowRecommendationEngine()
    private val autoButtonScheduler = AutoButtonScheduler(appContext)
    private val organizedPetiteRepository = OrganizedPetiteRepository(appContext)
    private var lastFlowRecommendationSignature: String? = null
    private var activeFlowRecommendationId: String? = null
    private var lastLoggedFlowRecommendationId: String? = null
    private var undoInProgress = false
    private val firestoreSyncRepository = FirestoreSyncRepository()
    private val undoPreferences = appContext.getSharedPreferences(
        PREFS_ACTIVITY_UNDO,
        Context.MODE_PRIVATE
    )
    private val timerPreferences = appContext.getSharedPreferences(
        PREFS_TIMER_STATE,
        Context.MODE_PRIVATE
    )
    private val mainButtonPrefs = appContext.getSharedPreferences(
        PREFS_MAIN_BUTTON_CONFIG,
        Context.MODE_PRIVATE
    )
    private val aiMessengerPrefs = appContext.getSharedPreferences(
        PREFS_AI_MESSENGER,
        Context.MODE_PRIVATE
    )
    init {
        _uiState.update { it.copy(lastAddedActivity = loadLastAddedActivity()) }
        val initialConfig = loadMainButtonConfig()
        _uiState.update { it.copy(
            mainButtonConfig = initialConfig,
            showMainButtonSetup = !initialConfig.configured || initialConfig.version < MainButtonConfig.CURRENT_VERSION,
            isMainButtonReorderMode = false,
            selectedMainButtonForSwapId = null,
            temporaryMainButtons = null
        ) }
        restoreBrushTimerState()
        restoreSnackButtonTimerState()
        restoreFocusModeState()
        restoreInactivityReminderState()
        inactivityReminderScheduler.rescheduleFromLastActivityIfNeeded()
        restoreActiveSession()
        seedMissingSleepRecord()
        observeAllActivities()
        observeTodayActivities()
        observeAnalytics()
        observeAutoButtonSchedules()
        observeWeekSkipDates()
        observeScheduledAutoButtonBlocks()
        observeTodayCalendarAutoStartPetites()
        observeRecommendedTodoBlocks()
        observeIncompleteTodos()
        observeRecommendedTodoCandidates()
        watchActiveSessionStore()
        observeFlowRecommendation()
    }

    private fun observeFlowRecommendation() {
        viewModelScope.launch {
            combine(
                combine(
                    dailyCueRepository.observeCueRecords(),
                    DailyCueCompletionStore.observeCompletedIdsToday(appContext)
                ) { routines, completedIds -> routines to completedIds },
                combine(
                    dailyGoalRepository.observeTodayRecommendedBlocks(),
                    dailyGoalRepository.observeTodayTimetableProgress()
                ) { blocks, progress -> blocks to progress },
                combine(
                    todoRepository.getIncompleteTodos(),
                    repository.getActivitiesByDateRange(
                        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(14),
                        Long.MAX_VALUE
                    )
                ) { todos, activities -> todos to activities },
                organizedPetiteRepository.observeActivePetites(),
                flowRecommendationMinuteTicker()
            ) { routineState, timetableState, todoActivityState, petites, now ->
                FlowRecommendationInputs(
                    routines = routineState.first,
                    completedRoutineIds = routineState.second,
                    timetableTodos = timetableState.first,
                    timetableProgress = timetableState.second,
                    todayTodos = todoActivityState.first,
                    activities = todoActivityState.second,
                    petites = petites,
                    now = now
                )
            }.collect { inputs ->
                val decision = flowRecommendationEngine.recommend(
                    now = inputs.now,
                    routines = inputs.routines,
                    completedRoutineIds = inputs.completedRoutineIds,
                    timetableTodos = inputs.timetableTodos,
                    timetableProgress = inputs.timetableProgress,
                    todayTodos = inputs.todayTodos,
                    activities = inputs.activities
                )
                val recommendations = decision
                    ?.let { buildFlowRecommendation(it, inputs.petites, inputs.activities) }
                    ?.let(::listOf)
                    .orEmpty()
                _uiState.update { state ->
                    state.copy(
                        activePetites = inputs.petites,
                        flowRecommendations = recommendations
                    )
                }
                recommendations.firstOrNull()?.let { recommendation ->
                    FlowRecommendationWidgetStore.save(
                        context = appContext,
                        title = recommendation.title,
                        category = recommendation.category,
                        now = inputs.now
                    )
                } ?: FlowRecommendationWidgetStore.clear(appContext)
                FlowStatusWidgetProvider.updateAll(appContext)
                recordFlowRecommendationShown(recommendations.firstOrNull())
            }
        }
    }

    private fun flowRecommendationMinuteTicker() = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            delay(TimeUnit.MINUTES.toMillis(1) - (now % TimeUnit.MINUTES.toMillis(1)))
        }
    }

    private fun buildFlowRecommendation(
        decision: FlowRecommendationDecision,
        petites: List<OrganizedPetite>,
        activities: List<ActivitySession>
    ): FlowActivityRecommendation {
        val recommendationId = recommendationIdFor(decision)
        return when (decision.source) {
            FlowRecommendationSource.ROUTINE -> {
                val routine = requireNotNull(decision.routine)
                FlowActivityRecommendation(
                    routine = routine,
                    source = decision.source,
                    stage = FlowRecommendationStage.SINGLE,
                    title = routine.title,
                    category = routine.timerCategory.ifBlank { "TODO" },
                    recommendationId = recommendationId,
                    algorithmStep = decision.step,
                    reasonCode = decision.reasonCode
                )
            }
            FlowRecommendationSource.TIMETABLE_TODO -> {
                val block = requireNotNull(decision.timetableTodo)
                val petite = block.petiteId?.let { id -> petites.firstOrNull { it.id == id } }
                FlowActivityRecommendation(
                    petite = petite,
                    timetableTodo = block,
                    source = decision.source,
                    stage = FlowRecommendationStage.SINGLE,
                    title = block.title,
                    category = "TODO",
                    recommendationId = recommendationId,
                    algorithmStep = decision.step,
                    reasonCode = decision.reasonCode
                )
            }
            FlowRecommendationSource.TODAY_TODO -> {
                val todo = requireNotNull(decision.todayTodo)
                FlowActivityRecommendation(
                    todayTodo = todo,
                    source = decision.source,
                    stage = FlowRecommendationStage.SINGLE,
                    title = todo.title,
                    category = "TODO",
                    recommendationId = recommendationId,
                    algorithmStep = decision.step,
                    reasonCode = decision.reasonCode
                )
            }
            FlowRecommendationSource.PETITE -> error("Engine does not emit legacy petites")
        }
    }

    private fun recommendationIdFor(decision: FlowRecommendationDecision): String {
        val sourceId = decision.routine?.id?.toString()
            ?: decision.timetableTodo?.itemId
            ?: decision.todayTodo?.id?.toString()
            ?: "none"
        val signature = "${decision.source}:$sourceId:${decision.step}:${decision.reasonCode}"
        if (signature != lastFlowRecommendationSignature) {
            lastFlowRecommendationSignature = signature
            activeFlowRecommendationId = UUID.randomUUID().toString()
        }
        return activeFlowRecommendationId ?: UUID.randomUUID().toString().also {
            activeFlowRecommendationId = it
        }
    }

    private suspend fun recordFlowRecommendationShown(recommendation: FlowActivityRecommendation?) {
        if (recommendation == null || recommendation.recommendationId == lastLoggedFlowRecommendationId) return
        lastLoggedFlowRecommendationId = recommendation.recommendationId
        logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_SHOWN, recommendation)
    }

    fun openFlowRecommendation(recommendation: FlowActivityRecommendation) {
        viewModelScope.launch {
            logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_OPENED, recommendation)
        }
    }

    private suspend fun logFlowRecommendationEvent(
        eventType: String,
        recommendation: FlowActivityRecommendation
    ) {
        val sourceId = recommendation.routine?.id?.toString()
            ?: recommendation.timetableTodo?.itemId
            ?: recommendation.todayTodo?.id?.toString()
            ?: recommendation.petite?.id
        val metadata = JSONObject().apply {
            put("recommendationId", recommendation.recommendationId)
            put("algorithmVersion", FLOW_RECOMMENDATION_ALGORITHM_VERSION)
            put("algorithmStep", recommendation.algorithmStep)
            put("reasonCode", recommendation.reasonCode)
            put("sourceType", recommendation.source.name)
            put("sourceId", sourceId)
            put("title", recommendation.title)
            put("category", recommendation.category)
            recommendation.routine?.let {
                put("routineTiming", it.recommendationTiming)
                put("routineTimerDurationMillis", it.timerDurationMillis)
            }
            recommendation.timetableTodo?.let {
                put("plannedStartMillis", it.plannedStartMillis)
                put("plannedEndMillis", it.plannedEndMillis)
                put("timetableRecommendationId", it.recommendationId)
            }
        }.toString()
        runCatching {
            eventLogRepository.log(
                eventType = eventType,
                entityType = EntityType.FLOW_RECOMMENDATION,
                entityId = recommendation.recommendationId,
                metadataJson = metadata,
                algorithmVersion = FLOW_RECOMMENDATION_ALGORITHM_VERSION
            )
        }
    }


    fun completeFlowRecommendation(recommendation: FlowActivityRecommendation) {
        if (recommendation.isCompleted) return
        viewModelScope.launch {
            when (recommendation.source) {
                FlowRecommendationSource.ROUTINE -> {
                    val routine = recommendation.routine ?: return@launch
                    DailyCueCompletionStore.markCompletedToday(appContext, routine.id)
                    recordDailyCueCheck(routine)
                }
                FlowRecommendationSource.TIMETABLE_TODO -> {
                    recommendation.timetableTodo?.let { completeRecommendedTodo(it) }
                }
                FlowRecommendationSource.TODAY_TODO -> {
                    recommendation.todayTodo?.let {
                        todoRepository.updateCompleted(it.id, true, System.currentTimeMillis())
                    }
                }
                FlowRecommendationSource.PETITE -> completeLegacyFlowRecommendation(recommendation)
            }
            logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_COMPLETED, recommendation)
        }
    }

    private suspend fun completeLegacyFlowRecommendation(recommendation: FlowActivityRecommendation) {
        val petite = recommendation.petite ?: return
        organizedPetiteRepository.dismiss(petite)
        if (petite.sourceType == PetiteSourceType.CALENDAR) {
            dailyGoalRepository.markCalendarPetiteCompleted(petite.id)
        }
    }

    private suspend fun recordDailyCueCheck(
        routine: DailyCueRecord,
        targetTimestamp: Long = System.currentTimeMillis()
    ) = recordDailyCueCheck(routine.id, routine.title, routine.timerCategory, targetTimestamp)

    private suspend fun recordDailyCueCheck(
        cueId: Long,
        title: String,
        category: String,
        targetTimestamp: Long = System.currentTimeMillis()
    ) {
        val sourceId = cueId.toString()
        if (repository.hasActivityBySourceForDate(ActivitySourceType.DAILY_CUE_CHECK, sourceId, targetTimestamp)) return
        repository.insertActivity(
            ActivitySession(
                category = category.ifBlank { "TODO" },
                title = title,
                startTime = targetTimestamp,
                endTime = targetTimestamp,
                durationMillis = 0L,
                sourceType = ActivitySourceType.DAILY_CUE_CHECK,
                sourceId = sourceId
            )
        )
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
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = 0L,
            timerGoalMillis = goalMillis
        )
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = category,
                startTime = startTime,
                linkedTodoId = null,
                linkedTodoCalendarSourceId = null,
                linkedPetiteId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = null,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
                statusMessage = null,
                exerciseSets = emptyList(),
                exerciseMemo = ""
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

    fun updateExerciseSets(sets: List<ExerciseSetRecord>) {
        _uiState.update { it.copy(exerciseSets = sets) }
        val json = if (sets.isEmpty()) null else undoJson.encodeToString(sets)
        TimerStateStore.saveExerciseSetsJson(appContext, json)
    }

    fun updateExerciseMemo(memo: String) {
        _uiState.update { it.copy(exerciseMemo = memo) }
    }

    fun startOverlappingActivity(category: String, startTime: Long) {
        if (category.isBlank() || startTime <= 0L) return
        val cleanCategory = category.trim()
        TimerStateStore.savePinnedTimer(
            context = appContext,
            category = cleanCategory,
            startTime = startTime,
            goalMillis = goalMillisForTimer(cleanCategory, startTime)
        )
        FlowStatusWidgetProvider.updateAll(appContext)
    }

    fun saveOverlappingActivity(category: String, startTime: Long, endTime: Long = System.currentTimeMillis()) {
        if (category.isBlank() || startTime <= 0L || endTime <= startTime) {
            TimerStateStore.clearPinnedTimer(appContext)
            FlowStatusWidgetProvider.updateAll(appContext)
            return
        }
        val cleanCategory = category.trim()
        val pinnedTimer = TimerStateStore.getPinnedTimer(appContext)
            ?.takeIf { it.category == cleanCategory && it.startTime == startTime }
        val activity = ActivitySession(
            category = cleanCategory,
            title = defaultTitle(cleanCategory),
            startTime = startTime,
            endTime = endTime,
            durationMillis = endTime - startTime,
            tags = emptyList(),
            sourceType = pinnedTimer?.sourceType ?: ActivitySourceType.MANUAL,
            sourceId = pinnedTimer?.sourceId
        )
        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            rememberLastAddedActivity(savedActivity)
            inactivityReminderScheduler.scheduleAfterActivityEnded(endTime)
            syncAfterActivitySaveIfNeeded(savedActivity)
            TimerStateStore.clearPinnedTimer(appContext)
            FlowStatusWidgetProvider.updateAll(appContext)
        }
    }

    fun saveSleepActivity(startTime: Long, endTime: Long) {
        if (startTime <= 0L || endTime <= startTime) return
        val activity = ActivitySession(
            category = "SLEEP",
            title = "수면",
            startTime = startTime,
            endTime = endTime,
            durationMillis = endTime - startTime,
            sourceType = ActivitySourceType.MANUAL
        )
        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            rememberLastAddedActivity(savedActivity)
            inactivityReminderScheduler.scheduleAfterActivityEnded(endTime)
            syncAfterActivitySaveIfNeeded(savedActivity)
        }
    }

    fun setRunningActivityTitle(title: String) {
        if (!_uiState.value.isRunning) return
        val cleanTitle = title.trim().takeIf { it.isNotBlank() }
        val state = _uiState.value
        _uiState.update {
            it.copy(pendingTitle = cleanTitle)
        }
        saveActiveSession(
            category = state.currentCategory,
            startTime = state.startTime,
            goalMillis = _timerDisplayState.value.timerGoalMillis,
            linkedTodoId = state.linkedTodoId,
            linkedTodoCalendarSourceId = state.linkedTodoCalendarSourceId,
            linkedPetiteId = state.linkedPetiteId,
            pendingNote = state.pendingNote,
            pendingTitle = cleanTitle,
            dailyCueId = state.dailyCueId,
            sourceType = state.sourceType,
            sourceId = state.sourceId
        )
    }

    fun startTodoActivity(todoId: Long, title: String, calendarSourceId: String? = null) {
        if (_uiState.value.isRunning) return

        val startTime = System.currentTimeMillis()
        val goalMillis = defaultGoalMillisForCategory("TODO")
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = 0L,
            timerGoalMillis = goalMillis
        )
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "TODO",
                startTime = startTime,
                linkedTodoId = todoId,
                linkedTodoCalendarSourceId = calendarSourceId,
                linkedPetiteId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = title,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = "TODO",
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = todoId,
            linkedTodoCalendarSourceId = calendarSourceId,
            linkedTodoTitle = title
        )
        activityTimerNotifier.showRunningTimer("TODO", startTime)
        startTimer()
    }

    fun startDailyCueRoutineActivity(
        cueId: Long,
        title: String,
        goalMillis: Long,
        category: String,
        targetDateKey: Long? = null
    ) {
        if (_uiState.value.isRunning) return
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanCategory = category.takeIf { isTimedCategory(it) } ?: "TODO"

        val startTime = System.currentTimeMillis()
        val routineGoalMillis = if (goalMillis > 0L) goalMillis else 0L
        val mainGoalMillis = TimerStateStore.DEFAULT_GOAL_MILLIS
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = 0L,
            timerGoalMillis = mainGoalMillis
        )
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = cleanCategory,
                isRoutineActive = routineGoalMillis > 0L,
                routineGoalMillis = routineGoalMillis,
                startTime = startTime,
                linkedTodoId = null,
                linkedTodoCalendarSourceId = null,
                linkedPetiteId = null,
                sourceType = ActivitySourceType.DAILY_CUE_ROUTINE,
                sourceId = cueId.toString(),
                pendingTitle = cleanTitle,
                pendingNote = null,
                dailyCueId = cueId,
                dailyCueTargetDateKey = targetDateKey,
                completedDailyCueGoalIds = it.completedDailyCueGoalIds - cueId,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = cleanCategory,
            startTime = startTime,
            goalMillis = mainGoalMillis,
            linkedTodoTitle = cleanTitle,
            dailyCueId = cueId,
            dailyCueTargetDateKey = targetDateKey,
            sourceType = ActivitySourceType.DAILY_CUE_ROUTINE,
            sourceId = cueId.toString(),
            routineGoalMillis = routineGoalMillis
        )
        if (routineGoalMillis > 0L) {
            routineGoalAlarmScheduler.schedule(
                cueId = cueId,
                title = cleanTitle,
                category = cleanCategory,
                startedAtMillis = startTime,
                goalMillis = routineGoalMillis
            )
        } else {
            routineGoalAlarmScheduler.cancel()
        }
        activityTimerNotifier.showRunningTimer(cleanCategory, startTime)
        startTimer()
    }

    fun startCalendarPetiteActivity(item: OrganizedPetite) {
        if (_uiState.value.isRunning) return

        val category = item.activityCategory?.takeIf { isTimedCategory(it) } ?: "TODO"
        val startTime = System.currentTimeMillis()
        val goalMillis = item.estimatedMinutes
            ?.takeIf { it > 0 }
            ?.let { TimeUnit.MINUTES.toMillis(it.toLong()) }
            ?: defaultGoalMillisForCategory(category)
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = 0L,
            timerGoalMillis = goalMillis
        )
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = category,
                startTime = startTime,
                linkedTodoId = null,
                linkedTodoCalendarSourceId = null,
                linkedPetiteId = item.id,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = item.id,
                pendingTitle = item.title,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = category,
            startTime = startTime,
            goalMillis = goalMillis,
            linkedPetiteId = item.id,
            linkedTodoTitle = item.title,
            sourceType = ActivitySourceType.MANUAL,
            sourceId = item.id
        )
        activityTimerNotifier.showRunningTimer(category, startTime)
        startTimer()
    }

    fun startFlowRecommendation(
        recommendation: FlowActivityRecommendation,
        plannedItemId: String? = null,
        logEvent: Boolean = true
    ) {
        if (recommendation.isCompleted || _uiState.value.isRunning) return
        when (recommendation.source) {
            FlowRecommendationSource.ROUTINE -> {
                val routine = recommendation.routine ?: return
                startDailyCueRoutineActivity(
                    cueId = routine.id,
                    title = routine.title,
                    goalMillis = routine.timerDurationMillis ?: 0L,
                    category = routine.timerCategory
                )
                viewModelScope.launch {
                    logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_STARTED, recommendation)
                }
                return
            }
            FlowRecommendationSource.TIMETABLE_TODO -> {
                val block = recommendation.timetableTodo ?: return
                startRecommendedTodoActivity(block)
                viewModelScope.launch {
                    dailyGoalRepository.markPlannedItemStarted(block.itemId)
                    logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_STARTED, recommendation)
                }
                return
            }
            FlowRecommendationSource.TODAY_TODO -> {
                val todo = recommendation.todayTodo ?: return
                startTodoActivity(todo.id, todo.title, todo.calendarSourceId)
                viewModelScope.launch {
                    logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_STARTED, recommendation)
                }
                return
            }
            FlowRecommendationSource.PETITE -> Unit
        }
        val petite = recommendation.petite ?: return
        startCalendarPetiteActivity(petite)
        if (logEvent) viewModelScope.launch {
            logFlowRecommendationEvent(EventType.FLOW_RECOMMENDATION_STARTED, recommendation)
        }
    }

    fun startRecommendedTodoActivity(block: RecommendedTodoBlock) {
        if (_uiState.value.isRunning) return

        val startTime = System.currentTimeMillis()
        val goalMillis = defaultGoalMillisForCategory("TODO")
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = 0L,
            timerGoalMillis = goalMillis
        )
        val linkedTodoId = if (block.petiteId != null) null else block.todoId
        val linkedTodoCalendarSourceId = if (block.petiteId == null) block.calendarSourceId else null
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "TODO",
                startTime = startTime,
                linkedTodoId = linkedTodoId,
                linkedTodoCalendarSourceId = linkedTodoCalendarSourceId,
                linkedPetiteId = block.petiteId,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = block.itemId,
                pendingTitle = block.title,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
                statusMessage = null
            )
        }
        saveActiveSession(
            category = "TODO",
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = linkedTodoId,
            linkedTodoCalendarSourceId = linkedTodoCalendarSourceId,
            linkedPetiteId = block.petiteId,
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
        clearBrushTimerState()
        if (triggerAtMillis != null) {
            rememberSnackButtonTimerState(triggerAtMillis)
        }
        if (!scheduled) {
            clearSnackButtonTimerState(clearNotification = false)
        }
        _uiState.update {
            it.copy(
                statusMessage = if (scheduled) {
                    "간식 양치 알림을 30분 뒤로 설정했어요."
                } else {
                    "알림 설정에 실패했어요."
                },
                snackButtonEndsAtMillis = triggerAtMillis ?: 0L
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
                    snackButtonEndsAtMillis = eatAllowedAtMillis,
                    isBrushTimerRunning = true,
                    statusMessage = null
                )
            }
        } else {
            clearBrushTimerState()
            _uiState.update { it.copy(statusMessage = "양치 타이머 설정에 실패했어요.") }
        }
    }

    fun cancelSnackTimer() {
        reminderScheduler.cancelSnackReminder()
        reminderScheduler.cancelMealReminder()
        reminderScheduler.cancelBrushEatTimer()
        clearSnackButtonTimerState(clearNotification = true)
        _uiState.update { it.copy(statusMessage = "간식 타이머를 껐어요.") }
    }

    fun cancelBrushTimers() {
        reminderScheduler.cancelBrushTimers()
        clearBrushTimerState()
        clearSnackButtonTimerState(clearNotification = true)
        _uiState.update { it.copy(statusMessage = "양치 타이머를 껐어요.") }
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
        val elapsedTime = currentElapsedTime()
        _uiState.update { it.copy(isRunning = false) }
        TimerStateStore.pauseActiveTimer(appContext, elapsedTime)
        routineGoalAlarmScheduler.cancel()
        FlowStatusWidgetProvider.updateAll(appContext)
        activityTimerNotifier.clearRunningTimer()
        return elapsedTime
    }

    fun stopActivityAndSave(
        titleOverride: String? = null,
        noteOverride: String? = null,
        exerciseSets: List<ExerciseSetRecord> = emptyList(),
        markLinkedAsComplete: Boolean = false
    ) {
        val state = _uiState.value
        if (!state.isRunning || state.currentCategory.isEmpty() || state.startTime == 0L) return

        timerJob?.cancel()
        val endTime = System.currentTimeMillis()
        val elapsedTime = currentElapsedTime()
        val durationMillis = if (elapsedTime > 0L) elapsedTime else endTime - state.startTime
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
            linkedPetiteId = state.linkedPetiteId ?: state.linkedTodoCalendarSourceId,
            sourceType = state.sourceType,
            sourceId = state.sourceId,
            note = noteOverride?.takeIf { it.isNotBlank() }
                ?: state.pendingNote?.takeIf { it.isNotBlank() },
            exerciseSets = exerciseSets
        )

        clearActiveSession()
        activityTimerNotifier.clearRunningTimer()
        finishFocusMode(reason = "activity_finished")

        // UI를 즉시 종료 상태로 전환 — Firebase sync 완료를 기다리지 않음
        _uiState.update {
            it.copy(
                isRunning = false,
                currentCategory = "",
                startTime = 0L,
                linkedTodoId = null,
                linkedTodoCalendarSourceId = null,
                linkedPetiteId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = null,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
                isFocusModeActive = false,
                focusModeEndsAtMillis = 0L,
                exerciseSets = emptyList(),
                exerciseMemo = ""
            )
        }
        resetTimerDisplayState()

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            if (markLinkedAsComplete) {
                if (state.linkedPetiteId != null) {
                    runCatching { organizedPetiteRepository.completeById(state.linkedPetiteId) }
                    state.sourceId?.let { sourceId ->
                        runCatching { dailyGoalRepository.markPlannedItemCompleted(sourceId, 0L, newId) }
                    }
                }
                state.linkedTodoId?.let { todoId ->
                    val todo = linkedTodoForState(state, todoId)
                    if (todo != null) {
                        todoRepository.addAccumulatedSeconds(todo, durationMillis / 1000L)
                    } else if (state.linkedTodoCalendarSourceId != null) {
                        todoRepository.addAccumulatedSecondsByCalendarSourceId(
                            state.linkedTodoCalendarSourceId,
                            durationMillis / 1000L
                        )
                    } else {
                        todoRepository.addAccumulatedSeconds(todoId, durationMillis / 1000L)
                    }
                    runCatching { organizedPetiteRepository.dismissTodoPetitesBySourceId(todoId.toString()) }
                    if (state.linkedPetiteId == null) {
                        if (todo?.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                            todoRepository.completeReviewTodo(todo)
                        } else if (todo != null) {
                            todoRepository.updateCompleted(todo, true, System.currentTimeMillis())
                        } else if (state.linkedTodoCalendarSourceId != null) {
                            todoRepository.updateCompletedByCalendarSourceId(
                                state.linkedTodoCalendarSourceId,
                                true
                            )
                        } else {
                            todoRepository.updateCompleted(todoId, true, System.currentTimeMillis())
                        }
                        val sourceId = state.sourceId
                        if (sourceId != null) {
                            runCatching { dailyGoalRepository.markPlannedItemCompleted(sourceId, todoId, newId) }
                        } else if (todoId != 0L) {
                            runCatching { dailyGoalRepository.markItemCompleted(dailyGoalRepository.todayDateKey(), todoId) }
                        }
                    }
                }
                state.dailyCueId?.let { cueId ->
                    val targetDay = state.dailyCueTargetDateKey ?: startOfDayMillis(state.startTime)
                    val targetTimestamp: Long
                    if (targetDay == startOfDayMillis(System.currentTimeMillis())) {
                        targetTimestamp = System.currentTimeMillis()
                        DailyCueCompletionStore.markCompletedToday(appContext, cueId)
                    } else {
                        targetTimestamp = endOfDayMillis(targetDay)
                        DailyCueCompletionStore.markCompletedForDate(appContext, cueId, targetDay)
                    }
                    recordDailyCueCheck(
                        cueId = cueId,
                        title = state.pendingTitle ?: defaultTitle(state.currentCategory),
                        category = state.currentCategory,
                        targetTimestamp = targetTimestamp
                    )
                }
            } else {
                state.linkedTodoId?.let { todoId ->
                    val todo = linkedTodoForState(state, todoId)
                    if (todo != null) {
                        todoRepository.addAccumulatedSeconds(todo, durationMillis / 1000L)
                    } else if (state.linkedTodoCalendarSourceId != null) {
                        todoRepository.addAccumulatedSecondsByCalendarSourceId(
                            state.linkedTodoCalendarSourceId,
                            durationMillis / 1000L
                        )
                    } else {
                        todoRepository.addAccumulatedSeconds(todoId, durationMillis / 1000L)
                    }
                    state.sourceId?.let { itemId ->
                        dailyGoalRepository.revertStartedItem(itemId)
                    }
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
            inactivityReminderScheduler.scheduleAfterActivityEnded(endTime)
            syncAfterActivitySaveIfNeeded(savedActivity)
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
        val elapsedTime = currentElapsedTime()
        val durationMillis = if (elapsedTime > 0L) elapsedTime else endTime - state.startTime
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
            linkedPetiteId = state.linkedPetiteId ?: state.linkedTodoCalendarSourceId,
            sourceType = state.sourceType,
            sourceId = state.sourceId
        )

        viewModelScope.launch {
            val newId = repository.insertActivity(activity)
            val savedActivity = activity.copy(id = newId)
            if (state.linkedPetiteId != null) {
                if (state.sourceId != null) {
                    dailyGoalRepository.markPlannedItemCompleted(state.sourceId, 0L, newId)
                }
            } else {
                state.linkedTodoId?.let { todoId ->
                    val todo = linkedTodoForState(state, todoId)
                    if (todo != null) {
                        todoRepository.addAccumulatedSeconds(todo, durationMillis / 1000L)
                    } else if (state.linkedTodoCalendarSourceId != null) {
                        todoRepository.addAccumulatedSecondsByCalendarSourceId(
                            state.linkedTodoCalendarSourceId,
                            durationMillis / 1000L
                        )
                    } else {
                        todoRepository.addAccumulatedSeconds(todoId, durationMillis / 1000L)
                    }
                    if (state.sourceId != null) {
                        dailyGoalRepository.markPlannedItemCompleted(state.sourceId, todoId, newId)
                    } else if (todoId != 0L) {
                        dailyGoalRepository.markOpenPlannedItemCompleted(todoId, newId)
                    }
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
            inactivityReminderScheduler.scheduleAfterActivityEnded(endTime)
            syncAfterActivitySaveIfNeeded(savedActivity)
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
            addAccumulatedSecondsForActivityTodo(activity, -activity.durationMillis / 1000L)
            rememberLastAddedActivity(activity)
        }
    }

    fun undoLastAddedActivity() {
        if (undoInProgress) return
        val lastAddedActivity = _uiState.value.lastAddedActivity ?: return
        undoInProgress = true
        viewModelScope.launch {
            try {
                val existingActivity = repository.getActivityById(lastAddedActivity.id)
                if (existingActivity != null) {
                    repository.deleteActivity(existingActivity)
                    addAccumulatedSecondsForActivityTodo(existingActivity, -existingActivity.durationMillis / 1000L)
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
                    addAccumulatedSecondsForActivityTodo(restoredActivity, restoredActivity.durationMillis / 1000L)
                    rememberLastAddedActivity(restoredActivity)
                    _uiState.update {
                        it.copy(statusMessage = "삭제된 최근 활동을 다시 불러왔습니다.")
                    }
                }
            } finally {
                undoInProgress = false
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

    fun saveEditedActivity(
        category: String,
        title: String,
        note: String?,
        exerciseSets: List<ExerciseSetRecord> = emptyList()
    ) {
        viewModelScope.launch {
            val currentEditing = _uiState.value.editingActivity ?: return@launch
            val cleanCategory = category.ifBlank { currentEditing.category }
            val updatedActivity = currentEditing.copy(
                category = cleanCategory,
                title = title.trim().ifBlank { defaultTitle(cleanCategory) },
                tags = emptyList(),
                note = note?.takeIf { it.isNotBlank() },
                exerciseSets = if (cleanCategory == "EXERCISE") exerciseSets else emptyList(),
                modifiedTime = System.currentTimeMillis()
            )
            repository.updateActivity(updatedActivity)
            syncAfterActivitySaveIfNeeded(updatedActivity)
            _uiState.update { it.copy(editingActivity = null) }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRunning) {
                val state = _uiState.value
                val elapsedTime = System.currentTimeMillis() - state.startTime
                val currentGoalMillis = _timerDisplayState.value.timerGoalMillis
                _timerDisplayState.value = TimerDisplayState(
                    elapsedTime = elapsedTime,
                    timerGoalMillis = currentGoalMillis
                )

                val cueId = state.dailyCueId
                val goalToCheck = state.routineGoalMillis.takeIf {
                    state.isRoutineActive && it > 0L
                } ?: 0L
                val shouldMarkCue = cueId != null &&
                    goalToCheck > 0L &&
                    elapsedTime >= goalToCheck &&
                    cueId !in state.completedDailyCueGoalIds
                if (shouldMarkCue) {
                    _dailyCueGoalReachedEvents.tryEmit(
                        DailyCueGoalReachedEvent(cueId!!, state.currentCategory, state.pendingTitle ?: "")
                    )
                    _uiState.update {
                        if (cueId in it.completedDailyCueGoalIds) {
                            it
                        } else {
                            it.copy(
                                completedDailyCueGoalIds = it.completedDailyCueGoalIds + cueId,
                                isRoutineActive = false
                            )
                        }
                    }
                }
                delay(1_000L)
            }
        }
    }

    private fun restoreActiveSession() {
        syncPinnedAutoButtonFromStore()
        val activeTimer = TimerStateStore.getActiveTimer(appContext) ?: return
        if (activeTimer.status != TimerStatus.RUNNING) return

        if (activeTimer.sourceType == ActivitySourceType.AUTO_BUTTON) {
            _uiState.update {
                it.copy(
                    activeAutoButtonCategory = activeTimer.category,
                    activeAutoButtonStartedAt = activeTimer.startTime
                )
            }
            return
        }

        val category = activeTimer.category
        val startTime = activeTimer.startTime
        val elapsedTime = (System.currentTimeMillis() - startTime).coerceAtLeast(0L)
        val goalMillis = activeTimer.goalMillis
        val routineGoalMillis = activeTimer.routineGoalMillis
        val isRoutineActive = routineGoalMillis > 0L && elapsedTime < routineGoalMillis
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = elapsedTime,
            timerGoalMillis = goalMillis
        )

        val restoredExerciseSets = if (category == "EXERCISE") {
            TimerStateStore.loadExerciseSetsJson(appContext)?.let { json ->
                runCatching { undoJson.decodeFromString<List<ExerciseSetRecord>>(json) }.getOrDefault(emptyList())
            } ?: emptyList()
        } else emptyList()

        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = category,
                isRoutineActive = isRoutineActive,
                routineGoalMillis = routineGoalMillis,
                startTime = startTime,
                linkedTodoId = activeTimer.linkedTodoId,
                linkedTodoCalendarSourceId = activeTimer.linkedTodoCalendarSourceId,
                linkedPetiteId = activeTimer.linkedPetiteId,
                sourceType = activeTimer.sourceType,
                sourceId = activeTimer.sourceId,
                pendingTitle = activeTimer.pendingTitle ?: activeTimer.linkedTodoTitle,
                pendingNote = activeTimer.pendingNote,
                dailyCueId = activeTimer.dailyCueId,
                dailyCueTargetDateKey = activeTimer.dailyCueTargetDateKey,
                statusMessage = null,
                exerciseSets = restoredExerciseSets
            )
        }
        if (category != "SLEEP") {
            activityTimerNotifier.showRunningTimer(category, startTime)
        }
        startTimer()
    }

    fun saveAutoButtonSchedule(schedule: AutoButtonSchedule) {
        viewModelScope.launch {
            val normalizedSchedule = schedule.normalizedForCalendarSource()
            val scheduleId = autoButtonScheduleRepository.upsertSchedule(normalizedSchedule)
            autoButtonScheduler.reschedule(scheduleId)
            syncCalendarAutoButtonSchedule(normalizedSchedule.copy(scheduleId = scheduleId))
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
        val hasPinnedAutoButton = syncPinnedAutoButtonFromStore()

        if (activeTimer == null) {
            if (state.activeAutoButtonCategory != null && !hasPinnedAutoButton) {
                _uiState.update { it.copy(activeAutoButtonCategory = null, activeAutoButtonStartedAt = 0L) }
            }
            if (state.isRunning) {
                timerJob?.cancel()
                timerJob = null
                resetTimerDisplayState()
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        currentCategory = "",
                        startTime = 0L,
                        linkedTodoId = null,
                        linkedTodoCalendarSourceId = null,
                        linkedPetiteId = null,
                        sourceType = ActivitySourceType.MANUAL,
                        sourceId = null,
                        pendingTitle = null,
                        pendingNote = null,
                        dailyCueId = null,
                        dailyCueTargetDateKey = null
                    )
                }
            }
            return
        }

        if (activeTimer.sourceType == ActivitySourceType.AUTO_BUTTON) {
            val isPinnedCategory = activeTimer.category == "SCHOOL" || activeTimer.category == "COMPANY"
            val changed = state.activeAutoButtonCategory != activeTimer.category ||
                state.activeAutoButtonStartedAt != activeTimer.startTime
            if (changed) {
                _uiState.update {
                    it.copy(
                        activeAutoButtonCategory = activeTimer.category,
                        activeAutoButtonStartedAt = activeTimer.startTime
                    )
                }
            }
            if (isPinnedCategory) return
            // 비 SCHOOL/COMPANY AUTO_BUTTON은 도넛 타이머에도 표시
        }

        if (state.activeAutoButtonCategory != null && !hasPinnedAutoButton) {
            _uiState.update { it.copy(activeAutoButtonCategory = null, activeAutoButtonStartedAt = 0L) }
        }

        val shouldUpdate = !state.isRunning ||
            state.currentCategory != activeTimer.category ||
            state.startTime != activeTimer.startTime ||
            state.linkedTodoId != activeTimer.linkedTodoId ||
            state.linkedTodoCalendarSourceId != activeTimer.linkedTodoCalendarSourceId ||
            state.linkedPetiteId != activeTimer.linkedPetiteId ||
            state.sourceType != activeTimer.sourceType ||
            state.sourceId != activeTimer.sourceId

        if (shouldUpdate) {
            _timerDisplayState.value = TimerDisplayState(
                elapsedTime = activeTimer.elapsedMillis,
                timerGoalMillis = activeTimer.goalMillis
            )
            _uiState.update {
                it.copy(
                    isRunning = true,
                    currentCategory = activeTimer.category,
                    startTime = activeTimer.startTime,
                    linkedTodoId = activeTimer.linkedTodoId,
                    linkedTodoCalendarSourceId = activeTimer.linkedTodoCalendarSourceId,
                    linkedPetiteId = activeTimer.linkedPetiteId,
                    sourceType = activeTimer.sourceType,
                    sourceId = activeTimer.sourceId,
                    pendingTitle = activeTimer.pendingTitle ?: activeTimer.linkedTodoTitle,
                    pendingNote = activeTimer.pendingNote,
                    dailyCueId = activeTimer.dailyCueId,
                    dailyCueTargetDateKey = activeTimer.dailyCueTargetDateKey,
                    isRoutineActive = activeTimer.routineGoalMillis > 0L &&
                        activeTimer.elapsedMillis < activeTimer.routineGoalMillis,
                    routineGoalMillis = activeTimer.routineGoalMillis,
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
            autoButtonScheduleRepository.getSchedule(scheduleId)?.let { schedule ->
                syncCalendarAutoButtonSchedule(schedule.copy(isEnabled = isEnabled))
            }
        }
    }

    fun skipAutoButtonToday(scheduleId: String) {
        viewModelScope.launch {
            autoButtonScheduleRepository.skipToday(scheduleId)
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun skipAutoButtonNextDay(scheduleId: String, dayOfWeek: Int) {
        viewModelScope.launch {
            autoButtonScheduleRepository.skipToday(scheduleId, nextDateKeyForDay(dayOfWeek))
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun unskipAutoButtonToday(scheduleId: String) {
        viewModelScope.launch {
            autoButtonScheduleRepository.unskipToday(scheduleId)
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun unskipAutoButtonNextDay(scheduleId: String, dayOfWeek: Int) {
        viewModelScope.launch {
            autoButtonScheduleRepository.unskipToday(scheduleId, nextDateKeyForDay(dayOfWeek))
            autoButtonScheduler.reschedule(scheduleId)
        }
    }

    fun deleteAutoButtonSchedule(scheduleId: String) {
        viewModelScope.launch {
            val schedule = autoButtonScheduleRepository.getSchedule(scheduleId)
            autoButtonScheduler.cancel(scheduleId)
            autoButtonScheduleRepository.deleteSchedule(scheduleId)
            schedule?.let { disableCalendarAutoStart(it) }
        }
    }

    private suspend fun syncCalendarAutoButtonSchedule(schedule: AutoButtonSchedule) {
        if (schedule.source != AutoButtonScheduleRepository.SOURCE_CALENDAR) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            if (schedule.isEnabled) {
                calendarScheduleSyncDataSource.upsertAutoStart(userId, schedule)
            } else {
                calendarScheduleSyncDataSource.disableAutoStart(userId, schedule)
            }
        }.onFailure { e ->
            Log.w(TAG, "Calendar auto-start sync failed: ${schedule.scheduleId} ${e.message}", e)
        }
    }

    private suspend fun disableCalendarAutoStart(schedule: AutoButtonSchedule) {
        if (schedule.source != AutoButtonScheduleRepository.SOURCE_CALENDAR) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            calendarScheduleSyncDataSource.disableAutoStart(userId, schedule)
        }.onFailure { e ->
            Log.w(TAG, "Calendar auto-start disable failed: ${schedule.scheduleId} ${e.message}", e)
        }
    }

    private fun AutoButtonSchedule.normalizedForCalendarSource(): AutoButtonSchedule {
        if (source != AutoButtonScheduleRepository.SOURCE_CALENDAR) return this
        val dateKeys = sourceDateKeys.ifEmpty { setOfNotNull(sourceDateKey) }
        if (dateKeys.isEmpty()) return this
        val repeatDays = dateKeys.map {
            Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_WEEK)
        }.toSet()
        return copy(repeatDays = repeatDays)
    }

    private fun nextDateKeyForDay(dayOfWeek: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val currentDay = get(Calendar.DAY_OF_WEEK)
            val daysUntil = (dayOfWeek - currentDay + 7) % 7
            add(Calendar.DAY_OF_YEAR, if (daysUntil == 0) 7 else daysUntil)
        }.timeInMillis
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

    fun completeRecommendedTodo(block: RecommendedTodoBlock) {
        Log.d(TAG, "user completeRecommendedTodo: itemId=${block.itemId} todoId=${block.todoId} title=${block.title}")
        viewModelScope.launch {
            if (block.petiteId != null) {
                dailyGoalRepository.markPlannedItemCompleted(
                    itemId = block.itemId,
                    todoLegacyId = 0L,
                    activityLegacyId = null
                )
            } else {
                val todo = when {
                    block.calendarSourceId != null ->
                        _uiState.value.incompleteTodos.firstOrNull { it.calendarSourceId == block.calendarSourceId }
                    else ->
                        _uiState.value.incompleteTodos.firstOrNull { it.id == block.todoId }
                }
                if (todo?.category == TodoCategory.REVIEW && todo.reviewStage < 2) {
                    todoRepository.completeReviewTodo(todo)
                } else if (todo != null) {
                    todoRepository.updateCompleted(todo, true, System.currentTimeMillis())
                } else {
                    todoRepository.updateCompleted(block.todoId, true, System.currentTimeMillis())
                }
                dailyGoalRepository.markPlannedItemCompleted(
                    itemId = block.itemId,
                    todoLegacyId = block.todoId,
                    activityLegacyId = null
                )
            }
            val todo = if (block.petiteId != null) null
            else _uiState.value.incompleteTodos.firstOrNull { it.id == block.todoId }
            _recommendedTodoCompletionEvents.emit(
                RecommendedTodoCompletionEvent(
                    block = block,
                    previousTodo = todo
                )
            )
        }
    }

    fun undoRecommendedTodoCompletion(event: RecommendedTodoCompletionEvent) {
        Log.d(TAG, "user undoRecommendedTodoCompletion: itemId=${event.block.itemId} todoId=${event.block.todoId}")
        viewModelScope.launch {
            if (event.block.petiteId == null) {
                val previousTodo = event.previousTodo
                if (previousTodo != null) {
                    todoRepository.updateTodo(previousTodo.copy(updatedAt = System.currentTimeMillis()))
                } else {
                    todoRepository.updateCompleted(event.block.todoId, false, null)
                }
            }
            dailyGoalRepository.revertPlannedItemCompleted(
                itemId = event.block.itemId,
                restoredStatus = event.block.userActionStatus
            )
        }
    }

    private fun observeIncompleteTodos() {
        viewModelScope.launch {
            todoRepository.getIncompleteTodos().collect { todos ->
                _uiState.update {
                    it.copy(incompleteTodos = todos.filter { t -> t.category != TodoCategory.UNIVERSITY_EXAM })
                }
            }
        }
    }

    private fun observeRecommendedTodoCandidates() {
        viewModelScope.launch {
            todoRepository.getAllTodos().collect { todos ->
                _uiState.update {
                    it.copy(recommendedTodoCandidates = todos.filter(::isRecommendedTodoCandidate))
                }
            }
        }
    }

    private fun isRecommendedTodoCandidate(todo: TodoItem): Boolean {
        if (todo.category == TodoCategory.TODAY || todo.category == TodoCategory.UNIVERSITY_EXAM) return false
        if (todo.isCompleted && !(todo.category == TodoCategory.REVIEW && todo.reviewStage == 1)) return false

        val todayStart = startOfDayMillis(System.currentTimeMillis())
        return when (todo.category) {
            TodoCategory.NORMAL -> true
            TodoCategory.REVIEW -> ReviewRecommendationPolicy.eligibility(todo, todayStart) != null
            TodoCategory.ASSIGNMENT -> {
                val dueDay = todo.selectedDate?.let(::startOfDayMillis) ?: return false
                daysDiff(todayStart, dueDay) in setOf(0L, 1L, 7L)
            }
            TodoCategory.TODAY,
            TodoCategory.UNIVERSITY_EXAM -> false
        }
    }

    private fun startOfDayMillis(millis: Long): Long {
        return startOfDay(Calendar.getInstance().apply { timeInMillis = millis }).timeInMillis
    }

    // dayStartMillis가 속한 날짜의 23:59:59.999를 Calendar 기준으로 계산한다.
    // dayStartMillis + DAY_MILLIS - 1L은 DST 전환일(23시간/25시간)에 잘못된 날짜로 넘어갈 수 있어 사용하지 않는다.
    private fun endOfDayMillis(dayStartMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = dayStartMillis
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
    }

    private fun daysDiff(fromMs: Long, toMs: Long): Long = (toMs - fromMs) / DAY_MILLIS

    fun startExamStudyActivity(todoId: Long, subjectTitle: String, dValue: Int) {
        if (_uiState.value.isRunning) return
        val dLabel = if (dValue == 0) "D-Day" else "D-$dValue"
        val title = "$subjectTitle 시험 공부 $dLabel"
        val startTime = System.currentTimeMillis()
        val goalMillis = defaultGoalMillisForCategory("STUDY")
        _timerDisplayState.value = TimerDisplayState(
            elapsedTime = 0L,
            timerGoalMillis = goalMillis
        )
        _uiState.update {
            it.copy(
                isRunning = true,
                currentCategory = "STUDY",
                startTime = startTime,
                linkedTodoId = todoId,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = title,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
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

    fun refreshTimerStates() {
        val now = System.currentTimeMillis()
        val state = _uiState.value
        syncActiveSessionFromStore()
        if (state.brushDoneEndsAtMillis > 0 && state.brushDoneEndsAtMillis <= now) {
            clearBrushTimerState()
        }
        if (state.snackButtonEndsAtMillis > 0 && state.snackButtonEndsAtMillis <= now) {
            clearSnackButtonTimerState(clearNotification = false)
        }
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
            clearSnackButtonTimerState(clearNotification = true)
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

    private fun clearSnackButtonTimerState(clearNotification: Boolean = false) {
        snackTimerJob?.cancel()
        snackTimerJob = null
        timerPreferences.edit()
            .remove(KEY_SNACK_BUTTON_TIMER_ENDS_AT)
            .apply()
        if (clearNotification) {
            activityTimerNotifier.clearSnackTimer()
            activityTimerNotifier.clearBrushEatTimer()
        }
        _uiState.update { it.copy(snackButtonEndsAtMillis = 0L) }
    }

    // region MainButtonConfig

    private data class MainButtonSyncMeta(
        val lastUid: String?,
        val dirty: Boolean,
        val lastEditAt: Long
    )

    private fun readSyncMeta() = MainButtonSyncMeta(
        lastUid = mainButtonPrefs.getString(KEY_LAST_MAIN_BUTTON_CONFIG_UID, null),
        dirty = mainButtonPrefs.getBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false),
        lastEditAt = mainButtonPrefs.getLong(KEY_LAST_LOCAL_MAIN_BUTTON_EDIT_AT, 0L)
    )

    private fun loadMainButtonConfig(): MainButtonConfig {
        val json = mainButtonPrefs.getString(KEY_MAIN_BUTTON_CONFIG, null)
            ?: return MainButtonConfig.EMPTY
        return try {
            undoJson.decodeFromString(json)
        } catch (_: Exception) {
            MainButtonConfig.EMPTY
        }
    }

    private fun persistMainButtonConfig(config: MainButtonConfig) {
        mainButtonPrefs.edit()
            .putString(KEY_MAIN_BUTTON_CONFIG, undoJson.encodeToString(config))
            .apply()
        _uiState.update { it.copy(mainButtonConfig = config) }
    }

    private fun persistAndSyncMainButtonConfig(config: MainButtonConfig) {
        persistMainButtonConfig(config)
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        mainButtonPrefs.edit().apply {
            putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, true)
            putLong(KEY_LAST_LOCAL_MAIN_BUTTON_EDIT_AT, System.currentTimeMillis())
            if (currentUid != null) putString(KEY_LAST_MAIN_BUTTON_CONFIG_UID, currentUid)
        }.apply()
        if (currentUid == null) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { firestoreSyncRepository.uploadMainButtonConfig(config) }
                .onSuccess {
                    mainButtonPrefs.edit()
                        .putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false)
                        .apply()
                }
        }
    }

    fun openMainButtonReplacePicker(category: String) {
        _uiState.update { it.copy(mainButtonSetupTarget = category) }
    }

    fun dismissMainButtonReplacePicker() {
        _uiState.update { it.copy(mainButtonSetupTarget = null) }
    }

    fun completeMainButtonSetup(selectedCategories: List<String>) {
        val newConfig = _uiState.value.mainButtonConfig.copy(
            buttons = selectedCategories.mapIndexed { i, cat ->
                MainButtonItem(category = cat, order = i, source = MainButtonSource.USER_ADDED)
            },
            configured = true,
            version = MainButtonConfig.CURRENT_VERSION
        )
        _uiState.update { it.copy(showMainButtonSetup = false) }
        persistAndSyncMainButtonConfig(newConfig)
    }

    fun handleLoginMainButtonSync() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val localConfig = _uiState.value.mainButtonConfig
                val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@runCatching
                val remoteConfig = firestoreSyncRepository.fetchMainButtonConfig()
                when {
                    // local 미설정 + remote 설정됨 → setup 화면에서 계정 설정 불러오기 흐름 유지
                    !localConfig.configured && remoteConfig != null && remoteConfig.configured -> {
                        _uiState.update {
                            it.copy(
                                pendingRemoteMainButtonConfig = remoteConfig.copy(version = MainButtonConfig.CURRENT_VERSION)
                            )
                        }
                    }
                    // local 설정됨 + remote 없음 → local 업로드
                    localConfig.configured && remoteConfig == null -> {
                        firestoreSyncRepository.uploadMainButtonConfig(localConfig)
                        mainButtonPrefs.edit().putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false).apply()
                    }
                    // 완전히 동일 → no-op
                    localConfig.configured && remoteConfig != null && configsAreEquivalent(localConfig, remoteConfig) -> Unit
                    // 다름 → 세부 분기
                    localConfig.configured && remoteConfig != null -> {
                        val meta = readSyncMeta()
                        when {
                            // category·isPinned 동일, order만 다름 → local 순서로 업로드
                            configsHaveSameButtonSet(localConfig, remoteConfig) -> {
                                firestoreSyncRepository.uploadMainButtonConfig(localConfig)
                                mainButtonPrefs.edit().putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false).apply()
                            }
                            // 같은 계정 + dirty → 미동기화 변경, local 업로드
                            meta.lastUid == currentUid && meta.dirty -> {
                                firestoreSyncRepository.uploadMainButtonConfig(localConfig)
                                mainButtonPrefs.edit().putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false).apply()
                            }
                            // 실제 충돌 → conflict 다이얼로그
                            else -> {
                                _uiState.update {
                                    it.copy(
                                        showMainButtonConflict = true,
                                        pendingRemoteMainButtonConfig = remoteConfig
                                    )
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun useLocalMainButtonConfig() {
        val local = _uiState.value.mainButtonConfig
        _uiState.update { it.copy(showMainButtonConflict = false, pendingRemoteMainButtonConfig = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { firestoreSyncRepository.uploadMainButtonConfig(local) }
                .onSuccess {
                    mainButtonPrefs.edit().putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false).apply()
                }
        }
    }

    fun useRemoteMainButtonConfig() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val remote = _uiState.value.pendingRemoteMainButtonConfig ?: return
        val toSave = remote.copy(version = MainButtonConfig.CURRENT_VERSION)
        persistMainButtonConfig(toSave)
        mainButtonPrefs.edit().apply {
            putBoolean(KEY_MAIN_BUTTON_CONFIG_DIRTY, false)
            if (currentUid != null) putString(KEY_LAST_MAIN_BUTTON_CONFIG_UID, currentUid)
        }.apply()
        _uiState.update { it.copy(showMainButtonConflict = false, pendingRemoteMainButtonConfig = null) }
    }

    fun enterConflictSetupMode() {
        _uiState.update {
            it.copy(
                showMainButtonConflict = false,
                pendingRemoteMainButtonConfig = null,
                showMainButtonSetup = true
            )
        }
    }

    // category + order + isPinned 모두 동일한지 확인
    private fun configsAreEquivalent(a: MainButtonConfig, b: MainButtonConfig): Boolean {
        val aSet = a.buttons.map { Triple(it.category, it.order, it.isPinned) }.toSet()
        val bSet = b.buttons.map { Triple(it.category, it.order, it.isPinned) }.toSet()
        return aSet == bSet
    }

    // category 집합과 category별 isPinned가 같고 order만 다른지 확인
    private fun configsHaveSameButtonSet(a: MainButtonConfig, b: MainButtonConfig): Boolean {
        val aMap = a.buttons.associate { it.category to it.isPinned }
        val bMap = b.buttons.associate { it.category to it.isPinned }
        return aMap == bMap
    }

    fun openAiMessenger() {
        _aiMessengerUiState.update { it.copy(showSheet = true, hasUnread = false) }
        maybeGenerateMainButtonRecommendationMessage()
    }

    fun debugInjectMainButtonRecommendation() {
        val configuredCategories = _uiState.value.mainButtonConfig.buttons.map { it.category }.toSet()
        val testCategory = MainButtonConfig.ALL_SELECTABLE_CATEGORIES
            .firstOrNull { it !in configuredCategories } ?: return
        _aiMessengerUiState.update { s ->
            s.copy(
                messages = s.messages + AiMessage.MainButtonRecommendation(category = testCategory),
                hasUnread = true
            )
        }
    }

    fun closeAiMessenger() {
        _aiMessengerUiState.update { it.copy(showSheet = false) }
    }

    fun maybeGenerateMainButtonRecommendationMessage() {
        viewModelScope.launch(Dispatchers.Default) {
            val config = _uiState.value.mainButtonConfig
            val configuredCategories = config.buttons.map { it.category }.toSet()
            val remainingSlots = MainButtonConfig.MAX_BUTTONS - config.buttons.size
            if (remainingSlots <= 0) return@launch
            val dismissedMap = loadDismissedCategories()
            val now = System.currentTimeMillis()
            val pendingCategories = _aiMessengerUiState.value.messages
                .filterIsInstance<AiMessage.MainButtonRecommendation>()
                .filter { it.status == RecommendationStatus.PENDING }
                .map { it.category }
                .toSet()
            val newRecommendations = _promotedButtons.value
                .filter { category ->
                    category !in configuredCategories &&
                        category !in pendingCategories &&
                        (dismissedMap[category] ?: 0L) < now
                }
                .take(remainingSlots)
            if (newRecommendations.isEmpty()) return@launch
            val newMessages = newRecommendations.map { category ->
                AiMessage.MainButtonRecommendation(category = category)
            }
            _aiMessengerUiState.update { s ->
                s.copy(
                    messages = s.messages + newMessages,
                    hasUnread = s.hasUnread || !s.showSheet
                )
            }
        }
    }

    fun acceptMainButtonRecommendation(messageId: String) {
        val message = _aiMessengerUiState.value.messages
            .filterIsInstance<AiMessage.MainButtonRecommendation>()
            .find { it.id == messageId } ?: return
        val config = _uiState.value.mainButtonConfig
        if (config.buttons.size >= MainButtonConfig.MAX_BUTTONS) {
            _uiState.update { it.copy(statusMessage = "버튼이 10개예요. 기존 버튼을 정리한 후 추가해 보세요.") }
            return
        }
        val nextOrder = (config.buttons.maxOfOrNull { it.order } ?: -1) + 1
        persistAndSyncMainButtonConfig(
            config.copy(
                buttons = config.buttons + MainButtonItem(
                    category = message.category,
                    order = nextOrder,
                    source = MainButtonSource.USER_ADDED
                )
            )
        )
        _aiMessengerUiState.update { s ->
            s.copy(messages = s.messages.map { msg ->
                if (msg.id == messageId && msg is AiMessage.MainButtonRecommendation)
                    msg.copy(status = RecommendationStatus.ACCEPTED)
                else msg
            })
        }
    }

    fun dismissMainButtonRecommendation(messageId: String) {
        val message = _aiMessengerUiState.value.messages
            .filterIsInstance<AiMessage.MainButtonRecommendation>()
            .find { it.id == messageId } ?: return
        val dismissedMap = loadDismissedCategories().toMutableMap()
        dismissedMap[message.category] = System.currentTimeMillis() + RECOMMENDATION_COOLDOWN_MILLIS
        saveDismissedCategories(dismissedMap)
        _aiMessengerUiState.update { s ->
            s.copy(messages = s.messages.map { msg ->
                if (msg.id == messageId && msg is AiMessage.MainButtonRecommendation)
                    msg.copy(status = RecommendationStatus.DISMISSED)
                else msg
            })
        }
    }

    private fun loadDismissedCategories(): Map<String, Long> {
        val json = aiMessengerPrefs.getString(KEY_DISMISSED_CATEGORIES, null) ?: return emptyMap()
        return try {
            undoJson.decodeFromString<Map<String, Long>>(json)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveDismissedCategories(map: Map<String, Long>) {
        aiMessengerPrefs.edit()
            .putString(KEY_DISMISSED_CATEGORIES, undoJson.encodeToString(map))
            .apply()
    }

    fun enterMainButtonReorderMode(selectedButtonId: String) {
        _uiState.update { it.copy(
            isMainButtonReorderMode = true,
            selectedMainButtonForSwapId = selectedButtonId,
            temporaryMainButtons = it.mainButtonConfig.buttons,
            showMainButtonSetup = false,
            mainButtonSetupTarget = null
        ) }
    }

    fun exitMainButtonReorderMode() {
        _uiState.update { it.copy(
            isMainButtonReorderMode = false,
            selectedMainButtonForSwapId = null,
            temporaryMainButtons = null
        ) }
    }

    fun selectMainButtonForSwap(buttonId: String) {
        val current = _uiState.value.selectedMainButtonForSwapId
        when {
            current == null -> _uiState.update { it.copy(selectedMainButtonForSwapId = buttonId) }
            current == buttonId -> _uiState.update { it.copy(selectedMainButtonForSwapId = null) }
            else -> {
                swapTemporaryMainButtonPositions(current, buttonId)
                _uiState.update { it.copy(selectedMainButtonForSwapId = null) }
            }
        }
    }

    private fun swapTemporaryMainButtonPositions(firstButtonId: String, secondButtonId: String) {
        val tempButtons = _uiState.value.temporaryMainButtons ?: return
        val sorted = tempButtons.sortedBy { it.order }.toMutableList()
        val idxA = sorted.indexOfFirst { it.category == firstButtonId }
        val idxB = sorted.indexOfFirst { it.category == secondButtonId }
        if (idxA < 0 || idxB < 0) return
        val orderA = sorted[idxA].order
        sorted[idxA] = sorted[idxA].copy(order = sorted[idxB].order)
        sorted[idxB] = sorted[idxB].copy(order = orderA)
        _uiState.update { it.copy(temporaryMainButtons = sorted.sortedBy { it.order }) }
    }

    fun confirmMainButtonReorder() {
        val tempButtons = _uiState.value.temporaryMainButtons ?: return
        val renormalized = tempButtons.sortedBy { it.order }.mapIndexed { i, btn -> btn.copy(order = i) }
        persistAndSyncMainButtonConfig(_uiState.value.mainButtonConfig.copy(buttons = renormalized))
        _uiState.update { it.copy(
            isMainButtonReorderMode = false,
            selectedMainButtonForSwapId = null,
            temporaryMainButtons = null
        ) }
    }

    fun hideMainButton(category: String) {
        val config = _uiState.value.mainButtonConfig
        if (config.buttons.size <= MainButtonConfig.MIN_BUTTONS) return
        val newButtons = config.buttons
            .filter { it.category != category }
            .mapIndexed { i, btn -> btn.copy(order = i) }
        persistAndSyncMainButtonConfig(config.copy(buttons = newButtons))
        dismissMainButtonReplacePicker()
    }

    fun swapMainButton(category: String, direction: Int) {
        val config = _uiState.value.mainButtonConfig
        val sorted = config.buttons.sortedBy { it.order }.toMutableList()
        val idx = sorted.indexOfFirst { it.category == category }
        if (idx < 0) return
        val targetIdx = idx + direction
        if (targetIdx < 0 || targetIdx >= sorted.size) return
        val a = sorted[idx]
        val b = sorted[targetIdx]
        sorted[idx] = b.copy(order = a.order)
        sorted[targetIdx] = a.copy(order = b.order)
        persistMainButtonConfig(config.copy(buttons = sorted.sortedBy { it.order }))
    }

    fun replaceMainButton(oldCategory: String, newCategory: String) {
        val config = _uiState.value.mainButtonConfig
        if (config.buttons.any { it.category == newCategory }) return
        val newButtons = config.buttons.map { btn ->
            if (btn.category == oldCategory)
                btn.copy(category = newCategory, source = MainButtonSource.USER_ADDED)
            else btn
        }
        persistAndSyncMainButtonConfig(config.copy(buttons = newButtons))
        dismissMainButtonReplacePicker()
    }



    // endregion

    private fun observeAllActivities() {
        viewModelScope.launch {
            repository.getAllActivities().collect { activities ->
                _uiState.update { it.copy(allActivities = activities) }
            }
        }
    }

    private fun observeAnalytics() {
        val todayStart = startOfDay(Calendar.getInstance()).timeInMillis
        val yesterdayStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }).timeInMillis
        val tomorrowStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }).timeInMillis
        viewModelScope.launch {
            repository.getActivitiesByDateRange(yesterdayStart, tomorrowStart).collect { activities ->
                val timedActivities = activities.filter { isTimedCategory(it.category) }
                val analytics = withContext(Dispatchers.Default) {
                    buildAnalytics(timedActivities)
                }
                _uiState.update { it.copy(analytics = analytics) }
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
                        todayActivities = timedActivities
                    )
                }
            }
        }
    }

    private fun buildAnalytics(activities: List<ActivitySession>): AnalyticsState {
        val now = System.currentTimeMillis()
        val todayStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = now
        }).timeInMillis
        val tomorrowStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, 1)
        }).timeInMillis
        val yesterdayStart = startOfDay(Calendar.getInstance().apply {
            timeInMillis = todayStart
            add(Calendar.DAY_OF_YEAR, -1)
        }).timeInMillis
        val analyticsActivities = splitActivitiesAcrossDays(
            activities = activities,
            rangeStartMillis = yesterdayStart,
            rangeEndMillis = tomorrowStart
        )
        val todayActivities = analyticsActivities.filter { it.startTime >= todayStart && it.startTime < tomorrowStart }
        val yesterdayActivities = analyticsActivities.filter { it.startTime >= yesterdayStart && it.startTime < todayStart }
        return AnalyticsState(
            todayCategoryStats = buildCategoryStats(todayActivities),
            yesterdayCategoryStats = buildCategoryStats(yesterdayActivities)
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

    private fun currentElapsedTime(): Long {
        val state = _uiState.value
        if (!state.isRunning || state.startTime == 0L) return _timerDisplayState.value.elapsedTime
        return (System.currentTimeMillis() - state.startTime).coerceAtLeast(0L)
    }

    private fun linkedTodoForState(state: ActivityUiState, todoId: Long): TodoItem? {
        state.linkedTodoCalendarSourceId?.let { calendarSourceId ->
            return state.incompleteTodos.firstOrNull { it.calendarSourceId == calendarSourceId }
        }
        return state.incompleteTodos.firstOrNull { it.id == todoId }
    }

    private suspend fun addAccumulatedSecondsForActivityTodo(activity: ActivitySession, seconds: Long) {
        val todoId = activity.linkedTodoId ?: return
        val calendarSourceId = activity.linkedPetiteId.takeIf { todoId == 0L }
        if (calendarSourceId != null) {
            todoRepository.addAccumulatedSecondsByCalendarSourceId(calendarSourceId, seconds)
        } else {
            todoRepository.addAccumulatedSeconds(todoId, seconds)
        }
    }

    private fun resetTimerDisplayState() {
        _timerDisplayState.value = TimerDisplayState()
    }

    private fun syncPinnedAutoButtonFromStore(): Boolean {
        val pinned = TimerStateStore.getPinnedTimer(appContext)
        val isPinnedAutoButton = pinned?.sourceType == ActivitySourceType.AUTO_BUTTON &&
            (pinned.category == "SCHOOL" || pinned.category == "COMPANY")
        val state = _uiState.value
        return if (isPinnedAutoButton) {
            val changed = state.activeAutoButtonCategory != pinned.category ||
                state.activeAutoButtonStartedAt != pinned.startTime
            if (changed) {
                _uiState.update {
                    it.copy(
                        activeAutoButtonCategory = pinned.category,
                        activeAutoButtonStartedAt = pinned.startTime
                    )
                }
            }
            true
        } else {
            false
        }
    }

    private fun clearPendingActivity() {
        timerJob?.cancel()
        timerJob = null
        activityTimerNotifier.clearRunningTimer()
        clearActiveSession()
        finishFocusMode(reason = "activity_cleared")
        resetTimerDisplayState()
        _uiState.update {
            it.copy(
                isRunning = false,
                currentCategory = "",
                startTime = 0L,
                linkedTodoId = null,
                linkedTodoCalendarSourceId = null,
                linkedPetiteId = null,
                sourceType = ActivitySourceType.MANUAL,
                sourceId = null,
                pendingTitle = null,
                pendingNote = null,
                dailyCueId = null,
                dailyCueTargetDateKey = null,
                isFocusModeActive = false,
                focusModeEndsAtMillis = 0L,
                statusMessage = null
            )
        }
    }

    private suspend fun syncAllPendingChanges() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseSyncCoordinator(appContext).syncAll(uid)
    }

    private suspend fun syncAfterActivitySaveIfNeeded(activity: ActivitySession) {
        if (!shouldSyncAfterActivitySave(activity)) return
        runCatching { syncAllPendingChanges() }
    }

    private fun shouldSyncAfterActivitySave(activity: ActivitySession): Boolean {
        val hasExerciseSets = activity.category == "EXERCISE" &&
            activity.exerciseSets.isNotEmpty()
        return activity.durationMillis >= IMMEDIATE_SYNC_ACTIVITY_DURATION_MILLIS ||
            hasExerciseSets
    }

    private fun saveActiveSession(
        category: String,
        startTime: Long,
        goalMillis: Long,
        linkedTodoId: Long? = null,
        linkedTodoCalendarSourceId: String? = null,
        linkedPetiteId: String? = null,
        linkedTodoTitle: String? = null,
        pendingNote: String? = null,
        pendingTitle: String? = null,
        dailyCueId: Long? = null,
        dailyCueTargetDateKey: Long? = null,
        sourceType: String = ActivitySourceType.MANUAL,
        sourceId: String? = null,
        routineGoalMillis: Long = 0L
    ) {
        TimerStateStore.saveActiveTimer(
            context = appContext,
            category = category,
            startTime = startTime,
            goalMillis = goalMillis,
            linkedTodoId = linkedTodoId,
            linkedTodoCalendarSourceId = linkedTodoCalendarSourceId,
            linkedPetiteId = linkedPetiteId,
            linkedTodoTitle = linkedTodoTitle,
            pendingNote = pendingNote,
            pendingTitle = pendingTitle,
            dailyCueId = dailyCueId,
            dailyCueTargetDateKey = dailyCueTargetDateKey,
            sourceType = sourceType,
            sourceId = sourceId,
            routineGoalMillis = routineGoalMillis
        )
        inactivityReminderScheduler.cancel()
        recordInactivityReminderResponseIfNeeded(startTime, category)
        FlowStatusWidgetProvider.updateAll(appContext)
    }

    private fun clearActiveSession() {
        routineGoalAlarmScheduler.cancel()
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

    private fun observeWeekSkipDates() {
        viewModelScope.launch {
            autoButtonScheduleRepository.observeWeekSkipDates().collect { map ->
                _uiState.update { it.copy(weekSkipDatesByDateKey = map) }
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

    private fun observeTodayCalendarAutoStartPetites() {
        viewModelScope.launch {
            val todayDateKey = AutoButtonScheduleRepository.todayDateKey()
            organizedPetiteRepository.observeTodayCalendarAutoStartPetites(todayDateKey).collect { petites ->
                _uiState.update { it.copy(todayCalendarPetites = petites) }
            }
        }
    }

    fun updateCalendarPetiteTime(petiteId: String, startTime24: String, endTime24: String) {
        viewModelScope.launch {
            val sourceId = organizedPetiteRepository.updateCalendarPetiteAutoStartTimes(petiteId, startTime24, endTime24)
            if (sourceId != null) {
                val scheduleId = "cal-$sourceId"
                val startMinute = parseTime24ToMinute(startTime24) ?: return@launch
                val endMinute = parseTime24ToMinute(endTime24) ?: return@launch
                autoButtonScheduleRepository.updateScheduleTime(scheduleId, startMinute, endMinute)
                autoButtonScheduler.reschedule(scheduleId)
            }
        }
    }

    fun dismissCalendarPetiteToday(petiteId: String) {
        viewModelScope.launch {
            val sourceId = organizedPetiteRepository.dismissCalendarPetiteById(petiteId)
            if (sourceId != null) {
                autoButtonScheduleRepository.skipToday("cal-$sourceId")
            }
        }
    }

    private fun parseTime24ToMinute(time24: String): Int? {
        val parts = time24.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h * 60 + m
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
            return defaultGoalMillisForCategory(category)
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

    private fun defaultGoalMillisForCategory(category: String): Long {
        return when {
            category in FOCUS_BANNER_CATEGORIES -> FocusModeStore.getFocusDurationMillis(appContext)
            category in SEVENTY_FIVE_MINUTE_GOAL_CATEGORIES -> SEVENTY_FIVE_MINUTE_GOAL_MILLIS
            else -> TimerStateStore.DEFAULT_GOAL_MILLIS
        }
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
            "MOVE" -> "\uC774\uB3D9"
            "WASH" -> "\uC53B\uAE30"
            "REST" -> "\uD734\uC2DD"
            "SCHOOL" -> "\uD559\uAD50"
            "HOBBY" -> "\uCDE8\uBBF8"
            else -> "\uD65C\uB3D9"
        }
    }

    fun startFocusMode(enableSystemDnd: Boolean = false) {
        FocusModeStore.setEnableSystemDndForFocus(appContext, enableSystemDnd)
        if (enableSystemDnd) {
            FocusDndController.enableDnd(appContext)
        }
        val now = System.currentTimeMillis()
        val endsAt = now + FocusModeStore.getFocusDurationMillis(appContext)
        FocusModeStore.saveFocusModeActive(appContext, startedAt = now, endsAt = endsAt)
        focusModeScheduler.schedule(endsAt)
        _uiState.update { it.copy(isFocusModeActive = true, focusModeEndsAtMillis = endsAt) }
        FlowStatusWidgetProvider.updateAll(appContext)
        startFocusModeCountdownJob(endsAt)
        viewModelScope.launch {
            runCatching {
                eventLogRepository.log(
                    eventType = EventType.FOCUS_MODE_STARTED,
                    entityType = EntityType.FOCUS_MODE,
                    metadataJson = """{"dnd_enabled":$enableSystemDnd}"""
                )
            }
        }
    }

    fun stopFocusMode() {
        finishFocusMode(reason = "manual")
    }

    private fun finishFocusMode(reason: String) {
        val wasActive = _uiState.value.isFocusModeActive || FocusModeStore.isFocusModeActive(appContext)
        if (!wasActive) return
        val dndEnabled = FocusModeStore.getEnableSystemDndForFocus(appContext)
        FocusDndController.restoreDnd(appContext)
        FocusModeStore.clearFocusMode(appContext)
        focusModeScheduler.cancel()
        focusModeJob?.cancel()
        _uiState.update { it.copy(isFocusModeActive = false, focusModeEndsAtMillis = 0L) }
        FlowStatusWidgetProvider.updateAll(appContext)
        viewModelScope.launch {
            runCatching {
                eventLogRepository.log(
                    eventType = EventType.FOCUS_MODE_STOPPED,
                    entityType = EntityType.FOCUS_MODE,
                    metadataJson = """{"dnd_enabled":$dndEnabled,"reason":"$reason"}"""
                )
            }
        }
    }

    fun toggleNotificationSound() {
        val next = !_isNotificationSoundEnabled.value
        FocusModeStore.setNotificationSoundEnabled(appContext, next)
        _isNotificationSoundEnabled.value = next
    }

    fun toggleInactivityReminder() {
        val next = !_isInactivityReminderEnabled.value
        InactivityReminderStore.setEnabled(appContext, next)
        _isInactivityReminderEnabled.value = next
        if (next) {
            inactivityReminderScheduler.rescheduleFromLastActivityIfNeeded()
        } else {
            inactivityReminderScheduler.cancel()
        }
    }

    private fun restoreFocusModeState() {
        val soundEnabled = FocusModeStore.isNotificationSoundEnabled(appContext)
        _isNotificationSoundEnabled.value = soundEnabled
        val state = FocusModeStore.getFocusModeState(appContext)
        val now = System.currentTimeMillis()
        if (state != null && state.endsAtMillis > now) {
            _uiState.update {
                it.copy(
                    isFocusModeActive = true,
                    focusModeEndsAtMillis = state.endsAtMillis
                )
            }
            startFocusModeCountdownJob(state.endsAtMillis)
        } else {
            if (state != null) {
                // 만료 상태: DND 복원 후 초기화
                FocusDndController.restoreDnd(appContext)
                FocusModeStore.clearFocusMode(appContext)
            }
        }
    }

    private fun restoreInactivityReminderState() {
        _isInactivityReminderEnabled.value = InactivityReminderStore.isEnabled(appContext)
    }

    private fun recordInactivityReminderResponseIfNeeded(startedAt: Long, category: String) {
        val pendingClick = InactivityReminderStore.consumePendingClick(appContext) ?: return
        viewModelScope.launch {
            runCatching {
                eventLogRepository.log(
                    eventType = EventType.ACTIVITY_STARTED,
                    source = EventSource.NOTIFICATION,
                    metadataJson = JSONObject()
                        .put("type", InactivityReminderReceiver.TYPE)
                        .put("notificationId", pendingClick.notificationId)
                        .put("clickedAt", pendingClick.clickedAt)
                        .put("activityStartedAt", startedAt)
                        .put("category", category)
                        .toString()
                )
            }
        }
    }

    private fun startFocusModeCountdownJob(endsAtMillis: Long) {
        focusModeJob?.cancel()
        focusModeJob = viewModelScope.launch {
            while (true) {
                val remaining = endsAtMillis - System.currentTimeMillis()
                if (remaining <= 0L) {
                    // AlarmReceiver도 동시에 실행될 수 있으나 restoreDnd/clearDndState는 멱등
                    FocusDndController.restoreDnd(appContext)
                    FocusModeStore.clearFocusMode(appContext)
                    _uiState.update { it.copy(isFocusModeActive = false, focusModeEndsAtMillis = 0L) }
                    FlowStatusWidgetProvider.updateAll(appContext)
                    break
                }
                delay(1_000L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        activeSessionWatcherJob?.cancel()
        brushTimerJob?.cancel()
        snackTimerJob?.cancel()
        focusModeJob?.cancel()
    }

    companion object {
        private const val TAG = "ActivityViewModel"
        private const val FLOW_RECOMMENDATION_ALGORITHM_VERSION = "flow-v1"
        private const val PREFS_MIGRATIONS = "flowlog_migrations"
        private const val PREFS_ACTIVITY_UNDO = "activity_undo"
        private const val PREFS_TIMER_STATE = "timer_state"
        private const val PREFS_MAIN_BUTTON_CONFIG = "main_button_config"
        private const val KEY_MAIN_BUTTON_CONFIG = "config"
        private const val KEY_LAST_MAIN_BUTTON_CONFIG_UID = "last_uid"
        private const val KEY_MAIN_BUTTON_CONFIG_DIRTY = "dirty"
        private const val KEY_LAST_LOCAL_MAIN_BUTTON_EDIT_AT = "last_edit_at"
        private const val PREFS_AI_MESSENGER = "ai_messenger"
        private const val KEY_DISMISSED_CATEGORIES = "dismissed_categories"
        private const val RECOMMENDATION_COOLDOWN_MILLIS = 14L * 24 * 60 * 60 * 1000L
        private const val KEY_LAST_ADDED_ACTIVITY = "last_added_activity"
        private const val KEY_BRUSH_TIMER_ENDS_AT = "brush_timer_ends_at"
        private const val KEY_SNACK_BUTTON_TIMER_ENDS_AT = "snack_button_timer_ends_at"
        private val undoJson = Json { ignoreUnknownKeys = true }
        private const val KEY_SLEEP_RECORD_2026_05_19 = "sleep_record_2026_05_19_212957"
        private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
        private val DEFAULT_SCHOOL_COMPANY_GOAL_MILLIS = TimeUnit.HOURS.toMillis(10)
        private val SEVENTY_FIVE_MINUTE_GOAL_MILLIS = TimeUnit.MINUTES.toMillis(75)
        private val IMMEDIATE_SYNC_ACTIVITY_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(10)
        private val SEVENTY_FIVE_MINUTE_GOAL_CATEGORIES = setOf("STUDY", "TODO", "WORK", "DEVELOPMENT", "EXERCISE", "ETC")
        private val FOCUS_BANNER_CATEGORIES = setOf("STUDY", "TODO", "WORK", "DEVELOPMENT", "ETC")
    }
}
