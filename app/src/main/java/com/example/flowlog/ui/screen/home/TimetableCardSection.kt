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

internal data class DisplayActivitySegment(
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
internal fun TimetableCard(
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
    onStartScheduled: (ScheduledAutoButtonBlock) -> Unit = {},
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
    var selectedStartBlock by remember { mutableStateOf<ScheduledAutoButtonBlock?>(null) }
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
                    onClick = { block -> selectedStartBlock = block },
                    onShowMenu = { block -> selectedBlock = block }
                )
            }
            flowRecommendations.forEachIndexed { index, recommendation ->
                ActivityRecommendationRow(
                    category = recommendation.category,
                    activityName = recommendation.title,
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

    selectedStartBlock?.let { block ->
        ScheduledAutoButtonStartSheet(
            block = block,
            onDismiss = { selectedStartBlock = null },
            onStart = {
                onStartScheduled(block)
                selectedStartBlock = null
            }
        )
    }

    selectedRecommendedBlock?.let { block ->
        val alreadyRecommendedKeys = recommendedBlocks
            .asSequence()
            .filter { it.itemId != block.itemId }
            .map { it.replacementKey() }
            .toSet()
        RecommendedTodoActionSheet(
            block = block,
            incompleteTodos = incompleteTodos.filter { it.replacementKey() !in alreadyRecommendedKeys },
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

internal fun buildDisplayActivitySegments(
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

internal fun prioritizeSchoolCompanyTimelineActivities(
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

internal fun subtractPriorityRanges(
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

internal fun ActivitySession.toDisplaySegment(): DisplayActivitySegment {
    return DisplayActivitySegment(
        category = category,
        title = title,
        startTime = startTime,
        endTime = endTime.coerceAtLeast(startTime + 1L),
        mergedSegments = listOf(this),
        hiddenSegments = emptyList()
    )
}

internal fun canMergeAsTimelineAnchor(activity: ActivitySession): Boolean {
    return activity.sourceType != ActivitySourceType.AUTO_BUTTON &&
        activity.category in MERGEABLE_TIMETABLE_CATEGORIES &&
        activity.category !in PRECISE_TIMETABLE_CATEGORIES
}

internal fun canHideBridgeSegment(activity: ActivitySession): Boolean {
    return activity.sourceType != ActivitySourceType.AUTO_BUTTON &&
        activity.category in HIDEABLE_TIMETABLE_BRIDGE_CATEGORIES &&
        activity.category !in PRECISE_TIMETABLE_CATEGORIES
}

internal fun smoothMicroDisplaySegments(
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

internal fun mergeAdjacentProductiveSegments(
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

internal fun DisplayActivitySegment.isMicroSegment(): Boolean {
    return durationMillis < MERGE_THRESHOLD_MILLIS
}

internal fun DisplayActivitySegment.isProtectedSegment(): Boolean {
    return category in PRECISE_TIMETABLE_CATEGORIES ||
        allOriginalSegments().any { it.sourceType == ActivitySourceType.AUTO_BUTTON }
}

internal fun DisplayActivitySegment.canAbsorbMicroSegment(): Boolean {
    return !isProtectedSegment()
}

internal fun DisplayActivitySegment.isProductiveSegment(): Boolean {
    return category in PRODUCTIVE_TIMETABLE_CATEGORIES && !isProtectedSegment()
}

internal fun DisplayActivitySegment.allOriginalSegments(): List<ActivitySession> {
    return mergedSegments + hiddenSegments
}

internal fun startOfLocalDay(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

internal fun timetableCategoryColor(category: String): Color {
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
        "HOBBY" -> Color(0xFF7581C8)
        "TODO" -> TimetableTodoColor
        else -> Color(0xFF8C8F98)
    }
}

internal fun overlapMillis(
    firstStart: Long,
    firstEnd: Long,
    secondStart: Long,
    secondEnd: Long
): Long {
    val start = maxOf(firstStart, secondStart)
    val end = minOf(firstEnd, secondEnd)
    return (end - start).coerceAtLeast(0L)
}

internal fun chooseProductiveFlowCategory(
    previous: DisplayActivitySegment,
    next: DisplayActivitySegment
): String {
    if (previous.category == next.category) return previous.category
    return if (previous.durationMillis >= next.durationMillis) previous.category else next.category
}

internal fun combineDisplaySegments(
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

internal fun RecommendedTodoBlock.displayEndMillis(): Long {
    return plannedStartMillis + RECOMMENDED_TODO_DISPLAY_DURATION_MILLIS
}

internal fun RecommendedTodoBlock.replacementKey(): String {
    return calendarSourceId?.let { "calendar_petite_$it" }
        ?: petiteId?.let { "calendar_petite_$it" }
        ?: "legacy_todo_$todoId"
}

internal fun TodoItem.replacementKey(): String {
    return calendarSourceId?.let { "calendar_petite_$it" } ?: "legacy_todo_$id"
}

internal fun displayTitleForMergedSegment(
    category: String,
    mergedSegments: List<ActivitySession>
): String {
    val titles = mergedSegments.map { it.title.trim() }.filter { it.isNotBlank() }.distinct()
    return if (titles.size == 1) titles.first() else displayCategory(category)
}

@Composable
internal fun TimetableBar(
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

internal fun DrawScope.drawRecommendedTodoBubble(
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

internal fun String.ellipsizeToWidth(paint: Paint, maxWidth: Float): String {
    if (paint.measureText(this) <= maxWidth) return this
    val ellipsis = "..."
    val ellipsisWidth = paint.measureText(ellipsis)
    if (ellipsisWidth >= maxWidth) return ellipsis
    val count = paint.breakText(this, true, maxWidth - ellipsisWidth, null)
    return take(count).trimEnd() + ellipsis
}

@Composable
internal fun ScheduledAutoButtonList(
    blocks: List<ScheduledAutoButtonBlock>,
    activeCategory: String?,
    onClick: (ScheduledAutoButtonBlock) -> Unit = {},
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
                        onClick = { onClick(block) },
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
internal fun RecommendedTodoBlockList(
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


