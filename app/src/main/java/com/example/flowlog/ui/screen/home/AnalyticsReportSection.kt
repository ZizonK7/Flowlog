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
internal fun AnalyticsCard(
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
internal fun TodayActivityReport(stats: List<CategoryStat>) {
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
internal fun YesterdayComparisonReport(
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
internal fun SectionHeaderRow(
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
internal fun ReportPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
internal fun TodayCategoryRow(stat: CategoryStat, maxMillis: Long) {
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
internal fun ComparisonCategoryRow(
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
internal fun ComparisonBar(fraction: Float, color: Color) {
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
internal fun ProgressTrack(
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
internal fun CategoryBadge(category: String) {
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
internal fun DeltaText(deltaMillis: Long, color: Color) {
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
internal fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
internal fun ReportDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(1.dp)
            .background(FlowDivider)
    )
}

@Composable
internal fun EmptyReportText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = FlowMuted,
        modifier = Modifier.padding(vertical = 14.dp)
    )
}

@Composable
internal fun ReportMoreButton(
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

internal fun reportCategoryBackground(category: String): Color {
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
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = FlowInk,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
internal fun AverageRows(stats: List<CategoryStat>) {
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

internal fun formatDurationWithoutSeconds(durationMillis: Long): String {
    val durationHours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60

    return when {
        durationHours > 0 -> "${durationHours}시간 ${durationMinutes}분"
        durationMinutes > 0 -> "${durationMinutes}분"
        else -> "1분 미만"
    }
}

internal sealed class TimelineBlock {
    data class ActualActivity(val segment: DisplayActivitySegment) : TimelineBlock()
    data class ScheduledAutoButton(val block: ScheduledAutoButtonBlock) : TimelineBlock()
    data class RecommendedTodo(val block: RecommendedTodoBlock) : TimelineBlock()
}


