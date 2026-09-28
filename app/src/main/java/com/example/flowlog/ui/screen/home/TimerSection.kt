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
import com.example.flowlog.data.recommendation.ActivityTitleSuggestionRanker
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
internal fun TimerPage(
    currentCategory: String,
    startTime: Long,
    elapsedTime: Long,
    timerGoalMillis: Long,
    initialAppliedTitle: String,
    titleSuggestions: List<String>,
    onStop: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onStopAndComplete: (String, String?, List<ExerciseSetRecord>) -> Unit = onStop,
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
    routineGoalMillis: Long = 0L,
    linkedTodoId: Long? = null,
    linkedPetiteId: String? = null,
    dailyCueId: Long? = null
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
    val defaultExerciseName = stringResource(R.string.timer_default_exercise_name)
    val defaultExerciseCategoryLabel = stringResource(R.string.timer_category_exercise)
    val exerciseSheetName = remember { mutableStateOf(defaultExerciseName) }
    val exercisePrefillRecord = remember { mutableStateOf<ExerciseSetRecord?>(null) }
    val editingExerciseSetIndex = remember { mutableStateOf<Int?>(null) }
    val activeTimedExerciseSet = remember { mutableStateOf<ExerciseTimedSetState?>(null) }
    val showExerciseSummaryDialog = remember { mutableStateOf(false) }
    // DND 체크박스 상태: 저장된 선호값으로 초기화
    val enableDnd = remember { mutableStateOf(FocusModeStore.getEnableSystemDndForFocus(context)) }
    // 시작됩니다 다이얼로그에서 DND 활성 여부 표시용
    val focusModeStartedWithDnd = remember { mutableStateOf(false) }
    var focusDurationLabel by remember { mutableStateOf(FocusModeStore.getFocusDurationLabel(context)) }
    val showFocusDurationPicker = remember { mutableStateOf(false) }

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
            appliedTitle = appliedTitleState.value,
            title = titleState.value,
            onApplyTitle = onApplyTitle,
            onStop = onStop,
            onShowExerciseSummary = { showExerciseSummaryDialog.value = true }
        )

        Spacer(modifier = Modifier.height(22.dp))
        if (currentCategory == "EXERCISE") {
            ExerciseSetControls(
                sets = exerciseSets,
                timedSetState = activeTimedExerciseSet.value,
                onAddSameExercise = {
                    val recentSet = exerciseSets.lastOrNull()
                    exercisePrefillRecord.value = recentSet
                    exerciseSheetName.value = recentSet?.name ?: appliedTitleState.value.ifBlank { defaultExerciseName }
                    showExerciseAddSheet.value = true
                },
                onAddOtherExercise = {
                    exercisePrefillRecord.value = null
                    exerciseSheetName.value = defaultExerciseName
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
                focusDurationLabel = focusDurationLabel,
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
                onLongPress = { showFocusDurationPicker.value = true },
                onRequestStop = { showFocusStopConfirmDialog.value = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(22.dp))
        }
        StopActionSection(
            currentCategory = currentCategory,
            isFocusFireActive = isFocusFireActive,
            isRoutineActive = isRoutineActive,
            linkedTodoId = linkedTodoId,
            linkedPetiteId = linkedPetiteId,
            dailyCueId = dailyCueId,
            title = titleState.value,
            appliedTitle = appliedTitleState.value,
            onApplyTitle = onApplyTitle,
            onStop = onStop,
            onStopAndComplete = onStopAndComplete,
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
                    ?: appliedTitleState.value.ifBlank { titleState.value }.ifBlank { defaultExerciseCategoryLabel }
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
    if (showFocusDurationPicker.value) {
        FocusDurationPickerSheet(
            currentMillis = FocusModeStore.getFocusDurationMillis(context),
            onSelect = { millis ->
                FocusModeStore.setFocusDurationMillis(context, millis)
                focusDurationLabel = FocusModeStore.getFocusDurationLabel(context)
                showFocusDurationPicker.value = false
            },
            onDismiss = { showFocusDurationPicker.value = false }
        )
    }
}

// elapsedTime이 실제로 필요한 유일한 섹션 — 진행률 링과 시간 텍스트만 담당.
// 타이머가 도는 동안 1초마다 재구성되는 범위를 이 섹션 하나로 한정한다.
@Composable
internal fun TimerRingSection(
    currentCategory: String,
    elapsedTime: Long,
    timerGoalMillis: Long,
    routineGoalMillis: Long,
    isRoutineActive: Boolean,
    isFocusFireActive: Boolean,
    appliedTitle: String,
    title: String,
    onApplyTitle: (String) -> Unit,
    onStop: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onShowExerciseSummary: () -> Unit
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

    val isComplete = !isFocusFireActive && progress >= 1f
    val isFireComplete = isFocusFireActive && progress >= 0.98f
    val barColor = when {
        isFireComplete -> Color(0xFF00D97E)
        isComplete -> Color(0xFFFFCC00)
        isFocusFireActive -> Color(0xFFFF7A2F)
        else -> accentColor
    }
    val displayTitle = appliedTitle.ifBlank { displayCategory(currentCategory) }
    val formattedTime = formatTime(elapsedTime)
    val timeFontSize = when {
        formattedTime.length <= 5 -> 40.sp
        formattedTime.length <= 7 -> 32.sp
        else -> 26.sp
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val iconColor = categoryColor(currentCategory)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = currentCategory,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedTime,
                fontSize = timeFontSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                color = FlowInk,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor)
                    .clickable {
                        val finalTitle = appliedTitle.ifBlank { title }.trim()
                        onApplyTitle(finalTitle)
                        if (currentCategory == "EXERCISE") {
                            onShowExerciseSummary()
                        } else {
                            onStop(finalTitle, null, emptyList())
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            }
        }
        if (progress > 0f) {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFE9E9F1))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
        }
    }
}

