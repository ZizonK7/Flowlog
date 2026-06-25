package com.example.flowlog.ui.screen

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FlowPurple = Color(0xFF5140D8)
private val FlowPurpleDeep = Color(0xFF2F238F)
private val FlowPurpleSoft = Color(0xFFEDE9FF)
private val FocusFire = Color(0xFFFF7A2F)
private val FocusFireSoft = Color(0xFFFFE8D8)
private val FocusFireSurface = Color(0xFFFFF5EE)
private val FocusFireBackground = Color(0xFFFFF0E6)
private val FlowInk = Color(0xFF10182C)
private val FlowMuted = Color(0xFF697386)
private val FlowDivider = Color(0xFFE8E8EE)
private val TimetableTodoColor = Color(0xFF7A72D8)
private const val DAY_DURATION_MILLIS = 24L * 60L * 60L * 1000L
private const val MERGE_THRESHOLD_MILLIS = 10 * 60 * 1000L
private const val RECOMMENDED_TODO_DISPLAY_DURATION_MILLIS = 60 * 60 * 1000L
private const val RECENT_RECORD_COLLAPSED_LIMIT = 3
private const val RECENT_RECORD_EXPANDED_LIMIT = 30

private data class ExerciseTimedSetState(
    val setIndex: Int,
    val record: ExerciseSetRecord,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val token: Long = System.currentTimeMillis()
)
private val MERGEABLE_TIMETABLE_CATEGORIES = setOf("TODO", "STUDY", "DEVELOPMENT", "WORK")
private val HIDEABLE_TIMETABLE_BRIDGE_CATEGORIES = setOf("REST", "ETC", "MOVE")
private val PRECISE_TIMETABLE_CATEGORIES = setOf(
    "SLEEP",
    "SCHOOL",
    "COMPANY",
    "WORKPLACE",
    "MEAL",
    "EXERCISE"
)
private val PRODUCTIVE_TIMETABLE_CATEGORIES = setOf("TODO", "STUDY", "DEVELOPMENT", "WORK")
private val PRIORITY_TIMETABLE_CATEGORIES = setOf("SCHOOL", "COMPANY")
private val FOCUS_FIRE_CATEGORIES = setOf("STUDY", "TODO", "WORK", "DEVELOPMENT", "EXERCISE", "ETC")

@Composable
fun DevTimetableScreen(modifier: Modifier = Modifier) {
    var samplePresetIndex by remember { mutableStateOf(0) }
    var cityPreset   by remember { mutableStateOf(CityTimetablePreset.BASIC_DAY) }
    var showCityView by remember { mutableStateOf(false) }
    var showLabels   by remember { mutableStateOf(true) }
    var showBadges   by remember { mutableStateOf(true) }
    var bigBoat      by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            TimetableCard(
                activities = SampleTimetableData.activitiesForIndex(samplePresetIndex),
                scheduledBlocks = emptyList(),
                recommendedBlocks = emptyList(),
                incompleteTodos = emptyList(),
                activeCategory = null,
                onSkipToday = {},
                onUnskipToday = {},
                onEditSchedule = {},
                onManageSchedules = {},
                onStartRecommended = {},
                onCompleteRecommended = {},
                onSetRecommendedTime = { _, _ -> },
                onReplaceRecommendedItem = { _, _ -> },
                isDeveloperMode = true,
                samplePresetIndex = samplePresetIndex,
                onCyclePreset = { samplePresetIndex = (samplePresetIndex + 1) % SampleTimetableData.presetCount }
            )
        }
        item {
            DevCitySection(
                cityPreset = cityPreset,
                showCityView = showCityView,
                showLabels = showLabels,
                showBadges = showBadges,
                bigBoat = bigBoat,
                onSelectPreset = { cityPreset = it },
                onToggleCityView = { showCityView = !showCityView },
                onToggleLabels   = { showLabels = !showLabels },
                onToggleBadges   = { showBadges = !showBadges },
                onToggleBigBoat  = { bigBoat = !bigBoat }
            )
        }
    }
}

@Composable
private fun DevCitySection(
    cityPreset: CityTimetablePreset,
    showCityView: Boolean,
    showLabels: Boolean,
    showBadges: Boolean,
    bigBoat: Boolean,
    onSelectPreset: (CityTimetablePreset) -> Unit,
    onToggleCityView: () -> Unit,
    onToggleLabels: () -> Unit,
    onToggleBadges: () -> Unit,
    onToggleBigBoat: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── 헤더 + 미리보기 토글 ────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "도시 타임테이블 샘플",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF5D4037),
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.material3.Switch(
                    checked = showCityView,
                    onCheckedChange = { onToggleCityView() },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedTrackColor = FlowPurple
                    )
                )
            }
            Text(
                text = "미리보기 ${if (showCityView) "ON" else "OFF"}",
                fontSize = 11.sp,
                color = Color(0xFF8D6E63),
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            // ── 샘플 프리셋 버튼 ────────────────────────────────────
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(CityTimetableSamples.presets.size) { idx ->
                    val preset   = CityTimetableSamples.presets[idx]
                    val selected = preset == cityPreset
                    Button(
                        onClick = { onSelectPreset(preset) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) FlowPurple else Color(0xFFFFE0B2),
                            contentColor   = if (selected) Color.White else Color(0xFFE65100)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(preset.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── 세부 옵션 칩 (라벨 / 배지 / 큰 보트) ────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.FilterChip(
                    selected = showLabels,
                    onClick = onToggleLabels,
                    label = { Text("라벨", fontSize = 11.sp) }
                )
                androidx.compose.material3.FilterChip(
                    selected = showBadges,
                    onClick = onToggleBadges,
                    label = { Text("카테고리 배지", fontSize = 11.sp) }
                )
                androidx.compose.material3.FilterChip(
                    selected = bigBoat,
                    onClick = onToggleBigBoat,
                    label = { Text("큰 보트", fontSize = 11.sp) }
                )
            }

            // ── 도시 시각화 (미리보기 ON일 때만) ────────────────────
            if (showCityView) {
                Spacer(modifier = Modifier.height(8.dp))
                CityTimetableCard(
                    activities = CityTimetableSamples.activitiesFor(cityPreset),
                    currentTimeMillis = null,  // 샘플 모드: 보트 스트립 중앙 고정
                    showLabels = showLabels,
                    showBadges = showBadges,
                    bigBoat = bigBoat
                )
            }
        }
    }
}

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
                        activities = uiState.allActivities
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
                incompleteTodos = if (isDeveloperMode) emptyList() else uiState.incompleteTodos,
                activeCategory = uiState.currentCategory.takeIf { uiState.isRunning },
                allActivities = if (isDeveloperMode) emptyList() else uiState.allActivities,
                timerStartMillis = if (!isDeveloperMode && uiState.isRunning) uiState.startTime else null,
                onSaveSleep = { start, end -> if (!isDeveloperMode) viewModel.saveSleepActivity(start, end) },
                onSkipToday = { if (!isDeveloperMode) viewModel.skipAutoButtonToday(it) },
                onUnskipToday = { if (!isDeveloperMode) viewModel.unskipAutoButtonToday(it) },
                onEditSchedule = { if (!isDeveloperMode) localAutoButtonManagerOpen = true },
                onManageSchedules = { if (!isDeveloperMode) localAutoButtonManagerOpen = true },
                onStartRecommended = { if (!isDeveloperMode) viewModel.startRecommendedTodoActivity(it) },
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

@Composable
private fun HomeHeader(
    dateText: String,
    isFocusFireActive: Boolean,
    actions: @Composable () -> Unit
) {
    val titleColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFire else FlowInk,
        animationSpec = tween(durationMillis = 420),
        label = "home-header-title-color"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, top = 28.dp, end = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Today's Flow",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = titleColor
            )
            Text(
                text = dateText,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        actions()
    }
}

@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun TodayFlowCard(
    isRunning: Boolean,
    currentCategory: String,
    startTime: Long,
    timerDisplayStateFlow: StateFlow<TimerDisplayState>,
    statusMessage: String?,
    appliedTitle: String,
    titleSuggestions: List<String>,
    mainButtonConfig: MainButtonConfig,
    onLongClickButton: (String) -> Unit,
    onPinQuickCategory: (String) -> Unit,
    pinnedQuickCategory: String?,
    onStop: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onApplyTitle: (String) -> Unit,
    onStart: (String) -> Unit,
    exerciseSets: List<ExerciseSetRecord> = emptyList(),
    exerciseMemo: String = "",
    onExerciseSetsChanged: (List<ExerciseSetRecord>) -> Unit = {},
    onExerciseMemoChanged: (String) -> Unit = {},
    isFocusModeActive: Boolean = false,
    focusModeEndsAtMillis: Long = 0L,
    onStartFocusMode: (enableDnd: Boolean) -> Unit = {},
    onStopFocusMode: () -> Unit = {},
    isFocusFireActive: Boolean = false,
    isRoutineActive: Boolean = false,
    routineGoalMillis: Long = 0L,
    isMainButtonReorderMode: Boolean = false,
    selectedMainButtonForSwapId: String? = null,
    onExitReorderMode: () -> Unit = {},
    onConfirmReorderMode: () -> Unit = {},
    onSelectButtonForSwap: (String) -> Unit = {},
    showMainButtonSetup: Boolean = false,
    onCompleteSetup: (List<String>) -> Unit = {}
) {
    val cardColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFireSurface else Color.White,
        animationSpec = tween(durationMillis = 420),
        label = "today-flow-card-color"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 9.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally(
                        animationSpec = tween(320),
                        initialOffsetX = { it }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(320),
                        targetOffsetX = { -it }
                    )
                } else {
                    slideInHorizontally(
                        animationSpec = tween(320),
                        initialOffsetX = { -it }
                    ) togetherWith slideOutHorizontally(
                        animationSpec = tween(320),
                        targetOffsetX = { it }
                    )
                }
            },
            label = "today-flow-page"
        ) {
            if (it) {
                val timerDisplayState by timerDisplayStateFlow.collectAsState()
                TimerPage(
                    currentCategory = currentCategory,
                    startTime = startTime,
                    elapsedTime = timerDisplayState.elapsedTime,
                    timerGoalMillis = timerDisplayState.timerGoalMillis,
                    initialAppliedTitle = appliedTitle,
                    titleSuggestions = titleSuggestions,
                    onStop = onStop,
                    onApplyTitle = onApplyTitle,
                    exerciseSets = exerciseSets,
                    exerciseMemo = exerciseMemo,
                    onExerciseSetsChanged = onExerciseSetsChanged,
                    onExerciseMemoChanged = onExerciseMemoChanged,
                    isFocusModeActive = isFocusModeActive,
                    focusModeEndsAtMillis = focusModeEndsAtMillis,
                    onStartFocusMode = onStartFocusMode,
                    onStopFocusMode = onStopFocusMode,
                    isFocusFireActive = isFocusFireActive,
                    isRoutineActive = isRoutineActive,
                    routineGoalMillis = routineGoalMillis
                )
            } else if (showMainButtonSetup) {
                MainButtonSetupPage(onComplete = onCompleteSetup)
            } else {
                FlowStartPage(
                    mainButtonConfig = mainButtonConfig,
                    onLongClickButton = onLongClickButton,
                    activeCategory = currentCategory.takeIf { isRunning },
                    onPinQuickCategory = onPinQuickCategory,
                    pinnedQuickCategory = pinnedQuickCategory,
                    statusMessage = statusMessage,
                    onStart = onStart,
                    isMainButtonReorderMode = isMainButtonReorderMode,
                    selectedMainButtonForSwapId = selectedMainButtonForSwapId,
                    onExitReorderMode = onExitReorderMode,
                    onConfirmReorderMode = onConfirmReorderMode,
                    onSelectButtonForSwap = onSelectButtonForSwap
                )
            }
        }
    }
}

