package com.example.flowlog.ui.screen

import android.graphics.Paint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.FocusModeStore
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.notification.FocusDndController
import com.example.flowlog.debug.CityTimetablePreset
import com.example.flowlog.debug.CityTimetableSamples
import com.example.flowlog.debug.SampleTimetableData
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.ui.city.CityTimetableCard
import com.example.flowlog.data.model.AutoButtonSchedule
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
import com.example.flowlog.ui.viewmodel.AnalyticsState
import com.example.flowlog.ui.viewmodel.CategoryStat
import com.example.flowlog.ui.viewmodel.TrendPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FlowPurple = Color(0xFF5140D8)
private val FlowPurpleDeep = Color(0xFF2F238F)
private val FlowPurpleSoft = Color(0xFFEDE9FF)
private val FlowInk = Color(0xFF10182C)
private val FlowMuted = Color(0xFF697386)
private val FlowDivider = Color(0xFFE8E8EE)
private const val TIMETABLE_TAG = "FlowlogTimetable"
private const val MERGE_THRESHOLD_MILLIS = 10 * 60 * 1000L
private const val RECOMMENDED_TODO_DISPLAY_DURATION_MILLIS = 60 * 60 * 1000L
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
    isDeveloperMode: Boolean = false
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val restoredPinnedTimer = remember(context) {
        TimerStateStore.getPinnedTimer(context)
    }
    var localAutoButtonManagerOpen by remember { mutableStateOf(false) }
    val activityCategories = remember {
        listOf(
            "TOOTHBRUSH",
            "SNACK",
            "MEAL",
            "STUDY",
            "WORK",
            "DEVELOPMENT",
            "READING",
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
    val visibleActivities = if (isActivityListExpanded) {
        displayActivities
    } else {
        displayActivities.take(3)
    }
    val hiddenActivityCount = displayActivities.size - visibleActivities.size

    val todayText = remember {
        SimpleDateFormat("M월 d일 (E)", Locale.KOREAN).format(Date())
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F8F9)),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                HomeHeader(
                dateText = todayText,
                actions = topActions
            )
        }

        item {
            val titleSuggestions = remember(uiState.currentCategory, uiState.allActivities) {
                buildTitleSuggestions(
                    category = uiState.currentCategory,
                    activities = uiState.allActivities
                )
            }
            TodayFlowCard(
                isRunning = uiState.isRunning,
                currentCategory = uiState.currentCategory,
                elapsedTime = uiState.elapsedTime,
                timerGoalMillis = uiState.timerGoalMillis,
                statusMessage = uiState.statusMessage,
                appliedTitle = uiState.pendingTitle.orEmpty(),
                titleSuggestions = titleSuggestions,
                promotedButtons = uiState.promotedButtons,
                onPinQuickCategory = { category ->
                    val startedAt = System.currentTimeMillis()
                    pinnedQuickCategory = category
                    pinnedQuickStartedAt = startedAt
                    viewModel.startOverlappingActivity(category, startedAt)
                },
                pinnedQuickCategory = pinnedQuickCategory,
                onStop = { title ->
                    viewModel.stopActivityAndSave(title)
                },
                onApplyTitle = { title ->
                    viewModel.setRunningActivityTitle(title)
                },
                onStart = { category ->
                    if (!uiState.isRunning || category == "SNACK" || category == "TOOTHBRUSH") {
                        viewModel.startActivity(category)
                    }
                },
                isFocusModeActive = uiState.isFocusModeActive,
                focusModeEndsAtMillis = uiState.focusModeEndsAtMillis,
                onStartFocusMode = { enableDnd -> viewModel.startFocusMode(enableDnd) },
                onStopFocusMode = { viewModel.stopFocusMode() }
            )
        }

        item {
            QuickTimerSection(
                categories = categories,
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
                }
            )
        }

        item {
            TimetableCard(
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
                onSetRecommendedTime = { block, hour -> if (!isDeveloperMode) viewModel.setRecommendedTodoTime(block, hour) },
                onReplaceRecommendedItem = { block, todo -> if (!isDeveloperMode) viewModel.replaceRecommendedTodoItem(block, todo) },
                isDeveloperMode = isDeveloperMode,
                samplePresetIndex = samplePresetIndex,
                onCyclePreset = { samplePresetIndex = (samplePresetIndex + 1) % SampleTimetableData.presetCount }
            )
        }

        item {
            RecentRecordsCard(
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

        if (displayActivities.size > 3) {
            item {
                androidx.compose.material3.TextButton(
                    onClick = { isActivityListExpanded = !isActivityListExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = FlowPurpleSoft.copy(alpha = 0.5f),
                        contentColor = FlowPurple
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
            AnalyticsCard(analytics = uiState.analytics)
        }

    } // LazyColumn 닫기

    uiState.editingActivity?.let { editingActivity ->
        EditActivityDialog(
            activity = editingActivity,
            categories = activityCategories,
            isVisible = true,
            onSave = { category, title, note ->
                viewModel.saveEditedActivity(category, title, note)
            },
            onDismiss = {
                viewModel.cancelEditActivity()
            }
        )
    }

    if (autoButtonManagerOpen || localAutoButtonManagerOpen) {
        AutoButtonManagerSheet(
            schedules = uiState.autoButtonSchedules,
            categories = activityCategories,
            onDismiss = {
                localAutoButtonManagerOpen = false
                onAutoButtonManagerDismiss()
            },
            onSave = viewModel::saveAutoButtonSchedule,
            onToggleEnabled = viewModel::setAutoButtonEnabled,
            onSkipToday = viewModel::skipAutoButtonToday,
            onUnskipToday = viewModel::unskipAutoButtonToday,
            onDelete = viewModel::deleteAutoButtonSchedule
        )
    }

    }
}

@Composable
private fun HomeHeader(
    dateText: String,
    actions: @Composable () -> Unit
) {
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
                color = FlowInk
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
    elapsedTime: Long,
    timerGoalMillis: Long,
    statusMessage: String?,
    appliedTitle: String,
    titleSuggestions: List<String>,
    promotedButtons: List<String>,
    onPinQuickCategory: (String) -> Unit,
    pinnedQuickCategory: String?,
    onStop: (String) -> Unit,
    onApplyTitle: (String) -> Unit,
    onStart: (String) -> Unit,
    isFocusModeActive: Boolean = false,
    focusModeEndsAtMillis: Long = 0L,
    onStartFocusMode: (enableDnd: Boolean) -> Unit = {},
    onStopFocusMode: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                TimerPage(
                    currentCategory = currentCategory,
                    elapsedTime = elapsedTime,
                    timerGoalMillis = timerGoalMillis,
                    initialAppliedTitle = appliedTitle,
                    titleSuggestions = titleSuggestions,
                    onStop = onStop,
                    onApplyTitle = onApplyTitle,
                    isFocusModeActive = isFocusModeActive,
                    focusModeEndsAtMillis = focusModeEndsAtMillis,
                    onStartFocusMode = onStartFocusMode,
                    onStopFocusMode = onStopFocusMode
                )
            } else {
                FlowStartPage(
                    promotedButtons = promotedButtons,
                    activeCategory = currentCategory.takeIf { isRunning },
                    onPinQuickCategory = onPinQuickCategory,
                    pinnedQuickCategory = pinnedQuickCategory,
                    statusMessage = statusMessage,
                    onStart = onStart
                )
            }
        }
    }
}

