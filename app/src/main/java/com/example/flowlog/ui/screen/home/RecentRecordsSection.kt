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

@Composable
internal fun RecentRecordsCard(
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
internal fun RecentRecordRow(
    activity: ActivitySession,
    onDelete: (ActivitySession) -> Unit,
    onEdit: (Long) -> Unit,
    onToggleFavorite: (ActivitySession) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTimeText = timeFormat.format(Date(activity.startTime))
    val endTimeText = timeFormat.format(Date(activity.endTime))
    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(activity.durationMillis).coerceAtLeast(1L)
    val durationText = if (durationMinutes >= 60) {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        if (m == 0L) "${h}시간" else "${h}시간 ${m}분"
    } else {
        "${durationMinutes}분"
    }
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
                text = durationText,
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
internal fun ActivityStartGrid(
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