@Composable
private fun TimerPage(
    currentCategory: String,
    startTime: Long,
    elapsedTime: Long,
    timerGoalMillis: Long,
    initialAppliedTitle: String,
    titleSuggestions: List<String>,
    onStop: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onApplyTitle: (String) -> Unit,
    exerciseSets: List<ExerciseSetRecord> = emptyList(),
    exerciseMemo: String = "",
    onExerciseSetsChanged: (List<ExerciseSetRecord>) -> Unit = {},
    onExerciseMemoChanged: (String) -> Unit = {},
    isFocusModeActive: Boolean = false,
    focusModeEndsAtMillis: Long = 0L,
    onStartFocusMode: (enableDnd: Boolean) -> Unit = {},
    onStopFocusMode: () -> Unit = {},
    isFocusFireActive: Boolean = false,
    isRoutineActive: Boolean = false,
    routineGoalMillis: Long = 0L
) {
    val titleState = remember(currentCategory, startTime) { mutableStateOf("") }
    val appliedTitleState = remember(currentCategory, startTime) { mutableStateOf(initialAppliedTitle) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val isFocusCategory = remember(currentCategory) {
        currentCategory in FOCUS_FIRE_CATEGORIES
    }
    val showFocusConfirmDialog = remember { mutableStateOf(false) }
    val showFocusStartedDialog = remember { mutableStateOf(false) }
    val showFocusStopConfirmDialog = remember { mutableStateOf(false) }
    val showDndPermissionDialog = remember { mutableStateOf(false) }
    val doNotShowAgain = remember { mutableStateOf(false) }
    val showExerciseAddSheet = remember { mutableStateOf(false) }
    val exerciseSheetName = remember { mutableStateOf("팔굽혀펴기") }
    val exercisePrefillRecord = remember { mutableStateOf<ExerciseSetRecord?>(null) }
    val editingExerciseSetIndex = remember { mutableStateOf<Int?>(null) }
    val activeTimedExerciseSet = remember { mutableStateOf<ExerciseTimedSetState?>(null) }
    val showExerciseSummaryDialog = remember { mutableStateOf(false) }
    // DND 체크박스 상태: 저장된 선호값으로 초기화
    val enableDnd = remember { mutableStateOf(FocusModeStore.getEnableSystemDndForFocus(context)) }
    // 시작됩니다 다이얼로그에서 DND 활성 여부 표시용
    val focusModeStartedWithDnd = remember { mutableStateOf(false) }

    activeTimedExerciseSet.value?.let { timedState ->
        LaunchedEffect(timedState.token) {
            val startDelay = (timedState.startsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            kotlinx.coroutines.delay(startDelay)
            KakaoStyleAlertPlayer.play(context)
            val endDelay = (timedState.endsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            kotlinx.coroutines.delay(endDelay)
            KakaoStyleAlertPlayer.play(context)
            if (activeTimedExerciseSet.value?.token == timedState.token) {
                activeTimedExerciseSet.value = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        TimerRingSection(
            currentCategory = currentCategory,
            elapsedTime = elapsedTime,
            timerGoalMillis = timerGoalMillis,
            routineGoalMillis = routineGoalMillis,
            isRoutineActive = isRoutineActive,
            isFocusFireActive = isFocusFireActive,
            appliedTitle = appliedTitleState.value
        )

        Spacer(modifier = Modifier.height(22.dp))
        if (currentCategory == "EXERCISE") {
            ExerciseSetControls(
                sets = exerciseSets,
                timedSetState = activeTimedExerciseSet.value,
                onAddSameExercise = {
                    val recentSet = exerciseSets.lastOrNull()
                    exercisePrefillRecord.value = recentSet
                    exerciseSheetName.value = recentSet?.name ?: appliedTitleState.value.ifBlank { "팔굽혀펴기" }
                    showExerciseAddSheet.value = true
                },
                onAddOtherExercise = {
                    exercisePrefillRecord.value = null
                    exerciseSheetName.value = "팔굽혀펴기"
                    showExerciseAddSheet.value = true
                },
                onEditSet = { index ->
                    editingExerciseSetIndex.value = index
                },
                onCompleteTimedSet = {
                    val timedState = activeTimedExerciseSet.value
                    if (timedState != null && timedState.setIndex in exerciseSets.indices) {
                        val plannedDuration = timedState.record.durationMillis ?: 0L
                        val actualDuration = (System.currentTimeMillis() - timedState.startsAtMillis)
                            .coerceAtLeast(0L)
                            .coerceAtMost(plannedDuration)
                        onExerciseSetsChanged(exerciseSets.toMutableList().also { sets ->
                            sets[timedState.setIndex] = sets[timedState.setIndex].copy(
                                durationMillis = actualDuration
                            )
                        })
                    }
                    activeTimedExerciseSet.value = null
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            TitleInputSection(
                titleSuggestions = titleSuggestions,
                title = titleState.value,
                isFocusFireActive = isFocusFireActive,
                focusManager = focusManager,
                onTitleChange = { titleState.value = it },
                onSuggestionSelected = { suggestion ->
                    titleState.value = suggestion
                    appliedTitleState.value = suggestion
                    onApplyTitle(suggestion)
                },
                onApply = {
                    val cleanTitle = titleState.value.trim()
                    titleState.value = cleanTitle
                    appliedTitleState.value = cleanTitle
                    onApplyTitle(cleanTitle)
                }
            )
        }
        if (currentCategory != "EXERCISE" && isFocusCategory) {
            Spacer(modifier = Modifier.height(10.dp))
            FocusBannerSection(
                isFocusModeActive = isFocusModeActive,
                focusModeEndsAtMillis = focusModeEndsAtMillis,
                isFocusFireActive = isFocusFireActive,
                onStart = {
                    if (FocusModeStore.isFocusConfirmAcknowledged(context)) {
                        val dndPref = FocusModeStore.getEnableSystemDndForFocus(context)
                        // 권한 만료 시 DND 없이 시작 (사용자 차단 방지)
                        val effectiveDnd = dndPref && FocusDndController.hasPolicyAccess(context)
                        focusModeStartedWithDnd.value = effectiveDnd
                        onStartFocusMode(effectiveDnd)
                        showFocusStartedDialog.value = true
                    } else {
                        enableDnd.value = FocusModeStore.getEnableSystemDndForFocus(context)
                        doNotShowAgain.value = false
                        showFocusConfirmDialog.value = true
                    }
                },
                onRequestStop = { showFocusStopConfirmDialog.value = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }
        StopActionSection(
            currentCategory = currentCategory,
            isFocusFireActive = isFocusFireActive,
            title = titleState.value,
            appliedTitle = appliedTitleState.value,
            onApplyTitle = onApplyTitle,
            onStop = onStop,
            onShowExerciseSummary = { showExerciseSummaryDialog.value = true }
        )
    }

    // ExerciseFinishDialog는 운동 종료 확인 중 실시간으로 늘어나는 총 시간을 보여줘야 해서
    // elapsedTime을 그대로 받는다 — TimerDialogsSection(elapsedTime 비의존)에는 포함하지 않음.
    if (showExerciseSummaryDialog.value) {
        ExerciseFinishDialog(
            sets = exerciseSets,
            elapsedTime = elapsedTime,
            memo = exerciseMemo,
            onMemoChange = { onExerciseMemoChanged(it) },
            onDismiss = { showExerciseSummaryDialog.value = false },
            onSave = {
                val finalTitle = exerciseSets.firstOrNull()?.name
                    ?: appliedTitleState.value.ifBlank { titleState.value }.ifBlank { "운동" }
                onApplyTitle(finalTitle)
                onStop(finalTitle, exerciseMemo.trim().ifBlank { null }, exerciseSets)
                showExerciseSummaryDialog.value = false
            }
        )
    }

    TimerDialogsSection(
        context = context,
        isFocusFireActive = isFocusFireActive,
        exerciseSets = exerciseSets,
        onExerciseSetsChanged = onExerciseSetsChanged,
        onApplyTitle = onApplyTitle,
        onStartFocusMode = onStartFocusMode,
        onStopFocusMode = onStopFocusMode,
        titleState = titleState,
        appliedTitleState = appliedTitleState,
        showExerciseAddSheetState = showExerciseAddSheet,
        editingExerciseSetIndexState = editingExerciseSetIndex,
        exerciseSheetNameState = exerciseSheetName,
        exercisePrefillRecordState = exercisePrefillRecord,
        activeTimedExerciseSetState = activeTimedExerciseSet,
        showFocusConfirmDialogState = showFocusConfirmDialog,
        showFocusStartedDialogState = showFocusStartedDialog,
        showFocusStopConfirmDialogState = showFocusStopConfirmDialog,
        showDndPermissionDialogState = showDndPermissionDialog,
        enableDndState = enableDnd,
        doNotShowAgainState = doNotShowAgain,
        focusModeStartedWithDndState = focusModeStartedWithDnd
    )
}

// elapsedTime이 실제로 필요한 유일한 섹션 — 진행률 링과 시간 텍스트만 담당.
// 타이머가 도는 동안 1초마다 재구성되는 범위를 이 섹션 하나로 한정한다.
@Composable
private fun TimerRingSection(
    currentCategory: String,
    elapsedTime: Long,
    timerGoalMillis: Long,
    routineGoalMillis: Long,
    isRoutineActive: Boolean,
    isFocusFireActive: Boolean,
    appliedTitle: String
) {
    val accentColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFire else FlowPurple,
        animationSpec = tween(durationMillis = 420),
        label = "timer-ring-accent-color"
    )
    val hasTimerGoal = timerGoalMillis > 0L
    val usesRoutineCycle = isRoutineActive && routineGoalMillis > 0L
    val progressCycleMillis = when {
        currentCategory == "EXPERIMENT_3" -> TimeUnit.SECONDS.toMillis(5)
        usesRoutineCycle -> routineGoalMillis
        else -> timerGoalMillis.coerceAtLeast(1L)
    }
    // TimerPage는 isRunning=true일 때만 보이므로 isFocusFireActive는 곧
    // "currentCategory in FOCUS_FIRE_CATEGORIES && isFocusModeActive"와 동치다.
    val progress = if (elapsedTime <= 0L) {
        0f
    } else if (!hasTimerGoal && !isFocusFireActive && !usesRoutineCycle) {
        0f
    } else if (isFocusFireActive) {
        ((elapsedTime % progressCycleMillis).toFloat() / progressCycleMillis.toFloat()).coerceAtLeast(0.01f)
    } else {
        (elapsedTime.toFloat() / progressCycleMillis.toFloat()).coerceIn(0f, 1f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "진행 중",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 14.dp)
            ) {
                Text(
                    text = displayCategory(currentCategory),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
                Spacer(Modifier.width(10.dp))
                CategoryGlyph(
                    category = currentCategory,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            val formattedTime = formatTime(elapsedTime)
            val timeFontSize = when {
                formattedTime.length <= 5 -> 34.sp   // MM:SS
                formattedTime.length <= 7 -> 26.sp   // H:MM:SS (1–9시간)
                else -> 22.sp                         // HH:MM:SS (10시간+)
            }
            Box(
                modifier = Modifier
                    .padding(top = 30.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = formattedTime,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = FlowInk,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
            if (appliedTitle.isNotBlank()) {
                Text(
                    text = appliedTitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        FlowProgressRing(
            progress = progress,
            isOnFire = isFocusFireActive,
            isRunning = true,
            modifier = Modifier.size(150.dp)
        )
    }
}

// elapsedTime을 받지 않음 — title/appliedTitle/titleSuggestions에만 의존.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleInputSection(
    titleSuggestions: List<String>,
    title: String,
    isFocusFireActive: Boolean,
    focusManager: FocusManager,
    onTitleChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onApply: () -> Unit
) {
    val accentColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFire else FlowPurple,
        animationSpec = tween(durationMillis = 420),
        label = "title-input-accent-color"
    )
    val accentSoftColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFireSoft else FlowPurpleSoft,
        animationSpec = tween(durationMillis = 420),
        label = "title-input-accent-soft-color"
    )
    if (titleSuggestions.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            titleSuggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionSelected(suggestion) },
                    label = {
                        Text(
                            text = suggestion,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = accentSoftColor,
                        labelColor = accentColor
                    ),
                    border = null
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .border(
                    1.dp,
                    if (isFocusFireActive) FocusFire.copy(alpha = 0.3f) else FlowDivider,
                    RoundedCornerShape(13.dp)
                )
                .background(
                    if (isFocusFireActive) FocusFireSoft.copy(alpha = 0.55f) else Color.White,
                    RoundedCornerShape(13.dp)
                )
                .padding(horizontal = 14.dp),
            singleLine = true,
            textStyle = TextStyle(
                color = FlowInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(accentColor),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.58f),
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (title.isEmpty()) {
                            Text(
                                text = "직접 입력",
                                color = FlowMuted.copy(alpha = 0.78f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        innerTextField()
                    }
                }
            }
        )
        Button(
            onClick = {
                onApply()
                focusManager.clearFocus()
            },
            modifier = Modifier
                .width(66.dp)
                .height(46.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            Text(
                text = "적용",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

// elapsedTime을 받지 않음 — focusModeEndsAtMillis 기준으로 자체 1초 tick을 갖는다.
@Composable
private fun FocusBannerSection(
    isFocusModeActive: Boolean,
    focusModeEndsAtMillis: Long,
    isFocusFireActive: Boolean,
    onStart: () -> Unit,
    onRequestStop: () -> Unit
) {
    val accentColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFire else FlowPurple,
        animationSpec = tween(durationMillis = 420),
        label = "focus-banner-accent-color"
    )
    val accentSoftColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFireSoft else FlowPurpleSoft,
        animationSpec = tween(durationMillis = 420),
        label = "focus-banner-accent-soft-color"
    )
    if (isFocusModeActive) {
        var remainingLabel by remember(focusModeEndsAtMillis) {
            mutableStateOf(formatCountdown((focusModeEndsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)))
        }
        LaunchedEffect(focusModeEndsAtMillis) {
            while (focusModeEndsAtMillis > System.currentTimeMillis()) {
                kotlinx.coroutines.delay(1_000L)
                remainingLabel = formatCountdown((focusModeEndsAtMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            }
            remainingLabel = formatCountdown(0L)
        }
        OutlinedButton(
            onClick = onRequestStop,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = accentSoftColor,
                contentColor = accentColor
            ),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "집중 중  ·  $remainingLabel  남음",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        OutlinedButton(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = accentColor
            ),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "집중하기 (${FocusModeStore.FOCUS_DURATION_LABEL})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// elapsedTime을 받지 않음 — currentCategory/title/appliedTitle만으로 종료 처리.
@Composable
private fun StopActionSection(
    currentCategory: String,
    isFocusFireActive: Boolean,
    title: String,
    appliedTitle: String,
    onApplyTitle: (String) -> Unit,
    onStop: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onShowExerciseSummary: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(if (isFocusFireActive) FocusFire.copy(alpha = 0.22f) else FlowDivider)
    )
    Button(
        onClick = {
            val finalTitle = appliedTitle.ifBlank { title }.trim()
            onApplyTitle(finalTitle)
            if (currentCategory == "EXERCISE") {
                onShowExerciseSummary()
            } else {
                onStop(finalTitle, null, emptyList())
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocusFireActive) FocusFireSoft.copy(alpha = 0.72f) else Color.Transparent,
            contentColor = if (isFocusFireActive) FocusFire else Color(0xFFFF4D5E)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        Text(
            text = if (currentCategory == "EXERCISE") "운동 종료하기" else "종료하기",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// elapsedTime을 받지 않음 — ExerciseAddSetSheet + 포커스/DND 다이얼로그 4종만 묶는다.
// (ExerciseFinishDialog는 elapsedTime이 꼭 필요해서 TimerPage에 별도로 남겨둠)
@Composable
private fun TimerDialogsSection(
    context: Context,
    isFocusFireActive: Boolean,
    exerciseSets: List<ExerciseSetRecord>,
    onExerciseSetsChanged: (List<ExerciseSetRecord>) -> Unit,
    onApplyTitle: (String) -> Unit,
    onStartFocusMode: (enableDnd: Boolean) -> Unit,
    onStopFocusMode: () -> Unit,
    titleState: MutableState<String>,
    appliedTitleState: MutableState<String>,
    showExerciseAddSheetState: MutableState<Boolean>,
    editingExerciseSetIndexState: MutableState<Int?>,
    exerciseSheetNameState: MutableState<String>,
    exercisePrefillRecordState: MutableState<ExerciseSetRecord?>,
    activeTimedExerciseSetState: MutableState<ExerciseTimedSetState?>,
    showFocusConfirmDialogState: MutableState<Boolean>,
    showFocusStartedDialogState: MutableState<Boolean>,
    showFocusStopConfirmDialogState: MutableState<Boolean>,
    showDndPermissionDialogState: MutableState<Boolean>,
    enableDndState: MutableState<Boolean>,
    doNotShowAgainState: MutableState<Boolean>,
    focusModeStartedWithDndState: MutableState<Boolean>
) {
    val accentColor by animateColorAsState(
        targetValue = if (isFocusFireActive) FocusFire else FlowPurple,
        animationSpec = tween(durationMillis = 420),
        label = "timer-dialogs-accent-color"
    )

    if (showExerciseAddSheetState.value || editingExerciseSetIndexState.value != null) {
        val editingIndex = editingExerciseSetIndexState.value
        val editingRecord = editingIndex?.let { exerciseSets.getOrNull(it) }
        ExerciseAddSetSheet(
            initialName = editingRecord?.name ?: exerciseSheetNameState.value,
            initialRecord = editingRecord,
            prefillRecord = if (editingRecord == null) exercisePrefillRecordState.value else null,
            onDismiss = {
                showExerciseAddSheetState.value = false
                editingExerciseSetIndexState.value = null
                exercisePrefillRecordState.value = null
            },
            onSave = { record ->
                val timedSetIndex = if (editingIndex != null && editingIndex in exerciseSets.indices) {
                    editingIndex
                } else {
                    exerciseSets.size
                }
                val updatedExerciseSets = if (editingIndex != null && editingIndex in exerciseSets.indices) {
                    exerciseSets.toMutableList().also { it[editingIndex] = record }
                } else {
                    exerciseSets + record
                }
                onExerciseSetsChanged(updatedExerciseSets)
                val nextTitle = when {
                    editingIndex == 0 -> record.name
                    updatedExerciseSets.isNotEmpty() -> updatedExerciseSets.first().name
                    else -> record.name
                }
                titleState.value = nextTitle
                appliedTitleState.value = nextTitle
                onApplyTitle(nextTitle)
                val isAddingNewSet = editingIndex == null || editingIndex !in exerciseSets.indices
                if (isAddingNewSet && record.mode == "TIME" && record.durationMillis != null) {
                    val startsAt = System.currentTimeMillis() + 5_000L
                    activeTimedExerciseSetState.value = ExerciseTimedSetState(
                        setIndex = timedSetIndex,
                        record = record,
                        startsAtMillis = startsAt,
                        endsAtMillis = startsAt + record.durationMillis
                    )
                }
                showExerciseAddSheetState.value = false
                editingExerciseSetIndexState.value = null
                exercisePrefillRecordState.value = null
            }
        )
    }

    if (showFocusConfirmDialogState.value) {
        AlertDialog(
            onDismissRequest = { showFocusConfirmDialogState.value = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "${FocusModeStore.FOCUS_DURATION_LABEL} 집중할까요?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FlowInk
                )
            },
            text = {
                Column {
                    Text(
                        text = "Flowlog의 알림 소리는 잠시 꺼지고,\n${FocusModeStore.FOCUS_DURATION_LABEL} 뒤에만 알림이 울려요.",
                        fontSize = 14.sp,
                        color = FlowMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // DND 체크박스
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = enableDndState.value,
                            onCheckedChange = { checked ->
                                if (checked && !FocusDndController.hasPolicyAccess(context)) {
                                    showDndPermissionDialogState.value = true
                                    // 권한 없으면 체크 반영 안 함
                                } else {
                                    enableDndState.value = checked
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = FlowMuted
                            )
                        )
                        Text(
                            text = "시스템 방해금지도 함께 켜기",
                            fontSize = 14.sp,
                            color = FlowInk,
                            modifier = Modifier.Companion.padding(start = 4.dp)
                        )
                    }
                    // 다시 보지 않기 체크박스
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = doNotShowAgainState.value,
                            onCheckedChange = { doNotShowAgainState.value = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = FlowMuted
                            )
                        )
                        Text(
                            text = "다시 보지 않기",
                            fontSize = 14.sp,
                            color = FlowInk,
                            modifier = Modifier.Companion.padding(start = 4.dp)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFocusConfirmDialogState.value = false }) {
                    Text("취소", color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (doNotShowAgainState.value) FocusModeStore.setFocusConfirmAcknowledged(context)
                        showFocusConfirmDialogState.value = false
                        focusModeStartedWithDndState.value = enableDndState.value
                        onStartFocusMode(enableDndState.value)
                        showFocusStartedDialogState.value = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("시작하기", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showFocusStartedDialogState.value) {
        AlertDialog(
            onDismissRequest = { showFocusStartedDialogState.value = false },
            containerColor = if (isFocusFireActive) FocusFireSurface else Color.White,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "집중 모드가 시작됩니다",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = FlowInk,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                val startedText = if (focusModeStartedWithDndState.value) {
                    "${FocusModeStore.FOCUS_DURATION_LABEL} 동안 알림음과 시스템 방해금지가 켜지고,\n${FocusModeStore.FOCUS_DURATION_LABEL} 뒤에 알람이 울립니다."
                } else {
                    "${FocusModeStore.FOCUS_DURATION_LABEL} 동안 알림음이 꺼지고,\n${FocusModeStore.FOCUS_DURATION_LABEL} 뒤에 알람이 울립니다."
                }
                Text(
                    text = startedText,
                    fontSize = 14.sp,
                    color = FlowMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showFocusStartedDialogState.value = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("확인", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showFocusStopConfirmDialogState.value) {
        AlertDialog(
            onDismissRequest = { showFocusStopConfirmDialogState.value = false },
            containerColor = if (isFocusFireActive) FocusFireSurface else Color.White,
            title = {
                Text(
                    text = "집중 모드를 종료할까요?",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FlowInk
                )
            },
            text = {
                Text(
                    text = "집중 모드를 종료하면 알림음이 다시 켜집니다.",
                    fontSize = 14.sp,
                    color = FlowMuted
                )
            },
            dismissButton = {
                TextButton(onClick = { showFocusStopConfirmDialogState.value = false }) {
                    Text("취소", color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStopFocusMode()
                        showFocusStopConfirmDialogState.value = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("종료", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showDndPermissionDialogState.value) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialogState.value = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "방해금지 권한이 필요해요",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FlowInk
                )
            },
            text = {
                Text(
                    text = "시스템 방해금지를 함께 켜려면\n'방해금지 액세스' 권한이 필요해요.\n설정에서 Flowlog를 허용해 주세요.",
                    fontSize = 14.sp,
                    color = FlowMuted
                )
            },
            dismissButton = {
                TextButton(onClick = { showDndPermissionDialogState.value = false }) {
                    Text("나중에", color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDndPermissionDialogState.value = false
                        FocusDndController.openPolicyAccessSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("설정 열기", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}

@Composable
private fun ExerciseSetControls(
    sets: List<ExerciseSetRecord>,
    timedSetState: ExerciseTimedSetState?,
    onAddSameExercise: () -> Unit,
    onAddOtherExercise: () -> Unit,
    onEditSet: (Int) -> Unit,
    onCompleteTimedSet: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        timedSetState?.let {
            ExerciseTimedSetCard(
                state = it,
                onComplete = onCompleteTimedSet
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(
            text = "최근 기록",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowInk
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (sets.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlowPurpleSoft.copy(alpha = 0.52f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sets.forEachIndexed { index, set ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = set.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FlowInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${index + 1}세트 · ${formatExerciseSetValue(set)} · ${set.intensity}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = FlowInk.copy(alpha = 0.62f),
                                modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                        IconButton(
                            onClick = { onEditSet(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "세트 수정",
                                tint = FlowPurple,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onAddSameExercise,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, FlowPurple),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = FlowPurple
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("같은 운동 세트 추가", fontWeight = FontWeight.ExtraBold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onAddOtherExercise,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, FlowDivider),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = FlowPurple
            )
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("다른 운동 추가", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun ExerciseTimedSetCard(
    state: ExerciseTimedSetState,
    onComplete: () -> Unit
) {
    var now by remember(state.token) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.token) {
        while (now < state.endsAtMillis + 1_000L) {
            kotlinx.coroutines.delay(250L)
            now = System.currentTimeMillis()
        }
    }
    val remainingToStart = (state.startsAtMillis - now).coerceAtLeast(0L)
    val elapsedAfterStart = (now - state.startsAtMillis).coerceAtLeast(0L)
    val duration = (state.endsAtMillis - state.startsAtMillis).coerceAtLeast(1L)
    val progress = if (remainingToStart > 0L) 0f else (elapsedAfterStart.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    val label = when {
        remainingToStart > 0L -> "${formatExerciseSetTime(remainingToStart)} 뒤 시작"
        now < state.endsAtMillis -> "${formatExerciseSetTime(state.endsAtMillis - now)} 남음"
        else -> "시간 완료"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlowPurpleSoft.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowProgressRing(
            progress = progress,
            isOnFire = false,
            isRunning = false,
            showCenterLabel = false,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(state.record.name, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = FlowPurple, modifier = Modifier.padding(top = 3.dp))
        }
        TextButton(onClick = onComplete) {
            Text("완료", color = FlowPurple, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseAddSetSheet(
    initialName: String,
    initialRecord: ExerciseSetRecord? = null,
    prefillRecord: ExerciseSetRecord? = null,
    onDismiss: () -> Unit,
    onSave: (ExerciseSetRecord) -> Unit
) {
    val context = LocalContext.current
    val seededExercise = initialRecord?.name
        ?: prefillRecord?.name
        ?: initialName.ifBlank { "팔굽혀펴기" }
    var selectedExercise by remember(initialName, initialRecord, prefillRecord) {
        mutableStateOf(seededExercise)
    }
    val isNewSet = initialRecord == null && prefillRecord == null
    var reps by remember(initialRecord, prefillRecord) {
        val saved = if (isNewSet) ExerciseOptionsStore.loadLastSettings(context, seededExercise) else null
        mutableStateOf(initialRecord?.reps ?: prefillRecord?.reps ?: saved?.reps ?: 12)
    }
    var recordMode by remember(initialRecord, prefillRecord) {
        val saved = if (isNewSet) ExerciseOptionsStore.loadLastSettings(context, seededExercise) else null
        mutableStateOf(initialRecord?.mode ?: prefillRecord?.mode ?: saved?.mode ?: "COUNT")
    }
    var durationMillis by remember(initialRecord, prefillRecord) {
        val saved = if (isNewSet) ExerciseOptionsStore.loadLastSettings(context, seededExercise) else null
        mutableStateOf(initialRecord?.durationMillis ?: prefillRecord?.durationMillis ?: saved?.durationMillis ?: 40_000L)
    }
    var intensity by remember(initialRecord, prefillRecord) {
        val saved = if (isNewSet) ExerciseOptionsStore.loadLastSettings(context, seededExercise) else null
        mutableStateOf(initialRecord?.intensity ?: prefillRecord?.intensity ?: saved?.intensity ?: "힘듦")
    }
    val defaultExerciseOptions = remember { listOf("팔굽혀펴기", "스쿼트", "플랭크") }
    var exerciseOptions by remember(initialName, initialRecord, prefillRecord) {
        val custom = ExerciseOptionsStore.loadCustomExercises(context)
        mutableStateOf((defaultExerciseOptions + custom + seededExercise).distinct())
    }
    var customExercise by remember { mutableStateOf("") }
    var showExerciseAddCard by remember { mutableStateOf(false) }
    var showMoreDropdown by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingExerciseItem by remember { mutableStateOf<String?>(null) }
    var editingExerciseNewName by remember { mutableStateOf("") }

    // 운동 칩 변경 시 해당 운동의 마지막 설정으로 폼 업데이트 (새 세트 추가 시에만)
    LaunchedEffect(selectedExercise) {
        if (isNewSet) {
            val saved = ExerciseOptionsStore.loadLastSettings(context, selectedExercise)
            if (saved != null) {
                recordMode = saved.mode
                durationMillis = saved.durationMillis
                reps = saved.reps
                intensity = saved.intensity
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (initialRecord == null) "세트 추가" else "세트 수정",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            // 기본 3개는 항상 메인 행, 커스텀은 항상 더보기에만
            val customExercises = exerciseOptions.filter { it !in defaultExerciseOptions }
            val isCustomSelected = selectedExercise !in defaultExerciseOptions

            // 운동 레이블 + 추가 / 수정 / 더보기 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("운동", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "추가",
                    color = FlowPurpleDeep,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { showExerciseAddCard = !showExerciseAddCard }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                if (customExercises.isNotEmpty()) {
                    Text(
                        "수정",
                        color = FlowPurpleDeep,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable {
                                editingExerciseItem = null
                                editingExerciseNewName = ""
                                showEditDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (customExercises.isNotEmpty()) {
                    Box {
                        // 커스텀 운동 선택 중이면 버튼에 이름 표시
                        val moreLabel = if (isCustomSelected) selectedExercise else "더보기"
                        Text(
                            moreLabel,
                            color = if (isCustomSelected) FlowPurpleDeep else FlowPurpleDeep,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clickable { showMoreDropdown = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        DropdownMenu(
                            expanded = showMoreDropdown,
                            onDismissRequest = { showMoreDropdown = false },
                            containerColor = Color.White
                        ) {
                            customExercises.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                option,
                                                fontWeight = if (option == selectedExercise) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = if (option == selectedExercise) FlowPurpleDeep else FlowInk,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (option == selectedExercise) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = null,
                                                    tint = FlowPurpleDeep,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedExercise = option
                                        showMoreDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(defaultExerciseOptions) { option ->
                    FilterChip(
                        selected = selectedExercise == option,
                        onClick = { selectedExercise = option },
                        label = { Text(option, fontWeight = FontWeight.Bold) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (showExerciseAddCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F6FC), RoundedCornerShape(12.dp))
                        .border(1.dp, FlowDivider, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customExercise,
                        onValueChange = { customExercise = it },
                        placeholder = { Text("운동 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = FlowInk,
                            unfocusedTextColor = FlowInk,
                            cursorColor = FlowPurple
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                customExercise = ""
                                showExerciseAddCard = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("취소", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val cleanExercise = customExercise.trim()
                                if (cleanExercise.isNotBlank()) {
                                    exerciseOptions = (exerciseOptions + cleanExercise).distinct()
                                    selectedExercise = cleanExercise
                                    ExerciseOptionsStore.addCustomExercise(context, cleanExercise)
                                    customExercise = ""
                                    showExerciseAddCard = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlowPurple,
                                contentColor = Color.White
                            )
                        ) {
                            Text("추가", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 수정 다이얼로그 — 커스텀 운동 이름 변경 / 삭제
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false; editingExerciseItem = null },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    title = { Text("운동 수정", fontWeight = FontWeight.ExtraBold, color = FlowInk) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            customExercises.forEach { option ->
                                if (editingExerciseItem == option) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editingExerciseNewName,
                                            onValueChange = { editingExerciseNewName = it },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = FlowInk,
                                                unfocusedTextColor = FlowInk,
                                                cursorColor = FlowPurple
                                            )
                                        )
                                        IconButton(onClick = {
                                            val newName = editingExerciseNewName.trim()
                                            if (newName.isNotBlank() && newName != option) {
                                                exerciseOptions = exerciseOptions.map { if (it == option) newName else it }
                                                ExerciseOptionsStore.removeCustomExercise(context, option)
                                                ExerciseOptionsStore.addCustomExercise(context, newName)
                                                if (selectedExercise == option) selectedExercise = newName
                                            }
                                            editingExerciseItem = null
                                        }) {
                                            Icon(Icons.Filled.Close, contentDescription = "완료", tint = FlowPurple)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            option,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold,
                                            color = FlowInk,
                                            fontSize = 14.sp
                                        )
                                        IconButton(onClick = {
                                            editingExerciseItem = option
                                            editingExerciseNewName = option
                                        }) {
                                            Icon(Icons.Filled.Edit, contentDescription = "이름 변경", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = {
                                            exerciseOptions = exerciseOptions - option
                                            ExerciseOptionsStore.removeCustomExercise(context, option)
                                            if (selectedExercise == option) selectedExercise = defaultExerciseOptions.first()
                                            if (customExercises.size == 1) showEditDialog = false
                                        }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "삭제", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEditDialog = false; editingExerciseItem = null }) {
                            Text("완료", color = FlowPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("기록 방식", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { recordMode = "COUNT" },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recordMode == "COUNT") FlowPurple else Color(0xFFF1F0F7),
                        contentColor = if (recordMode == "COUNT") Color.White else FlowInk
                    )
                ) {
                    Text("개수 (반복)", fontWeight = FontWeight.ExtraBold)
                }
                Button(
                    onClick = { recordMode = "TIME" },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (recordMode == "TIME") FlowPurple else Color(0xFFF1F0F7),
                        contentColor = if (recordMode == "TIME") Color.White else FlowInk
                    )
                ) {
                    Text("시간", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (recordMode == "TIME") {
                Text("시간", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExerciseStepButton(label = "-", onClick = { durationMillis = (durationMillis - 5_000L).coerceAtLeast(5_000L) })
                    Text(formatExerciseSetTime(durationMillis), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                    ExerciseStepButton(label = "+", onClick = { durationMillis += 5_000L })
                }
            } else {
                Text("개수", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExerciseStepButton(label = "-", onClick = { reps = (reps - 1).coerceAtLeast(1) })
                    Text(reps.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                    ExerciseStepButton(label = "+", onClick = { reps += 1 })
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("강도 (RPE 느낌)", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("가벼움", "보통", "힘듦").forEach { option ->
                    IntensityChip(
                        label = option,
                        selected = intensity == option,
                        onClick = { intensity = option },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(22.dp))
            Button(
                onClick = {
                    val name = selectedExercise.trim().ifBlank { "운동" }
                    ExerciseOptionsStore.saveLastSettings(
                        context, name,
                        ExerciseLastSettings(
                            mode = recordMode,
                            durationMillis = durationMillis,
                            reps = reps,
                            intensity = intensity
                        )
                    )
                    onSave(
                        ExerciseSetRecord(
                            name = name,
                            reps = reps,
                            intensity = intensity,
                            mode = recordMode,
                            durationMillis = if (recordMode == "TIME") durationMillis else null
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPurple, contentColor = Color.White)
            ) {
                Text(if (initialRecord == null) "저장하기" else "수정하기", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ExerciseStepButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = FlowPurpleSoft,
            contentColor = FlowPurple
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun IntensityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) FlowPurple else Color(0xFFF1F0F7),
            contentColor = if (selected) Color.White else FlowInk.copy(alpha = 0.7f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun ExerciseFinishDialog(
    sets: List<ExerciseSetRecord>,
    elapsedTime: Long,
    memo: String,
    onMemoChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("운동 완료!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
        },
        text = {
            Column {
                Text(
                    text = "총 ${formatDuration(elapsedTime)} · ${sets.size}세트",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Spacer(modifier = Modifier.height(14.dp))
                sets.groupBy { it.name }.forEach { (name, records) ->
                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                    Spacer(modifier = Modifier.height(6.dp))
                    records.forEachIndexed { index, record ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${index + 1}세트",
                                fontSize = 12.sp,
                                color = FlowMuted,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatExerciseSetValue(record),
                                fontSize = 12.sp,
                                color = FlowInk,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(record.intensity, fontSize = 12.sp, color = FlowPurple, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = memo,
                    onValueChange = onMemoChange,
                    placeholder = { Text("오늘 운동 느낌이나 메모를 남겨보세요") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FlowInk,
                        unfocusedTextColor = FlowInk,
                        cursorColor = FlowPurple
                    )
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("취소", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPurple, contentColor = Color.White)
            ) {
                Text("저장하기", fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%d:%02d:%02d".format(h, m, s)
}

private fun formatExerciseSetValue(record: ExerciseSetRecord): String {
    return if (record.mode == "TIME") {
        formatExerciseSetTime(record.durationMillis ?: 0L)
    } else {
        "${record.reps}개"
    }
}

private fun formatExerciseSetTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun buildTitleSuggestions(
    category: String,
    activities: List<ActivitySession>
): List<String> {
    val defaultTitle = defaultActivityTitle(category)
    return activities
        .filter { it.category == category && it.title.isNotBlank() && it.title != defaultTitle }
        .groupBy { it.title.trim() }
        .map { (title, sessions) -> Triple(title, sessions.size, sessions.maxOf { it.startTime }) }
        .sortedWith(compareByDescending<Triple<String, Int, Long>> { it.second }.thenByDescending { it.third })
        .map { it.first }
        .take(5)
}

private fun defaultActivityTitle(category: String): String {
    return when (category) {
        "MEAL" -> "식사"
        "EXERCISE" -> "운동"
        "SLEEP" -> "수면"
        "STUDY" -> "공부"
        "WORK" -> "업무"
        "COMPANY" -> "회사"
        "DEVELOPMENT" -> "개발"
        "READING" -> "독서"
        "MOVE" -> "이동"
        "WASH" -> "씻기"
        "REST" -> "휴식"
        "SCHOOL" -> "학교"
        else -> "활동"
    }
}

@Composable
private fun MainButtonConflictDialog(
    localConfig: MainButtonConfig,
    remoteConfig: MainButtonConfig,
    onUseLocal: () -> Unit,
    onUseRemote: () -> Unit,
    onSetupNew: () -> Unit
) {
    val isOrderOnlyConflict = remember(localConfig, remoteConfig) {
        localConfig.buttons.map { it.category }.toSet() ==
            remoteConfig.buttons.map { it.category }.toSet()
    }
    val subtitle = if (isOrderOnlyConflict) {
        "이 기기와 계정의 버튼 순서가 달라요.\n어느 쪽 순서를 사용할까요?"
    } else {
        "이 기기와 계정의 버튼 설정이 달라요.\n어느 쪽을 사용할까요?"
    }
    val localLabel = if (isOrderOnlyConflict) "현재 기기 순서 사용" else "현재 기기 설정 사용"
    val remoteLabel = if (isOrderOnlyConflict) "계정 순서 사용" else "계정 설정 사용"

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "메인 버튼 설정이 달라요",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = FlowMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ConflictConfigPreview(
                        label = "현재 기기",
                        config = localConfig,
                        modifier = Modifier.weight(1f)
                    )
                    ConflictConfigPreview(
                        label = "계정",
                        config = remoteConfig,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onUseLocal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = localLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUseRemote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, FlowPurple)
                ) {
                    Text(
                        text = remoteLabel,
                        color = FlowPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onSetupNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "직접 다시 고르기",
                        color = FlowMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictConfigPreview(
    label: String,
    config: MainButtonConfig,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF4F4F8), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        config.buttons.sortedBy { it.order }.forEach { btn ->
            Text(
                text = displayCategory(btn.category),
                fontSize = 12.sp,
                color = FlowInk,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun MainButtonSetupPage(
    onComplete: (List<String>) -> Unit
) {
    val initialRecommended = remember {
        linkedSetOf("STUDY", "REST", "EXERCISE", "MEAL", "SLEEP", "ETC")
    }
    var selected by remember { mutableStateOf<Set<String>>(initialRecommended) }
    var showMaxWarning by remember { mutableStateOf(false) }

    val count = selected.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "메인 버튼을 설정해 주세요",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowInk
        )
        Text(
            text = "자주 쓰는 활동을 추려봤어요. 필요하면 바꿀 수 있어요.\n나중에 Flowlog AI가 더 맞는 버튼을 추천해 드려요.",
            fontSize = 13.sp,
            color = FlowMuted,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Text(
            text = "${count}개 선택됨  •  최소 ${MainButtonConfig.MIN_BUTTONS}개, 최대 ${MainButtonConfig.MAX_BUTTONS}개",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                count < MainButtonConfig.MIN_BUTTONS -> Color(0xFFE53935)
                count >= MainButtonConfig.MAX_BUTTONS -> FlowPurple
                else -> FlowMuted
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val allCategories = MainButtonConfig.ALL_SELECTABLE_CATEGORIES
        val rowCount = (allCategories.size + 1) / 2
        val gridHeight = (rowCount * 84 + (rowCount - 1) * 12 + 8).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(items = allCategories, key = { it }) { category ->
                val isSelected = category in selected
                CategoryButton(
                    category = category,
                    label = displayCategory(category),
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            selected = selected - category
                            showMaxWarning = false
                        } else {
                            if (selected.size >= MainButtonConfig.MAX_BUTTONS) {
                                showMaxWarning = true
                            } else {
                                selected = selected + category
                                showMaxWarning = false
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(visible = showMaxWarning) {
            Text(
                text = "메인 버튼은 최대 ${MainButtonConfig.MAX_BUTTONS}개까지 둘 수 있어요.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { onComplete(selected.toList()) },
            enabled = count >= MainButtonConfig.MIN_BUTTONS,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FlowPurple,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFDED9F5),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "이대로 시작하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FlowStartPage(
    mainButtonConfig: MainButtonConfig,
    onLongClickButton: (String) -> Unit,
    activeCategory: String?,
    onPinQuickCategory: (String) -> Unit,
    pinnedQuickCategory: String?,
    statusMessage: String?,
    onStart: (String) -> Unit,
    isMainButtonReorderMode: Boolean = false,
    selectedMainButtonForSwapId: String? = null,
    onExitReorderMode: () -> Unit = {},
    onConfirmReorderMode: () -> Unit = {},
    onSelectButtonForSwap: (String) -> Unit = {}
) {
    val displayCategories = remember(mainButtonConfig) {
        mainButtonConfig.buttons.sortedBy { it.order }.map { it.category }
    }

    val visibleCategories = remember(displayCategories, activeCategory, pinnedQuickCategory) {
        val pinIsSchoolOrCompany = pinnedQuickCategory == "SCHOOL" || pinnedQuickCategory == "COMPANY"
        displayCategories.filterNot { category ->
            category == activeCategory || (pinIsSchoolOrCompany && category == pinnedQuickCategory)
        }
    }

    val gridCategories = if (isMainButtonReorderMode) displayCategories else visibleCategories
    val rowCount = (gridCategories.size + 1) / 2
    val gridHeight = (rowCount * 84 + (rowCount - 1) * 12 + 8).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMainButtonReorderMode) "자리 바꾸기" else "활동 시작",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Text(
                    text = statusMessage ?: "기록할 활동을 선택하세요.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (isMainButtonReorderMode) {
                TextButton(onClick = onConfirmReorderMode) {
                    Text(
                        text = "완료",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowPurple
                    )
                }
            } else {
                FlowPageDots(activePage = 1)
            }
        }

        Text(
            text = "활동",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(
                items = gridCategories,
                key = { category -> category }
            ) { category ->
                if (isMainButtonReorderMode) {
                    CategoryButton(
                        category = category,
                        label = displayCategory(category),
                        isSelected = category == selectedMainButtonForSwapId,
                        onClick = { onSelectButtonForSwap(category) },
                        onLongClick = null,
                        modifier = Modifier.animateItem()
                    )
                } else {
                    CategoryButton(
                        category = category,
                        label = displayCategory(category),
                        onClick = {
                            when {
                                category == "SCHOOL" || category == "COMPANY" -> {
                                    if (pinnedQuickCategory != category) {
                                        onPinQuickCategory(category)
                                    }
                                }
                                category != activeCategory -> onStart(category)
                            }
                        },
                        onLongClick = { onLongClickButton(category) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationBannerCard(
    category: String,
    activityName: String,
    reasonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF00BCD4)
    val cardBg = Color(0xFFE2F5F5)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = category,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "추천 · $reasonText",
                    fontSize = 11.sp,
                    color = FlowMuted
                )
            }
        }
    }
}

@Composable
private fun FlowPageDots(activePage: Int) {
    Row(
        modifier = Modifier.padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index == activePage) FlowPurple else Color(0xFFC8CBD4),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ActivityRecommendationRow(
    category: String,
    activityName: String,
    isEnabled: Boolean,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isEnabled || isCompleted) FlowPurpleSoft.copy(alpha = 0.6f)
                else Color(0xFFF1F1F4)
            )
            .clickable(enabled = isEnabled || isCompleted, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF18A058) else if (isEnabled) FlowPurple else FlowMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "추천 흐름",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted
            )
            Text(
                text = when {
                    isCompleted -> "$activityName · 완료"
                    !isEnabled -> "$activityName · 예습 후 시작"
                    else -> activityName
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isEnabled || isCompleted) FlowInk else FlowMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FlowMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityRecommendationSheet(
    category: String,
    activityName: String,
    reasonText: String?,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    isEnabled: Boolean = true,
    isCompleted: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(categoryColor(category).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = category,
                    tint = categoryColor(category),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = activityName,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            if (!reasonText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reasonText,
                    fontSize = 14.sp,
                    color = FlowMuted,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onStart,
                enabled = isEnabled && !isCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isCompleted) "완료됨" else "시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onComplete,
                enabled = isEnabled && !isCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isCompleted) "완료됨" else "완료로 표시",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted
                )
            }
        }
    }
}

@Composable
private fun QuickTimerSection(
    categories: List<String>,
    isFocusFireActive: Boolean,
    pinnedCategory: String?,
    onUnpinCategory: () -> Unit,
    isBrushTimerRunning: Boolean,
    brushDoneEndsAtMillis: Long,
    snackButtonEndsAtMillis: Long,
    onStart: (String) -> Unit,
    onToggleBrushTimer: () -> Unit,
    onToggleSnackTimer: () -> Unit
) {
    val context = LocalContext.current
    val quickTimerPrefs = remember(context) {
        context.getSharedPreferences(PREFS_QUICK_TIMER_CONFIG, Context.MODE_PRIVATE)
    }
    var isQuickTimerSwapped by remember {
        mutableStateOf(quickTimerPrefs.getBoolean(KEY_QUICK_TIMER_SWAPPED, false))
    }
    var showQuickTimerControls by remember { mutableStateOf(false) }
    val lastQuickClickTimes = remember { mutableMapOf<String, Long>() }

    val quickCategories = remember(
        categories,
        isQuickTimerSwapped
    ) {
        val base = categories.filter { it == "TOOTHBRUSH" || it == "SNACK" }
        if (isQuickTimerSwapped) base.reversed() else base
    }

    var brushLabel by remember(brushDoneEndsAtMillis) {
        mutableStateOf(formatBrushCountdown(brushDoneEndsAtMillis))
    }
    var snackLabel by remember(snackButtonEndsAtMillis) {
        mutableStateOf(formatSnackCountdown(snackButtonEndsAtMillis))
    }

    androidx.compose.runtime.LaunchedEffect(brushDoneEndsAtMillis) {
        while (brushDoneEndsAtMillis > System.currentTimeMillis()) {
            kotlinx.coroutines.delay(500L)
            brushLabel = formatBrushCountdown(brushDoneEndsAtMillis)
        }
        brushLabel = "양치"
    }
    androidx.compose.runtime.LaunchedEffect(snackButtonEndsAtMillis) {
        while (snackButtonEndsAtMillis > System.currentTimeMillis()) {
            kotlinx.coroutines.delay(10_000L)
            snackLabel = formatSnackCountdown(snackButtonEndsAtMillis)
        }
        snackLabel = "30분"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        pinnedCategory?.takeIf { it == "SCHOOL" || it == "COMPANY" }?.let { category ->
            CategoryButton(
                category = category,
                isSelected = true,
                label = displayCategory(category),
                onClick = onUnpinCategory,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Text(
            text = "빠른 타이머",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFocusFireActive) FocusFire else FlowMuted,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            quickCategories.forEach { category ->
                CategoryButton(
                    category = category,
                    isSelected = (category == "TOOTHBRUSH" && isBrushTimerRunning)
                        || (category == "SNACK" && snackButtonEndsAtMillis > 0L),
                    label = when (category) {
                        "TOOTHBRUSH" -> brushLabel
                        "SNACK" -> displayCategory(category)
                        else -> displayCategory(category)
                    },
                    subLabel = when (category) {
                        "TOOTHBRUSH" -> "3분"
                        "SNACK" -> snackLabel
                        else -> null
                    },
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - (lastQuickClickTimes[category] ?: 0L) > 1500L) {
                            lastQuickClickTimes[category] = now
                            onStart(category)
                        }
                    },
                    onLongClick = { showQuickTimerControls = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        AnimatedVisibility(visible = showQuickTimerControls) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    OutlinedButton(
                        onClick = onToggleBrushTimer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, FlowDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowInk)
                    ) {
                        Text(if (isBrushTimerRunning || brushDoneEndsAtMillis > 0L) "양치 타이머 끄기" else "양치 타이머 켜기")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = onToggleSnackTimer,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, FlowDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowInk)
                    ) {
                        Text(if (snackButtonEndsAtMillis > 0L) "간식 타이머 끄기" else "간식 타이머 켜기")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            isQuickTimerSwapped = !isQuickTimerSwapped
                            quickTimerPrefs.edit()
                                .putBoolean(KEY_QUICK_TIMER_SWAPPED, isQuickTimerSwapped)
                                .apply()
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, FlowDivider),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlowInk)
                    ) {
                        Text("순서 바꾸기")
                    }
                }
                item {
                    TextButton(onClick = { showQuickTimerControls = false }) {
                        Text(
                            text = "닫기",
                            color = FlowMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

private const val PREFS_QUICK_TIMER_CONFIG = "quick_timer_config"
private const val KEY_QUICK_TIMER_SWAPPED = "swapped"

private fun formatBrushCountdown(endsAtMillis: Long): String {
    val remaining = endsAtMillis - System.currentTimeMillis()
    if (remaining <= 0L) return "양치"
    val totalSeconds = remaining / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSnackCountdown(endsAtMillis: Long): String {
    val remaining = endsAtMillis - System.currentTimeMillis()
    if (remaining <= 0L) return "30분"
    val minutes = ((remaining + 59_000L) / 60_000L).coerceAtLeast(1L)
    return "${minutes}분"
}

@Composable
private fun ExperimentSection(
    categories: List<String>,
    onStart: (String) -> Unit
) {
    val experimentCategories = remember(categories) {
        categories.filter { it == "EXPERIMENT_1" || it == "EXPERIMENT_2" || it == "EXPERIMENT_3" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text = "실험",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            experimentCategories.take(2).forEach { category ->
                CategoryButton(
                    category = category,
                    onClick = { onStart(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        experimentCategories.drop(2).forEach { category ->
            CategoryButton(
                category = category,
                onClick = { onStart(category) },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun FlowProgressRing(
    progress: Float,
    isOnFire: Boolean,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    showCenterLabel: Boolean = true
) {
    val firePhase: Float
    val firePulse: Float
    if (isOnFire) {
        val fireTransition = rememberInfiniteTransition(label = "flow-fire")
        val animatedFirePhase by fireTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 920),
                repeatMode = RepeatMode.Restart
            ),
            label = "flow-fire-phase"
        )
        val animatedFirePulse by fireTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 720),
                repeatMode = RepeatMode.Reverse
            ),
            label = "flow-fire-pulse"
        )
        firePhase = animatedFirePhase
        firePulse = animatedFirePulse
    } else {
        firePhase = 0f
        firePulse = 1f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            val diameter = size.minDimension - stroke.width
            val topLeft = Offset(stroke.width / 2f, stroke.width / 2f)
            val arcSize = Size(diameter, diameter)
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = diameter / 2f
            val sweepAngle = 360f * progress.coerceIn(0f, 1f)
            val startAngle = -90f
            val headAngle = startAngle + sweepAngle
            val isComplete = !isOnFire && progress >= 1f
            val isFireComplete = isOnFire && progress >= 0.98f

            drawArc(
                color = Color(0xFFE9E9F1),
                startAngle = startAngle,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            if (isOnFire) {
                drawFireRingGlow(
                    topLeft = topLeft,
                    arcSize = arcSize,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    pulse = firePulse
                )
            }
            drawArc(
                color = when {
                    isFireComplete -> Color(0xFF00D97E)
                    isComplete -> Color(0xFFFFCC00)
                    isOnFire -> Color(0xFFFF7A2F)
                    else -> FlowPurple
                },
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = when {
                    isFireComplete -> Color(0xFF80FFD0)
                    isComplete -> Color(0xFFFFEE80)
                    isOnFire -> Color(0xFFFFE18A)
                    else -> FlowPurple.copy(alpha = 0.38f)
                },
                startAngle = headAngle - 12f,
                sweepAngle = 18f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            if (isOnFire) {
                drawFireHead(
                    center = center,
                    radius = radius,
                    headAngleDegrees = headAngle,
                    phase = firePhase,
                    pulse = firePulse
                )
            }
        }
        if (isRunning) {
            Icon(
                imageVector = Icons.Filled.Pause,
                contentDescription = null,
                tint = if (isOnFire) FocusFire else FlowPurple,
                modifier = Modifier.size(58.dp)
            )
        } else if (showCenterLabel) {
            Text(
                text = "Flow",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowPurple
            )
        }
    }
}

private fun DrawScope.drawFireRingGlow(
    topLeft: Offset,
    arcSize: Size,
    startAngle: Float,
    sweepAngle: Float,
    pulse: Float
) {
    drawArc(
        color = Color(0xFFFFA646).copy(alpha = 0.14f * pulse),
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Round)
    )
    drawArc(
        color = Color(0xFFFFD173).copy(alpha = 0.20f * pulse),
        startAngle = startAngle + (sweepAngle - 72f).coerceAtLeast(0f),
        sweepAngle = sweepAngle.coerceAtMost(72f),
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = 22.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawFireHead(
    center: Offset,
    radius: Float,
    headAngleDegrees: Float,
    phase: Float,
    pulse: Float
) {
    val headAngle = headAngleDegrees * PI.toFloat() / 180f
    val head = Offset(
        x = center.x + cos(headAngle) * radius,
        y = center.y + sin(headAngle) * radius
    )

    drawCircle(
        color = Color(0xFFFF8A2F).copy(alpha = 0.20f * pulse),
        radius = 17.dp.toPx() * pulse,
        center = head
    )
    drawCircle(
        color = Color(0xFFFFD66E).copy(alpha = 0.88f),
        radius = 6.8.dp.toPx(),
        center = head
    )

    repeat(7) { index ->
        val flutter = sin((phase * 360f + index * 53f) * PI.toFloat() / 180f)
        val particleAngle = headAngle - (0.10f + index * 0.055f) + flutter * 0.035f
        val particleRadius = radius + (5.dp.toPx() + (index % 3) * 3.dp.toPx()) * pulse
        val particleCenter = Offset(
            x = center.x + cos(particleAngle) * particleRadius,
            y = center.y + sin(particleAngle) * particleRadius
        )
        drawCircle(
            color = when (index % 3) {
                0 -> Color(0xFFFF6A2A).copy(alpha = 0.68f)
                1 -> Color(0xFFFFB13D).copy(alpha = 0.58f)
                else -> Color(0xFFFFE59A).copy(alpha = 0.62f)
            },
            radius = (4.4f - index * 0.22f).dp.toPx().coerceAtLeast(1.8.dp.toPx()),
            center = particleCenter
        )
    }

    repeat(5) { index ->
        val emberAngle = headAngle - 0.24f + index * 0.08f + phase * 0.34f
        val emberDistance = radius + 15.dp.toPx() + index * 2.dp.toPx()
        val emberCenter = Offset(
            x = center.x + cos(emberAngle) * emberDistance,
            y = center.y + sin(emberAngle) * emberDistance
        )
        drawCircle(
            color = Color(0xFFFFC247).copy(alpha = 0.42f - index * 0.045f),
            radius = (2.2f - index * 0.16f).dp.toPx().coerceAtLeast(1.1.dp.toPx()),
            center = emberCenter
        )
    }
}

@Composable
private fun RecentRecordsCard(
    isFocusFireActive: Boolean,
    title: String,
    activities: List<ActivitySession>,
    isFiltered: Boolean,
    onClearFilter: () -> Unit,
    canUndo: Boolean,
    onUndo: () -> Unit,
    onDelete: (ActivitySession) -> Unit,
    onEdit: (Long) -> Unit,
    onToggleFavorite: (ActivitySession) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.weight(1f)
            )
            if (isFiltered) {
                Button(
                    onClick = onClearFilter,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFocusFireActive) FocusFireSoft else FlowPurpleSoft,
                        contentColor = if (isFocusFireActive) FocusFire else FlowPurple
                    )
                ) {
                    Text("초기화", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onUndo,
                    enabled = canUndo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFocusFireActive) FocusFireSoft else FlowPurpleSoft,
                        contentColor = if (isFocusFireActive) FocusFire else FlowPurple
                    )
                ) {
                    Text("되돌리기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isFocusFireActive) FocusFireSurface else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
            shape = RoundedCornerShape(22.dp)
        ) {
            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 표시할 기록이 없습니다.",
                        fontSize = 14.sp,
                        color = FlowMuted
                    )
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    activities.forEachIndexed { index, activity ->
                        RecentRecordRow(
                            activity = activity,
                            onDelete = onDelete,
                            onEdit = onEdit,
                            onToggleFavorite = onToggleFavorite
                        )
                        if (index != activities.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 58.dp)
                                    .height(1.dp)
                                    .background(FlowDivider)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentRecordRow(
    activity: ActivitySession,
    onDelete: (ActivitySession) -> Unit,
    onEdit: (Long) -> Unit,
    onToggleFavorite: (ActivitySession) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTimeText = timeFormat.format(Date(activity.startTime))
    val endTimeText = timeFormat.format(Date(activity.endTime))
    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(activity.durationMillis).coerceAtLeast(1L)
    val categoryColor = categoryColor(activity.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onEdit(activity.id) })
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(categoryColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            CategoryGlyph(
                category = activity.category,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = activity.title.ifBlank { displayCategory(activity.category) },
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                maxLines = 1
            )
            Text(
                text = "$startTimeText - $endTimeText",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "${durationMinutes}\uBD84",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FlowMuted,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        if (false) {
        Text(
            text = "${durationMinutes}분",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowMuted,
            modifier = Modifier.padding(end = 2.dp)
        )
        }
        IconButton(
            onClick = { onToggleFavorite(activity) },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (activity.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = "즐겨찾기",
                tint = if (activity.isFavorite) Color(0xFFFFB300) else Color(0xFFB7BBC6),
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        if (false) {
        IconButton(
            onClick = { onEdit(activity.id) },
            modifier = Modifier.size(0.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "수정",
                tint = Color(0xFF697386),
                modifier = Modifier.size(0.dp)
            )
        }
        }
        IconButton(
            onClick = { onDelete(activity) },
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFFFEEF1), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "삭제",
                tint = Color(0xFFFF4D5E),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ActivityStartGrid(
    categories: List<String>,
    isRunning: Boolean,
    currentCategory: String,
    onStart: (String) -> Unit
) {
    val quickCategories = remember(categories) {
        categories.filter { it == "TOOTHBRUSH" || it == "SNACK" }
    }
    val activityCategories = remember(categories) {
        categories.filter { it != "TOOTHBRUSH" && it != "SNACK" }
    }
    val visibleActivityCategories = remember(activityCategories, isRunning, currentCategory) {
        if (isRunning) {
            activityCategories.filterNot { category -> category == currentCategory }
        } else {
            activityCategories
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(
            text = "활동 시작",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF9EA3B2)
        )
        Text(
            text = "빠른 타이머",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(top = 30.dp, bottom = 14.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            quickCategories.forEach { category ->
                CategoryButton(
                    category = category,
                    isSelected = isRunning && currentCategory == category,
                    onClick = { onStart(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Text(
            text = "활동",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(top = 30.dp, bottom = 14.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(594.dp),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            userScrollEnabled = false
        ) {
            items(
                items = visibleActivityCategories,
                key = { category -> category }
            ) { category ->
                CategoryButton(
                    category = category,
                    isSelected = isRunning && currentCategory == category,
                    onClick = {
                        if (!isRunning || currentCategory != category) {
                            onStart(category)
                        }
                    },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}


@Composable
private fun AnalyticsCard(
    analytics: AnalyticsState,
    isFocusFireActive: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocusFireActive) FocusFireSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "통계 리포트",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = if (isFocusFireActive) FocusFire else FlowPurple.copy(alpha = 0.62f),
                    modifier = Modifier.size(28.dp)
                )
            }
            TodayActivityReport(analytics.todayCategoryStats)
            YesterdayComparisonReport(
                todayStats = analytics.todayCategoryStats,
                yesterdayStats = analytics.yesterdayCategoryStats
            )
        }
    }
}

@Composable
private fun TodayActivityReport(stats: List<CategoryStat>) {
    var isExpanded by remember { mutableStateOf(false) }
    val visibleStats = if (isExpanded) stats else stats.take(4)

    SectionHeaderRow(
        title = "오늘 한 일들",
        icon = {
            Icon(
                imageVector = Icons.Filled.CalendarToday,
                contentDescription = null,
                tint = FlowPurple.copy(alpha = 0.62f),
                modifier = Modifier.size(22.dp)
            )
        }
    )

    ReportPanel {
        if (stats.isEmpty()) {
            EmptyReportText("오늘 기록된 활동이 없습니다.")
            return@ReportPanel
        }

        val maxMillis = remember(stats) {
            stats.maxOfOrNull { it.totalMillis }?.coerceAtLeast(1L) ?: 1L
        }
        visibleStats.forEachIndexed { index, stat ->
            TodayCategoryRow(stat = stat, maxMillis = maxMillis)
            if (index < visibleStats.lastIndex) ReportDivider()
        }
        if (stats.size > 4) {
            ReportMoreButton(
                text = if (isExpanded) "접기" else "나머지 ${stats.size - 4}개 보기",
                onClick = { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
private fun YesterdayComparisonReport(
    todayStats: List<CategoryStat>,
    yesterdayStats: List<CategoryStat>
) {
    var isExpanded by remember { mutableStateOf(false) }
    val yesterdayByCategory = remember(yesterdayStats) {
        yesterdayStats.associateBy { it.category }
    }
    val categories = remember(todayStats, yesterdayStats) {
        (todayStats.map { it.category } + yesterdayStats.map { it.category })
            .distinct()
            .sortedByDescending { category ->
                maxOf(
                    todayStats.firstOrNull { it.category == category }?.totalMillis ?: 0L,
                    yesterdayByCategory[category]?.totalMillis ?: 0L
                )
            }
    }
    val visibleCategories = if (isExpanded) categories else categories.take(4)
    val maxMillis = remember(todayStats, yesterdayStats) {
        (todayStats + yesterdayStats).maxOfOrNull { it.totalMillis }?.coerceAtLeast(1L) ?: 1L
    }

    SectionHeaderRow(
        title = "어제와 비교",
        icon = {
            Icon(
                imageVector = Icons.Filled.BarChart,
                contentDescription = null,
                tint = FlowPurple.copy(alpha = 0.62f),
                modifier = Modifier.size(23.dp)
            )
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(Color(0xFFC9CBD2))
                Text("어제", fontSize = 12.sp, color = FlowMuted)
                Spacer(modifier = Modifier.width(10.dp))
                LegendDot(FlowPurple)
                Text("오늘", fontSize = 12.sp, color = FlowMuted)
            }
        }
    )

    ReportPanel {
        if (categories.isEmpty()) {
            EmptyReportText("비교할 활동 기록이 없습니다.")
            return@ReportPanel
        }

        visibleCategories.forEachIndexed { index, category ->
            val today = todayStats.firstOrNull { it.category == category }
            val yesterday = yesterdayByCategory[category]
            ComparisonCategoryRow(
                category = category,
                todayMillis = today?.totalMillis ?: 0L,
                yesterdayMillis = yesterday?.totalMillis ?: 0L,
                maxMillis = maxMillis
            )
            if (index < visibleCategories.lastIndex) ReportDivider()
        }
        if (categories.size > 4) {
            ReportMoreButton(
                text = if (isExpanded) "접기" else "나머지 ${categories.size - 4}개 보기",
                onClick = { isExpanded = !isExpanded }
            )
        }
    }
    Text(
        text = "* 비교 기준: 어제 하루 전체 기록",
        fontSize = 11.sp,
        color = FlowMuted.copy(alpha = 0.72f),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SectionHeaderRow(
    title: String,
    icon: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowInk,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f)
        )
        trailing?.invoke()
    }
}

@Composable
private fun ReportPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
private fun TodayCategoryRow(stat: CategoryStat, maxMillis: Long) {
    val fraction = (stat.totalMillis.toFloat() / maxMillis.toFloat())
        .coerceIn(0f, 1f)
    val visibleFraction = if (stat.totalMillis > 0L) fraction.coerceAtLeast(0.08f) else 0f
    val color = categoryColor(stat.category)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(stat.category)
        Text(
            text = displayCategory(stat.category),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier
                .padding(start = 12.dp)
                .width(68.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        ProgressTrack(
            fraction = visibleFraction,
            color = color,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatDurationWithoutSeconds(stat.totalMillis),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier
                .padding(start = 10.dp)
                .width(56.dp)
        )
    }
}

@Composable
private fun ComparisonCategoryRow(
    category: String,
    todayMillis: Long,
    yesterdayMillis: Long,
    maxMillis: Long
) {
    val color = categoryColor(category)
    val todayFraction = (todayMillis.toFloat() / maxMillis.toFloat()).coerceIn(0f, 1f)
    val yesterdayFraction = (yesterdayMillis.toFloat() / maxMillis.toFloat()).coerceIn(0f, 1f)
    val delta = todayMillis - yesterdayMillis

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryBadge(category)
        Text(
            text = displayCategory(category),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier
                .padding(start = 12.dp)
                .width(58.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Column(modifier = Modifier.weight(1f)) {
            ComparisonBar(
                fraction = if (yesterdayMillis > 0L) yesterdayFraction.coerceAtLeast(0.08f) else 0f,
                color = Color(0xFFC9CBD2)
            )
            Spacer(modifier = Modifier.height(7.dp))
            ComparisonBar(
                fraction = if (todayMillis > 0L) todayFraction.coerceAtLeast(0.08f) else 0f,
                color = color
            )
        }
        DeltaText(deltaMillis = delta, color = color)
    }
}

@Composable
private fun ComparisonBar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .background(Color(0xFFE7E7EA), RoundedCornerShape(10.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(9.dp)
                .background(color, RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun ProgressTrack(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(11.dp)
            .background(Color(0xFFE7E7EA), RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(11.dp)
                .background(color, RoundedCornerShape(12.dp))
        )
    }
}

@Composable
private fun CategoryBadge(category: String) {
    val color = categoryColor(category)
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(reportCategoryBackground(category), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        CategoryGlyph(
            category = category,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun DeltaText(deltaMillis: Long, color: Color) {
    val isUp = deltaMillis >= 0L
    val deltaColor = if (isUp) color else Color(0xFF19A7B0)
    val sign = if (isUp) "+" else "-"
    val arrow = if (isUp) "↑" else "↓"
    Text(
        text = "$arrow $sign${formatDurationWithoutSeconds(abs(deltaMillis))}",
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = deltaColor,
        textAlign = TextAlign.End,
        modifier = Modifier
            .padding(start = 8.dp)
            .width(70.dp)
    )
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun ReportDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(1.dp)
            .background(FlowDivider)
    )
}

@Composable
private fun EmptyReportText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = FlowMuted,
        modifier = Modifier.padding(vertical = 14.dp)
    )
}

@Composable
private fun ReportMoreButton(
    text: String,
    onClick: () -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = FlowPurpleSoft.copy(alpha = 0.56f),
            contentColor = FlowPurple
        )
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun reportCategoryBackground(category: String): Color {
    return when (category) {
        "TOOTHBRUSH" -> Color(0xFFEDE8FF)
        "SNACK" -> Color(0xFFFFF1DD)
        "MEAL" -> Color(0xFFFFEDE4)
        "STUDY" -> Color(0xFFE6F6E8)
        "WORK" -> Color(0xFFE9EAF0)
        "DEVELOPMENT" -> Color(0xFFE9E7FF)
        "WASH" -> Color(0xFFE4F5FF)
        "SCHOOL" -> Color(0xFFFCE4ED)
        "COMPANY" -> Color(0xFFE6EEF2)
        "EXERCISE" -> Color(0xFFE4F1FF)
        "SLEEP" -> Color(0xFFF0E4FF)
        "REST" -> Color(0xFFE2F5F5)
        else -> Color(0xFFEDEDF1)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = FlowInk,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun AverageRows(stats: List<CategoryStat>) {
    if (stats.isEmpty()) {
        Text("최근 7일 평균을 계산할 데이터가 없습니다.", fontSize = 12.sp, color = FlowMuted)
        return
    }

    val maxAverageMillis = remember(stats) {
        stats.maxOfOrNull { it.averageMillis }?.coerceAtLeast(1L) ?: 1L
    }

    stats.take(6).forEach { stat ->
        val fraction = (stat.averageMillis.toFloat() / maxAverageMillis.toFloat())
            .coerceIn(0f, 1f)
        val visibleFraction = if (stat.averageMillis > 0L) fraction.coerceAtLeast(0.04f) else 0f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displayCategory(stat.category), fontSize = 12.sp, color = FlowMuted, modifier = Modifier.width(72.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(Color(0xFFE0E0E0), shape = MaterialTheme.shapes.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(visibleFraction)
                        .height(10.dp)
                        .background(categoryColor(stat.category), shape = MaterialTheme.shapes.small)
                )
            }
            Text(
                formatDurationWithoutSeconds(stat.averageMillis),
                fontSize = 12.sp,
                color = FlowMuted,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .width(84.dp)
            )
        }
    }
}

private fun formatDurationWithoutSeconds(durationMillis: Long): String {
    val durationHours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60

    return when {
        durationHours > 0 -> "${durationHours}시간 ${durationMinutes}분"
        durationMinutes > 0 -> "${durationMinutes}분"
        else -> "1분 미만"
    }
}

private sealed class TimelineBlock {
    data class ActualActivity(val segment: DisplayActivitySegment) : TimelineBlock()
    data class ScheduledAutoButton(val block: ScheduledAutoButtonBlock) : TimelineBlock()
    data class RecommendedTodo(val block: RecommendedTodoBlock) : TimelineBlock()
}

private data class DisplayActivitySegment(
    val category: String,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val mergedSegments: List<ActivitySession>,
    val hiddenSegments: List<ActivitySession>
) {
    val durationMillis: Long = (endTime - startTime).coerceAtLeast(1L)
}

@Composable
private fun TimetableCard(
    isFocusFireActive: Boolean = false,
    activities: List<ActivitySession>,
    scheduledBlocks: List<ScheduledAutoButtonBlock>,
    recommendedBlocks: List<RecommendedTodoBlock>,
    incompleteTodos: List<TodoItem>,
    activeCategory: String?,
    allActivities: List<ActivitySession> = emptyList(),
    timerStartMillis: Long? = null,
    onSaveSleep: (startMillis: Long, endMillis: Long) -> Unit = { _, _ -> },
    onSkipToday: (String) -> Unit,
    onUnskipToday: (String) -> Unit,
    onEditSchedule: (String) -> Unit,
    onManageSchedules: () -> Unit,
    onStartRecommended: (RecommendedTodoBlock) -> Unit,
    onCompleteRecommended: (RecommendedTodoBlock) -> Unit,
    onSetRecommendedTime: (RecommendedTodoBlock, Int) -> Unit,
    onReplaceRecommendedItem: (RecommendedTodoBlock, TodoItem) -> Unit,
    onStartFlowRecommendation: (FlowActivityRecommendation) -> Unit = {},
    onOpenFlowRecommendation: (FlowActivityRecommendation) -> Unit = {},
    onCompleteFlowRecommendation: (FlowActivityRecommendation) -> Unit = {},
    flowRecommendations: List<FlowActivityRecommendation> = emptyList(),
    isDeveloperMode: Boolean = false,
    samplePresetIndex: Int = 0,
    onCyclePreset: () -> Unit = {}
) {
    var selectedBlock by remember { mutableStateOf<ScheduledAutoButtonBlock?>(null) }
    var selectedRecommendedBlock by remember { mutableStateOf<RecommendedTodoBlock?>(null) }
    var pendingSleepRange by remember { mutableStateOf<EmptyRange?>(null) }
    var selectedFlowRecommendation by remember { mutableStateOf<FlowActivityRecommendation?>(null) }
    val activitiesForSleepRange = remember(allActivities, activities) {
        allActivities.ifEmpty { activities }
    }
    val timetableActivities = remember(activities, allActivities) {
        activitiesForTodayTimetable(
            todayActivities = activities,
            allActivities = allActivities
        )
    }
    val displayActivitySegments by remember(timetableActivities) {
        derivedStateOf {
            buildDisplayActivitySegments(prioritizeSchoolCompanyTimelineActivities(timetableActivities))
        }
    }
    val visibleScheduledBlocks by remember(scheduledBlocks, activeCategory) {
        derivedStateOf {
            scheduledBlocks.filterNot { block -> block.category == activeCategory }
        }
    }
    val timelineItems by remember(displayActivitySegments, visibleScheduledBlocks, recommendedBlocks) {
        derivedStateOf {
            (
                displayActivitySegments.map { TimelineBlock.ActualActivity(it) } +
                    visibleScheduledBlocks.map { TimelineBlock.ScheduledAutoButton(it) } +
                    recommendedBlocks.map { TimelineBlock.RecommendedTodo(it) }
                ).sortedBy { block ->
                when (block) {
                    is TimelineBlock.ActualActivity -> block.segment.startTime
                    is TimelineBlock.ScheduledAutoButton -> block.block.startTime
                    is TimelineBlock.RecommendedTodo -> block.block.plannedStartMillis
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocusFireActive) FocusFireSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "타임테이블",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowInk,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onManageSchedules,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFocusFireActive) FocusFireSoft else FlowPurpleSoft.copy(alpha = 0.72f),
                        contentColor = if (isFocusFireActive) FocusFire else FlowPurpleDeep
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "반복 루틴",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                if (isDeveloperMode) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onCyclePreset,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFE0B2),
                            contentColor = Color(0xFFE65100)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "샘플 ${samplePresetIndex + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            if (timelineItems.isEmpty()) {
                Text(
                    text = "아직 기록된 활동이 없습니다.",
                    fontSize = 14.sp,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                TimetableBar(
                    blocks = timelineItems,
                    onScheduledLongPress = { block -> selectedBlock = block },
                    onRecommendedClick = { block -> selectedRecommendedBlock = block },
                    onEmptySpaceLongPress = { pressedTimeMillis ->
                        val range = findEmptyRangeAroundPressedTime(
                            pressedTimeMillis = pressedTimeMillis,
                            allActivities = activitiesForSleepRange,
                            runningTimerStartMillis = timerStartMillis
                        )
                        val isCandidate = range != null && isSleepCandidateRange(range.startMillis, range.endMillis)
                        if (isCandidate) {
                            pendingSleepRange = range
                        }
                    }
                )
                ScheduledAutoButtonList(
                    blocks = visibleScheduledBlocks,
                    activeCategory = activeCategory,
                    onShowMenu = { block -> selectedBlock = block }
                )
            }
            flowRecommendations.forEachIndexed { index, recommendation ->
                ActivityRecommendationRow(
                    category = recommendation.category,
                    activityName = recommendation.title,
                    isEnabled = recommendation.isEnabled,
                    isCompleted = recommendation.isCompleted,
                    onClick = {
                        onOpenFlowRecommendation(recommendation)
                        selectedFlowRecommendation = recommendation
                    },
                    modifier = Modifier.padding(top = if (index == 0) 12.dp else 8.dp)
                )
            }
        }
    }

    selectedFlowRecommendation?.let { recommendation ->
        ActivityRecommendationSheet(
            category = recommendation.category,
            activityName = recommendation.title,
            reasonText = recommendation.petite?.aiComment,
            onDismiss = { selectedFlowRecommendation = null },
            onStart = {
                onStartFlowRecommendation(recommendation)
                selectedFlowRecommendation = null
            },
            onComplete = {
                onCompleteFlowRecommendation(recommendation)
                selectedFlowRecommendation = null
            },
            isEnabled = recommendation.isEnabled,
            isCompleted = recommendation.isCompleted
        )
    }

    selectedBlock?.let { block ->
        ScheduledAutoButtonActionSheet(
            block = block,
            onDismiss = { selectedBlock = null },
            onSkipToday = onSkipToday,
            onUnskipToday = onUnskipToday,
            onEditSchedule = onEditSchedule
        )
    }

    selectedRecommendedBlock?.let { block ->
        RecommendedTodoActionSheet(
            block = block,
            incompleteTodos = incompleteTodos,
            onDismiss = { selectedRecommendedBlock = null },
            onStart = {
                onStartRecommended(block)
                selectedRecommendedBlock = null
            },
            onComplete = {
                onCompleteRecommended(block)
                selectedRecommendedBlock = null
            },
            onSetTime = { hour ->
                onSetRecommendedTime(block, hour)
                selectedRecommendedBlock = null
            },
            onReplaceItem = { todo ->
                onReplaceRecommendedItem(block, todo)
                selectedRecommendedBlock = null
            }
        )
    }

    pendingSleepRange?.let { range ->
        SleepFillConfirmDialog(
            emptyRange = range,
            allActivities = activitiesForSleepRange,
            onDismiss = { pendingSleepRange = null },
            onConfirm = { start, end ->
                onSaveSleep(start, end)
                pendingSleepRange = null
            }
        )
    }
}

private fun buildDisplayActivitySegments(
    activities: List<ActivitySession>
): List<DisplayActivitySegment> {
    val sortedActivities = activities.sortedBy { it.startTime }
    val segments = mutableListOf<DisplayActivitySegment>()
    var index = 0

    while (index < sortedActivities.size) {
        val first = sortedActivities[index]
        if (!canMergeAsTimelineAnchor(first)) {
            segments += first.toDisplaySegment()
            index += 1
            continue
        }

        val merged = mutableListOf(first)
        val hidden = mutableListOf<ActivitySession>()
        var endTime = first.endTime.coerceAtLeast(first.startTime + 1L)
        var cursor = index

        while (cursor + 2 < sortedActivities.size) {
            val bridge = sortedActivities[cursor + 1]
            val next = sortedActivities[cursor + 2]
            val bridgeDuration = (bridge.endTime - bridge.startTime).coerceAtLeast(0L)
            val canBridge = bridgeDuration < MERGE_THRESHOLD_MILLIS &&
                canHideBridgeSegment(bridge) &&
                canMergeAsTimelineAnchor(next) &&
                next.category == first.category

            if (!canBridge) break

            hidden += bridge
            merged += next
            endTime = next.endTime.coerceAtLeast(next.startTime + 1L)
            cursor += 2
        }

        segments += DisplayActivitySegment(
            category = first.category,
            title = displayTitleForMergedSegment(first.category, merged),
            startTime = first.startTime,
            endTime = endTime,
            mergedSegments = merged,
            hiddenSegments = hidden
        )
        index = cursor + 1
    }

    return mergeAdjacentProductiveSegments(smoothMicroDisplaySegments(segments))
}

private fun prioritizeSchoolCompanyTimelineActivities(
    activities: List<ActivitySession>
): List<ActivitySession> {
    val priorityRanges = activities
        .filter { activity -> activity.category in PRIORITY_TIMETABLE_CATEGORIES }
        .map { activity ->
            activity.startTime to activity.endTime.coerceAtLeast(activity.startTime + 1L)
        }
        .sortedBy { it.first }

    if (priorityRanges.isEmpty()) return activities

    return activities.flatMap { activity ->
        if (activity.category in PRIORITY_TIMETABLE_CATEGORIES) {
            listOf(activity)
        } else {
            subtractPriorityRanges(activity, priorityRanges)
        }
    }.sortedBy { it.startTime }
}

private fun subtractPriorityRanges(
    activity: ActivitySession,
    priorityRanges: List<Pair<Long, Long>>
): List<ActivitySession> {
    val activityStart = activity.startTime
    val activityEnd = activity.endTime.coerceAtLeast(activity.startTime + 1L)
    val remainingRanges = mutableListOf(activityStart to activityEnd)

    priorityRanges.forEach { (priorityStart, priorityEnd) ->
        var index = 0
        while (index < remainingRanges.size) {
            val (rangeStart, rangeEnd) = remainingRanges[index]
            if (priorityEnd <= rangeStart || priorityStart >= rangeEnd) {
                index += 1
                continue
            }

            remainingRanges.removeAt(index)
            val splitRanges = listOf(
                rangeStart to priorityStart.coerceAtMost(rangeEnd),
                priorityEnd.coerceAtLeast(rangeStart) to rangeEnd
            ).filter { (start, end) -> end > start }

            if (splitRanges.isNotEmpty()) {
                remainingRanges.addAll(index, splitRanges)
                index += splitRanges.size
            }
        }
    }

    return remainingRanges.map { (start, end) ->
        activity.copy(
            startTime = start,
            endTime = end,
            durationMillis = end - start
        )
    }
}

private fun ActivitySession.toDisplaySegment(): DisplayActivitySegment {
    return DisplayActivitySegment(
        category = category,
        title = title,
        startTime = startTime,
        endTime = endTime.coerceAtLeast(startTime + 1L),
        mergedSegments = listOf(this),
        hiddenSegments = emptyList()
    )
}

private fun canMergeAsTimelineAnchor(activity: ActivitySession): Boolean {
    return activity.sourceType != ActivitySourceType.AUTO_BUTTON &&
        activity.category in MERGEABLE_TIMETABLE_CATEGORIES &&
        activity.category !in PRECISE_TIMETABLE_CATEGORIES
}

private fun canHideBridgeSegment(activity: ActivitySession): Boolean {
    return activity.sourceType != ActivitySourceType.AUTO_BUTTON &&
        activity.category in HIDEABLE_TIMETABLE_BRIDGE_CATEGORIES &&
        activity.category !in PRECISE_TIMETABLE_CATEGORIES
}

private fun smoothMicroDisplaySegments(
    initialSegments: List<DisplayActivitySegment>
): List<DisplayActivitySegment> {
    val segments = initialSegments.toMutableList()
    var changed: Boolean

    do {
        changed = false
        var index = 0
        while (index < segments.size) {
            val micro = segments[index]
            if (!micro.isMicroSegment() || micro.isProtectedSegment()) {
                index += 1
                continue
            }

            val previous = segments.getOrNull(index - 1)
            val next = segments.getOrNull(index + 1)
            val previousCanAbsorb = previous?.canAbsorbMicroSegment() == true
            val nextCanAbsorb = next?.canAbsorbMicroSegment() == true

            when {
                previous != null &&
                    next != null &&
                    previousCanAbsorb &&
                    nextCanAbsorb &&
                    previous.isProductiveSegment() &&
                    next.isProductiveSegment() -> {
                    val category = chooseProductiveFlowCategory(previous, next)
                    segments[index - 1] = combineDisplaySegments(
                        visibleSegments = listOf(previous, next),
                        hiddenSegments = listOf(micro),
                        category = category
                    )
                    segments.removeAt(index + 1)
                    segments.removeAt(index)
                    changed = true
                    index = (index - 1).coerceAtLeast(0)
                }
                previousCanAbsorb && nextCanAbsorb -> {
                    val previousSegment = requireNotNull(previous)
                    val nextSegment = requireNotNull(next)
                    val attachToPrevious = when {
                        previousSegment.category == micro.category && nextSegment.category != micro.category -> true
                        nextSegment.category == micro.category && previousSegment.category != micro.category -> false
                        previousSegment.durationMillis != nextSegment.durationMillis ->
                            previousSegment.durationMillis > nextSegment.durationMillis
                        else -> true
                    }
                    if (attachToPrevious) {
                        segments[index - 1] = combineDisplaySegments(
                            visibleSegments = listOf(previousSegment),
                            hiddenSegments = listOf(micro),
                            category = previousSegment.category
                        )
                        segments.removeAt(index)
                        changed = true
                        index = (index - 1).coerceAtLeast(0)
                    } else {
                        segments[index] = combineDisplaySegments(
                            visibleSegments = listOf(nextSegment),
                            hiddenSegments = listOf(micro),
                            category = nextSegment.category
                        )
                        segments.removeAt(index + 1)
                        changed = true
                    }
                }
                previousCanAbsorb -> {
                    val previousSegment = requireNotNull(previous)
                    segments[index - 1] = combineDisplaySegments(
                        visibleSegments = listOf(previousSegment),
                        hiddenSegments = listOf(micro),
                        category = previousSegment.category
                    )
                    segments.removeAt(index)
                    changed = true
                    index = (index - 1).coerceAtLeast(0)
                }
                nextCanAbsorb -> {
                    val nextSegment = requireNotNull(next)
                    segments[index] = combineDisplaySegments(
                        visibleSegments = listOf(nextSegment),
                        hiddenSegments = listOf(micro),
                        category = nextSegment.category
                    )
                    segments.removeAt(index + 1)
                    changed = true
                }
                else -> index += 1
            }
        }
    } while (changed)

    return segments
}

private fun mergeAdjacentProductiveSegments(
    initialSegments: List<DisplayActivitySegment>
): List<DisplayActivitySegment> {
    val segments = initialSegments.toMutableList()
    var index = 0

    while (index < segments.lastIndex) {
        val current = segments[index]
        val next = segments[index + 1]
        if (current.isProductiveSegment() && next.isProductiveSegment()) {
            val category = chooseProductiveFlowCategory(current, next)
            segments[index] = combineDisplaySegments(
                visibleSegments = listOf(current, next),
                hiddenSegments = emptyList(),
                category = category
            )
            segments.removeAt(index + 1)
            index = (index - 1).coerceAtLeast(0)
        } else {
            index += 1
        }
    }

    return segments
}

private fun DisplayActivitySegment.isMicroSegment(): Boolean {
    return durationMillis < MERGE_THRESHOLD_MILLIS
}

private fun DisplayActivitySegment.isProtectedSegment(): Boolean {
    return category in PRECISE_TIMETABLE_CATEGORIES ||
        allOriginalSegments().any { it.sourceType == ActivitySourceType.AUTO_BUTTON }
}

private fun DisplayActivitySegment.canAbsorbMicroSegment(): Boolean {
    return !isProtectedSegment()
}

private fun DisplayActivitySegment.isProductiveSegment(): Boolean {
    return category in PRODUCTIVE_TIMETABLE_CATEGORIES && !isProtectedSegment()
}

private fun DisplayActivitySegment.allOriginalSegments(): List<ActivitySession> {
    return mergedSegments + hiddenSegments
}

private fun startOfLocalDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun timetableCategoryColor(category: String): Color {
    return when (category) {
        "STUDY" -> Color(0xFF6EBD7A)
        "MEAL" -> Color(0xFFE3A55F)
        "SNACK" -> Color(0xFFE2BE55)
        "TOOTHBRUSH" -> Color(0xFF68BDB3)
        "EXERCISE" -> Color(0xFF6CA8DF)
        "WORK" -> Color(0xFF7B8790)
        "COMPANY" -> Color(0xFF6F7E87)
        "DEVELOPMENT" -> Color(0xFF6672C7)
        "READING" -> Color(0xFF55A99E)
        "MOVE" -> Color(0xFF58AAB4)
        "WASH" -> Color(0xFF70AFE0)
        "SLEEP" -> Color(0xFFA373C8)
        "REST" -> Color(0xFF62BBC5)
        "SCHOOL" -> Color(0xFFD37B9A)
        "GAME" -> Color(0xFF7581C8)
        "TODO" -> TimetableTodoColor
        else -> Color(0xFF8C8F98)
    }
}

private fun overlapMillis(
    firstStart: Long,
    firstEnd: Long,
    secondStart: Long,
    secondEnd: Long
): Long {
    val start = maxOf(firstStart, secondStart)
    val end = minOf(firstEnd, secondEnd)
    return (end - start).coerceAtLeast(0L)
}

private fun chooseProductiveFlowCategory(
    previous: DisplayActivitySegment,
    next: DisplayActivitySegment
): String {
    if (previous.category == next.category) return previous.category
    return if (previous.durationMillis >= next.durationMillis) previous.category else next.category
}

private fun combineDisplaySegments(
    visibleSegments: List<DisplayActivitySegment>,
    hiddenSegments: List<DisplayActivitySegment>,
    category: String
): DisplayActivitySegment {
    val allSegments = (visibleSegments + hiddenSegments).sortedBy { it.startTime }
    val visibleOriginals = visibleSegments.flatMap { it.mergedSegments }.sortedBy { it.startTime }
    val hiddenOriginals = (
        visibleSegments.flatMap { it.hiddenSegments } +
            hiddenSegments.flatMap { it.allOriginalSegments() }
        ).sortedBy { it.startTime }

    return DisplayActivitySegment(
        category = category,
        title = displayTitleForMergedSegment(category, visibleOriginals),
        startTime = allSegments.minOf { it.startTime },
        endTime = allSegments.maxOf { it.endTime.coerceAtLeast(it.startTime + 1L) },
        mergedSegments = visibleOriginals,
        hiddenSegments = hiddenOriginals
    )
}

private fun RecommendedTodoBlock.displayEndMillis(): Long {
    return plannedStartMillis + RECOMMENDED_TODO_DISPLAY_DURATION_MILLIS
}

private fun displayTitleForMergedSegment(
    category: String,
    mergedSegments: List<ActivitySession>
): String {
    val titles = mergedSegments.map { it.title.trim() }.filter { it.isNotBlank() }.distinct()
    return if (titles.size == 1) titles.first() else displayCategory(category)
}

@Composable
private fun TimetableBar(
    blocks: List<TimelineBlock>,
    onScheduledLongPress: (ScheduledAutoButtonBlock) -> Unit,
    onRecommendedClick: (RecommendedTodoBlock) -> Unit,
    onEmptySpaceLongPress: (pressedTimeMillis: Long) -> Unit = {}
) {
    if (blocks.isEmpty()) return
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val windowBlocks = remember(blocks) {
        blocks.filter { it !is TimelineBlock.RecommendedTodo || !it.block.isBubbleOnly }
            .ifEmpty { blocks }
    }
    val firstStart = remember(windowBlocks) {
        windowBlocks.minOf {
            when (it) {
                is TimelineBlock.ActualActivity -> it.segment.startTime
                is TimelineBlock.ScheduledAutoButton -> it.block.startTime
                is TimelineBlock.RecommendedTodo -> it.block.plannedStartMillis
            }
        }
    }
    val windowStart = remember(firstStart) { startOfLocalDay(firstStart) }
    val windowEnd = remember(windowStart) { windowStart + DAY_DURATION_MILLIS }
    val windowDuration = remember(windowStart, windowEnd) { (windowEnd - windowStart).coerceAtLeast(1L) }
    val scheduled = remember(blocks) {
        blocks.filterIsInstance<TimelineBlock.ScheduledAutoButton>().map { it.block }
    }
    val recommended = remember(blocks) {
        blocks.filterIsInstance<TimelineBlock.RecommendedTodo>().map { it.block }
    }
    val categories = remember(blocks) {
        blocks.map {
            when (it) {
                is TimelineBlock.ActualActivity -> it.segment.category
                is TimelineBlock.ScheduledAutoButton -> it.block.category
                is TimelineBlock.RecommendedTodo -> "TODO"
            }
        }.distinct()
    }

    val actualSegments = remember(blocks) {
        blocks.filterIsInstance<TimelineBlock.ActualActivity>().map { it.segment }
    }
    val density = LocalDensity.current
    val bubbleTextPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = FlowPurpleDeep.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
    }
    val recommendedTodoStroke = remember(density) {
        Stroke(
            width = with(density) { 1.5.dp.toPx() },
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(
                    with(density) { 4.dp.toPx() },
                    with(density) { 3.dp.toPx() }
                )
            )
        )
    }

    // pointerInput은 Canvas가 아닌 Box 컨테이너에 붙여
    // LazyColumn 스크롤과의 경합 없이 long press를 안정적으로 감지한다.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .pointerInput(blocks, windowStart, windowDuration) {
                detectTapGestures(
                    onTap = { offset ->
                        val barTop = 30.dp.toPx()
                        val barBottom = 44.dp.toPx()
                        val maxBubbleHalf = minOf(95.dp.toPx(), (size.width - 8.dp.toPx()) / 2f)
                            .coerceAtLeast(24.dp.toPx())
                        val hit = recommended.withIndex().firstOrNull { (index, block) ->
                            val startFraction = ((block.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(0f, 1f)
                            val endFraction = ((block.displayEndMillis() - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(startFraction, 1f)
                            val startX = size.width * startFraction
                            val endX = size.width * endFraction
                            val centerX = (startX + endX) / 2f
                            val bubbleLeft = (centerX - maxBubbleHalf).coerceAtLeast(0f)
                            val bubbleRight = (centerX + maxBubbleHalf).coerceAtMost(size.width.toFloat())
                            val showAbove = index % 2 == 0
                            when {
                                offset.y in barTop..barBottom -> offset.x in startX..endX
                                showAbove && offset.y < barTop -> offset.x in bubbleLeft..bubbleRight
                                !showAbove && offset.y > barBottom -> offset.x in bubbleLeft..bubbleRight
                                else -> false
                            }
                        }?.value
                        if (hit != null) onRecommendedClick(hit)
                    },
                    onLongPress = { offset ->
                        // 1) ScheduledAutoButton 히트 테스트
                        val scheduledHit = scheduled.firstOrNull { block ->
                            val startFraction = ((block.startTime - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(0f, 1f)
                            val endFraction = ((block.endTime - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(startFraction, 1f)
                            val startX = size.width * startFraction
                            val endX = size.width * endFraction
                            offset.x in startX..endX
                        }
                        if (scheduledHit != null) {
                            onScheduledLongPress(scheduledHit)
                            return@detectTapGestures
                        }

                        // 2) ActualActivity 히트 테스트 (기존 기록 위에서는 수면 제안 안 함)
                        val actualHit = actualSegments.firstOrNull { segment ->
                            val startFraction = ((segment.startTime - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(0f, 1f)
                            val endFraction = ((segment.endTime.coerceAtLeast(segment.startTime + 1L) - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(startFraction, 1f)
                            val startX = size.width * startFraction
                            val endX = size.width * endFraction
                            offset.x in startX..endX
                        }
                        if (actualHit != null) return@detectTapGestures

                        // 3) RecommendedTodo 히트 테스트
                        val recommendedHit = recommended.firstOrNull { block ->
                            val startFraction = ((block.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(0f, 1f)
                            val endFraction = ((block.displayEndMillis() - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(startFraction, 1f)
                            val startX = size.width * startFraction
                            val endX = size.width * endFraction
                            offset.x in startX..endX
                        }
                        if (recommendedHit != null) return@detectTapGestures

                        // 4) 빈 공간 — Canvas 전체 높이에서 받음 (Y 제한 없음)
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val pressedTimeMillis = windowStart + (fraction * windowDuration).toLong()
                        onEmptySpaceLongPress(pressedTimeMillis)
                    }
                )
            }
    ) {
        val timeFormatInner = timeFormat
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val trackColor = Color(0xFFF0F1F5)
            val barHeight = 14.dp.toPx()
            val top = 30.dp.toPx()
            val segmentCount = 48
            val segmentGap = 2.dp.toPx()
            val segmentWidth = ((size.width - segmentGap * (segmentCount - 1)) / segmentCount)
                .coerceAtLeast(2.dp.toPx())
            val radius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            val slotKeys = MutableList<String?>(segmentCount) { null }
            val slotColors = MutableList(segmentCount) { trackColor }

            repeat(segmentCount) { index ->
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(index * (segmentWidth + segmentGap), top + 1.5.dp.toPx()),
                    size = Size(segmentWidth, barHeight - 3.dp.toPx()),
                    cornerRadius = radius
                )
            }

            repeat(segmentCount) { index ->
                val segmentStart = windowStart + (windowDuration * index / segmentCount)
                val segmentEnd = windowStart + (windowDuration * (index + 1) / segmentCount)
                val actualSegment = actualSegments
                    .map { segment ->
                        segment to overlapMillis(
                            firstStart = segmentStart,
                            firstEnd = segmentEnd,
                            secondStart = segment.startTime,
                            secondEnd = segment.endTime.coerceAtLeast(segment.startTime + 1L)
                        )
                    }
                    .filter { (_, overlap) -> overlap > 0L }
                    .maxByOrNull { (_, overlap) -> overlap }
                    ?.first
                val scheduledBlock = scheduled
                    .map { item ->
                        item to overlapMillis(
                            firstStart = segmentStart,
                            firstEnd = segmentEnd,
                            secondStart = item.startTime,
                            secondEnd = item.endTime.coerceAtLeast(item.startTime + 1L)
                        )
                    }
                    .filter { (_, overlap) -> overlap > 0L }
                    .maxByOrNull { (_, overlap) -> overlap }
                    ?.first
                when {
                    actualSegment != null -> {
                        slotKeys[index] = "actual:${actualSegment.startTime}:${actualSegment.endTime}:${actualSegment.category}"
                        slotColors[index] = timetableCategoryColor(actualSegment.category)
                    }
                    scheduledBlock != null -> {
                        slotKeys[index] = "scheduled:${scheduledBlock.scheduleId}:${scheduledBlock.startTime}:${scheduledBlock.endTime}"
                        slotColors[index] = timetableCategoryColor(scheduledBlock.category)
                            .copy(alpha = if (scheduledBlock.isSkippedToday) 0.28f else 0.58f)
                    }
                }
            }

            var slotIndex = 0
            while (slotIndex < segmentCount) {
                val key = slotKeys[slotIndex]
                if (key == null) {
                    slotIndex += 1
                    continue
                }
                var endIndex = slotIndex + 1
                while (endIndex < segmentCount && slotKeys[endIndex] == key) {
                    endIndex += 1
                }
                val left = slotIndex * (segmentWidth + segmentGap)
                val right = (endIndex - 1) * (segmentWidth + segmentGap) + segmentWidth
                drawRoundRect(
                    color = slotColors[slotIndex],
                    topLeft = Offset(left, top),
                    size = Size(right - left, barHeight),
                    cornerRadius = radius
                )
                slotIndex = endIndex
            }

            recommended.filterNot { it.isBubbleOnly }.forEach { item ->
                val startFraction = ((item.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                    .coerceIn(0f, 1f)
                val endFraction = ((item.displayEndMillis() - windowStart).toFloat() / windowDuration.toFloat())
                    .coerceIn(startFraction, 1f)
                val x = size.width * startFraction
                val width = (size.width * (endFraction - startFraction)).coerceAtLeast(4.dp.toPx())
                val outlineMinWidth = 8.dp.toPx()
                val outlineWidth = width.coerceIn(outlineMinWidth, size.width)
                val outlineLeft = x.coerceIn(0f, size.width - outlineWidth)
                drawRoundRect(
                    color = TimetableTodoColor.copy(alpha = 0.68f),
                    topLeft = Offset(
                        outlineLeft,
                        top - 3.dp.toPx()
                    ),
                    size = Size(
                        outlineWidth,
                        barHeight + 6.dp.toPx()
                    ),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = recommendedTodoStroke
                )
            }

            var recommendedBubbleIndex = 0
            blocks.forEach { block ->
                when (block) {
                    is TimelineBlock.ActualActivity,
                    is TimelineBlock.ScheduledAutoButton -> Unit
                    is TimelineBlock.RecommendedTodo -> {
                        val item = block.block
                        val startFraction = ((item.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(0f, 1f)
                        val endFraction = ((item.displayEndMillis() - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(startFraction, 1f)
                        val x = size.width * startFraction
                        val width = (size.width * (endFraction - startFraction)).coerceAtLeast(4.dp.toPx())
                        val label = "${timeFormatInner.format(Date(item.plannedStartMillis))} ${item.title}"
                        drawRecommendedTodoBubble(
                            label = label,
                            anchorCenterX = x + width / 2f,
                            barTop = top,
                            barBottom = top + barHeight,
                            showAbove = recommendedBubbleIndex % 2 == 0,
                            textPaint = bubbleTextPaint
                        )
                        recommendedBubbleIndex += 1
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-8).dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (hour in 0..24 step 4) {
            Text(
                text = "%02d:00".format(hour),
                fontSize = 10.sp,
                color = FlowMuted
            )
        }
    }

    LazyRow(
        modifier = Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = categories,
            key = { category -> category }
        ) { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(timetableCategoryColor(category), shape = MaterialTheme.shapes.small)
                )
                Text(
                    text = displayCategory(category),
                    fontSize = 11.sp,
                    color = FlowMuted,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

private fun DrawScope.drawRecommendedTodoBubble(
    label: String,
    anchorCenterX: Float,
    barTop: Float,
    barBottom: Float,
    showAbove: Boolean,
    textPaint: Paint
) {
    val horizontalPadding = 7.dp.toPx()
    val bubbleHeight = 20.dp.toPx()
    val bubbleRadius = 6.dp.toPx()
    val tailWidth = 8.dp.toPx()
    val tailHeight = 5.dp.toPx()
    val maxBubbleWidth = minOf(190.dp.toPx(), size.width - 8.dp.toPx()).coerceAtLeast(48.dp.toPx())
    val availableTextWidth = (maxBubbleWidth - horizontalPadding * 2f).coerceAtLeast(1f)
    val displayLabel = label.ellipsizeToWidth(textPaint, availableTextWidth)
    val measuredTextWidth = textPaint.measureText(displayLabel)
    val bubbleWidth = (measuredTextWidth + horizontalPadding * 2f)
        .coerceIn(48.dp.toPx(), maxBubbleWidth)
    val bubbleLeft = (anchorCenterX - bubbleWidth / 2f)
        .coerceIn(0f, size.width - bubbleWidth)
    val bubbleTop = if (showAbove) {
        0f
    } else {
        (barBottom + tailHeight + 5.dp.toPx()).coerceAtMost(size.height - bubbleHeight)
    }
    val bubbleBottom = bubbleTop + bubbleHeight
    val tailCenterX = anchorCenterX.coerceIn(
        bubbleLeft + bubbleRadius,
        bubbleLeft + bubbleWidth - bubbleRadius
    )

    drawRoundRect(
        color = FlowPurpleSoft.copy(alpha = 0.95f),
        topLeft = Offset(bubbleLeft, bubbleTop),
        size = Size(bubbleWidth, bubbleHeight),
        cornerRadius = CornerRadius(bubbleRadius, bubbleRadius)
    )
    drawPath(
        path = Path().apply {
            if (showAbove) {
                moveTo(tailCenterX - tailWidth / 2f, bubbleBottom - 1.dp.toPx())
                lineTo(tailCenterX + tailWidth / 2f, bubbleBottom - 1.dp.toPx())
                lineTo(tailCenterX, (bubbleBottom + tailHeight).coerceAtMost(barTop - 1.dp.toPx()))
            } else {
                moveTo(tailCenterX - tailWidth / 2f, bubbleTop + 1.dp.toPx())
                lineTo(tailCenterX + tailWidth / 2f, bubbleTop + 1.dp.toPx())
                lineTo(tailCenterX, (bubbleTop - tailHeight).coerceAtLeast(barBottom + 1.dp.toPx()))
            }
            close()
        },
        color = FlowPurpleSoft.copy(alpha = 0.95f)
    )

    drawIntoCanvas { canvas ->
        val baseline = bubbleTop + bubbleHeight / 2f -
            (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.nativeCanvas.drawText(
            displayLabel,
            bubbleLeft + horizontalPadding,
            baseline,
            textPaint
        )
    }
}

private fun String.ellipsizeToWidth(paint: Paint, maxWidth: Float): String {
    if (paint.measureText(this) <= maxWidth) return this
    val ellipsis = "..."
    val ellipsisWidth = paint.measureText(ellipsis)
    if (ellipsisWidth >= maxWidth) return ellipsis
    val count = paint.breakText(this, true, maxWidth - ellipsisWidth, null)
    return take(count).trimEnd() + ellipsis
}

@Composable
private fun ScheduledAutoButtonList(
    blocks: List<ScheduledAutoButtonBlock>,
    activeCategory: String?,
    onShowMenu: (ScheduledAutoButtonBlock) -> Unit
) {
    if (blocks.isEmpty()) return
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = blocks,
            key = { block -> block.scheduleId }
        ) { block ->
            Row(
                modifier = Modifier
                    .animateItem()
                    .border(
                        width = 1.dp,
                        color = categoryColor(block.category).copy(alpha = if (block.isSkippedToday) 0.2f else 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        categoryColor(block.category).copy(alpha = if (block.isSkippedToday) 0.04f else 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { onShowMenu(block) }
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "${timeFormat.format(Date(block.startTime))}-${timeFormat.format(Date(block.endTime))}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowMuted
                )
                Text(
                    text = block.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (block.isSkippedToday) FlowMuted else FlowInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (block.isSkippedToday) {
                    Text(
                        text = "꺼짐",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(FlowPurple.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedTodoBlockList(
    blocks: List<RecommendedTodoBlock>,
    onShowMenu: (RecommendedTodoBlock) -> Unit
) {
    if (blocks.isEmpty()) return
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = FlowPurple.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        FlowPurpleSoft.copy(alpha = 0.42f),
                        RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(onClick = { onShowMenu(block) }, onLongClick = { onShowMenu(block) })
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "${timeFormat.format(Date(block.plannedStartMillis))} 시작 추천",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowMuted
                )
                Text(
                    text = block.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledAutoButtonActionSheet(
    block: ScheduledAutoButtonBlock,
    onDismiss: () -> Unit,
    onSkipToday: (String) -> Unit,
    onUnskipToday: (String) -> Unit,
    onEditSchedule: (String) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                block.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            Text(
                "${timeFormat.format(Date(block.startTime))} - ${timeFormat.format(Date(block.endTime))}",
                fontSize = 13.sp,
                color = FlowMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(
                onClick = {
                    if (block.isSkippedToday) onUnskipToday(block.scheduleId) else onSkipToday(block.scheduleId)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                )
            ) {
                Text(if (block.isSkippedToday) "오늘 다시 켜기" else "오늘만 끄기")
            }
            TextButton(
                onClick = {
                    onEditSchedule(block.scheduleId)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
            ) {
                Text("설정 수정", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
            ) {
                Text("닫기")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedTodoActionSheet(
    block: RecommendedTodoBlock,
    incompleteTodos: List<TodoItem>,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    onSetTime: (hourOfDay: Int) -> Unit,
    onReplaceItem: (TodoItem) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val todoDateFormat = remember { SimpleDateFormat("M월 d일", Locale.KOREAN) }
    val initialHour = remember(block.plannedStartMillis) {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = block.plannedStartMillis
        cal.get(java.util.Calendar.HOUR_OF_DAY)
    }
    var mode by remember(block.itemId) { mutableStateOf("MAIN") }
    var selectedHour by remember(block.itemId) { mutableStateOf(initialHour) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    LaunchedEffect(mode) {
        if (mode == "CHANGE_TIME" || mode == "CHANGE_ITEM") sheetState.expand()
    }
    val blockOverscroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = available

            override suspend fun onPreFling(available: Velocity): Velocity =
                available
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        when (mode) {
            "CHANGE_TIME" -> {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .nestedScroll(blockOverscroll)
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp, bottom = 24.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { mode = "MAIN" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로",
                                tint = FlowInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            "시작 시간 설정",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FlowInk,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Text(
                        "몇 시에 시작할까요?",
                        fontSize = 13.sp,
                        color = FlowMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "%02d:00".format(selectedHour),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF27324D),
                        modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
                    )
                    WheelPickerColumn(
                        values = (0..23).toList(),
                        selectedValue = selectedHour,
                        formatter = { "%02d:00".format(it) },
                        onSelect = { selectedHour = it },
                        selectedHighlightColor = FlowPurple.copy(alpha = 0.44f),
                        unselectedTextColor = FlowMuted.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { mode = "MAIN" },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                        ) {
                            Text("취소", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = { onSetTime(selectedHour) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FlowPurple,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("확인", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            "CHANGE_ITEM" -> {
                val selectableTodos = remember(incompleteTodos, block.todoId) {
                    incompleteTodos.filter { it.id != block.todoId }
                }
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxHeight(0.9f)
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp, bottom = 24.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { mode = "MAIN" }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로",
                                tint = FlowInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            "다른 할 일 선택",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FlowInk,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Text(
                        "선택하면 시간이 자동으로 배정됩니다.",
                        fontSize = 13.sp,
                        color = FlowMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    if (selectableTodos.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "선택할 수 있는 다른 할 일이 없습니다.",
                                fontSize = 14.sp,
                                color = FlowMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val todoDateFormat = remember { SimpleDateFormat("M월 d일", Locale.KOREAN) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .nestedScroll(blockOverscroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectableTodos.size) { index ->
                                val todo = selectableTodos[index]
                                val dateLabel = todo.selectedDate?.let { todoDateFormat.format(Date(it)) }
                                val categoryLabel = when (todo.category) {
                                    TodoCategory.REVIEW -> "복습"
                                    TodoCategory.ASSIGNMENT -> "과제"
                                    else -> null
                                }
                                val metaLabel = listOfNotNull(dateLabel, categoryLabel).joinToString(" · ")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF7F7FA), RoundedCornerShape(14.dp))
                                        .combinedClickable(onClick = { onReplaceItem(todo) })
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(FlowPurple.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = todo.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = FlowInk,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (metaLabel.isNotEmpty()) {
                                            Text(
                                                text = metaLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = FlowMuted,
                                                modifier = Modifier.padding(top = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val dateLabel = block.selectedDate?.let { todoDateFormat.format(Date(it)) } ?: "날짜 없음"
                val categoryLabel = block.category?.let { recommendedTodoCategoryLabel(it) } ?: "일반"
                val metaLabel = "$dateLabel / $categoryLabel / ${timeFormat.format(Date(block.plannedStartMillis))} 시작 추천"
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        block.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk
                    )
                    Text(
                        metaLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlowPurpleDeep,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 5.dp, bottom = 12.dp)
                    )
                    Button(
                        onClick = onStart,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlowPurple,
                            contentColor = Color.White
                        )
                    ) {
                        Text("시작하기", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF18A058),
                            contentColor = Color.White
                        )
                    ) {
                        Text("완료하기", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { mode = "CHANGE_TIME" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                    ) {
                        Text("다른 시간으로 바꾸기", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { mode = "CHANGE_ITEM" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                    ) {
                        Text("다른 할 일로 바꾸기", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                    ) {
                        Text("닫기")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun recommendedTodoCategoryLabel(category: TodoCategory): String {
    return when (category) {
        TodoCategory.NORMAL -> "일반"
        TodoCategory.TODAY -> "오늘"
        TodoCategory.REVIEW -> "복습"
        TodoCategory.ASSIGNMENT -> "마감 있는 일"
        TodoCategory.UNIVERSITY_EXAM -> "마감 있는 일"
    }
}

private fun burdenLabel(level: String): String {
    return when (level) {
        "HEAVY" -> "과중함"
        "LIGHT" -> "가벼움"
        else -> "보통"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainButtonEditBottomSheet(
    category: String,
    config: MainButtonConfig,
    onDismiss: () -> Unit,
    onHide: (String) -> Unit,
    onEnterReorderMode: (String) -> Unit,
    onReplace: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val canHide = config.buttons.size > MainButtonConfig.MIN_BUTTONS

    var showCategoryPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFCFF),
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 헤더
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showCategoryPicker) {
                    IconButton(onClick = { showCategoryPicker = false }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = FlowInk
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (showCategoryPicker) "다른 활동으로 바꾸기" else displayCategory(category),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = FlowMuted
                    )
                }
            }

            if (showCategoryPicker) {
                // 카테고리 선택 화면
                val currentCategories = config.buttons.map { it.category }.toSet()
                val available = MainButtonConfig.ALL_SELECTABLE_CATEGORIES
                    .filter { it !in currentCategories }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    available.forEach { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onReplace(category, candidate) }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            com.example.flowlog.ui.component.CategoryGlyph(
                                category = candidate,
                                tint = com.example.flowlog.ui.component.categoryColor(candidate),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = displayCategory(candidate),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FlowInk
                            )
                        }
                    }
                    if (available.isEmpty()) {
                        Text(
                            text = "모든 활동이 이미 메인에 있습니다.",
                            fontSize = 14.sp,
                            color = FlowMuted,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            } else {
                // 메인 메뉴
                MainButtonMenuRow(
                    label = "다른 활동으로 바꾸기",
                    icon = Icons.Default.Edit,
                    enabled = true,
                    onClick = { showCategoryPicker = true }
                )
                MainButtonMenuRow(
                    label = "자리 바꾸기",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    enabled = true,
                    onClick = { onEnterReorderMode(category) }
                )
                MainButtonMenuRow(
                    label = "메인에서 숨기기",
                    icon = Icons.Default.Close,
                    enabled = canHide,
                    tint = if (canHide) Color(0xFFE53935) else FlowMuted,
                    subtitle = if (!canHide) "최소 ${MainButtonConfig.MIN_BUTTONS}개 버튼이 필요합니다" else null,
                    onClick = { onHide(category) }
                )
            }
        }
    }
}

@Composable
private fun MainButtonMenuRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    tint: Color = FlowInk,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else FlowMuted,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) tint else FlowMuted
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoButtonManagerSheet(
    schedules: List<AutoButtonSchedule>,
    weekSkipDatesByDateKey: Map<Long, Set<String>>,
    calendarPetites: List<OrganizedPetiteEntity>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (AutoButtonSchedule) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onSkipToday: (String) -> Unit,
    onSkipNextDay: (String, Int) -> Unit,
    onUnskipToday: (String) -> Unit,
    onUnskipNextDay: (String, Int) -> Unit,
    onDelete: (String) -> Unit,
    onCalendarPetiteTimeUpdate: (String, String, String) -> Unit,
    onCalendarPetiteDismiss: (String) -> Unit
) {
    var editing by remember { mutableStateOf<AutoButtonSchedule?>(null) }
    var selectedDay by remember { mutableStateOf(currentDayOfWeek()) }
    var actionSchedule by remember { mutableStateOf<AutoButtonSchedule?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedDaySchedules = remember(schedules, selectedDay) {
        schedules
            .filter { selectedDay in it.repeatDays }
            .sortedWith(compareBy<AutoButtonSchedule> { it.startMinuteOfDay }.thenBy { it.title })
    }
    val timetableSchedules = remember(selectedDaySchedules, selectedDay, weekSkipDatesByDateKey) {
        val dateKey = currentWeekDateKeyForDay(selectedDay)
        val skippedIds = weekSkipDatesByDateKey[dateKey] ?: emptySet()
        selectedDaySchedules.filter { it.scheduleId !in skippedIds }
    }
    val blockUpwardOverscroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = if (available.y < 0) available else Offset.Zero

            override suspend fun onPreFling(available: Velocity): Velocity =
                if (available.y < 0) available else Velocity.Zero
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFCFF),
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "반복 루틴 관리",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk
                    )
                    Text(
                        "반복되는 학교/회사 시간을 추가하면\n타임테이블에 예정으로 표시됩니다.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = FlowMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = FlowMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(blockUpwardOverscroll)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WeekdayRoutineSelector(
                    selectedDay = selectedDay,
                    onSelectedDayChange = { selectedDay = it }
                )
                DayRoutineTimetable(
                    selectedDay = selectedDay,
                    schedules = timetableSchedules,
                    onScheduleClick = { actionSchedule = it }
                )
                if (calendarPetites.isNotEmpty()) {
                    Text(
                        "오늘 고정 시간",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowMuted
                    )
                    calendarPetites.forEach { petite ->
                        CalendarPetiteRow(
                            petite = petite,
                            onEditTime = { onCalendarPetiteTimeUpdate(petite.id, it.first, it.second) },
                            onDismiss = { onCalendarPetiteDismiss(petite.id) }
                        )
                    }
                    if (schedules.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(FlowDivider))
                    }
                }
                if (selectedDaySchedules.isNotEmpty()) {
                    if (calendarPetites.isNotEmpty()) {
                        Text(
                            "반복 루틴",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FlowMuted
                        )
                    }
                    selectedDaySchedules.forEach { schedule ->
                        AutoButtonScheduleRow(
                            schedule = schedule,
                            isToday = selectedDay == currentDayOfWeek(),
                            onActionClick = { actionSchedule = schedule },
                            onEdit = { editing = schedule },
                            onToggleEnabled = { onToggleEnabled(schedule.scheduleId, it) },
                            onSkipToday = {
                                if (schedule.isSkippedToday) onUnskipToday(schedule.scheduleId)
                                else onSkipToday(schedule.scheduleId)
                            },
                            onDelete = { confirmDeleteId = schedule.scheduleId }
                        )
                    }
                }
            }
            Button(
                onClick = { editing = defaultAutoButtonSchedule(selectedDay) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("반복 루틴 추가", fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    actionSchedule?.let { schedule ->
        val isSkippedForSelectedDay = weekSkipDatesByDateKey[currentWeekDateKeyForDay(selectedDay)]
            ?.contains(schedule.scheduleId) ?: false
        AutoButtonScheduleActionSheet(
            schedule = schedule,
            selectedDay = selectedDay,
            isSkippedForSelectedDay = isSkippedForSelectedDay,
            onDismiss = { actionSchedule = null },
            onEdit = {
                editing = schedule
                actionSchedule = null
            },
            onToggleNextDay = {
                if (isSkippedForSelectedDay) onUnskipNextDay(schedule.scheduleId, selectedDay)
                else onSkipNextDay(schedule.scheduleId, selectedDay)
                actionSchedule = null
            },
            onSkipToday = {
                if (isSkippedForSelectedDay) onUnskipToday(schedule.scheduleId)
                else onSkipToday(schedule.scheduleId)
                actionSchedule = null
            },
            onDelete = {
                confirmDeleteId = schedule.scheduleId
                actionSchedule = null
            }
        )
    }

    editing?.let { schedule ->
        AutoButtonEditSheet(
            initial = schedule,
            categories = categories,
            onDismiss = { editing = null },
            onSave = {
                onSave(it)
                editing = null
            }
        )
    }

    confirmDeleteId?.let { scheduleId ->
        AlertDialog(
            onDismissRequest = { confirmDeleteId = null },
            containerColor = Color.White,
            titleContentColor = FlowInk,
            textContentColor = FlowMuted,
            title = { Text("반복 루틴 삭제", fontWeight = FontWeight.ExtraBold) },
            text = { Text("이 반복 루틴을 삭제할까요? 되돌릴 수 없습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(scheduleId)
                        confirmDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text("삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDeleteId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun WeekdayRoutineSelector(
    selectedDay: Int,
    onSelectedDayChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF4F4F8), RoundedCornerShape(16.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        dayOptions.forEach { (day, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(
                        if (selectedDay == day) FlowPurple else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(onClick = { onSelectedDayChange(day) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (selectedDay == day) Color.White else FlowMuted
                )
            }
        }
    }
}

@Composable
private fun DayRoutineTimetable(
    selectedDay: Int,
    schedules: List<AutoButtonSchedule>,
    onScheduleClick: (AutoButtonSchedule) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${dayLabel(selectedDay)} 타임테이블",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${schedules.size}개",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FlowMuted
            )
        }

        RoutineTimelinePreview(
            startMinute = schedules.minOfOrNull { it.startMinuteOfDay } ?: 0,
            endMinute = schedules.maxOfOrNull { it.endMinuteOfDay } ?: 0,
            accent = FlowPurple
        )

        if (schedules.isEmpty()) {
            Text(
                "이 요일에는 반복 루틴이 없습니다.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            schedules.forEach { schedule ->
                RoutineTimelineBlockRow(
                    schedule = schedule,
                    onClick = { onScheduleClick(schedule) }
                )
            }
        }
    }
}

@Composable
private fun RoutineTimelineBlockRow(
    schedule: AutoButtonSchedule,
    onClick: () -> Unit
) {
    val accent = categoryColor(schedule.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(accent.copy(alpha = if (schedule.isEnabled) 0.12f else 0.05f), RoundedCornerShape(14.dp))
            .border(1.dp, accent.copy(alpha = if (schedule.isEnabled) 0.26f else 0.10f), RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(accent.copy(alpha = if (schedule.isEnabled) 0.20f else 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CategoryGlyph(
                category = schedule.category,
                tint = if (schedule.isEnabled) accent else FlowMuted,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                schedule.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (schedule.isEnabled) FlowInk else FlowMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatMinuteOfDay(schedule.startMinuteOfDay)} - ${formatMinuteOfDay(schedule.endMinuteOfDay)}  ·  ${displayCategory(schedule.category)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted,
                modifier = Modifier.padding(top = 3.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!schedule.isEnabled || schedule.isSkippedToday) {
            Text(
                if (!schedule.isEnabled) "꺼짐" else "오늘 끔",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(if (!schedule.isEnabled) FlowMuted else FlowPurple, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoButtonScheduleActionSheet(
    schedule: AutoButtonSchedule,
    selectedDay: Int,
    isSkippedForSelectedDay: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleNextDay: () -> Unit,
    onSkipToday: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                schedule.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            Text(
                "${dayLabel(selectedDay)} · ${formatMinuteOfDay(schedule.startMinuteOfDay)} - ${formatMinuteOfDay(schedule.endMinuteOfDay)}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SheetActionRow("수정", Icons.Filled.Edit, onEdit)
            if (selectedDay != currentDayOfWeek() && schedule.canSkipNextDay(selectedDay)) {
                val nextDayLabel = if (isSkippedForSelectedDay) unskipNextDayLabel(selectedDay) else skipNextDayLabel(selectedDay)
                SheetActionRow(nextDayLabel, Icons.Filled.CalendarToday, onToggleNextDay)
            }
            if (selectedDay == currentDayOfWeek()) {
                SheetActionRow(if (isSkippedForSelectedDay) "오늘 다시 켜기" else "오늘만 끄기", Icons.Filled.CalendarToday, onSkipToday)
            }
            SheetActionRow("삭제", Icons.Filled.Delete, onDelete, tint = Color(0xFFD32F2F))
        }
    }
}

@Composable
private fun SheetActionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = FlowInk
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tint
        )
    }
}

@Composable
private fun CalendarPetiteRow(
    petite: OrganizedPetiteEntity,
    onEditTime: (Pair<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    var editingTime by remember { mutableStateOf(false) }
    val accent = Color(0xFF00897B)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "오늘만",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(accent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                    Text(
                        "캘린더",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Text(
                    petite.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${petite.autoStartTime24} - ${petite.autoStartEndTime24}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            IconButton(onClick = { editingTime = true }) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "시간 수정",
                    tint = FlowMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "오늘 삭제",
                    tint = FlowMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    if (editingTime) {
        CalendarPetiteTimeEditSheet(
            initialStart = petite.autoStartTime24,
            initialEnd = petite.autoStartEndTime24,
            onDismiss = { editingTime = false },
            onSave = { start, end ->
                onEditTime(start to end)
                editingTime = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarPetiteTimeEditSheet(
    initialStart: String,
    initialEnd: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var startMinute by remember { mutableStateOf(parseMinuteOfDay(initialStart) ?: 0) }
    var endMinute by remember { mutableStateOf(parseMinuteOfDay(initialEnd) ?: 0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFCFF),
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "오늘 시간 수정",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "오늘 하루만 적용됩니다.",
                fontSize = 13.sp,
                color = FlowMuted
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TimePickerCard(
                    label = "시작",
                    minuteOfDay = startMinute,
                    onChange = { startMinute = it },
                    modifier = Modifier.weight(1f)
                )
                Text("~", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = FlowMuted)
                TimePickerCard(
                    label = "종료",
                    minuteOfDay = endMinute,
                    onChange = { endMinute = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    onSave(
                        formatMinuteOfDay(startMinute),
                        formatMinuteOfDay(endMinute)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("저장", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun AutoButtonScheduleRow(
    schedule: AutoButtonSchedule,
    isToday: Boolean,
    onActionClick: () -> Unit,
    onEdit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSkipToday: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryAccent = categoryColor(schedule.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onActionClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(categoryAccent.copy(alpha = 0.13f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryGlyph(
                            category = schedule.category,
                            tint = categoryAccent,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        displayCategory(schedule.category),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowMuted
                    )
                    if (schedule.isSkippedToday) {
                        Text(
                            text = "오늘 꺼짐",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(FlowPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    schedule.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatMinuteOfDay(schedule.startMinuteOfDay)} - ${formatMinuteOfDay(schedule.endMinuteOfDay)}  ·  ${formatRepeatDays(schedule.repeatDays)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "수정",
                    tint = FlowMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint = FlowMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        RoutineTimelinePreview(
            startMinute = schedule.startMinuteOfDay,
            endMinute = schedule.endMinuteOfDay,
            accent = categoryAccent
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF7F6FD), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "활성화",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = FlowMuted
            )
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggleEnabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = FlowPurple,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFC8CBD4),
                    uncheckedBorderColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isToday) {
                TextButton(
                    onClick = onSkipToday,
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowPurple),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (schedule.isSkippedToday) "오늘 다시 켜기" else "오늘만 끄기",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineTimelinePreview(
    startMinute: Int,
    endMinute: Int,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted.copy(alpha = 0.64f),
                    modifier = Modifier.weight(1f),
                    textAlign = when (label) {
                        "00:00" -> TextAlign.Start
                        "24:00" -> TextAlign.End
                        else -> TextAlign.Center
                    }
                )
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val trackTop = 2.dp.toPx()
            val trackHeight = 24.dp.toPx()
            val segmentGap = 2.dp.toPx()
            val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            repeat(4) { index ->
                val left = size.width * index / 4f + if (index == 0) 0f else segmentGap / 2f
                val right = size.width * (index + 1) / 4f - if (index == 3) 0f else segmentGap / 2f
                drawRoundRect(
                    color = Color(0xFFF0F1F5),
                    topLeft = Offset(left, trackTop),
                    size = Size(right - left, trackHeight),
                    cornerRadius = radius
                )
                if (index > 0) {
                    val x = size.width * index / 4f
                    drawLine(
                        color = Color(0xFFD7D9E2),
                        start = Offset(x, trackTop),
                        end = Offset(x, trackTop + trackHeight),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            val startFraction = (startMinute.coerceIn(0, 1439) / 1440f).coerceIn(0f, 1f)
            val endFraction = (endMinute.coerceIn(0, 1439) / 1440f).coerceIn(startFraction, 1f)
            val startX = size.width * startFraction
            val endX = (size.width * endFraction).coerceAtLeast(startX + 4.dp.toPx())
            drawRoundRect(
                color = accent.copy(alpha = 0.86f),
                topLeft = Offset(startX, trackTop),
                size = Size((endX - startX).coerceAtMost(size.width - startX), trackHeight),
                cornerRadius = radius
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AutoButtonEditSheet(
    initial: AutoButtonSchedule,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (AutoButtonSchedule) -> Unit
) {
    var title by remember(initial.scheduleId) { mutableStateOf(initial.title) }
    val routineCategories = remember(categories) {
        (listOf("COMPANY", "SCHOOL") + categories)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }
    var category by remember(initial.scheduleId, routineCategories) {
        mutableStateOf(initial.category.takeIf { it in routineCategories } ?: "SCHOOL")
    }
    var startMinute by remember(initial.scheduleId) { mutableStateOf(initial.startMinuteOfDay.coerceIn(0, 23 * 60 + 59)) }
    var endMinute by remember(initial.scheduleId) { mutableStateOf(initial.endMinuteOfDay.coerceIn(0, 23 * 60 + 59)) }
    var repeatDays by remember(initial.scheduleId) { mutableStateOf(initial.repeatDays.ifEmpty { weekdayDefaults() }) }
    var notifyOnStart by remember(initial.scheduleId) { mutableStateOf(initial.notifyOnStart) }
    var notifyOnEnd by remember(initial.scheduleId) { mutableStateOf(initial.notifyOnEnd) }
    var isEnabled by remember(initial.scheduleId) { mutableStateOf(initial.isEnabled) }
    var errorMessage by remember(initial.scheduleId) { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = Color(0xFFFCFCFF),
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(top = 16.dp, start = 22.dp, end = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = FlowInk,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (initial.scheduleId.isBlank()) "반복 루틴 추가" else "반복 루틴 수정",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk
                    )
                    Text(
                        "반복되는 학교/회사 시간을 입력하세요.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlowMuted,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "카테고리",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F4F8), RoundedCornerShape(18.dp))
                        .border(1.dp, FlowDivider, RoundedCornerShape(18.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    routineCategories.forEach { item ->
                        AutoButtonCategorySegment(
                            category = item,
                            selected = category == item,
                            onClick = {
                                val previousCategoryTitle = displayCategory(category)
                                category = item
                                if (title.isBlank() || title == previousCategoryTitle) {
                                    title = displayCategory(item)
                                }
                            }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "루틴 이름",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text(
                            displayCategory(category),
                            color = FlowMuted.copy(alpha = 0.58f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlowPurple,
                        unfocusedBorderColor = FlowDivider,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = FlowInk,
                        unfocusedTextColor = FlowInk,
                        cursorColor = FlowPurple
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("시간", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TimePickerCard(
                        label = "시작",
                        minuteOfDay = startMinute,
                        onChange = { startMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "~",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowMuted
                    )
                    TimePickerCard(
                        label = "종료",
                        minuteOfDay = endMinute,
                        onChange = { endMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("반복 요일", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayOptions.forEach { (day, label) ->
                        RepeatDayButton(
                            label = label,
                            selected = day in repeatDays,
                            onClick = {
                                repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            RoutineInfoPanel()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "알림 설정",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk,
                        modifier = Modifier.weight(1f)
                    )
                    Text("선택", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FlowMuted)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, FlowDivider, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ToggleRow("활성화", isEnabled) { isEnabled = it }
                    ToggleRow("시작 알림", notifyOnStart) { notifyOnStart = it }
                    ToggleRow("종료 알림", notifyOnEnd) { notifyOnEnd = it }
                }
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                ) {
                    Text("취소", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val cleanTitle = title.trim().ifBlank { displayCategory(category) }
                        errorMessage = when {
                            endMinute <= startMinute -> "종료 시간은 시작 시간보다 늦어야 합니다."
                            repeatDays.isEmpty() -> "반복 요일을 하나 이상 선택해주세요."
                            else -> null
                        }
                        if (errorMessage == null) {
                            onSave(
                                initial.copy(
                                    title = cleanTitle,
                                    category = category,
                                    repeatDays = repeatDays,
                                    startMinuteOfDay = startMinute,
                                    endMinuteOfDay = endMinute,
                                    isEnabled = isEnabled,
                                    notifyOnStart = notifyOnStart,
                                    notifyOnEnd = notifyOnEnd
                                )
                            )
                        }
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("저장", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun AutoButtonCategorySegment(
    category: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = categoryColor(category)
    Row(
        modifier = modifier
            .height(42.dp)
            .background(
                if (selected) FlowPurple else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .combinedClickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CategoryGlyph(
            category = category,
            tint = if (selected) Color.White else accent.copy(alpha = 0.52f),
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = displayCategory(category),
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) Color.White else FlowMuted
        )
    }
}

@Composable
private fun RepeatDayButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(47.dp)
            .background(
                if (selected) FlowPurple else Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (selected) FlowPurple.copy(alpha = 0.28f) else FlowDivider,
                RoundedCornerShape(12.dp)
            )
            .combinedClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) Color.White else FlowMuted
        )
    }
}

@Composable
private fun RoutineInfoPanel() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlowPurpleSoft.copy(alpha = 0.56f), RoundedCornerShape(14.dp))
            .border(1.dp, FlowPurple.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = FlowMuted.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                "선택한 요일에 반복 루틴이 타임라인에 표시됩니다.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FlowMuted
            )
            Text(
                "오늘 하루만 비활성화하려면 목록에서 ‘오늘만 끄기’를 사용하세요.",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun TimePickerCard(
    label: String,
    minuteOfDay: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPickerOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .height(124.dp)
            .background(Color(0xFFF9F9FC), RoundedCornerShape(12.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { isPickerOpen = true }
            )
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowPurple)
        Text(
            text = formatMinuteOfDay(minuteOfDay),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowInk,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
        )
        Text(
            "탭하여 변경",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted.copy(alpha = 0.6f)
        )
    }

    if (isPickerOpen) {
        TimePickerSheet(
            title = "$label 시간 설정",
            subtitle = "$label 시간을 선택하세요.",
            minuteOfDay = minuteOfDay,
            onDismiss = { isPickerOpen = false },
            onConfirm = {
                onChange(it)
                isPickerOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerSheet(
    title: String,
    subtitle: String,
    minuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hour by remember(minuteOfDay) { mutableStateOf(minuteOfDay.coerceIn(0, 1439) / 60) }
    var minute by remember(minuteOfDay) { mutableStateOf(minuteOfDay.coerceIn(0, 1439) % 60) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
        ) {
            PickerWaveBackground(
                color = FlowPurpleSoft.copy(alpha = 0.54f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
                    .align(Alignment.BottomCenter)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    text = "%02d:%02d".format(hour, minute),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF27324D),
                    modifier = Modifier.padding(top = 28.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    WheelPickerColumn(
                        values = (0..23).toList(),
                        selectedValue = hour,
                        formatter = { "%02d".format(it) },
                        onSelect = { hour = it },
                        selectedHighlightColor = FlowPurple.copy(alpha = 0.44f),
                        unselectedTextColor = FlowMuted.copy(alpha = 0.45f)
                    )
                    Text(
                        ":",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF27324D)
                    )
                    WheelPickerColumn(
                        values = (0..55 step 5).toList(),
                        selectedValue = minute,
                        formatter = { "%02d".format(it) },
                        onSelect = { minute = it },
                        selectedHighlightColor = FlowPurple.copy(alpha = 0.44f),
                        unselectedTextColor = FlowMuted.copy(alpha = 0.45f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                    ) {
                        Text("취소", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { onConfirm(hour * 60 + minute) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FlowPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("확인", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF9F9FC), RoundedCornerShape(14.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(14.dp))
            .padding(12.dp),
        content = content
    )
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text, fontSize = 13.sp, color = FlowInk)
    }
}

private val dayOptions = listOf(
    Calendar.SUNDAY to "일",
    Calendar.MONDAY to "월",
    Calendar.TUESDAY to "화",
    Calendar.WEDNESDAY to "수",
    Calendar.THURSDAY to "목",
    Calendar.FRIDAY to "금",
    Calendar.SATURDAY to "토"
)

private fun weekdayDefaults(): Set<Int> = setOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY
)

private fun defaultAutoButtonSchedule(selectedDay: Int = Calendar.MONDAY): AutoButtonSchedule {
    return AutoButtonSchedule(
        title = "학교",
        category = "SCHOOL",
        repeatDays = setOf(selectedDay),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 17 * 60
    )
}

private fun currentDayOfWeek(): Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

private fun dayLabel(day: Int): String = dayOptions.firstOrNull { it.first == day }?.second.orEmpty()

private fun skipNextDayLabel(day: Int): String {
    return "돌아오는 ${fullDayLabel(day)} 끄기"
}

private fun unskipNextDayLabel(day: Int): String {
    return "돌아오는 ${fullDayLabel(day)} 다시 켜기"
}

private fun AutoButtonSchedule.canSkipNextDay(day: Int): Boolean {
    if (source != "CALENDAR" || sourceDateKeys.isEmpty()) return true
    return nextDateKeyForDay(day) in sourceDateKeys
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

// 오늘 포함 0~6일 뒤 범위 내 해당 요일의 dateKey (오늘이면 오늘 dateKey 반환)
private fun currentWeekDateKeyForDay(dayOfWeek: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val currentDay = get(Calendar.DAY_OF_WEEK)
        val daysUntil = (dayOfWeek - currentDay + 7) % 7
        add(Calendar.DAY_OF_YEAR, daysUntil)
    }.timeInMillis
}

private fun fullDayLabel(day: Int): String = when (day) {
    Calendar.SUNDAY -> "일요일"
    Calendar.MONDAY -> "월요일"
    Calendar.TUESDAY -> "화요일"
    Calendar.WEDNESDAY -> "수요일"
    Calendar.THURSDAY -> "목요일"
    Calendar.FRIDAY -> "금요일"
    Calendar.SATURDAY -> "토요일"
    else -> "해당 요일"
}

private fun formatRepeatDays(days: Set<Int>): String {
    if (days == weekdayDefaults()) return "평일"
    return dayOptions.filter { it.first in days }.joinToString(" ") { it.second }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val minute = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    return "%02d:%02d".format(minute / 60, minute % 60)
}

private fun parseMinuteOfDay(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}
