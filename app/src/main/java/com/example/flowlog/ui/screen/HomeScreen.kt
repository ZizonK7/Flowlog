package com.example.flowlog.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.ui.component.ActivityItem
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

@Composable
fun HomeScreen(
    viewModel: ActivityViewModel,
    startCategoryRequest: String? = null,
    onStartCategoryConsumed: () -> Unit = {},
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
            "EXPERIMENT_2"
        )
    }
    val categories = remember(activityCategories, experimentCategories) {
        activityCategories + experimentCategories
    }
    val timedCategories = remember(activityCategories) {
        activityCategories.filter { it != "SNACK" && it != "TOOTHBRUSH" }
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

    LaunchedEffect(startCategoryRequest) {
        val category = startCategoryRequest ?: return@LaunchedEffect
        viewModel.startActivity(category)
        onStartCategoryConsumed()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA)),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            topActions()
        }

        item {
            TimerCard(
                isRunning = uiState.isRunning,
                currentCategory = uiState.currentCategory,
                elapsedTime = uiState.elapsedTime,
                statusMessage = uiState.statusMessage,
                onStop = {
                    viewModel.stopActivityAndSave()
                }
            )
        }

        item {
            QuickActionsCard(
                favorites = uiState.favoriteActivities,
                lastActivity = uiState.lastTimedActivity,
                onRestart = { viewModel.restartActivity(it) }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                text = "활동 시작",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
                Button(
                    onClick = { viewModel.undoLastAddedActivity() },
                    enabled = uiState.lastAddedActivity != null,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("되돌리기", fontSize = 12.sp)
                }
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(416.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                userScrollEnabled = false
            ) {
                items(categories.size) { index ->
                    val category = categories[index]
                    CategoryButton(
                        category = category,
                        isSelected = uiState.isRunning && uiState.currentCategory == category,
                        onClick = {
                            when (category) {
                                "EXPERIMENT_1" -> viewModel.scheduleBrushDoneExperiment()
                                "EXPERIMENT_2" -> viewModel.scheduleEatAllowedExperiment()
                                else -> {
                                    if (!uiState.isRunning || category == "SNACK" || category == "TOOTHBRUSH") {
                                        viewModel.startActivity(category)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        item {
            TimetableCard(activities = uiState.todayActivities)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedCategory == null) "오늘 기록" else "${displayCategory(selectedCategory)} 기록",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isFiltered) {
                    Button(
                        onClick = { viewModel.clearFilter() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("초기화", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(timedCategories.size) { index ->
                    val category = timedCategories[index]
                    FilterChip(
                        selected = uiState.selectedCategory == category,
                        onClick = { viewModel.filterByCategory(category) },
                        label = { Text(displayCategory(category)) }
                    )
                }
            }
        }

        if (displayActivities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFiltered) "조건에 맞는 활동이 없습니다." else "오늘 저장된 활동이 없습니다.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(
                items = visibleActivities,
                key = { it.id }
            ) { activity ->
                ActivityItem(
                    activity = activity,
                    onDelete = { viewModel.deleteActivity(it) },
                    onEdit = { viewModel.startEditActivity(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                )
            }
            if (displayActivities.size > 3) {
                item {
                    Button(
                        onClick = { isActivityListExpanded = !isActivityListExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isActivityListExpanded) {
                                "\uC811\uAE30"
                            } else {
                                "\uB354\uBCF4\uAE30 ${hiddenActivityCount}\uAC1C"
                            },
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            AnalyticsCard(analytics = uiState.analytics)
        }
    }

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

@Composable
private fun QuickActionsCard(
    favorites: List<ActivitySession>,
    lastActivity: ActivitySession?,
    onRestart: (ActivitySession) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("빠른 시작", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (lastActivity != null) {
                Button(
                    onClick = { onRestart(lastActivity) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Text("지난 활동 다시 시작: ${displayCategory(lastActivity.category)} / ${lastActivity.title}")
                }
            } else {
                Text(
                    "아직 다시 시작할 활동이 없습니다.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (favorites.isNotEmpty()) {
                Text(
                    "즐겨찾기",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(favorites.size) { index ->
                        val favorite = favorites[index]
                        FilterChip(
                            selected = false,
                            onClick = { onRestart(favorite) },
                            label = { Text("${displayCategory(favorite.category)}: ${favorite.title}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    isRunning: Boolean,
    currentCategory: String,
    elapsedTime: Long,
    statusMessage: String?,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRunning) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("진행 중", fontSize = 14.sp, color = Color.Gray)
                    Text(
                        text = displayCategory(currentCategory),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = formatTime(elapsedTime),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = onStop,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
                    ) {
                        Text("종료하고 저장", fontSize = 16.sp, color = Color.White)
                    }
                }
            } else {
                Text(
                    text = statusMessage ?: "활동을 선택해서 시작하세요.",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
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
            Text("통계 리포트", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            SectionLabel("어제까지 7일 활동별 하루 평균")
            AverageRows(analytics.weeklyDailyAverageStats)
            SectionLabel("주간 추세")
            TrendBars(analytics.weeklyTrend)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun AverageRows(stats: List<CategoryStat>) {
    if (stats.isEmpty()) {
        Text("최근 7일 평균을 계산할 데이터가 없습니다.", fontSize = 12.sp, color = Color.Gray)
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
            Text(displayCategory(stat.category), fontSize = 12.sp, modifier = Modifier.width(72.dp))
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
                color = Color.Gray,
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
private fun TrendBars(points: List<TrendPoint>) {
    val visiblePoints = remember(points) {
        if (points.size > 14) {
            points.filterIndexed { index, _ -> index % 3 == 0 || index == points.lastIndex }
        } else {
            points
        }
    }
    val categories = remember(visiblePoints) {
        visiblePoints
            .flatMap { it.categoryMillis.keys }
            .distinct()
            .sorted()
    }
    val maxMillis = remember(visiblePoints) {
        visiblePoints.maxOfOrNull { it.totalMillis }?.coerceAtLeast(1L) ?: 1L
    }

    if (categories.isEmpty()) {
        Text("아직 추세 데이터가 없습니다.", fontSize = 12.sp, color = Color.Gray)
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
                Text(point.label, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
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
                    color = Color.Gray,
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
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "오늘 활동 흐름을 한 줄 색상 막대로 보여줍니다.",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (timelineItems.isEmpty()) {
                Text(
                    text = "아직 기록된 활동이 없습니다.",
                    fontSize = 14.sp,
                    color = Color.Gray,
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
            color = Color.Gray
        )
        Text(
            text = timeFormat.format(Date((windowStart + windowEnd) / 2L)),
            fontSize = 10.sp,
            color = Color.Gray
        )
        Text(
            text = timeFormat.format(Date(windowEnd)),
            fontSize = 10.sp,
            color = Color.Gray
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
                    color = Color.Gray,
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
