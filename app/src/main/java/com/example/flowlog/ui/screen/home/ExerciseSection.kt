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
internal fun ExerciseSetControls(
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
            text = stringResource(R.string.exercise_recent_records),
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
                                text = stringResource(R.string.exercise_set_summary, index + 1, formatExerciseSetValue(set), set.intensity),
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
                                contentDescription = stringResource(R.string.exercise_edit_set_content_desc),
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
            Text(stringResource(R.string.exercise_add_same_set), fontWeight = FontWeight.ExtraBold)
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
            Text(stringResource(R.string.exercise_add_other), fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
internal fun ExerciseTimedSetCard(
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
        remainingToStart > 0L -> stringResource(R.string.exercise_starts_in, formatExerciseSetTime(remainingToStart))
        now < state.endsAtMillis -> stringResource(R.string.exercise_remaining, formatExerciseSetTime(state.endsAtMillis - now))
        else -> stringResource(R.string.exercise_time_complete)
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
            Text(stringResource(R.string.exercise_complete_action), color = FlowPurple, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ExerciseAddSetSheet(
    initialName: String,
    initialRecord: ExerciseSetRecord? = null,
    prefillRecord: ExerciseSetRecord? = null,
    onDismiss: () -> Unit,
    onSave: (ExerciseSetRecord) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val defaultExerciseName = stringResource(R.string.exercise_default_option_pushup)
    val defaultIntensity = stringResource(R.string.exercise_default_intensity)
    val defaultExercisePushup = stringResource(R.string.exercise_default_option_pushup)
    val defaultExerciseSquat = stringResource(R.string.exercise_default_option_squat)
    val defaultExercisePlank = stringResource(R.string.exercise_default_option_plank)
    val defaultExerciseCategoryLabel = stringResource(R.string.exercise_label)
    val seededExercise = initialRecord?.name
        ?: prefillRecord?.name
        ?: initialName.ifBlank { defaultExerciseName }
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
        mutableStateOf(initialRecord?.intensity ?: prefillRecord?.intensity ?: saved?.intensity ?: defaultIntensity)
    }
    val defaultExerciseOptions = remember(defaultExercisePushup, defaultExerciseSquat, defaultExercisePlank) {
        listOf(defaultExercisePushup, defaultExerciseSquat, defaultExercisePlank)
    }
    var exerciseOptions by remember(initialName, initialRecord, prefillRecord) {
        val custom = ExerciseOptionsStore.loadCustomExercises(context)
        val hiddenDefaults = ExerciseOptionsStore.loadHiddenDefaultExercises(context)
        val visibleDefaults = defaultExerciseOptions.filter { it !in hiddenDefaults || it == seededExercise }
        val baseOptions = (visibleDefaults + custom + seededExercise).distinct()
        val savedOrder = ExerciseOptionsStore.loadExerciseOrder(context)
        mutableStateOf(savedOrder.filter { it in baseOptions } + baseOptions.filter { it !in savedOrder })
    }
    var customExercise by remember { mutableStateOf("") }
    var showExerciseAddCard by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingExerciseItem by remember { mutableStateOf<String?>(null) }
    var editingExerciseNewName by remember { mutableStateOf("") }
    var movingExercise by remember { mutableStateOf<String?>(null) }
    var dragPositionInRoot by remember { mutableStateOf<Offset?>(null) }
    var editListPositionInRoot by remember { mutableStateOf(Offset.Zero) }
    var editListBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var exerciseInsertIndex by remember { mutableStateOf<Int?>(null) }
    var exerciseSwapTarget by remember { mutableStateOf<String?>(null) }
    var exerciseAutoScrollJob by remember { mutableStateOf<Job?>(null) }
    var exerciseAutoScrollDelta by remember { mutableStateOf(0) }
    val exerciseChipBounds = remember { mutableStateMapOf<String, Rect>() }
    val editExerciseScrollState = rememberScrollState()
    val editExerciseDragScope = rememberCoroutineScope()

    fun dragPositionIsInsideList(position: Offset): Boolean {
        val bounds = editListBoundsInRoot ?: return false
        return position.x in bounds.left..bounds.right
    }

    fun insertionIndexForDrag(from: String, position: Offset): Int? {
        val fromIndex = exerciseOptions.indexOf(from)
        if (fromIndex < 0) return null
        if (!dragPositionIsInsideList(position)) return null
        val visibleOptions = exerciseOptions.filter { it != from }
        val insertInVisible = visibleOptions.indexOfFirst { option ->
            val bounds = exerciseChipBounds[option] ?: return@indexOfFirst false
            position.y < bounds.center.y
        }.let { index -> if (index == -1) visibleOptions.size else index }
        return insertInVisible.coerceIn(0, visibleOptions.size)
    }

    fun swapExerciseOptions(from: String, to: String) {
        if (from == to) return
        val fromIndex = exerciseOptions.indexOf(from)
        val toIndex = exerciseOptions.indexOf(to)
        if (fromIndex >= 0 && toIndex >= 0) {
            exerciseOptions = exerciseOptions.toMutableList().also { options ->
                options[fromIndex] = to
                options[toIndex] = from
            }
            ExerciseOptionsStore.saveExerciseOrder(context, exerciseOptions)
        }
    }

    fun swapTargetForDrag(from: String, position: Offset): String? {
        val visibleOptions = exerciseOptions.filter { it != from }
        return visibleOptions.firstOrNull { option ->
            val bounds = exerciseChipBounds[option] ?: return@firstOrNull false
            if (position.x !in bounds.left..bounds.right) return@firstOrNull false
            val centerBandTop = bounds.top + bounds.height * 0.38f
            val centerBandBottom = bounds.bottom - bounds.height * 0.38f
            position.y in centerBandTop..centerBandBottom
        }
    }

    fun moveExerciseOption(from: String, targetIndex: Int?) {
        val fromIndex = exerciseOptions.indexOf(from)
        val insertIndex = targetIndex ?: return
        if (fromIndex >= 0) {
            val moved = exerciseOptions.toMutableList()
            moved.removeAt(fromIndex)
            moved.add(insertIndex.coerceIn(0, moved.size), from)
            exerciseOptions = moved
            ExerciseOptionsStore.saveExerciseOrder(context, exerciseOptions)
        }
    }

    fun updateExerciseDropTargets(position: Offset) {
        val from = movingExercise
        exerciseSwapTarget = if (from != null) swapTargetForDrag(from, position) else null
        exerciseInsertIndex = if (from != null && exerciseSwapTarget == null) {
            insertionIndexForDrag(from, position)
        } else {
            null
        }
    }

    fun stopExerciseAutoScroll() {
        exerciseAutoScrollJob?.cancel()
        exerciseAutoScrollJob = null
        exerciseAutoScrollDelta = 0
    }

    fun updateExerciseAutoScroll(position: Offset) {
        val listBounds = editListBoundsInRoot
        val edge = with(density) { 56.dp.toPx() }
        val nextDelta = if (listBounds != null && dragPositionIsInsideList(position)) {
            when {
                position.y < listBounds.top + edge -> -14
                position.y > listBounds.bottom - edge -> 14
                else -> 0
            }
        } else {
            0
        }

        if (nextDelta == 0) {
            stopExerciseAutoScroll()
            return
        }

        if (exerciseAutoScrollJob?.isActive == true && exerciseAutoScrollDelta == nextDelta) return

        exerciseAutoScrollJob?.cancel()
        exerciseAutoScrollDelta = nextDelta
        exerciseAutoScrollJob = editExerciseDragScope.launch {
            while (true) {
                val next = (editExerciseScrollState.value + exerciseAutoScrollDelta)
                    .coerceIn(0, editExerciseScrollState.maxValue)
                if (next == editExerciseScrollState.value) {
                    delay(16L)
                } else {
                    editExerciseScrollState.scrollTo(next)
                    dragPositionInRoot?.let { updateExerciseDropTargets(it) }
                    delay(16L)
                }
            }
        }
    }

    fun updateExerciseDragPosition(position: Offset) {
        dragPositionInRoot = position
        updateExerciseDropTargets(position)
        updateExerciseAutoScroll(position)
    }

    fun clearExerciseDragState() {
        movingExercise = null
        dragPositionInRoot = null
        exerciseInsertIndex = null
        exerciseSwapTarget = null
        stopExerciseAutoScroll()
    }

    fun renameExerciseOption(option: String, newName: String) {
        if (newName.isBlank() || newName == option) return
        val wasSelected = selectedExercise == option
        exerciseOptions = exerciseOptions.map { if (it == option) newName else it }.distinct()
        if (option in defaultExerciseOptions) {
            ExerciseOptionsStore.hideDefaultExercise(context, option)
        } else {
            ExerciseOptionsStore.removeCustomExercise(context, option)
        }
        if (newName in defaultExerciseOptions) {
            ExerciseOptionsStore.showDefaultExercise(context, newName)
        } else {
            ExerciseOptionsStore.addCustomExercise(context, newName)
        }
        ExerciseOptionsStore.saveExerciseOrder(context, exerciseOptions)
        if (wasSelected) selectedExercise = newName
        if (movingExercise == option) movingExercise = newName
    }

    fun deleteExerciseOption(option: String) {
        if (exerciseOptions.size <= 1) return
        exerciseOptions = exerciseOptions - option
        if (option in defaultExerciseOptions) {
            ExerciseOptionsStore.hideDefaultExercise(context, option)
        } else {
            ExerciseOptionsStore.removeCustomExercise(context, option)
        }
        ExerciseOptionsStore.saveExerciseOrder(context, exerciseOptions)
        if (selectedExercise == option) selectedExercise = exerciseOptions.firstOrNull() ?: defaultExerciseOptions.first()
        if (movingExercise == option) clearExerciseDragState()
    }

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Text(
                text = if (initialRecord == null) stringResource(R.string.exercise_add_set_title) else stringResource(R.string.exercise_edit_set_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            // 운동 레이블 + 추가 / 수정 액션
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.exercise_label), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.exercise_add_label),
                    color = FlowPurpleDeep,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { showExerciseAddCard = !showExerciseAddCard }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Text(
                    stringResource(R.string.exercise_edit_label),
                    color = FlowPurpleDeep,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable {
                            editingExerciseItem = null
                            editingExerciseNewName = ""
                            clearExerciseDragState()
                            showEditDialog = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                exerciseOptions.forEach { option ->
                    FilterChip(
                        selected = selectedExercise == option,
                        onClick = { selectedExercise = option },
                        label = {
                            Text(
                                option,
                                fontWeight = if (selectedExercise == option) {
                                    FontWeight.ExtraBold
                                } else {
                                    FontWeight.Bold
                                }
                            )
                        }
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
                        placeholder = { Text(stringResource(R.string.exercise_name_placeholder)) },
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
                            Text(stringResource(R.string.exercise_cancel), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                val cleanExercise = customExercise.trim()
                                if (cleanExercise.isNotBlank()) {
                                    exerciseOptions = (exerciseOptions + cleanExercise).distinct()
                                    selectedExercise = cleanExercise
                                    ExerciseOptionsStore.addCustomExercise(context, cleanExercise)
                                    ExerciseOptionsStore.saveExerciseOrder(context, exerciseOptions)
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
                            Text(stringResource(R.string.exercise_add_label), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 수정 다이얼로그 — 커스텀 운동 이름 변경 / 삭제
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showEditDialog = false
                        editingExerciseItem = null
                        clearExerciseDragState()
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    title = { Text(stringResource(R.string.exercise_edit_exercise_title), fontWeight = FontWeight.ExtraBold, color = FlowInk) },
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates ->
                                    editListPositionInRoot = coordinates.positionInRoot()
                                    val position = coordinates.positionInRoot()
                                    val size = coordinates.size
                                    editListBoundsInRoot = Rect(
                                        left = position.x,
                                        top = position.y,
                                        right = position.x + size.width,
                                        bottom = position.y + size.height
                                    )
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = 420.dp)
                                    .verticalScroll(editExerciseScrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var visibleIndex = 0
                                exerciseOptions.forEach { option ->
                                    val isMoving = movingExercise == option
                                    if (!isMoving && exerciseInsertIndex == visibleIndex) {
                                        ExerciseInsertIndicator()
                                    }
                                    val bounds = exerciseChipBounds[option]
                                    val isSwapTarget = movingExercise != null && exerciseSwapTarget == option
                                    val isDropTarget = movingExercise != null &&
                                        !isMoving &&
                                        exerciseInsertIndex == visibleIndex

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
                                                renameExerciseOption(option, newName)
                                                editingExerciseItem = null
                                            }) {
                                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.exercise_complete_action), tint = FlowPurple)
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    alpha = when {
                                                        isMoving -> 0.28f
                                                        movingExercise != null && !isDropTarget && !isSwapTarget -> 0.82f
                                                        else -> 1f
                                                    }
                                                }
                                                .background(
                                                    if (isDropTarget || isSwapTarget) FlowPurpleSoft else Color.Transparent,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .onGloballyPositioned { coordinates ->
                                                    val position = coordinates.positionInRoot()
                                                    val size = coordinates.size
                                                    exerciseChipBounds[option] = Rect(
                                                        left = position.x,
                                                        top = position.y,
                                                        right = position.x + size.width,
                                                        bottom = position.y + size.height
                                                    )
                                                }
                                                .pointerInput(option, exerciseOptions, showEditDialog) {
                                                    detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        val rowBounds = exerciseChipBounds[option]
                                                        movingExercise = option
                                                        val startPosition = rowBounds?.let {
                                                            Offset(it.left + offset.x, it.top + offset.y)
                                                        }
                                                        if (startPosition != null) {
                                                            updateExerciseDragPosition(startPosition)
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        val from = movingExercise
                                                        if (from != null) {
                                                            val swapTarget = exerciseSwapTarget
                                                            if (swapTarget != null) {
                                                                swapExerciseOptions(from, swapTarget)
                                                            } else {
                                                                moveExerciseOption(from, exerciseInsertIndex)
                                                            }
                                                        }
                                                        clearExerciseDragState()
                                                    },
                                                    onDragCancel = {
                                                        clearExerciseDragState()
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        updateExerciseDragPosition((dragPositionInRoot ?: Offset.Zero) + dragAmount)
                                                    }
                                                )
                                            }
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                option,
                                                modifier = Modifier.weight(1f),
                                                fontWeight = if (isDropTarget || isSwapTarget) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = if (isDropTarget || isSwapTarget) FlowPurpleDeep else FlowInk,
                                                fontSize = 14.sp
                                            )
                                            IconButton(onClick = {
                                                editingExerciseItem = option
                                                editingExerciseNewName = option
                                            }) {
                                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.exercise_rename_content_desc), tint = Color.Gray, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(
                                                enabled = exerciseOptions.size > 1,
                                                onClick = { deleteExerciseOption(option) }
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.exercise_delete_content_desc),
                                                    tint = if (exerciseOptions.size > 1) Color.Gray else Color.LightGray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (!isMoving) {
                                        visibleIndex += 1
                                    }
                                }
                                if (exerciseInsertIndex == visibleIndex) {
                                    ExerciseInsertIndicator()
                                }
                            }

                            val previewName = movingExercise
                            val previewBounds = previewName?.let { exerciseChipBounds[it] }
                            val previewPosition = dragPositionInRoot
                            if (previewName != null && previewBounds != null && previewPosition != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(2f)
                                        .graphicsLayer {
                                            translationY = previewPosition.y - editListPositionInRoot.y - (previewBounds.height / 2f)
                                            scaleX = 1.01f
                                            scaleY = 1.01f
                                            shadowElevation = with(density) { 4.dp.toPx() }
                                        }
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .border(1.dp, FlowPurpleSoft, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        previewName,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FlowPurpleDeep,
                                        fontSize = 14.sp
                                    )
                                    Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp).padding(15.dp))
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp).padding(15.dp))
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showEditDialog = false
                            editingExerciseItem = null
                            clearExerciseDragState()
                        }) {
                            Text(stringResource(R.string.exercise_complete_action), color = FlowPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(stringResource(R.string.exercise_record_method_label), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
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
                    Text(stringResource(R.string.exercise_count_reps_label), fontWeight = FontWeight.ExtraBold)
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
                    Text(stringResource(R.string.exercise_time_label), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            if (recordMode == "TIME") {
                Text(stringResource(R.string.exercise_time_label), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
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
                Text(stringResource(R.string.exercise_count_label), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
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
            Text(stringResource(R.string.exercise_intensity_rpe_label), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    stringResource(R.string.exercise_intensity_light),
                    stringResource(R.string.exercise_intensity_normal),
                    stringResource(R.string.exercise_intensity_heavy)
                ).forEach { option ->
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
                    val name = selectedExercise.trim().ifBlank { defaultExerciseCategoryLabel }
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
                Text(if (initialRecord == null) stringResource(R.string.exercise_save_action) else stringResource(R.string.exercise_edit_action_button), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun ExerciseStepButton(label: String, onClick: () -> Unit) {
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
internal fun ExerciseInsertIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(FlowPurple, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
internal fun IntensityChip(
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
internal fun ExerciseFinishDialog(
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
            Text(stringResource(R.string.exercise_complete_title), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.exercise_total_duration_sets, formatDuration(elapsedTime), sets.size),
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
                                text = stringResource(R.string.exercise_set_number, index + 1),
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
                    placeholder = { Text(stringResource(R.string.exercise_memo_placeholder)) },
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
                Text(stringResource(R.string.exercise_cancel), fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlowPurple, contentColor = Color.White)
            ) {
                Text(stringResource(R.string.exercise_save_action), fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

internal fun formatCountdown(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%d:%02d:%02d".format(h, m, s)
}

@Composable
internal fun formatExerciseSetValue(record: ExerciseSetRecord): String {
    return if (record.mode == "TIME") {
        formatExerciseSetTime(record.durationMillis ?: 0L)
    } else {
        stringResource(R.string.exercise_reps_count, record.reps)
    }
}

internal fun formatExerciseSetTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}


