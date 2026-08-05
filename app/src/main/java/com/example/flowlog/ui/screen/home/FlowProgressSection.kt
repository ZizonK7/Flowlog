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
internal fun ExperimentSection(
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
internal fun FlowProgressRing(
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

internal fun DrawScope.drawFireRingGlow(
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

internal fun DrawScope.drawFireHead(
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


