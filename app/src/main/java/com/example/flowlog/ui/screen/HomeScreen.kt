package com.example.flowlog.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.ui.component.ActivityTitleDialog
import com.example.flowlog.ui.component.CategoryButton
import com.example.flowlog.ui.component.EditActivityDialog
import com.example.flowlog.ui.component.categoryColor
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.AnalyticsState
import com.example.flowlog.ui.viewmodel.CategoryStat
import com.example.flowlog.ui.viewmodel.TrendPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FlowPurple = Color(0xFF5140D8)
private val FlowPurpleSoft = Color(0xFFEDE9FF)
private val FlowInk = Color(0xFF10182C)
private val FlowMuted = Color(0xFF697386)
private val FlowDivider = Color(0xFFE8E8EE)

@Composable
fun HomeScreen(
    viewModel: ActivityViewModel,
    topActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val activityCategories = remember {
        listOf(
            "TOOTHBRUSH",
            "SNACK",
            "MEAL",
            "STUDY",
            "WORK",
            "DEVELOPMENT",
            "WASH",
            "SCHOOL",
            "EXERCISE",
            "SLEEP",
            "REST",
            "ETC"
        )
    }
    val experimentCategories = remember {
        listOf(
            "EXPERIMENT_1",
            "EXPERIMENT_2",
            "EXPERIMENT_3"
        )
    }
    val categories = remember(activityCategories, experimentCategories) {
        activityCategories + experimentCategories
    }
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
    var isActivityListExpanded by remember(selectedCategory) { mutableStateOf(false) }
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
            TodayFlowCard(
                isRunning = uiState.isRunning,
                currentCategory = uiState.currentCategory,
                elapsedTime = uiState.elapsedTime,
                statusMessage = uiState.statusMessage,
                categories = categories,
                onStop = {
                    viewModel.stopActivityAndSave()
                },
                onStart = { category ->
                    when (category) {
                        "EXPERIMENT_1" -> viewModel.scheduleBrushDoneExperiment()
                        "EXPERIMENT_2" -> viewModel.scheduleEatAllowedExperiment()
                        "EXPERIMENT_3" -> viewModel.startActivity(category)
                        else -> {
                            if (!uiState.isRunning || category == "SNACK" || category == "TOOTHBRUSH") {
                                viewModel.startActivity(category)
                            }
                        }
                    }
                }
            )
        }

        item {
            QuickTimerSection(
                categories = categories,
                isBrushTimerRunning = uiState.isBrushTimerRunning,
                onStart = { category ->
                    when (category) {
                        "TOOTHBRUSH" -> viewModel.startActivity(category)
                        "SNACK" -> viewModel.startActivity(category)
                    }
                }
            )
        }

        item {
            TimetableCard(activities = uiState.todayActivities)
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

        item {
            ExperimentSection(
                categories = categories,
                onStart = { category ->
                    when (category) {
                        "EXPERIMENT_1" -> viewModel.scheduleBrushDoneExperiment()
                        "EXPERIMENT_2" -> viewModel.scheduleEatAllowedExperiment()
                        "EXPERIMENT_3" -> viewModel.startActivity(category)
                    }
                }
            )
        }
    } // LazyColumn 닫기

    if (uiState.editingActivity != null) {
        EditActivityDialog(
                activity = uiState.editingActivity!!,
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

        val pendingSavedActivity = uiState.pendingSavedActivity
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ActivityTitleDialog(
                isVisible = pendingSavedActivity != null,
                category = pendingSavedActivity?.category.orEmpty(),
                categories = activityCategories,
                onSave = { category, title, note ->
                    viewModel.updatePendingSavedActivity(category, title, note)
                },
                initialTitle = pendingSavedActivity?.title,
                initialNote = pendingSavedActivity?.note,
                onDismiss = {
                    viewModel.dismissPendingSavedActivity()
                }
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
    statusMessage: String?,
    categories: List<String>,
    onStop: () -> Unit,
    onStart: (String) -> Unit
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
                    onStop = onStop
                )
            } else {
                FlowStartPage(
                    categories = categories,
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
    onStop: () -> Unit
) {
    val progressCycleMillis = if (currentCategory == "EXPERIMENT_3") {
        TimeUnit.SECONDS.toMillis(5)
    } else {
        TimeUnit.HOURS.toMillis(2)
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
                Text(
                    text = formatTime(elapsedTime),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    modifier = Modifier.padding(top = 30.dp)
                )
                FlowPageDots(activePage = 0)
            }
            FlowProgressRing(
                progress = progress,
                isOnFire = isOnFire,
                isRunning = true,
                modifier = Modifier.size(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FlowDivider)
        )
        Button(
            onClick = onStop,
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
}

@Composable
private fun FlowStartPage(
    categories: List<String>,
    statusMessage: String?,
    onStart: (String) -> Unit
) {
    val activityCategories = remember(categories) {
        categories.filter { it !in setOf("TOOTHBRUSH", "SNACK", "EXPERIMENT_1", "EXPERIMENT_2", "EXPERIMENT_3") }
    }

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
                .height(476.dp),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(activityCategories.size) { index ->
                val category = activityCategories[index]
                CategoryButton(
                    category = category,
                    onClick = { onStart(category) }
                )
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
    isBrushTimerRunning: Boolean,
    onStart: (String) -> Unit
) {
    val quickCategories = remember(categories) {
        categories.filter { it == "TOOTHBRUSH" || it == "SNACK" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
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
                    isSelected = category == "TOOTHBRUSH" && isBrushTimerRunning,
                    label = if (category == "TOOTHBRUSH" && isBrushTimerRunning) {
                        "양치중"
                    } else {
                        displayCategory(category)
                    },
                    onClick = { onStart(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
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
            Text(
                text = categorySymbol(activity.category),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
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
            items(activityCategories.size) { index ->
                val category = activityCategories[index]
                CategoryButton(
                    category = category,
                    isSelected = isRunning && currentCategory == category,
                    onClick = { onStart(category) }
                )
            }
        }
    }
}

private fun categorySymbol(category: String): String {
    return when (category) {
        "MEAL" -> "식"
        "REST" -> "휴"
        "TOOTHBRUSH" -> "양"
        "DEVELOPMENT" -> "</>"
        "WASH" -> "씻"
        "STUDY" -> "공"
        "WORK" -> "업"
        "SLEEP" -> "잠"
        "EXERCISE" -> "운"
        "SCHOOL" -> "학"
        "SNACK" -> "간"
        "TODO" -> "✓"
        else -> displayCategory(category).take(1)
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
            Text("통계 리포트", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FlowInk)
            SectionLabel("7일 활동별 하루 평균")
            AverageRows(analytics.weeklyDailyAverageStats)
            SectionLabel("주간 추세")
            TrendBars(analytics.weeklyTrend, analytics.weeklyDailyAverageStats)
        }
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

@Composable
private fun TimetableCard(activities: List<ActivitySession>) {
    val timelineItems = remember(activities) {
        activities.sortedBy { it.startTime }
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
            Text(
                text = "타임테이블",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = FlowInk
            )
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
                TimetableBar(activities = timelineItems)
            }
        }
    }
}

@Composable
private fun TimetableBar(
    activities: List<ActivitySession>
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val firstStart = activities.minOf { it.startTime }
    val lastEnd = activities.maxOf { it.endTime.coerceAtLeast(it.startTime + 1L) }
    val paddingMillis = 15L * 60L * 1000L
    val windowStart = (firstStart - paddingMillis).coerceAtLeast(0L)
    val windowEnd = (lastEnd + paddingMillis).coerceAtLeast(windowStart + 60L * 60L * 1000L)
    val windowDuration = (windowEnd - windowStart).coerceAtLeast(1L)
    val categories = remember(activities) {
        activities.map { it.category }.distinct()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        val guideColor = Color(0x332196F3)
        val trackColor = Color(0xFFE8EDF3)
        val barHeight = 26.dp.toPx()
        val top = 18.dp.toPx()
        val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

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

        activities.forEach { activity ->
            val startFraction = ((activity.startTime - windowStart).toFloat() / windowDuration.toFloat())
                .coerceIn(0f, 1f)
            val endFraction = ((activity.endTime.coerceAtLeast(activity.startTime + 1L) - windowStart).toFloat() / windowDuration.toFloat())
                .coerceIn(startFraction, 1f)
            val x = size.width * startFraction
            val width = (size.width * (endFraction - startFraction)).coerceAtLeast(3.dp.toPx())
            drawRoundRect(
                color = categoryColor(activity.category),
                topLeft = Offset(x, top),
                size = Size(width, barHeight),
                cornerRadius = radius
            )
        }

        drawLine(
            color = Color(0xFF64B5F6),
            start = Offset(0f, top + barHeight + 8.dp.toPx()),
            end = Offset(size.width, top + barHeight + 8.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
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

fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}
