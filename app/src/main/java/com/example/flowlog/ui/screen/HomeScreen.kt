package com.example.flowlog.ui.screen

import android.app.TimePickerDialog
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
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.example.flowlog.ui.component.CategoryButton
import com.example.flowlog.ui.component.CategoryGlyph
import com.example.flowlog.ui.component.EditActivityDialog
import com.example.flowlog.ui.component.categoryColor
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.AnalyticsState
import com.example.flowlog.ui.viewmodel.CategoryStat
import com.example.flowlog.ui.viewmodel.TrendPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FlowPurple = Color(0xFF5140D8)
private val FlowPurpleDeep = Color(0xFF2F238F)
private val FlowPurpleSoft = Color(0xFFEDE9FF)
private val FlowInk = Color(0xFF10182C)
private val FlowMuted = Color(0xFF697386)
private val FlowDivider = Color(0xFFE8E8EE)

@Composable
fun HomeScreen(
    viewModel: ActivityViewModel,
    topActions: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    autoButtonManagerOpen: Boolean = false,
    onAutoButtonManagerDismiss: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var localAutoButtonManagerOpen by remember { mutableStateOf(false) }
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
            "COMPANY",
            "EXERCISE",
            "SLEEP",
            "REST",
            "ETC"
        )
    }
    val categories = activityCategories
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
            val titleSuggestions = remember(uiState.currentCategory, uiState.allActivities) {
                buildTitleSuggestions(
                    category = uiState.currentCategory,
                    activities = uiState.allActivities
                )
            }
            TodayFlowCard(
                isRunning = uiState.isRunning,
                currentCategory = uiState.currentCategory,
                elapsedTime = uiState.elapsedTime,
                timerGoalMillis = uiState.timerGoalMillis,
                statusMessage = uiState.statusMessage,
                appliedTitle = uiState.pendingTitle.orEmpty(),
                titleSuggestions = titleSuggestions,
                categories = categories,
                onStop = { title ->
                    viewModel.stopActivityAndSave(title)
                },
                onApplyTitle = { title ->
                    viewModel.setRunningActivityTitle(title)
                },
                onStart = { category ->
                    if (!uiState.isRunning || category == "SNACK" || category == "TOOTHBRUSH") {
                        viewModel.startActivity(category)
                    }
                }
            )
        }

        item {
            QuickTimerSection(
                categories = categories,
                isBrushTimerRunning = uiState.isBrushTimerRunning,
                brushDoneEndsAtMillis = uiState.brushDoneEndsAtMillis,
                snackButtonEndsAtMillis = uiState.snackButtonEndsAtMillis,
                onStart = { category ->
                    when (category) {
                        "TOOTHBRUSH" -> viewModel.startActivity(category)
                        "SNACK" -> viewModel.startActivity(category)
                    }
                }
            )
        }

        item {
            TimetableCard(
                activities = uiState.todayActivities,
                scheduledBlocks = uiState.scheduledAutoButtonBlocks,
                recommendedBlocks = uiState.recommendedTodoBlocks,
                onSkipToday = { viewModel.skipAutoButtonToday(it) },
                onUnskipToday = { viewModel.unskipAutoButtonToday(it) },
                onEditSchedule = { scheduleId ->
                    localAutoButtonManagerOpen = true
                },
                onManageSchedules = { localAutoButtonManagerOpen = true },
                onStartRecommended = { viewModel.startRecommendedTodoActivity(it) },
                onDismissRecommended = { viewModel.dismissRecommendedTodoBlock(it) }
            )
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

    } // LazyColumn 닫기

    uiState.editingActivity?.let { editingActivity ->
        EditActivityDialog(
            activity = editingActivity,
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

    if (autoButtonManagerOpen || localAutoButtonManagerOpen) {
        AutoButtonManagerSheet(
            schedules = uiState.autoButtonSchedules,
            categories = activityCategories,
            onDismiss = {
                localAutoButtonManagerOpen = false
                onAutoButtonManagerDismiss()
            },
            onSave = viewModel::saveAutoButtonSchedule,
            onToggleEnabled = viewModel::setAutoButtonEnabled,
            onSkipToday = viewModel::skipAutoButtonToday,
            onUnskipToday = viewModel::unskipAutoButtonToday,
            onDelete = viewModel::deleteAutoButtonSchedule
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
    timerGoalMillis: Long,
    statusMessage: String?,
    appliedTitle: String,
    titleSuggestions: List<String>,
    categories: List<String>,
    onStop: (String) -> Unit,
    onApplyTitle: (String) -> Unit,
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
                    timerGoalMillis = timerGoalMillis,
                    initialAppliedTitle = appliedTitle,
                    titleSuggestions = titleSuggestions,
                    onStop = onStop,
                    onApplyTitle = onApplyTitle
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
    timerGoalMillis: Long,
    initialAppliedTitle: String,
    titleSuggestions: List<String>,
    onStop: (String) -> Unit,
    onApplyTitle: (String) -> Unit
) {
    var title by remember(currentCategory) { mutableStateOf("") }
    var appliedTitle by remember(currentCategory) { mutableStateOf(initialAppliedTitle) }
    val focusManager = LocalFocusManager.current
    val progressCycleMillis = if (currentCategory == "EXPERIMENT_3") {
        TimeUnit.SECONDS.toMillis(5)
    } else {
        timerGoalMillis.coerceAtLeast(1L)
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
                val formattedTime = formatTime(elapsedTime)
                val timeFontSize = when {
                    formattedTime.length <= 5 -> 34.sp   // MM:SS
                    formattedTime.length <= 7 -> 26.sp   // H:MM:SS (1–9시간)
                    else -> 22.sp                         // HH:MM:SS (10시간+)
                }
                Box(
                    modifier = Modifier
                        .padding(top = 30.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = formattedTime,
                        fontSize = timeFontSize,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = FlowInk,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                if (appliedTitle.isNotBlank()) {
                    Text(
                        text = appliedTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            FlowProgressRing(
                progress = progress,
                isOnFire = isOnFire,
                isRunning = true,
                modifier = Modifier.size(150.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        if (titleSuggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(titleSuggestions.size) { index ->
                    SuggestionChip(
                        onClick = { title = titleSuggestions[index] },
                        label = {
                            Text(
                                text = titleSuggestions[index],
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = FlowPurpleSoft,
                            labelColor = FlowPurple
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
                onValueChange = { title = it },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .border(1.dp, FlowDivider, RoundedCornerShape(13.dp))
                    .background(Color.White, RoundedCornerShape(13.dp))
                    .padding(horizontal = 14.dp),
                singleLine = true,
                textStyle = TextStyle(
                    color = FlowInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                cursorBrush = SolidColor(FlowPurple),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = FlowPurple.copy(alpha = 0.58f),
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "직접 입력",
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
                    val cleanTitle = title.trim()
                    title = cleanTitle
                    appliedTitle = cleanTitle
                    onApplyTitle(cleanTitle)
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .width(66.dp)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Text(
                    text = "적용",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FlowDivider)
        )
        Button(
            onClick = {
                val finalTitle = appliedTitle.ifBlank { title }.trim()
                onApplyTitle(finalTitle)
                onStop(finalTitle)
            },
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

private fun buildTitleSuggestions(
    category: String,
    activities: List<ActivitySession>
): List<String> {
    val defaultTitle = defaultActivityTitle(category)
    return activities
        .filter { it.category == category && it.title.isNotBlank() && it.title != defaultTitle }
        .groupBy { it.title.trim() }
        .map { (title, sessions) -> Triple(title, sessions.size, sessions.maxOf { it.startTime }) }
        .sortedWith(compareByDescending<Triple<String, Int, Long>> { it.second }.thenByDescending { it.third })
        .map { it.first }
        .take(5)
}

private fun defaultActivityTitle(category: String): String {
    return when (category) {
        "MEAL" -> "식사"
        "EXERCISE" -> "운동"
        "SLEEP" -> "수면"
        "STUDY" -> "공부"
        "WORK" -> "업무"
        "COMPANY" -> "회사"
        "DEVELOPMENT" -> "개발"
        "WASH" -> "씻기"
        "REST" -> "휴식"
        "SCHOOL" -> "학교"
        else -> "활동"
    }
}

@Composable
private fun FlowStartPage(
    categories: List<String>,
    statusMessage: String?,
    onStart: (String) -> Unit
) {
    val activityCategories = remember(categories) {
        categories.filter { it !in setOf("TOOTHBRUSH", "SNACK", "COMPANY", "EXPERIMENT_1", "EXPERIMENT_2", "EXPERIMENT_3") }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("flowlog_prefs", android.content.Context.MODE_PRIVATE) }
    var schoolSlotCategory by remember {
        mutableStateOf(
            when (prefs.getString("school_slot_category", "SCHOOL")) {
                "WORK" -> "COMPANY"
                "COMPANY" -> "COMPANY"
                else -> "SCHOOL"
            }
        )
    }
    var showToggleDialog by remember { mutableStateOf(false) }

    if (showToggleDialog) {
        val isSchool = schoolSlotCategory == "SCHOOL"
        AlertDialog(
            onDismissRequest = { showToggleDialog = false },
            title = { Text("버튼 변경") },
            text = {
                Text(
                    if (isSchool) "'학교' 버튼을 '회사' 버튼으로 변경할까요?"
                    else "'회사' 버튼을 '학교' 버튼으로 변경할까요?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = if (isSchool) "COMPANY" else "SCHOOL"
                    schoolSlotCategory = next
                    prefs.edit().putString("school_slot_category", next).apply()
                    showToggleDialog = false
                }) {
                    Text(if (isSchool) "회사로 변경" else "학교로 변경")
                }
            },
            dismissButton = {
                TextButton(onClick = { showToggleDialog = false }) { Text("취소") }
            }
        )
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
                val rawCategory = activityCategories[index]
                val effectiveCategory = if (rawCategory == "SCHOOL") schoolSlotCategory else rawCategory
                CategoryButton(
                    category = effectiveCategory,
                    label = displayCategory(effectiveCategory),
                    onClick = { onStart(effectiveCategory) },
                    onLongClick = if (rawCategory == "SCHOOL") {
                        { showToggleDialog = true }
                    } else null
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
    brushDoneEndsAtMillis: Long,
    snackButtonEndsAtMillis: Long,
    onStart: (String) -> Unit
) {
    val quickCategories = remember(categories) {
        categories.filter { it == "TOOTHBRUSH" || it == "SNACK" }
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
            kotlinx.coroutines.delay(30_000L)
            snackLabel = formatSnackCountdown(snackButtonEndsAtMillis)
        }
        snackLabel = "간식"
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
                    isSelected = (category == "TOOTHBRUSH" && isBrushTimerRunning)
                        || (category == "SNACK" && snackButtonEndsAtMillis > 0L),
                    label = when (category) {
                        "TOOTHBRUSH" -> brushLabel
                        "SNACK" -> snackLabel
                        else -> displayCategory(category)
                    },
                    onClick = { onStart(category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatBrushCountdown(endsAtMillis: Long): String {
    val remaining = endsAtMillis - System.currentTimeMillis()
    if (remaining <= 0L) return "양치"
    val totalSeconds = remaining / 1000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatSnackCountdown(endsAtMillis: Long): String {
    val remaining = endsAtMillis - System.currentTimeMillis()
    if (remaining <= 0L) return "간식"
    val minutes = ((remaining + 59_000L) / 60_000L).coerceAtLeast(1L)
    return "${minutes}분"
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
                    tint = FlowPurple.copy(alpha = 0.62f),
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
private fun TodayActivityReport(stats: List<CategoryStat>) {
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
private fun YesterdayComparisonReport(
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
private fun SectionHeaderRow(
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
private fun ReportPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        content = content
    )
}

@Composable
private fun TodayCategoryRow(stat: CategoryStat, maxMillis: Long) {
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
private fun ComparisonCategoryRow(
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
private fun ComparisonBar(fraction: Float, color: Color) {
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
private fun ProgressTrack(
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
private fun CategoryBadge(category: String) {
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
private fun DeltaText(deltaMillis: Long, color: Color) {
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
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(8.dp)
            .background(color, CircleShape)
    )
}

@Composable
private fun ReportDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp)
            .height(1.dp)
            .background(FlowDivider)
    )
}

@Composable
private fun EmptyReportText(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = FlowMuted,
        modifier = Modifier.padding(vertical = 14.dp)
    )
}

@Composable
private fun ReportMoreButton(
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

private fun reportCategoryBackground(category: String): Color {
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

private sealed class TimelineBlock {
    data class ActualActivity(val activity: ActivitySession) : TimelineBlock()
    data class ScheduledAutoButton(val block: ScheduledAutoButtonBlock) : TimelineBlock()
    data class RecommendedTodo(val block: RecommendedTodoBlock) : TimelineBlock()
}

@Composable
private fun TimetableCard(
    activities: List<ActivitySession>,
    scheduledBlocks: List<ScheduledAutoButtonBlock>,
    recommendedBlocks: List<RecommendedTodoBlock>,
    onSkipToday: (String) -> Unit,
    onUnskipToday: (String) -> Unit,
    onEditSchedule: (String) -> Unit,
    onManageSchedules: () -> Unit,
    onStartRecommended: (RecommendedTodoBlock) -> Unit,
    onDismissRecommended: (String) -> Unit
) {
    var selectedBlock by remember { mutableStateOf<ScheduledAutoButtonBlock?>(null) }
    var selectedRecommendedBlock by remember { mutableStateOf<RecommendedTodoBlock?>(null) }
    val timelineItems = remember(activities, scheduledBlocks, recommendedBlocks) {
        activities.map { TimelineBlock.ActualActivity(it) } +
            scheduledBlocks.map { TimelineBlock.ScheduledAutoButton(it) } +
            recommendedBlocks.map { TimelineBlock.RecommendedTodo(it) }
    }.sortedBy { block ->
        when (block) {
            is TimelineBlock.ActualActivity -> block.activity.startTime
            is TimelineBlock.ScheduledAutoButton -> block.block.startTime
            is TimelineBlock.RecommendedTodo -> block.block.plannedStartMillis
        }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "타임테이블",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowInk,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onManageSchedules,
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
                ) {
                    Text("고정 시간", fontWeight = FontWeight.Bold)
                }
            }
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
                TimetableBar(
                    blocks = timelineItems,
                    onScheduledLongPress = { block -> selectedBlock = block },
                    onRecommendedClick = { block -> selectedRecommendedBlock = block }
                )
                ScheduledAutoButtonList(
                    blocks = scheduledBlocks,
                    onShowMenu = { block -> selectedBlock = block }
                )
                RecommendedTodoBlockList(
                    blocks = recommendedBlocks,
                    onShowMenu = { block -> selectedRecommendedBlock = block }
                )
            }
        }
    }

    selectedBlock?.let { block ->
        ScheduledAutoButtonActionSheet(
            block = block,
            onDismiss = { selectedBlock = null },
            onSkipToday = onSkipToday,
            onUnskipToday = onUnskipToday,
            onEditSchedule = onEditSchedule
        )
    }

    selectedRecommendedBlock?.let { block ->
        RecommendedTodoActionSheet(
            block = block,
            onDismiss = { selectedRecommendedBlock = null },
            onStart = {
                onStartRecommended(block)
                selectedRecommendedBlock = null
            },
            onHideToday = {
                onDismissRecommended(block.itemId)
                selectedRecommendedBlock = null
            }
        )
    }
}

@Composable
private fun TimetableBar(
    blocks: List<TimelineBlock>,
    onScheduledLongPress: (ScheduledAutoButtonBlock) -> Unit,
    onRecommendedClick: (RecommendedTodoBlock) -> Unit
) {
    if (blocks.isEmpty()) return
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val firstStart = blocks.minOf {
        when (it) {
            is TimelineBlock.ActualActivity -> it.activity.startTime
            is TimelineBlock.ScheduledAutoButton -> it.block.startTime
            is TimelineBlock.RecommendedTodo -> it.block.plannedStartMillis
        }
    }
    val lastEnd = blocks.maxOf {
        when (it) {
            is TimelineBlock.ActualActivity -> it.activity.endTime.coerceAtLeast(it.activity.startTime + 1L)
            is TimelineBlock.ScheduledAutoButton -> it.block.endTime.coerceAtLeast(it.block.startTime + 1L)
            is TimelineBlock.RecommendedTodo -> it.block.plannedEndMillis.coerceAtLeast(it.block.plannedStartMillis + 1L)
        }
    }
    val paddingMillis = 15L * 60L * 1000L
    val windowStart = (firstStart - paddingMillis).coerceAtLeast(0L)
    val windowEnd = (lastEnd + paddingMillis).coerceAtLeast(windowStart + 60L * 60L * 1000L)
    val windowDuration = (windowEnd - windowStart).coerceAtLeast(1L)
    val scheduled = blocks.filterIsInstance<TimelineBlock.ScheduledAutoButton>().map { it.block }
    val recommended = blocks.filterIsInstance<TimelineBlock.RecommendedTodo>().map { it.block }
    val categories = remember(blocks) {
        blocks.map {
            when (it) {
                is TimelineBlock.ActualActivity -> it.activity.category
                is TimelineBlock.ScheduledAutoButton -> it.block.category
                is TimelineBlock.RecommendedTodo -> "TODO"
            }
        }.distinct()
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(scheduled, recommended, windowStart, windowDuration) {
                detectTapGestures(
                    onTap = { offset ->
                        val hit = recommended.firstOrNull { block ->
                            val startFraction = ((block.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(0f, 1f)
                            val endFraction = ((block.plannedEndMillis - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(startFraction, 1f)
                            val startX = size.width * startFraction
                            val endX = size.width * endFraction
                            offset.x in startX..endX
                        }
                        if (hit != null) onRecommendedClick(hit)
                    },
                    onLongPress = { offset ->
                        val hit = scheduled.firstOrNull { block ->
                            val startFraction = ((block.startTime - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(0f, 1f)
                            val endFraction = ((block.endTime - windowStart).toFloat() / windowDuration.toFloat())
                                .coerceIn(startFraction, 1f)
                            val startX = size.width * startFraction
                            val endX = size.width * endFraction
                            offset.x in startX..endX
                        }
                        if (hit != null) onScheduledLongPress(hit)
                    }
                )
            }
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

        blocks.forEach { block ->
            when (block) {
                is TimelineBlock.ActualActivity -> {
                    val activity = block.activity
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
                is TimelineBlock.ScheduledAutoButton -> {
                    val item = block.block
                    val startFraction = ((item.startTime - windowStart).toFloat() / windowDuration.toFloat())
                        .coerceIn(0f, 1f)
                    val endFraction = ((item.endTime.coerceAtLeast(item.startTime + 1L) - windowStart).toFloat() / windowDuration.toFloat())
                        .coerceIn(startFraction, 1f)
                    val x = size.width * startFraction
                    val width = (size.width * (endFraction - startFraction)).coerceAtLeast(3.dp.toPx())
                    val strokeColor = categoryColor(item.category).copy(alpha = if (item.isSkippedToday) 0.28f else 0.58f)
                    drawRoundRect(
                        color = strokeColor.copy(alpha = if (item.isSkippedToday) 0.08f else 0.16f),
                        topLeft = Offset(x, top),
                        size = Size(width, barHeight),
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(x, top),
                        size = Size(width, barHeight),
                        cornerRadius = radius,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                is TimelineBlock.RecommendedTodo -> {
                    val item = block.block
                    val startFraction = ((item.plannedStartMillis - windowStart).toFloat() / windowDuration.toFloat())
                        .coerceIn(0f, 1f)
                    val endFraction = ((item.plannedEndMillis.coerceAtLeast(item.plannedStartMillis + 1L) - windowStart).toFloat() / windowDuration.toFloat())
                        .coerceIn(startFraction, 1f)
                    val x = size.width * startFraction
                    val width = (size.width * (endFraction - startFraction)).coerceAtLeast(4.dp.toPx())
                    val color = FlowPurple.copy(alpha = 0.7f)
                    drawRoundRect(
                        color = FlowPurpleSoft.copy(alpha = 0.45f),
                        topLeft = Offset(x, top + 4.dp.toPx()),
                        size = Size(width, barHeight - 8.dp.toPx()),
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, top + 4.dp.toPx()),
                        size = Size(width, barHeight - 8.dp.toPx()),
                        cornerRadius = radius,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
                        )
                    )
                }
            }
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

@Composable
private fun ScheduledAutoButtonList(
    blocks: List<ScheduledAutoButtonBlock>,
    onShowMenu: (ScheduledAutoButtonBlock) -> Unit
) {
    if (blocks.isEmpty()) return
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyRow(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(blocks.size) { index ->
            val block = blocks[index]
            Row(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = categoryColor(block.category).copy(alpha = if (block.isSkippedToday) 0.2f else 0.45f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        categoryColor(block.category).copy(alpha = if (block.isSkippedToday) 0.04f else 0.08f),
                        RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(onClick = {}, onLongClick = { onShowMenu(block) })
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "${timeFormat.format(Date(block.startTime))}-${timeFormat.format(Date(block.endTime))}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowMuted
                )
                Text(
                    text = block.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (block.isSkippedToday) FlowMuted else FlowInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (block.isSkippedToday) {
                    Text(
                        text = "꺼짐",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(FlowPurple.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedTodoBlockList(
    blocks: List<RecommendedTodoBlock>,
    onShowMenu: (RecommendedTodoBlock) -> Unit
) {
    if (blocks.isEmpty()) return
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(blocks.size) { index ->
            val block = blocks[index]
            Row(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = FlowPurple.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        FlowPurpleSoft.copy(alpha = 0.42f),
                        RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(onClick = { onShowMenu(block) }, onLongClick = { onShowMenu(block) })
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "${timeFormat.format(Date(block.plannedStartMillis))} 시작 추천",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = FlowMuted
                )
                Text(
                    text = "${block.title} - ${burdenLabel(block.burdenLevel)} 추천",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "추천",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(FlowPurple.copy(alpha = 0.78f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduledAutoButtonActionSheet(
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
                Text(if (block.isSkippedToday) "오늘 다시 켜기" else "오늘만 끄기")
            }
            TextButton(
                onClick = {
                    onEditSchedule(block.scheduleId)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
            ) {
                Text("설정 수정", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
            ) {
                Text("닫기")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendedTodoActionSheet(
    block: RecommendedTodoBlock,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onHideToday: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var showReason by remember(block.itemId) { mutableStateOf(false) }
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
                "${block.title} - ${burdenLabel(block.burdenLevel)} 추천",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            Text(
                "${timeFormat.format(Date(block.plannedStartMillis))} 시작 추천",
                fontSize = 13.sp,
                color = FlowMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                )
            ) {
                Text("시작하기", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onHideToday,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
            ) {
                Text("오늘 숨기기", fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = { showReason = !showReason },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowInk)
            ) {
                Text("추천 이유 보기", fontWeight = FontWeight.Bold)
            }
            if (showReason) {
                Text(
                    text = block.reason ?: "오늘의 목표와 최근 시간대 기록을 바탕으로 추천했어요.",
                    fontSize = 13.sp,
                    color = FlowMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F7FA), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
            ) {
                Text("닫기")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun burdenLabel(level: String): String {
    return when (level) {
        "HEAVY" -> "과중함"
        "LIGHT" -> "가벼움"
        else -> "보통"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoButtonManagerSheet(
    schedules: List<AutoButtonSchedule>,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (AutoButtonSchedule) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onSkipToday: (String) -> Unit,
    onUnskipToday: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var editing by remember { mutableStateOf<AutoButtonSchedule?>(null) }
    var confirmDeleteId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "고정 시간 버튼 관리",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            Text(
                "반복되는 학교/회사 시간을 추가하면 타임테이블에 예정으로 표시됩니다.",
                fontSize = 13.sp,
                color = FlowMuted
            )
            if (schedules.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    schedules.forEach { schedule ->
                        AutoButtonScheduleRow(
                            schedule = schedule,
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
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { editing = defaultAutoButtonSchedule() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurpleDeep,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("+ 고정 시간 추가", fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    editing?.let { schedule ->
        AutoButtonEditSheet(
            initial = schedule,
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
            title = { Text("고정 시간 삭제", fontWeight = FontWeight.ExtraBold) },
            text = { Text("이 고정 시간 설정을 삭제할까요? 되돌릴 수 없습니다.") },
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
                    Text("삭제", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDeleteId = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                ) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun AutoButtonScheduleRow(
    schedule: AutoButtonSchedule,
    onEdit: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSkipToday: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FlowDivider, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        schedule.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = FlowInk
                    )
                    if (schedule.isSkippedToday) {
                        Text(
                            text = "오늘 꺼짐",
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
                    "${displayCategory(schedule.category)} · ${formatMinuteOfDay(schedule.startMinuteOfDay)}-${formatMinuteOfDay(schedule.endMinuteOfDay)} · ${formatRepeatDays(schedule.repeatDays)}",
                    fontSize = 12.sp,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "수정",
                    tint = FlowMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint = FlowMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = schedule.isEnabled,
                onCheckedChange = onToggleEnabled
            )
            Text(
                "활성화",
                fontSize = 12.sp,
                color = FlowMuted,
                modifier = Modifier.padding(start = 6.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onSkipToday,
                colors = ButtonDefaults.textButtonColors(contentColor = FlowInk),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    if (schedule.isSkippedToday) "오늘 다시 켜기" else "오늘만 끄기",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoButtonEditSheet(
    initial: AutoButtonSchedule,
    onDismiss: () -> Unit,
    onSave: (AutoButtonSchedule) -> Unit
) {
    var title by remember(initial.scheduleId) { mutableStateOf(initial.title) }
    val fixedCategories = remember { listOf("COMPANY", "SCHOOL") }
    var category by remember(initial.scheduleId) {
        mutableStateOf(initial.category.takeIf { it in fixedCategories } ?: "SCHOOL")
    }
    var startMinute by remember(initial.scheduleId) { mutableStateOf(initial.startMinuteOfDay.coerceIn(0, 23 * 60 + 59)) }
    var endMinute by remember(initial.scheduleId) { mutableStateOf(initial.endMinuteOfDay.coerceIn(0, 23 * 60 + 59)) }
    var repeatDays by remember(initial.scheduleId) { mutableStateOf(initial.repeatDays.ifEmpty { weekdayDefaults() }) }
    var notifyOnStart by remember(initial.scheduleId) { mutableStateOf(initial.notifyOnStart) }
    var notifyOnEnd by remember(initial.scheduleId) { mutableStateOf(initial.notifyOnEnd) }
    var isEnabled by remember(initial.scheduleId) { mutableStateOf(initial.isEnabled) }
    var errorMessage by remember(initial.scheduleId) { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    if (initial.scheduleId.isBlank()) "고정 시간 추가" else "고정 시간 수정",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Text(
                    "반복되는 회사/학교 시간을 입력하세요.",
                    fontSize = 12.sp,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            FormPanel {
                Text("이름", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("예: 회사, 학교", color = FlowMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
            FormPanel {
                Text("카테고리", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    fixedCategories.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = {
                                category = item
                                if (title.isBlank()) title = displayCategory(item)
                            },
                            label = {
                                Text(
                                    displayCategory(item),
                                    fontWeight = FontWeight.Bold,
                                    color = if (category == item) Color.White else FlowInk
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlowPurpleDeep,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF7F7FA),
                                labelColor = FlowInk
                            )
                        )
                    }
                }
            }
            FormPanel {
                Text("시간", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    TimePickerCard(
                        label = "시작",
                        minuteOfDay = startMinute,
                        onChange = { startMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerCard(
                        label = "종료",
                        minuteOfDay = endMinute,
                        onChange = { endMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            FormPanel {
                Text("반복 요일", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(dayOptions.size) { index ->
                        val (day, label) = dayOptions[index]
                        FilterChip(
                            selected = day in repeatDays,
                            onClick = {
                                repeatDays = if (day in repeatDays) repeatDays - day else repeatDays + day
                            },
                            label = {
                                Text(
                                    label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day in repeatDays) Color.White else FlowInk
                                )
                            },
                            colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FlowPurpleDeep,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF7F7FA),
                                labelColor = FlowInk
                            )
                        )
                    }
                }
            }
            FormPanel {
                ToggleRow("활성화", isEnabled) { isEnabled = it }
                ToggleRow("시작 알림", notifyOnStart) { notifyOnStart = it }
                ToggleRow("종료 알림", notifyOnEnd) { notifyOnEnd = it }
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = FlowMuted)
                ) {
                    Text("취소", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val cleanTitle = title.trim()
                        errorMessage = when {
                            cleanTitle.isBlank() -> "이름을 입력해주세요."
                            endMinute <= startMinute -> "종료 시간은 시작 시간보다 늦어야 합니다."
                            repeatDays.isEmpty() -> "반복 요일을 하나 이상 선택해주세요."
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
                        containerColor = FlowPurpleDeep,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("저장", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun TimePickerCard(
    label: String,
    minuteOfDay: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .background(Color(0xFFF9F9FC), RoundedCornerShape(12.dp))
            .border(1.dp, FlowDivider, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute -> onChange(hourOfDay * 60 + minute) },
                        minuteOfDay / 60,
                        minuteOfDay % 60,
                        true
                    ).show()
                }
            )
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FlowMuted)
        Text(
            text = formatMinuteOfDay(minuteOfDay),
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowInk,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
        )
        Text(
            "탭하여 변경",
            fontSize = 10.sp,
            color = FlowMuted.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun FormPanel(content: @Composable ColumnScope.() -> Unit) {
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
private fun ToggleRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text, fontSize = 13.sp, color = FlowInk)
    }
}

private val dayOptions = listOf(
    Calendar.SUNDAY to "일",
    Calendar.MONDAY to "월",
    Calendar.TUESDAY to "화",
    Calendar.WEDNESDAY to "수",
    Calendar.THURSDAY to "목",
    Calendar.FRIDAY to "금",
    Calendar.SATURDAY to "토"
)

private fun weekdayDefaults(): Set<Int> = setOf(
    Calendar.MONDAY,
    Calendar.TUESDAY,
    Calendar.WEDNESDAY,
    Calendar.THURSDAY,
    Calendar.FRIDAY
)

private fun defaultAutoButtonSchedule(): AutoButtonSchedule {
    return AutoButtonSchedule(
        title = "학교",
        category = "SCHOOL",
        repeatDays = weekdayDefaults(),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 17 * 60
    )
}

private fun formatRepeatDays(days: Set<Int>): String {
    if (days == weekdayDefaults()) return "평일"
    return dayOptions.filter { it.first in days }.joinToString(" ") { it.second }
}

private fun formatMinuteOfDay(minuteOfDay: Int): String {
    val minute = minuteOfDay.coerceIn(0, 24 * 60 - 1)
    return "%02d:%02d".format(minute / 60, minute % 60)
}

private fun parseMinuteOfDay(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%02d:%02d", minutes, seconds)
    }
}
