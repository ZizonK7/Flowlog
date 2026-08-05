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
internal fun WeekdayRoutineSelector(
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
internal fun DayRoutineTimetable(
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
                stringResource(R.string.routine_manager_day_timetable_title, dayLabel(selectedDay)),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.routine_manager_schedule_count, schedules.size),
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
                stringResource(R.string.routine_manager_no_routine_for_day),
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
internal fun RoutineTimelineBlockRow(
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
                stringResource(R.string.routine_manager_time_range_with_label, formatMinuteOfDay(schedule.startMinuteOfDay), formatMinuteOfDay(schedule.endMinuteOfDay), displayCategory(schedule.category)),
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
                if (!schedule.isEnabled) stringResource(R.string.routine_manager_off_label) else stringResource(R.string.routine_manager_off_today_label),
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
internal fun AutoButtonScheduleActionSheet(
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
                stringResource(R.string.routine_manager_day_time_range, dayLabel(selectedDay), formatMinuteOfDay(schedule.startMinuteOfDay), formatMinuteOfDay(schedule.endMinuteOfDay)),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            SheetActionRow(stringResource(R.string.routine_manager_edit_action), Icons.Filled.Edit, onEdit)
            if (selectedDay != currentDayOfWeek() && schedule.canSkipNextDay(selectedDay)) {
                val nextDayLabel = if (isSkippedForSelectedDay) unskipNextDayLabel(selectedDay) else skipNextDayLabel(selectedDay)
                SheetActionRow(nextDayLabel, Icons.Filled.CalendarToday, onToggleNextDay)
            }
            if (selectedDay == currentDayOfWeek()) {
                SheetActionRow(if (isSkippedForSelectedDay) stringResource(R.string.routine_manager_skip_today_toggle_on) else stringResource(R.string.routine_manager_skip_today_toggle_off), Icons.Filled.CalendarToday, onSkipToday)
            }
            SheetActionRow(stringResource(R.string.routine_manager_delete_action), Icons.Filled.Delete, onDelete, tint = Color(0xFFD32F2F))
        }
    }
}

@Composable
internal fun SheetActionRow(
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
internal fun CalendarPetiteRow(
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
                        stringResource(R.string.routine_manager_today_only_label),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(accent, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                    Text(
                        stringResource(R.string.routine_manager_calendar_label),
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
                    contentDescription = stringResource(R.string.routine_manager_edit_time_content_desc),
                    tint = FlowMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.routine_manager_delete_today_content_desc),
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
internal fun CalendarPetiteTimeEditSheet(
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
                stringResource(R.string.routine_manager_edit_today_time_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                stringResource(R.string.routine_manager_today_only_applies_desc),
                fontSize = 13.sp,
                color = FlowMuted
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TimePickerCard(
                    label = stringResource(R.string.routine_manager_start_label),
                    minuteOfDay = startMinute,
                    onChange = { startMinute = it },
                    modifier = Modifier.weight(1f)
                )
                Text("~", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = FlowMuted)
                TimePickerCard(
                    label = stringResource(R.string.routine_manager_end_label),
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
                Text(stringResource(R.string.routine_manager_save), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun AutoButtonScheduleRow(
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
                            text = stringResource(R.string.routine_manager_today_off_label),
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
                    stringResource(R.string.routine_manager_time_range_with_label, formatMinuteOfDay(schedule.startMinuteOfDay), formatMinuteOfDay(schedule.endMinuteOfDay), formatRepeatDays(schedule.repeatDays)),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.routine_manager_edit_content_desc),
                    tint = FlowMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.routine_manager_delete_content_desc),
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
                stringResource(R.string.routine_manager_active_label),
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
                        if (schedule.isSkippedToday) stringResource(R.string.routine_manager_skip_today_toggle_on) else stringResource(R.string.routine_manager_skip_today_toggle_off),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
internal fun RoutineTimelinePreview(
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
internal fun AutoButtonEditSheet(
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
    val endBeforeStartError = stringResource(R.string.routine_manager_end_before_start_error)
    val selectRepeatDayError = stringResource(R.string.routine_manager_select_repeat_day_error)
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
                        contentDescription = stringResource(R.string.routine_manager_back_content_desc),
                        tint = FlowInk,
                        modifier = Modifier.size(23.dp)
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (initial.scheduleId.isBlank()) stringResource(R.string.routine_manager_add_routine_title) else stringResource(R.string.routine_manager_edit_routine_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk
                    )
                    Text(
                        stringResource(R.string.routine_manager_routine_desc_input),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = FlowMuted,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.routine_manager_category_label),
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
                    stringResource(R.string.routine_manager_routine_name_label),
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
                Text(stringResource(R.string.routine_manager_time_label), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TimePickerCard(
                        label = stringResource(R.string.routine_manager_start_label),
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
                        label = stringResource(R.string.routine_manager_end_label),
                        minuteOfDay = endMinute,
                        onChange = { endMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.routine_manager_repeat_day_label), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
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
                        stringResource(R.string.routine_manager_notification_settings),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk,
                        modifier = Modifier.weight(1f)
                    )
                    Text(stringResource(R.string.routine_manager_select_label), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FlowMuted)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.dp, FlowDivider, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ToggleRow(stringResource(R.string.routine_manager_active_label), isEnabled) { isEnabled = it }
                    ToggleRow(stringResource(R.string.routine_manager_start_notification), notifyOnStart) { notifyOnStart = it }
                    ToggleRow(stringResource(R.string.routine_manager_end_notification), notifyOnEnd) { notifyOnEnd = it }
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
                    Text(stringResource(R.string.routine_manager_cancel), fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val cleanTitle = title.trim().ifBlank { displayCategory(category) }
                        errorMessage = when {
                            endMinute <= startMinute -> endBeforeStartError
                            repeatDays.isEmpty() -> selectRepeatDayError
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
                    Text(stringResource(R.string.routine_manager_save), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
internal fun AutoButtonCategorySegment(
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
internal fun RepeatDayButton(
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
internal fun RoutineInfoPanel() {
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
                stringResource(R.string.routine_manager_routine_timeline_desc),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = FlowMuted
            )
            Text(
                stringResource(R.string.routine_manager_disable_today_hint),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
internal fun TimePickerCard(
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
            stringResource(R.string.routine_manager_tap_to_change),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted.copy(alpha = 0.6f)
        )
    }

    if (isPickerOpen) {
        TimePickerSheet(
            title = stringResource(R.string.routine_manager_time_setting_title, label),
            subtitle = stringResource(R.string.routine_manager_time_setting_subtitle, label),
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
internal fun TimePickerSheet(
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
                        values = (0..59).toList(),
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
                        Text(stringResource(R.string.routine_manager_cancel), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
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
                        Text(stringResource(R.string.routine_manager_confirm), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun FormPanel(content: @Composable ColumnScope.() -> Unit) {
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
internal fun ToggleRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text, fontSize = 13.sp, color = FlowInk)
    }
}