// elapsedTime을 받지 않음 — title/appliedTitle/titleSuggestions에만 의존.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TitleInputSection(
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
                                text = stringResource(R.string.timer_manual_input),
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
                text = stringResource(R.string.timer_apply),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

// elapsedTime을 받지 않음 — focusModeEndsAtMillis 기준으로 자체 1초 tick을 갖는다.
@Composable
internal fun FocusBannerSection(
    isFocusModeActive: Boolean,
    focusModeEndsAtMillis: Long,
    isFocusFireActive: Boolean,
    focusDurationLabel: String,
    onStart: () -> Unit,
    onLongPress: () -> Unit,
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
                text = stringResource(R.string.timer_focusing_remaining, remainingLabel),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .combinedClickable(
                    onClick = onStart,
                    onLongClick = onLongPress
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.timer_focus_button, focusDurationLabel),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FocusDurationPickerSheet(
    currentMillis: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = listOf(
        15L to stringResource(R.string.timer_minutes_label, 15),
        25L to stringResource(R.string.timer_minutes_label, 25),
        50L to stringResource(R.string.timer_minutes_label, 50),
        75L to stringResource(R.string.timer_minutes_label, 75)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.timer_focus_time_label),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = FlowInk
            )
            Spacer(modifier = Modifier.height(16.dp))
            options.forEach { (mins, label) ->
                val millis = mins * 60_000L
                val isSelected = currentMillis == millis
                OutlinedButton(
                    onClick = { onSelect(millis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) FlowPurpleSoft else Color.Transparent,
                        contentColor = if (isSelected) FlowPurple else FlowInk
                    ),
                    border = BorderStroke(1.dp, if (isSelected) FlowPurple else FlowDivider)
                ) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

// elapsedTime을 받지 않음 — currentCategory/title/appliedTitle만으로 종료 처리.
@Composable
internal fun StopActionSection(
    currentCategory: String,
    isFocusFireActive: Boolean,
    isRoutineActive: Boolean,
    linkedTodoId: Long?,
    linkedPetiteId: String?,
    dailyCueId: Long?,
    title: String,
    appliedTitle: String,
    onApplyTitle: (String) -> Unit,
    onStop: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onStopAndComplete: (String, String?, List<ExerciseSetRecord>) -> Unit,
    onShowExerciseSummary: () -> Unit
) {
    val hasLinkedCompletion = linkedTodoId != null || linkedPetiteId != null || dailyCueId != null || isRoutineActive
    if (!hasLinkedCompletion) return

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
                onStopAndComplete(finalTitle, null, emptyList())
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
            text = when {
                currentCategory == "EXERCISE" -> stringResource(R.string.timer_exercise_complete_and_stop)
                dailyCueId != null -> stringResource(R.string.timer_routine_complete_and_stop)
                else -> stringResource(R.string.timer_complete_and_stop)
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// elapsedTime을 받지 않음 — ExerciseAddSetSheet + 포커스/DND 다이얼로그 4종만 묶는다.
// (ExerciseFinishDialog는 elapsedTime이 꼭 필요해서 TimerPage에 별도로 남겨둠)
@Composable
internal fun TimerDialogsSection(
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
                    text = stringResource(R.string.timer_focus_question, FocusModeStore.getFocusDurationLabel(context)),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FlowInk
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.timer_focus_desc, FocusModeStore.getFocusDurationLabel(context)),
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
                            text = stringResource(R.string.timer_enable_system_dnd),
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
                            text = stringResource(R.string.timer_dont_show_again),
                            fontSize = 14.sp,
                            color = FlowInk,
                            modifier = Modifier.Companion.padding(start = 4.dp)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFocusConfirmDialogState.value = false }) {
                    Text(stringResource(R.string.timer_cancel), color = FlowMuted, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.timer_start_action), fontWeight = FontWeight.ExtraBold)
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
                        text = stringResource(R.string.timer_focus_started_title),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = FlowInk,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                val durationLabel = FocusModeStore.getFocusDurationLabel(context)
                val startedText = if (focusModeStartedWithDndState.value) {
                    stringResource(R.string.timer_focus_started_with_dnd_desc, durationLabel)
                } else {
                    stringResource(R.string.timer_focus_started_desc, durationLabel)
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
                    Text(stringResource(R.string.timer_confirm), fontWeight = FontWeight.ExtraBold)
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
                    text = stringResource(R.string.timer_focus_end_question),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FlowInk
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.timer_focus_end_desc),
                    fontSize = 14.sp,
                    color = FlowMuted
                )
            },
            dismissButton = {
                TextButton(onClick = { showFocusStopConfirmDialogState.value = false }) {
                    Text(stringResource(R.string.timer_cancel), color = FlowMuted, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.timer_end_action), fontWeight = FontWeight.ExtraBold)
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
                    text = stringResource(R.string.timer_dnd_permission_needed_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = FlowInk
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.timer_dnd_permission_desc),
                    fontSize = 14.sp,
                    color = FlowMuted
                )
            },
            dismissButton = {
                TextButton(onClick = { showDndPermissionDialogState.value = false }) {
                    Text(stringResource(R.string.timer_later), color = FlowMuted, fontWeight = FontWeight.Bold)
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
                    Text(stringResource(R.string.timer_open_settings), fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}


internal fun buildTitleSuggestions(
    category: String,
    activities: List<ActivitySession>,
    categoryLabels: Map<String, String>,
    nowMillis: Long = System.currentTimeMillis()
): List<String> {
    return ActivityTitleSuggestionRanker.suggest(
        category = category,
        activities = activities,
        defaultTitle = defaultActivityTitle(category, categoryLabels),
        nowMillis = nowMillis
    )
}

internal fun defaultActivityTitle(category: String, categoryLabels: Map<String, String>): String {
    return categoryLabels[category] ?: categoryLabels.getValue("ETC")
}