@Composable
private fun TimerPage(
    currentCategory: String,
    elapsedTime: Long,
    timerGoalMillis: Long,
    initialAppliedTitle: String,
    titleSuggestions: List<String>,
    onStop: (String) -> Unit,
    onApplyTitle: (String) -> Unit,
    isFocusModeActive: Boolean = false,
    focusModeEndsAtMillis: Long = 0L,
    onStartFocusMode: (enableDnd: Boolean) -> Unit = {},
    onStopFocusMode: () -> Unit = {}
) {
    var title by remember(currentCategory) { mutableStateOf("") }
    var appliedTitle by remember(currentCategory) { mutableStateOf(initialAppliedTitle) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val isFocusCategory = remember(currentCategory) {
        currentCategory in setOf("STUDY", "DEVELOPMENT", "WORK", "TODO", "ETC")
    }
    var showFocusConfirmDialog by remember { mutableStateOf(false) }
    var showFocusStartedDialog by remember { mutableStateOf(false) }
    var showFocusStopConfirmDialog by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }
    var doNotShowAgain by remember { mutableStateOf(false) }
    // DND 체크박스 상태: 저장된 선호값으로 초기화
    var enableDnd by remember { mutableStateOf(FocusModeStore.getEnableSystemDndForFocus(context)) }
    // 시작됩니다 다이얼로그에서 DND 활성 여부 표시용
    var focusModeStartedWithDnd by remember { mutableStateOf(false) }
    val progressCycleMillis = if (currentCategory == "EXPERIMENT_3") {
        TimeUnit.SECONDS.toMillis(5)
    } else {
        timerGoalMillis.coerceAtLeast(1L)
    }
    val cycleProgress = (elapsedTime % progressCycleMillis).toFloat() / progressCycleMillis.toFloat()
    val progress = if (elapsedTime > 0L) cycleProgress.coerceAtLeast(0.01f) else 0f
    val isOnFire = elapsedTime >= progressCycleMillis

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "진행 중",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowPurple
                )
                Text(
                    text = displayCategory(currentCategory),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowPurple,
                    modifier = Modifier.padding(top = 14.dp)
                )
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
                isOnFire = isOnFire,
                isRunning = true,
                modifier = Modifier.size(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        if (titleSuggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(titleSuggestions.size) { index ->
                    SuggestionChip(
                        onClick = { title = titleSuggestions[index] },
                        label = {
                            Text(
                                text = titleSuggestions[index],
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = FlowPurpleSoft,
                            labelColor = FlowPurple
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
                onValueChange = { title = it },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .border(1.dp, FlowDivider, RoundedCornerShape(13.dp))
                    .background(Color.White, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp),
                singleLine = true,
                textStyle = TextStyle(
                    color = FlowInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(FlowPurple),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = FlowPurple.copy(alpha = 0.58f),
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
                    val cleanTitle = title.trim()
                    title = cleanTitle
                    appliedTitle = cleanTitle
                    onApplyTitle(cleanTitle)
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .width(66.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
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
        if (isFocusCategory) {
            Spacer(modifier = Modifier.height(10.dp))
            if (isFocusModeActive) {
                val focusRemainingMillis = (focusModeEndsAtMillis - System.currentTimeMillis())
                    .coerceAtLeast(0L)
                OutlinedButton(
                    onClick = { showFocusStopConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = FlowPurpleSoft,
                        contentColor = FlowPurple
                    ),
                    border = BorderStroke(1.dp, FlowPurple.copy(alpha = 0.4f)),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "집중 중  ·  ${formatCountdown(focusRemainingMillis)}  남음",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                OutlinedButton(
                    onClick = {
                        if (FocusModeStore.isFocusConfirmAcknowledged(context)) {
                            val dndPref = FocusModeStore.getEnableSystemDndForFocus(context)
                            // 권한 만료 시 DND 없이 시작 (사용자 차단 방지)
                            val effectiveDnd = dndPref && FocusDndController.hasPolicyAccess(context)
                            focusModeStartedWithDnd = effectiveDnd
                            onStartFocusMode(effectiveDnd)
                            showFocusStartedDialog = true
                        } else {
                            enableDnd = FocusModeStore.getEnableSystemDndForFocus(context)
                            doNotShowAgain = false
                            showFocusConfirmDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = FlowPurple
                    ),
                    border = BorderStroke(1.dp, FlowPurple.copy(alpha = 0.4f)),
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
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FlowDivider)
        )
        Button(
            onClick = {
                val finalTitle = appliedTitle.ifBlank { title }.trim()
                onApplyTitle(finalTitle)
                onStop(finalTitle)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFFFF4D5E)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            Text(
                text = "종료하기",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }

    if (showFocusConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFocusConfirmDialog = false },
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
                            checked = enableDnd,
                            onCheckedChange = { checked ->
                                if (checked && !FocusDndController.hasPolicyAccess(context)) {
                                    showDndPermissionDialog = true
                                    // 권한 없으면 체크 반영 안 함
                                } else {
                                    enableDnd = checked
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = FlowPurple,
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
                            checked = doNotShowAgain,
                            onCheckedChange = { doNotShowAgain = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = FlowPurple,
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
                TextButton(onClick = { showFocusConfirmDialog = false }) {
                    Text("취소", color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (doNotShowAgain) FocusModeStore.setFocusConfirmAcknowledged(context)
                        showFocusConfirmDialog = false
                        focusModeStartedWithDnd = enableDnd
                        onStartFocusMode(enableDnd)
                        showFocusStartedDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("시작하기", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showFocusStartedDialog) {
        AlertDialog(
            onDismissRequest = { showFocusStartedDialog = false },
            containerColor = Color.White,
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = FlowPurple,
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
                val startedText = if (focusModeStartedWithDnd) {
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
                    onClick = { showFocusStartedDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("확인", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showFocusStopConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFocusStopConfirmDialog = false },
            containerColor = Color.White,
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
                TextButton(onClick = { showFocusStopConfirmDialog = false }) {
                    Text("취소", color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStopFocusMode()
                        showFocusStopConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlowPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("종료", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    if (showDndPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showDndPermissionDialog = false },
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
                TextButton(onClick = { showDndPermissionDialog = false }) {
                    Text("나중에", color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDndPermissionDialog = false
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

private fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%d:%02d:%02d".format(h, m, s)
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
        "WASH" -> "씻기"
        "REST" -> "휴식"
        "SCHOOL" -> "학교"
        else -> "활동"
    }
}

private val DEFAULT_MAIN_BUTTON_CATEGORIES = listOf(
    "SLEEP", "REST",
    "WORK", "STUDY",
    "EXERCISE", "WASH",
    "MEAL", "ETC"
)

@Composable
private fun FlowStartPage(
    promotedButtons: List<String>,
    activeCategory: String?,
    onPinQuickCategory: (String) -> Unit,
    pinnedQuickCategory: String?,
    statusMessage: String?,
    onStart: (String) -> Unit
) {
    // 기본 6개(3행) + promoted 버튼 + 하단 2개(식사/기타)
    val displayCategories = remember(promotedButtons) {
        if (promotedButtons.isNotEmpty()) {
            DEFAULT_MAIN_BUTTON_CATEGORIES.take(6) +
                promotedButtons.asReversed() +
                DEFAULT_MAIN_BUTTON_CATEGORIES.drop(6)
        } else {
            DEFAULT_MAIN_BUTTON_CATEGORIES
        }
    }

    // 행 수에 따른 그리드 높이: n행 × 84dp + (n-1) × 12dp 간격 + 8dp 여유(카드 그림자 클리핑 방지)
    var hidingPromotedCategories by remember(promotedButtons) { mutableStateOf(emptySet<String>()) }
    var hiddenPromotedCategories by remember(promotedButtons) { mutableStateOf(emptySet<String>()) }
    LaunchedEffect(pinnedQuickCategory) {
        if (pinnedQuickCategory == null) {
            hidingPromotedCategories = emptySet()
            hiddenPromotedCategories = emptySet()
        }
    }
    LaunchedEffect(hidingPromotedCategories) {
        if (hidingPromotedCategories.isNotEmpty()) {
            kotlinx.coroutines.delay(300L)
            hiddenPromotedCategories = hiddenPromotedCategories + hidingPromotedCategories
            hidingPromotedCategories = emptySet()
        }
    }
    val visibleCategories = remember(displayCategories, activeCategory, hiddenPromotedCategories, pinnedQuickCategory) {
        displayCategories.filterNot { category ->
            category == activeCategory ||
                category == pinnedQuickCategory ||
                category in hiddenPromotedCategories
        }
    }
    val rowCount = (visibleCategories.size + 1) / 2
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
                    text = "활동 시작",
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
            FlowPageDots(activePage = 1)
        }

        Text(
            text = "활동",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
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
                items = visibleCategories,
                key = { category -> category }
            ) { category ->
                AnimatedVisibility(
                    visible = category !in hidingPromotedCategories,
                    enter = slideInVertically(
                        animationSpec = tween(260),
                        initialOffsetY = { -it / 3 }
                    ) + fadeIn(animationSpec = tween(180)),
                    exit = slideOutVertically(
                        animationSpec = tween(280),
                        targetOffsetY = { it }
                    ) + fadeOut(animationSpec = tween(180)),
                    modifier = Modifier.animateItem()
                ) {
                    CategoryButton(
                        category = category,
                        label = displayCategory(category),
                        onClick = {
                            when {
                                category == "SCHOOL" || category == "COMPANY" -> {
                                    if (pinnedQuickCategory == null) {
                                        hidingPromotedCategories = hidingPromotedCategories + category
                                        onPinQuickCategory(category)
                                    }
                                }
                                category != activeCategory -> onStart(category)
                            }
                        }
                    )
                }
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
private fun QuickTimerSection(
    categories: List<String>,
    pinnedCategory: String?,
    onUnpinCategory: () -> Unit,
    isBrushTimerRunning: Boolean,
    brushDoneEndsAtMillis: Long,
    snackButtonEndsAtMillis: Long,
    onStart: (String) -> Unit
) {
    val quickCategories = remember(categories) {
        categories.filter { it == "TOOTHBRUSH" || it == "SNACK" }
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
            kotlinx.coroutines.delay(30_000L)
            snackLabel = formatSnackCountdown(snackButtonEndsAtMillis)
        }
        snackLabel = "간식"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        pinnedCategory?.let { category ->
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
            color = FlowMuted,
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
                        "SNACK" -> snackLabel
                        else -> displayCategory(category)
                    },
                    onClick = { onStart(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

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
    if (remaining <= 0L) return "간식"
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
    modifier: Modifier = Modifier
) {
    val fireTransition = rememberInfiniteTransition(label = "flow-fire")
    val firePhase by fireTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 920),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow-fire-phase"
    )
    val firePulse by fireTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flow-fire-pulse"
    )

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
                color = if (isOnFire) Color(0xFFFF7A2F) else FlowPurple,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            drawArc(
                color = if (isOnFire) Color(0xFFFFE18A) else FlowPurple.copy(alpha = 0.38f),
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
                tint = FlowPurple,
                modifier = Modifier.size(58.dp)
            )
        } else {
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
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPurpleSoft, contentColor = FlowPurple)
                ) {
                    Text("초기화", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onUndo,
                    enabled = canUndo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPurpleSoft, contentColor = FlowPurple)
                ) {
                    Text("되돌리기", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
        }
        Text(
            text = "${durationMinutes}분",
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowMuted,
            modifier = Modifier.padding(end = 2.dp)
        )
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
        IconButton(
            onClick = { onEdit(activity.id) },
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "수정",
                tint = Color(0xFF697386),
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(
            onClick = { onDelete(activity) },
            modifier = Modifier.size(34.dp)
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
private fun AnalyticsCard(analytics: AnalyticsState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                    tint = FlowPurple.copy(alpha = 0.62f),
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

@Composable
private fun TrendBars(points: List<TrendPoint>, stats: List<CategoryStat>) {
    val visiblePoints = remember(points) {
        if (points.size > 14) {
            points.filterIndexed { index, _ -> index % 3 == 0 || index == points.lastIndex }
        } else {
            points
        }
    }
    val categories = remember(visiblePoints, stats) {
        val statMap = stats.associate { it.category to it.averageMillis }
        visiblePoints
            .flatMap { it.categoryMillis.keys }
            .distinct()
            .sortedBy { statMap[it] ?: 0L }
    }
    val maxMillis = remember(visiblePoints) {
        visiblePoints.maxOfOrNull { it.totalMillis }?.coerceAtLeast(1L) ?: 1L
    }

    if (categories.isEmpty()) {
        Text("아직 추세 데이터가 없습니다.", fontSize = 12.sp, color = FlowMuted)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        visiblePoints.forEach { point ->
            val totalHeight = ((point.totalMillis.toFloat() / maxMillis.toFloat()) * 66f)
                .coerceAtLeast(if (point.totalMillis > 0L) 3f else 0f)
                .dp
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Column(
                    modifier = Modifier
                        .width(14.dp)
                        .height(totalHeight),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    categories.forEach { category ->
                        val categoryMillis = point.categoryMillis[category] ?: 0L
                        if (categoryMillis > 0L && point.totalMillis > 0L) {
                            val segmentHeight = ((totalHeight.value * categoryMillis.toFloat()) / point.totalMillis.toFloat())
                                .coerceAtLeast(2f)
                                .dp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(segmentHeight)
                                    .background(categoryColor(category))
                            )
                        }
                    }
                }
                Text(point.label, fontSize = 9.sp, color = FlowMuted, maxLines = 1)
            }
        }
    }

    CategoryLegend(categories)
}

@Composable
private fun CategoryLegend(categories: List<String>) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(categoryColor(category), shape = MaterialTheme.shapes.small)
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
    onSetRecommendedTime: (RecommendedTodoBlock, Int) -> Unit,
    onReplaceRecommendedItem: (RecommendedTodoBlock, TodoItem) -> Unit,
    isDeveloperMode: Boolean = false,
    samplePresetIndex: Int = 0,
    onCyclePreset: () -> Unit = {}
) {
    var selectedBlock by remember { mutableStateOf<ScheduledAutoButtonBlock?>(null) }
    var selectedRecommendedBlock by remember { mutableStateOf<RecommendedTodoBlock?>(null) }
    var pendingSleepRange by remember { mutableStateOf<EmptyRange?>(null) }
    val displayActivitySegments = remember(activities) {
        buildDisplayActivitySegments(activities).also { segments ->
            Log.d(
                TIMETABLE_TAG,
                "displaySegments original=${activities.size}, display=${segments.size}, hidden=${segments.sumOf { it.hiddenSegments.size }}"
            )
        }
    }
    val visibleScheduledBlocks = remember(scheduledBlocks, activeCategory) {
        scheduledBlocks.filterNot { block -> block.category == activeCategory }
    }
    val timelineItems = remember(displayActivitySegments, visibleScheduledBlocks, recommendedBlocks) {
        displayActivitySegments.map { TimelineBlock.ActualActivity(it) } +
            visibleScheduledBlocks.map { TimelineBlock.ScheduledAutoButton(it) } +
            recommendedBlocks.map { TimelineBlock.RecommendedTodo(it) }
    }.sortedBy { block ->
        when (block) {
            is TimelineBlock.ActualActivity -> block.segment.startTime
            is TimelineBlock.ScheduledAutoButton -> block.block.startTime
            is TimelineBlock.RecommendedTodo -> block.block.plannedStartMillis
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                        containerColor = FlowPurpleSoft.copy(alpha = 0.72f),
                        contentColor = FlowPurpleDeep
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
            Text(
                text = "오늘 활동 흐름을 한 줄 색상 막대로 보여줍니다.",
                fontSize = 12.sp,
                color = FlowMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

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
                        Log.d("SleepFill", "onEmptySpaceLongPress: pressedTimeMillis=$pressedTimeMillis")
                        val activitiesForRange = allActivities.ifEmpty { activities }
                        val range = findEmptyRangeAroundPressedTime(
                            pressedTimeMillis = pressedTimeMillis,
                            allActivities = activitiesForRange,
                            runningTimerStartMillis = timerStartMillis
                        )
                        Log.d("SleepFill", "emptyRange=$range")
                        val isCandidate = range != null && isSleepCandidateRange(range.startMillis, range.endMillis)
                        Log.d("SleepFill", "isSleepCandidate=$isCandidate")
                        if (isCandidate) {
                            Log.d("SleepFill", "pendingSleepRange 설정: $range")
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
        }
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
            allActivities = allActivities.ifEmpty { activities },
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
    val windowBlocks = blocks.filter { it !is TimelineBlock.RecommendedTodo || !it.block.isBubbleOnly }
        .ifEmpty { blocks }
    val firstStart = windowBlocks.minOf {
        when (it) {
            is TimelineBlock.ActualActivity -> it.segment.startTime
            is TimelineBlock.ScheduledAutoButton -> it.block.startTime
            is TimelineBlock.RecommendedTodo -> it.block.plannedStartMillis
        }
    }
    val lastEnd = windowBlocks.maxOf {
        when (it) {
            is TimelineBlock.ActualActivity -> it.segment.endTime.coerceAtLeast(it.segment.startTime + 1L)
            is TimelineBlock.ScheduledAutoButton -> it.block.endTime.coerceAtLeast(it.block.startTime + 1L)
            is TimelineBlock.RecommendedTodo -> it.block.displayEndMillis()
        }
    }
    val paddingMillis = 15L * 60L * 1000L
    val windowStart = (firstStart - paddingMillis).coerceAtLeast(0L)
    val windowEnd = (lastEnd + paddingMillis).coerceAtLeast(windowStart + 60L * 60L * 1000L)
    val windowDuration = (windowEnd - windowStart).coerceAtLeast(1L)
    val scheduled = blocks.filterIsInstance<TimelineBlock.ScheduledAutoButton>().map { it.block }
    val recommended = blocks.filterIsInstance<TimelineBlock.RecommendedTodo>().map { it.block }
    val categories = remember(blocks) {
        blocks.map {
            when (it) {
                is TimelineBlock.ActualActivity -> it.segment.category
                is TimelineBlock.ScheduledAutoButton -> it.block.category
                is TimelineBlock.RecommendedTodo -> "TODO"
            }
        }.distinct()
    }

    val actualSegments = blocks.filterIsInstance<TimelineBlock.ActualActivity>().map { it.segment }

    // pointerInput은 Canvas가 아닌 Box 컨테이너에 붙여
    // LazyColumn 스크롤과의 경합 없이 long press를 안정적으로 감지한다.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(blocks, windowStart, windowDuration) {
                detectTapGestures(
                    onTap = { offset ->
                        val barTop = 34.dp.toPx()
                        val barBottom = 60.dp.toPx()
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
                        Log.d("SleepFill", "onLongPress 진입 offset=$offset size=${size.width}x${size.height}")

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
                        Log.d("SleepFill", "scheduledHit=${scheduledHit?.category}")
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
                        Log.d("SleepFill", "actualHit=${actualHit?.category}")
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
                        Log.d("SleepFill", "recommendedHit=${recommendedHit?.title}")
                        if (recommendedHit != null) return@detectTapGestures

                        // 4) 빈 공간 — Canvas 전체 높이에서 받음 (Y 제한 없음)
                        val fraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val pressedTimeMillis = windowStart + (fraction * windowDuration).toLong()
                        Log.d("SleepFill", "빈 공간 long press: fraction=$fraction pressedTime=$pressedTimeMillis")
                        onEmptySpaceLongPress(pressedTimeMillis)
                    }
                )
            }
    ) {
        val timeFormatInner = timeFormat
        Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            val guideColor = Color(0x332196F3)
            val trackColor = Color(0xFFE8EDF3)
            val barHeight = 26.dp.toPx()
            val top = 34.dp.toPx()
            val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = FlowPurpleDeep.toArgb()
                textSize = 10.sp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            repeat(5) { index ->
                val x = size.width * index / 4f
                drawLine(
                    color = guideColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height - 18.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, top),
                size = Size(size.width, barHeight),
                cornerRadius = radius
            )

            var recommendedBubbleIndex = 0
            blocks.forEach { block ->
                when (block) {
                    is TimelineBlock.ActualActivity -> {
                        val segment = block.segment
                        val startFraction = ((segment.startTime - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(0f, 1f)
                        val endFraction = ((segment.endTime.coerceAtLeast(segment.startTime + 1L) - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(startFraction, 1f)
                        val x = size.width * startFraction
                        val width = (size.width * (endFraction - startFraction)).coerceAtLeast(3.dp.toPx())
                        drawRoundRect(
                            color = categoryColor(segment.category),
                            topLeft = Offset(x, top),
                            size = Size(width, barHeight),
                            cornerRadius = radius
                        )
                    }
                    is TimelineBlock.ScheduledAutoButton -> {
                        val item = block.block
                        val startFraction = ((item.startTime - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(0f, 1f)
                        val endFraction = ((item.endTime.coerceAtLeast(item.startTime + 1L) - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(startFraction, 1f)
                        val x = size.width * startFraction
                        val width = (size.width * (endFraction - startFraction)).coerceAtLeast(3.dp.toPx())
                        val strokeColor = categoryColor(item.category).copy(alpha = if (item.isSkippedToday) 0.28f else 0.58f)
                        drawRoundRect(
                            color = strokeColor.copy(alpha = if (item.isSkippedToday) 0.08f else 0.16f),
                            topLeft = Offset(x, top),
                            size = Size(width, barHeight),
                            cornerRadius = radius
                        )
                        drawRoundRect(
                            color = strokeColor,
                            topLeft = Offset(x, top),
                            size = Size(width, barHeight),
                            cornerRadius = radius,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    is TimelineBlock.RecommendedTodo -> {
                        val item = block.block
                        val startFraction = ((item.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(0f, 1f)
                        val endFraction = ((item.displayEndMillis() - windowStart).toFloat() / windowDuration.toFloat())
                            .coerceIn(startFraction, 1f)
                        val x = size.width * startFraction
                        val width = (size.width * (endFraction - startFraction)).coerceAtLeast(4.dp.toPx())
                        if (!item.isBubbleOnly) {
                            val color = FlowPurple.copy(alpha = 0.7f)
                            drawRoundRect(
                                color = FlowPurpleSoft.copy(alpha = 0.45f),
                                topLeft = Offset(x, top + 4.dp.toPx()),
                                size = Size(width, barHeight - 8.dp.toPx()),
                                cornerRadius = radius
                            )
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, top + 4.dp.toPx()),
                                size = Size(width, barHeight - 8.dp.toPx()),
                                cornerRadius = radius,
                                style = Stroke(
                                    width = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
                                )
                            )
                        }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = timeFormat.format(Date(windowStart)),
            fontSize = 10.sp,
            color = FlowMuted
        )
        Text(
            text = timeFormat.format(Date((windowStart + windowEnd) / 2L)),
            fontSize = 10.sp,
            color = FlowMuted
        )
        Text(
            text = timeFormat.format(Date(windowEnd)),
            fontSize = 10.sp,
            color = FlowMuted
        )
    }

    LazyRow(
        modifier = Modifier.padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(categories.size) { index ->
            val category = categories[index]
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(categoryColor(category), shape = MaterialTheme.shapes.small)
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
    onSetTime: (hourOfDay: Int) -> Unit,
    onReplaceItem: (TodoItem) -> Unit
) {
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
        TodoCategory.ASSIGNMENT -> "과제"
        TodoCategory.UNIVERSITY_EXAM -> "시험"
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
private fun AutoButtonManagerSheet(
    schedules: List<AutoButtonSchedule>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (AutoButtonSchedule) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onSkipToday: (String) -> Unit,
    onUnskipToday: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<AutoButtonSchedule?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
                if (schedules.isNotEmpty()) {
                    schedules.forEach { schedule ->
                        AutoButtonScheduleRow(
                            schedule = schedule,
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
                onClick = { editing = defaultAutoButtonSchedule() },
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

    editing?.let { schedule ->
        AutoButtonEditSheet(
            initial = schedule,
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
private fun AutoButtonScheduleRow(
    schedule: AutoButtonSchedule,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoButtonEditSheet(
    initial: AutoButtonSchedule,
    onDismiss: () -> Unit,
    onSave: (AutoButtonSchedule) -> Unit
) {
    var title by remember(initial.scheduleId) { mutableStateOf(initial.title) }
    val fixedCategories = remember { listOf("COMPANY", "SCHOOL") }
    var category by remember(initial.scheduleId) {
        mutableStateOf(initial.category.takeIf { it in fixedCategories } ?: "SCHOOL")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF4F4F8), RoundedCornerShape(18.dp))
                        .border(1.dp, FlowDivider, RoundedCornerShape(18.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    fixedCategories.forEach { item ->
                        AutoButtonCategorySegment(
                            category = item,
                            selected = category == item,
                            onClick = {
                                val previousCategoryTitle = displayCategory(category)
                                category = item
                                if (title.isBlank() || title == previousCategoryTitle) {
                                    title = displayCategory(item)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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

private fun defaultAutoButtonSchedule(): AutoButtonSchedule {
    return AutoButtonSchedule(
        title = "학교",
        category = "SCHOOL",
        repeatDays = weekdayDefaults(),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 17 * 60
    )
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
