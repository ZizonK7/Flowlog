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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduledAutoButtonActionSheet(
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
                Text(if (block.isSkippedToday) stringResource(R.string.auto_button_skip_today_toggle_on) else stringResource(R.string.auto_button_skip_today_toggle_off))
            }
            TextButton(
                onClick = {
                    onEditSchedule(block.scheduleId)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
            ) {
                Text(stringResource(R.string.auto_button_edit_settings), fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
            ) {
                Text(stringResource(R.string.auto_button_close))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduledAutoButtonStartSheet(
    block: ScheduledAutoButtonBlock,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isBeforeStart = remember(block.startTime) { System.currentTimeMillis() < block.startTime }
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
                    .background(categoryColor(block.category).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = block.category,
                    tint = categoryColor(block.category),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = block.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auto_button_scheduled_time, timeFormat.format(Date(block.startTime)), timeFormat.format(Date(block.endTime))),
                fontSize = 14.sp,
                color = FlowMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onStart,
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
                    text = if (isBeforeStart) stringResource(R.string.auto_button_start_early) else stringResource(R.string.auto_button_start_now),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.auto_button_close),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecommendedTodoActionSheet(
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
                                contentDescription = stringResource(R.string.auto_button_back_content_desc),
                                tint = FlowInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            stringResource(R.string.auto_button_set_start_time_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FlowInk,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Text(
                        stringResource(R.string.auto_button_pick_time_question),
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
                            Text(stringResource(R.string.auto_button_cancel), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
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
                            Text(stringResource(R.string.auto_button_confirm), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            "CHANGE_ITEM" -> {
                val currentKey = remember(block) { block.replacementKey() }
                val selectableTodos = remember(incompleteTodos, currentKey) {
                    incompleteTodos.filter { it.replacementKey() != currentKey }
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
                                contentDescription = stringResource(R.string.auto_button_back_content_desc),
                                tint = FlowInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            stringResource(R.string.auto_button_pick_other_todo_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FlowInk,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.size(48.dp))
                    }
                    Text(
                        stringResource(R.string.auto_button_auto_assign_desc),
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
                                stringResource(R.string.auto_button_no_other_todo),
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
                                    TodoCategory.REVIEW -> stringResource(R.string.auto_button_category_review)
                                    TodoCategory.ASSIGNMENT -> stringResource(R.string.auto_button_category_assignment)
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
                val dateLabel = block.selectedDate?.let { todoDateFormat.format(Date(it)) } ?: stringResource(R.string.auto_button_no_date)
                val categoryLabel = block.category?.let { recommendedTodoCategoryLabel(it) } ?: stringResource(R.string.auto_button_category_normal)
                val metaLabel = "$dateLabel / $categoryLabel / " + stringResource(R.string.auto_button_start_recommended_suffix, timeFormat.format(Date(block.plannedStartMillis)))
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
                        Text(stringResource(R.string.auto_button_start_action), fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF18A058),
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.auto_button_complete_action), fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { mode = "CHANGE_TIME" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                    ) {
                        Text(stringResource(R.string.auto_button_change_time_action), fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { mode = "CHANGE_ITEM" },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                    ) {
                        Text(stringResource(R.string.auto_button_change_todo_action), fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                    ) {
                        Text(stringResource(R.string.auto_button_close))
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun recommendedTodoCategoryLabel(category: TodoCategory): String {
    return when (category) {
        TodoCategory.NORMAL -> stringResource(R.string.auto_button_category_normal)
        TodoCategory.TODAY -> stringResource(R.string.auto_button_category_today)
        TodoCategory.REVIEW -> stringResource(R.string.auto_button_category_review)
        TodoCategory.ASSIGNMENT -> stringResource(R.string.auto_button_category_deadline)
    }
}

@Composable
internal fun burdenLabel(level: String): String {
    return when (level) {
        "HEAVY" -> stringResource(R.string.auto_button_burden_heavy)
        "LIGHT" -> stringResource(R.string.auto_button_burden_light)
        else -> stringResource(R.string.auto_button_burden_normal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainButtonEditBottomSheet(
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
                            contentDescription = stringResource(R.string.auto_button_back_content_desc),
                            tint = FlowInk
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = if (showCategoryPicker) stringResource(R.string.auto_button_change_activity_action) else displayCategory(category),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.auto_button_close),
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
                            text = stringResource(R.string.auto_button_all_activities_in_main),
                            fontSize = 14.sp,
                            color = FlowMuted,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            } else {
                // 메인 메뉴
                MainButtonMenuRow(
                    label = stringResource(R.string.auto_button_change_activity_action),
                    icon = Icons.Default.Edit,
                    enabled = true,
                    onClick = { showCategoryPicker = true }
                )
                MainButtonMenuRow(
                    label = stringResource(R.string.auto_button_swap_position_label),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    enabled = true,
                    onClick = { onEnterReorderMode(category) }
                )
                MainButtonMenuRow(
                    label = stringResource(R.string.auto_button_hide_from_main_label),
                    icon = Icons.Default.Close,
                    enabled = canHide,
                    tint = if (canHide) Color(0xFFE53935) else FlowMuted,
                    subtitle = if (!canHide) stringResource(R.string.auto_button_min_buttons_required, MainButtonConfig.MIN_BUTTONS) else null,
                    onClick = { onHide(category) }
                )
            }
        }
    }
}

@Composable
internal fun MainButtonMenuRow(
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
internal fun AutoButtonManagerSheet(
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
                        stringResource(R.string.auto_button_manage_routine_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk
                    )
                    Text(
                        stringResource(R.string.auto_button_manage_routine_desc),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = FlowMuted,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.auto_button_close),
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
                        stringResource(R.string.auto_button_today_fixed_time),
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
                            stringResource(R.string.auto_button_repeat_routine),
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
                Text(stringResource(R.string.auto_button_add_repeat_routine), fontWeight = FontWeight.ExtraBold)
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
            title = { Text(stringResource(R.string.auto_button_delete_repeat_routine_title), fontWeight = FontWeight.ExtraBold) },
            text = { Text(stringResource(R.string.auto_button_delete_repeat_routine_confirm)) },
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
                    Text(stringResource(R.string.auto_button_delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDeleteId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                ) {
                    Text(stringResource(R.string.auto_button_cancel))
                }
            }
        )
    }
}


