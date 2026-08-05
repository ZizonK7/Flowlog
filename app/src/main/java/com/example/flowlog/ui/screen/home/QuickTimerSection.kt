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
internal fun QuickTimerSection(
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

internal const val PREFS_QUICK_TIMER_CONFIG = "quick_timer_config"
internal const val KEY_QUICK_TIMER_SWAPPED = "swapped"

internal fun formatBrushCountdown(endsAtMillis: Long): String {
    val remaining = endsAtMillis - System.currentTimeMillis()
    if (remaining <= 0L) return "양치"
    val totalSeconds = remaining / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

internal fun formatSnackCountdown(endsAtMillis: Long): String {
    val remaining = endsAtMillis - System.currentTimeMillis()
    if (remaining <= 0L) return "30분"
    val minutes = ((remaining + 59_000L) / 60_000L).coerceAtLeast(1L)
    return "${minutes}분"
}


