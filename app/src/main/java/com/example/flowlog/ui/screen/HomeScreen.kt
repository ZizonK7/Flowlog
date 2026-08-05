package com.example.flowlog.ui.screen

import com.example.flowlog.R
import android.content.Context
import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.ExerciseLastSettings
import com.example.flowlog.data.local.ExerciseOptionsStore
import com.example.flowlog.data.local.FocusModeStore
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.notification.KakaoStyleAlertPlayer
import com.example.flowlog.notification.FocusDndController
import com.example.flowlog.debug.CityTimetablePreset
import com.example.flowlog.debug.CityTimetableSamples
import com.example.flowlog.debug.SampleTimetableData
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.ExerciseSetRecord
import com.example.flowlog.ui.city.CityTimetableCard
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.CategoryButton
import com.example.flowlog.ui.component.CategoryGlyph
import com.example.flowlog.ui.component.EditActivityDialog
import com.example.flowlog.ui.component.PickerWaveBackground
import com.example.flowlog.ui.component.WheelPickerColumn
import com.example.flowlog.ui.component.categoryColor
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.FlowActivityRecommendation
import com.example.flowlog.ui.viewmodel.TimerDisplayState
import com.example.flowlog.ui.viewmodel.AnalyticsState
import com.example.flowlog.ui.viewmodel.CategoryStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    viewModel: ActivityViewModel,
    topActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    autoButtonManagerOpen: Boolean = false,
    onAutoButtonManagerDismiss: () -> Unit = {},
    isDeveloperMode: Boolean = false,
    scrollToMainTimerRequest: Int = 0
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val homeListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val restoredPinnedTimer = remember(context) {
        TimerStateStore.getPinnedTimer(context)
    }
    var localAutoButtonManagerOpen by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshTimerStates()
        }
    }
    LaunchedEffect(scrollToMainTimerRequest) {
        if (scrollToMainTimerRequest > 0) {
            homeListState.animateScrollToItem(index = 1)
        }
    }
    val activityCategories = remember {
        listOf(
            "TOOTHBRUSH",
            "SNACK",
            "MEAL",
            "STUDY",
            "WORK",
            "DEVELOPMENT",
            "READING",
            "MOVE",
            "WASH",
            "SCHOOL",
            "COMPANY",
            "EXERCISE",
            "SLEEP",
            "REST",
            "ETC"
        )
    }
    val categories = activityCategories
    val editCategories = remember(uiState.mainButtonConfig) {
        uiState.mainButtonConfig.buttons.sortedBy { it.order }.map { it.category }
    }
    val selectedCategory = uiState.selectedCategory
    val isFiltered = selectedCategory != null
    val displayActivities by remember(
        selectedCategory,
        uiState.todayActivities,
        uiState.allActivities
    ) {
        derivedStateOf {
            if (selectedCategory == null) {
                uiState.todayActivities
            } else {
                uiState.allActivities.filter { it.category == selectedCategory }
            }
        }
    }
    var samplePresetIndex by remember { mutableStateOf(0) }
    var isActivityListExpanded by remember(selectedCategory) { mutableStateOf(false) }
    var pinnedQuickCategory by remember(restoredPinnedTimer) {
        mutableStateOf(restoredPinnedTimer?.category)
    }
    var pinnedQuickStartedAt by remember(restoredPinnedTimer) {
        mutableStateOf(restoredPinnedTimer?.startTime ?: 0L)
    }
    var autoButtonPinned by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.activeAutoButtonCategory) {
        val autoCategory = uiState.activeAutoButtonCategory
        if (autoCategory != null) {
            if (pinnedQuickCategory == null) {
                pinnedQuickCategory = autoCategory
                pinnedQuickStartedAt = uiState.activeAutoButtonStartedAt
                autoButtonPinned = true
            }
        } else if (autoButtonPinned) {
            pinnedQuickCategory = null
            pinnedQuickStartedAt = 0L
            autoButtonPinned = false
        }
    }
    val recommendedUndoSnackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.recommendedTodoCompletionEvents.collect { event ->
            val result = recommendedUndoSnackbarHostState.showSnackbar(
                message = "${event.block.title} 완료됨",
                actionLabel = "되돌리기",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoRecommendedTodoCompletion(event)
            }
        }
    }
    val visibleActivities = remember(displayActivities, isActivityListExpanded) {
        val displayLimit = if (isActivityListExpanded) {
            RECENT_RECORD_EXPANDED_LIMIT
        } else {
            RECENT_RECORD_COLLAPSED_LIMIT
        }
        displayActivities.take(displayLimit)
    }
    val hiddenActivityCount = remember(displayActivities, visibleActivities) {
        displayActivities.size - visibleActivities.size
    }

    val isFocusFireActive = uiState.isRunning &&
        uiState.isFocusModeActive &&
        uiState.currentCategory in FOCUS_FIRE_CATEGORIES
    val homeBackgroundColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFireBackground else Color(0xFFF8F8F9),
        animationSpec = tween(durationMillis = 420),
        label = "home-background-color"
    )

    val todayText = remember {
        SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).format(Date())
    }
    val activityCategoryLabels = mapOf(
        "MEAL" to stringResource(R.string.timer_category_meal),
        "EXERCISE" to stringResource(R.string.timer_category_exercise),
        "SLEEP" to stringResource(R.string.timer_category_sleep),
        "STUDY" to stringResource(R.string.timer_category_study),
        "WORK" to stringResource(R.string.timer_category_work),
        "COMPANY" to stringResource(R.string.timer_category_company),
        "DEVELOPMENT" to stringResource(R.string.timer_category_development),
        "READING" to stringResource(R.string.timer_category_reading),
        "MOVE" to stringResource(R.string.timer_category_move),
        "WASH" to stringResource(R.string.timer_category_wash),
        "REST" to stringResource(R.string.timer_category_rest),
        "SCHOOL" to stringResource(R.string.timer_category_school),
        "ETC" to stringResource(R.string.timer_category_etc)
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = homeListState,
            modifier = Modifier
                .fillMaxSize()
                .background(homeBackgroundColor),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                HomeHeader(
                    dateText = todayText,
                    isFocusFireActive = isFocusFireActive,
                    actions = topActions
                )
        }

        item {
            val titleSuggestions by remember(uiState.currentCategory, uiState.allActivities) {
                derivedStateOf {
                    buildTitleSuggestions(
                        category = uiState.currentCategory,
                        activities = uiState.allActivities,
                        categoryLabels = activityCategoryLabels
                    )
                }
            }
            val effectiveMainButtonConfig = if (uiState.isMainButtonReorderMode && uiState.temporaryMainButtons != null)
                uiState.mainButtonConfig.copy(buttons = uiState.temporaryMainButtons!!)
            else
                uiState.mainButtonConfig
            TodayFlowCard(
                isRunning = uiState.isRunning,
                currentCategory = uiState.currentCategory,
                startTime = uiState.startTime,
                timerDisplayStateFlow = viewModel.timerDisplayState,
                statusMessage = uiState.statusMessage,
                appliedTitle = uiState.pendingTitle.orEmpty(),
                titleSuggestions = titleSuggestions,
                mainButtonConfig = effectiveMainButtonConfig,
                onLongClickButton = { category -> viewModel.openMainButtonReplacePicker(category) },
                onPinQuickCategory = { category ->
                    val startedAt = System.currentTimeMillis()
                    pinnedQuickCategory = category
                    pinnedQuickStartedAt = startedAt
                    viewModel.startOverlappingActivity(category, startedAt)
                },
                pinnedQuickCategory = pinnedQuickCategory,
                onStop = { title, note, sets ->
                    viewModel.stopActivityAndSave(title, note, sets)
                },
                onStopAndComplete = { title, note, sets ->
                    viewModel.stopActivityAndSave(title, note, sets, markLinkedAsComplete = true)
                },
                onApplyTitle = { title ->
                    viewModel.setRunningActivityTitle(title)
                },
                exerciseSets = uiState.exerciseSets,
                exerciseMemo = uiState.exerciseMemo,
                onExerciseSetsChanged = { viewModel.updateExerciseSets(it) },
                onExerciseMemoChanged = { viewModel.updateExerciseMemo(it) },
                onStart = { category ->
                    if (!uiState.isRunning || category == "SNACK" || category == "TOOTHBRUSH") {
                        viewModel.startActivity(category)
                    }
                },
                isFocusModeActive = uiState.isFocusModeActive,
                focusModeEndsAtMillis = uiState.focusModeEndsAtMillis,
                onStartFocusMode = { enableDnd -> viewModel.startFocusMode(enableDnd) },
                onStopFocusMode = { viewModel.stopFocusMode() },
                isFocusFireActive = isFocusFireActive,
                isRoutineActive = uiState.isRoutineActive,
                routineGoalMillis = uiState.routineGoalMillis,
                linkedTodoId = uiState.linkedTodoId,
                linkedPetiteId = uiState.linkedPetiteId,
                dailyCueId = uiState.dailyCueId,
                isMainButtonReorderMode = uiState.isMainButtonReorderMode,
                selectedMainButtonForSwapId = uiState.selectedMainButtonForSwapId,
                onExitReorderMode = { viewModel.exitMainButtonReorderMode() },
                onConfirmReorderMode = { viewModel.confirmMainButtonReorder() },
                onSelectButtonForSwap = { cat -> viewModel.selectMainButtonForSwap(cat) },
                showMainButtonSetup = uiState.showMainButtonSetup,
                onCompleteSetup = { selected -> viewModel.completeMainButtonSetup(selected) }
            )
        }

        item {
            QuickTimerSection(
                categories = categories,
                isFocusFireActive = isFocusFireActive,
                pinnedCategory = pinnedQuickCategory,
                onUnpinCategory = {
                    val category = pinnedQuickCategory
                    val startedAt = pinnedQuickStartedAt
                    if (category != null && startedAt > 0L) {
                        viewModel.saveOverlappingActivity(category, startedAt)
                    }
                    pinnedQuickCategory = null
                    pinnedQuickStartedAt = 0L
                },
                isBrushTimerRunning = uiState.isBrushTimerRunning,
                brushDoneEndsAtMillis = uiState.brushDoneEndsAtMillis,
                snackButtonEndsAtMillis = uiState.snackButtonEndsAtMillis,
                onStart = { category ->
                    when (category) {
                        "TOOTHBRUSH" -> viewModel.startActivity(category)
                        "SNACK" -> viewModel.startActivity(category)
                    }
                },
                onToggleBrushTimer = {
                    if (uiState.isBrushTimerRunning || uiState.brushDoneEndsAtMillis > 0L) {
                        viewModel.cancelBrushTimers()
                    } else {
                        viewModel.startActivity("TOOTHBRUSH")
                    }
                },
                onToggleSnackTimer = {
                    if (uiState.snackButtonEndsAtMillis > 0L) {
                        viewModel.cancelSnackTimer()
                    } else {
                        viewModel.startActivity("SNACK")
                    }
                }
            )
        }

        item {
            TimetableCard(
                isFocusFireActive = isFocusFireActive,
                activities = if (isDeveloperMode) SampleTimetableData.activitiesForIndex(samplePresetIndex) else uiState.todayActivities,
                scheduledBlocks = if (isDeveloperMode) emptyList() else uiState.scheduledAutoButtonBlocks,
                recommendedBlocks = if (isDeveloperMode) emptyList() else uiState.recommendedTodoBlocks,
                incompleteTodos = if (isDeveloperMode) emptyList() else uiState.recommendedTodoCandidates,
                activeCategory = uiState.currentCategory.takeIf { uiState.isRunning },
                allActivities = if (isDeveloperMode) emptyList() else uiState.allActivities,
                timerStartMillis = if (!isDeveloperMode && uiState.isRunning) uiState.startTime else null,
                onSaveSleep = { start, end -> if (!isDeveloperMode) viewModel.saveSleepActivity(start, end) },
                onSkipToday = { if (!isDeveloperMode) viewModel.skipAutoButtonToday(it) },
                onUnskipToday = { if (!isDeveloperMode) viewModel.unskipAutoButtonToday(it) },
                onEditSchedule = { if (!isDeveloperMode) localAutoButtonManagerOpen = true },
                onManageSchedules = { if (!isDeveloperMode) localAutoButtonManagerOpen = true },
                onStartScheduled = {
                    if (!isDeveloperMode) {
                        viewModel.startScheduledAutoButtonNow(it)
                        coroutineScope.launch {
                            homeListState.animateScrollToItem(index = 1)
                        }
                    }
                },
                onStartRecommended = {
                    if (!isDeveloperMode) {
                        viewModel.startRecommendedTodoActivity(it)
                        coroutineScope.launch {
                            homeListState.animateScrollToItem(index = 1)
                        }
                    }
                },
                onCompleteRecommended = { if (!isDeveloperMode) viewModel.completeRecommendedTodo(it) },
                onSetRecommendedTime = { block, hour -> if (!isDeveloperMode) viewModel.setRecommendedTodoTime(block, hour) },
                onReplaceRecommendedItem = { block, todo -> if (!isDeveloperMode) viewModel.replaceRecommendedTodoItem(block, todo) },
                onStartFlowRecommendation = {
                    if (!isDeveloperMode) {
                        viewModel.startFlowRecommendation(it)
                        coroutineScope.launch {
                            homeListState.animateScrollToItem(index = 1)
                        }
                    }
                },
                onOpenFlowRecommendation = { if (!isDeveloperMode) viewModel.openFlowRecommendation(it) },
                onCompleteFlowRecommendation = { if (!isDeveloperMode) viewModel.completeFlowRecommendation(it) },
                flowRecommendations = if (isDeveloperMode) emptyList() else uiState.flowRecommendations,
                isDeveloperMode = isDeveloperMode,
                samplePresetIndex = samplePresetIndex,
                onCyclePreset = { samplePresetIndex = (samplePresetIndex + 1) % SampleTimetableData.presetCount }
            )
        }

        item {
            RecentRecordsCard(
                isFocusFireActive = isFocusFireActive,
                title = if (selectedCategory == null) "최근 기록" else "${displayCategory(selectedCategory)} 기록",
                activities = visibleActivities,
                isFiltered = isFiltered,
                onClearFilter = { viewModel.clearFilter() },
                canUndo = uiState.lastAddedActivity != null,
                onUndo = { viewModel.undoLastAddedActivity() },
                onDelete = { viewModel.deleteActivity(it) },
                onEdit = { viewModel.startEditActivity(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )
        }

        if (displayActivities.size > RECENT_RECORD_COLLAPSED_LIMIT) {
            item {
                androidx.compose.material3.TextButton(
                    onClick = { isActivityListExpanded = !isActivityListExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isFocusFireActive) {
                            FocusFireSoft
                        } else {
                            FlowPurpleSoft.copy(alpha = 0.5f)
                        },
                        contentColor = if (isFocusFireActive) FocusFire else FlowPurple
                    )
                ) {
                    Text(
                        text = if (isActivityListExpanded) "접기" else "더보기 ${hiddenActivityCount}개",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            AnalyticsCard(
                analytics = uiState.analytics,
                isFocusFireActive = isFocusFireActive
            )
        }

    } // LazyColumn 닫기

    uiState.editingActivity?.let { editingActivity ->
        EditActivityDialog(
            activity = editingActivity,
            categories = editCategories,
            isVisible = true,
            onSave = { category, title, note, exerciseSets ->
                viewModel.saveEditedActivity(category, title, note, exerciseSets)
            },
            onDismiss = {
                viewModel.cancelEditActivity()
            }
        )
    }

    SnackbarHost(
        hostState = recommendedUndoSnackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = FlowPurpleDeep,
                contentColor = Color.White,
                actionColor = Color(0xFFD7D1FF),
                shape = RoundedCornerShape(12.dp)
            )
        }
    )

    if (autoButtonManagerOpen || localAutoButtonManagerOpen) {
        AutoButtonManagerSheet(
            schedules = uiState.autoButtonSchedules,
            weekSkipDatesByDateKey = uiState.weekSkipDatesByDateKey,
            calendarPetites = emptyList(),
            categories = editCategories,
            onDismiss = {
                localAutoButtonManagerOpen = false
                onAutoButtonManagerDismiss()
            },
            onSave = viewModel::saveAutoButtonSchedule,
            onToggleEnabled = viewModel::setAutoButtonEnabled,
            onSkipToday = viewModel::skipAutoButtonToday,
            onSkipNextDay = viewModel::skipAutoButtonNextDay,
            onUnskipToday = viewModel::unskipAutoButtonToday,
            onUnskipNextDay = viewModel::unskipAutoButtonNextDay,
            onDelete = viewModel::deleteAutoButtonSchedule,
            onCalendarPetiteTimeUpdate = viewModel::updateCalendarPetiteTime,
            onCalendarPetiteDismiss = viewModel::dismissCalendarPetiteToday
        )
    }

    val mainButtonSetupTarget = uiState.mainButtonSetupTarget
    if (mainButtonSetupTarget != null) {
        MainButtonEditBottomSheet(
            category = mainButtonSetupTarget,
            config = uiState.mainButtonConfig,
            onDismiss = { viewModel.dismissMainButtonReplacePicker() },
            onHide = { viewModel.hideMainButton(it) },
            onEnterReorderMode = { cat -> viewModel.enterMainButtonReorderMode(cat) },
            onReplace = { old, new -> viewModel.replaceMainButton(old, new) }
        )
    }

    if (uiState.showMainButtonConflict) {
        val remoteConfig = uiState.pendingRemoteMainButtonConfig
        if (remoteConfig != null) {
            MainButtonConflictDialog(
                localConfig = uiState.mainButtonConfig,
                remoteConfig = remoteConfig,
                onUseLocal = { viewModel.useLocalMainButtonConfig() },
                onUseRemote = { viewModel.useRemoteMainButtonConfig() },
                onSetupNew = { viewModel.enterConflictSetupMode() }
            )
        }
    }

    }
}


