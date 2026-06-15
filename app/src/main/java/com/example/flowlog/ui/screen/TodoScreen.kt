package com.example.flowlog.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.flowlog.util.CalendarIntentHelper
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.PickerWaveBackground
import com.example.flowlog.ui.component.WheelPickerColumn
import com.example.flowlog.ui.component.CategoryPicker
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.DailyCueItem
import com.example.flowlog.ui.viewmodel.TodoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── 컬러 팔레트 ───────────────────────────────────────────────────────────────
private val BgPage       = Color(0xFFFAFAFC)
private val TextPrimary  = Color(0xFF11182F)
private val TextMuted    = Color(0xFF7D8190)
private val Purple       = Color(0xFF6757E7)
private val PurpleSoft   = Color(0xFFF0EEFF)
private val BorderLight  = Color(0xFFD8D3E2)
private val GreenSoft    = Color(0xFFE8F5E9)
private val GreenTint    = Color(0xFF4CAF50)
private val CardShape    = RoundedCornerShape(18.dp)
private val ChipShape    = RoundedCornerShape(20.dp)
private val DefaultDailyCueTimerCategories = listOf(
    "SLEEP", "REST",
    "WORK", "STUDY",
    "EXERCISE", "WASH",
    "MEAL", "ETC"
)

// ── 메인 화면 ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    onStartTodo: (TodoItem) -> Unit,
    onStartDailyCueRoutine: (Long, String, Long, String) -> Unit,
    onStartExamStudy: (todoId: Long, subjectTitle: String, dValue: Int) -> Unit = { _, _, _ -> },
    routineTimerCategories: List<String> = DefaultDailyCueTimerCategories,
    isDeveloperMode: Boolean = false,
    isAiOrganizerAllowed: Boolean = isDeveloperMode,
    modifier: Modifier = Modifier
) {
    val todos         by viewModel.todos.collectAsState()
    val normalTodosOrdered by viewModel.normalTodosOrdered.collectAsState()
    val focusTodos    by viewModel.todayFocusItems.collectAsState()
    val dailyCues     by viewModel.dailyCues.collectAsState()
    val organizedPetites by viewModel.organizedPetites.collectAsState()
    val isTodayOrganizerRunning by viewModel.isTodayOrganizerRunning.collectAsState()
    val focusIds      = remember(focusTodos) { focusTodos.map { it.id }.toSet() }
    val todayStart = startOfDay(System.currentTimeMillis())
    // TODAY(오늘 할 일)만 Petites 섹션에 표시. NORMAL(미분류)은 전체 할 일로.
    val normalTodos = normalTodosOrdered
    val activeTodos   = remember(todos, todayStart) {
        todos.filter { todo ->
            !todo.isCompleted &&
            todo.category != TodoCategory.TODAY &&
            !(todo.category == TodoCategory.UNIVERSITY_EXAM &&
              todo.selectedDate != null &&
              startOfDay(todo.selectedDate) < todayStart)
        }
    }
    val tomorrowStart = todayStart + DAY_MILLIS
    val completedRegular = remember(todos, focusIds, todayStart) {
        todos.filter {
            val completedAt = it.completedAt
            it.isCompleted &&
                it.id !in focusIds &&
                it.category != TodoCategory.TODAY &&
                completedAt != null &&
                completedAt in todayStart until tomorrowStart
        }.maxByOrNull { it.completedAt ?: 0L }
    }

    // 입력 카드 상태
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var newTitle          by remember { mutableStateOf("") }
    var isInputExpanded   by remember { mutableStateOf(false) }
    var inputCategory     by remember { mutableStateOf<TodoCategory?>(null) }
    var inputDate         by remember { mutableStateOf<Long?>(null) }
    var showInputDatePick by remember { mutableStateOf(false) }
    val titleSrc = remember { MutableInteractionSource() }
    val isTitleFocused by titleSrc.collectIsFocusedAsState()
    LaunchedEffect(isTitleFocused) { if (isTitleFocused) isInputExpanded = true }
    LaunchedEffect(isAiOrganizerAllowed) {
        viewModel.setTodayOrganizerAllowed(isAiOrganizerAllowed)
    }

    // 카드 인터랙션 상태
    var editingId          by remember { mutableStateOf<Long?>(null) }
    var completingId       by remember { mutableStateOf<Long?>(null) }
    var isActiveExpanded   by remember { mutableStateOf(false) }
    var isAnchorsExpanded  by remember { mutableStateOf(false) }
    val visibleNormalTodos = remember(normalTodos, isAnchorsExpanded) { if (isAnchorsExpanded) normalTodos else normalTodos.take(4) }
    val adminOrganizedPetites = remember(organizedPetites, isAiOrganizerAllowed) {
        if (isAiOrganizerAllowed) organizedPetites else emptyList()
    }
    val visibleOrganizedPetites = remember(adminOrganizedPetites, isAnchorsExpanded) {
        if (isAnchorsExpanded) adminOrganizedPetites else adminOrganizedPetites.take(4)
    }
    // CALENDAR-only organized petites must not hide the normal todos (TODAY-category) section.
    // Normal todos are treated as "로컬 Petites" when the AI organizer has never been run.
    val hasNonCalendarOrganizedPetites = remember(adminOrganizedPetites) {
        adminOrganizedPetites.any { it.sourceType != PetiteSourceType.CALENDAR }
    }
    val scope = rememberCoroutineScope()

    val normalSnackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.organizedPetiteUndoEvents.collect { event ->
            val result = normalSnackbarHostState.showSnackbar(
                message = "${event.item.title} 완료했어요",
                actionLabel = "되돌리기",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoOrganizedPetiteCompletion(event)
            }
        }
    }


    val visibleActive = if (isActiveExpanded) activeTodos else activeTodos.take(2)
    val hiddenCount   = (activeTodos.size - 2).coerceAtLeast(0)

    // 오늘의 목표 완료: 항목이 섹션에 그대로 남음 (다음 날 자동 삭제)
    val onCompleteFocus: (TodoItem) -> Unit = { todo ->
        scope.launch {
            completingId = todo.id
            delay(380)
            viewModel.completeFocusTodo(todo)
            completingId = null
        }
    }

    // 오늘 할 일 완료: 완료 후 되돌리기 스낵바 표시
    val onCompleteNormal: (TodoItem) -> Unit = { todo ->
        scope.launch {
            completingId = todo.id
            delay(380)
            viewModel.completeTodo(todo)
            completingId = null
            if (editingId == todo.id) editingId = null
            val result = normalSnackbarHostState.showSnackbar(
                message = "${todo.title} 완료됨",
                actionLabel = "되돌리기",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.uncompleteTodo(todo)
            }
        }
    }

    // 전체 할 일 완료: 이전 완료 항목 삭제 후 현재 항목만 하단에 표시
    val onCompleteRegular: (TodoItem) -> Unit = { todo ->
        scope.launch {
            completingId = todo.id
            delay(380)
            viewModel.completeTodo(todo)
            completingId = null
            if (editingId == todo.id) editingId = null
        }
    }

    // 입력 날짜 선택 다이얼로그
    if (showInputDatePick) {
        val state = rememberDatePickerState(initialSelectedDateMillis = inputDate)
        DatePickerDialog(
            onDismissRequest = { showInputDatePick = false },
            confirmButton = {
                TextButton(onClick = { inputDate = state.selectedDateMillis; showInputDatePick = false }) {
                    Text("확인", color = Purple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDatePick = false }) { Text("취소", color = TextMuted) }
            }
        ) { DatePicker(state = state) }
    }

    Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgPage),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // ── 제목 ──────────────────────────────────────────────────────────────
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Todo",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isAiOrganizerAllowed) {
                    OutlinedButton(
                        onClick = { viewModel.runTodayOrganizer() },
                        enabled = !isTodayOrganizerRunning,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, Purple.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(if (isTodayOrganizerRunning) "AI..." else "AI", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetTodayOrganizer() },
                        enabled = !isTodayOrganizerRunning && adminOrganizedPetites.isNotEmpty(),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.25f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("초기화", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (isDeveloperMode) {
                    IconButton(onClick = { viewModel.refreshSort() }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "정렬 새로고침",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        // ── 입력 카드 ─────────────────────────────────────────────────────────
        item(key = "input") {
            NewTodoCard(
                title        = newTitle,
                onTitleChange = { newTitle = it },
                isExpanded   = isInputExpanded,
                category     = inputCategory,
                onCategoryChange = { inputCategory = it },
                date         = inputDate,
                onDateClick  = { showInputDatePick = true },
                interactionSource = titleSrc,
                onAdd = {
                    viewModel.addTodo(newTitle, inputCategory ?: TodoCategory.NORMAL, inputDate)
                    newTitle = ""; isInputExpanded = false; inputCategory = null; inputDate = null
                    focusManager.clearFocus()
                },
                onAddToCalendar = {
                    val titleToSave = newTitle.trim()
                    val dateToSave = inputDate
                    viewModel.addTodo(titleToSave, inputCategory ?: TodoCategory.NORMAL, dateToSave)
                    CalendarIntentHelper.openInsertEvent(context, titleToSave, dateToSave)
                    newTitle = ""; isInputExpanded = false; inputCategory = null; inputDate = null
                    focusManager.clearFocus()
                }
            )
        }

        // ── 오늘의 목표 ───────────────────────────────────────────────────────
        if (normalTodos.isNotEmpty() || adminOrganizedPetites.isNotEmpty()) {
            item(key = "focus_header") {
                // When both sections show (CALENDAR-only + normal todos), count both for "더보기" button.
                val petiteCount = when {
                    hasNonCalendarOrganizedPetites -> adminOrganizedPetites.size
                    adminOrganizedPetites.isNotEmpty() -> adminOrganizedPetites.size + normalTodos.size
                    else -> normalTodos.size
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("Petites", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Purple)
                        Spacer(Modifier.width(5.dp))
                        Text("✦", fontSize = 14.sp, color = Purple.copy(alpha = 0.7f))
                    }
                    TextButton(
                        onClick = { isAnchorsExpanded = !isAnchorsExpanded },
                        enabled = petiteCount > 4,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            if (isAnchorsExpanded) "접기" else "더보기",
                            fontSize = 13.sp,
                            color = if (petiteCount > 4) Purple else TextMuted.copy(alpha = 0.45f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (adminOrganizedPetites.isNotEmpty()) {
                item(key = "petites_drag_block") {
                    DragDropSingleColumn(
                        items = visibleOrganizedPetites,
                        spacing = 6.dp,
                        onReorder = { fromVis, toVis ->
                            val fromId = visibleOrganizedPetites.getOrNull(fromVis)?.id ?: return@DragDropSingleColumn
                            val toId   = visibleOrganizedPetites.getOrNull(toVis)?.id  ?: return@DragDropSingleColumn
                            val from = organizedPetites.indexOfFirst { it.id == fromId }
                            val to   = organizedPetites.indexOfFirst { it.id == toId }
                            if (from >= 0 && to >= 0) viewModel.reorderOrganizedPetites(from, to)
                        }
                    ) { item, _, dragMod ->
                        OrganizedPetiteCard(
                            item = item,
                            modifier = dragMod,
                            onStart = {
                                when (item.sourceType) {
                                    PetiteSourceType.PETITE,
                                    PetiteSourceType.TODO -> item.sourceId
                                        ?.toLongOrNull()
                                        ?.let { id -> todos.firstOrNull { it.id == id } }
                                        ?.let(onStartTodo)
                                    PetiteSourceType.ROUTINE -> item.sourceId
                                        ?.toLongOrNull()
                                        ?.let { cueId ->
                                            onStartDailyCueRoutine(
                                                cueId,
                                                item.title,
                                                item.routineTimerDurationMillis ?: 0L,
                                                item.routineTimerCategory ?: "TODO"
                                            )
                                        }
                                    PetiteSourceType.EXAM -> item.sourceId
                                        ?.toLongOrNull()
                                        ?.let { examId ->
                                            onStartExamStudy(
                                                examId,
                                                item.linkedActivityName ?: item.title,
                                                item.examDValue ?: 0
                                            )
                                        }
                                    PetiteSourceType.CALENDAR -> {}
                                }
                            },
                            onComplete = { viewModel.completeOrganizedPetite(item) }
                        )
                    }
                }
            }
            // Show normal todos when there are no NON-CALENDAR organized petites.
            // A CALENDAR-only organized list must not hide the TODAY-category todos.
            if (!hasNonCalendarOrganizedPetites) {
                item(key = "normal_todo_drag_block") {
                    DragDropTodoColumn(
                        items = visibleNormalTodos,
                        onReorder = { from, to -> viewModel.reorderNormalTodos(from, to) }
                    ) { todo, _, dragMod ->
                        TodoCard(
                            todo         = todo,
                            modifier     = dragMod,
                            isEditing    = editingId == todo.id,
                            isCompleting = completingId == todo.id,
                            isFocus      = false,
                            onStartTodo  = { onStartTodo(todo) },
                            onComplete   = { onCompleteNormal(todo) },
                            onUncomplete = { viewModel.uncompleteTodo(todo) },
                            onEditToggle = { editingId = if (editingId == todo.id) null else todo.id },
                            onSave       = { t, c, d -> viewModel.updateTodo(todo.copy(title = t, category = c, selectedDate = d)); editingId = null },
                            onDelete     = { viewModel.deleteTodo(todo); editingId = null }
                        )
                    }
                }
            }
        }

        item(key = "daily_cues") {
            DailyCuesSection(
                cues = dailyCues,
                onToggleCue = viewModel::toggleDailyCue,
                onAddCue = viewModel::addDailyCue,
                onUpdateCue = viewModel::updateDailyCue,
                onDeleteCue = viewModel::deleteDailyCue,
                onStartRoutine = onStartDailyCueRoutine,
                onReorderCue = viewModel::reorderDailyCues,
                timerCategories = routineTimerCategories
            )
        }

        // ── 전체 할 일 ────────────────────────────────────────────────────────
        item(key = "active_header") {
            SectionHeader(title = "전체 할 일")
        }

        if (activeTodos.isEmpty() && normalTodos.isEmpty() && completedRegular == null) {
            item(key = "empty") {
                Box(
                    Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("아직 할 일이 없습니다.", color = TextMuted, fontSize = 15.sp)
                }
            }
        }

        items(visibleActive, key = { "active_${it.id}" }) { todo ->
            TodoCard(
                todo         = todo,
                isEditing    = editingId == todo.id,
                isCompleting = completingId == todo.id,
                isFocus      = false,
                onStartTodo  = { onStartTodo(todo) },
                onComplete   = { onCompleteRegular(todo) },
                onUncomplete = { viewModel.uncompleteTodo(todo) },
                onEditToggle = { editingId = if (editingId == todo.id) null else todo.id },
                onSave       = { t, c, d -> viewModel.updateTodo(todo.copy(title = t, category = c, selectedDate = d)); editingId = null },
                onDelete     = { viewModel.deleteTodo(todo); editingId = null }
            )
        }

        if (!isActiveExpanded && hiddenCount > 0) {
            item(key = "show_more") {
                MoreButton(hiddenCount) { isActiveExpanded = true }
            }
        }

        completedRegular?.let { todo ->
            item(key = "completed_regular_${todo.id}") {
                CompletedCard(
                    todo     = todo,
                    onUndo   = { viewModel.uncompleteTodo(todo) },
                    onDelete = { viewModel.deleteTodo(todo) }
                )
            }
        }
    }

    // 오늘 할 일 완료 되돌리기 Snackbar
    SnackbarHost(
        hostState = normalSnackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 16.dp),
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = Color(0xFF1565C0),
                contentColor = Color.White,
                actionColor = Color(0xFFBBDEFB),
                shape = RoundedCornerShape(12.dp)
            )
        }
    )
    } // Box 닫기
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DragDropSingleColumn(
    items: List<OrganizedPetite>,
    onReorder: (Int, Int) -> Unit,
    spacing: Dp = 6.dp,
    modifier: Modifier = Modifier,
    content: @Composable (OrganizedPetite, Boolean, Modifier) -> Unit
) {
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var slotPx by remember { mutableStateOf(0) }
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val haptic = LocalHapticFeedback.current

    val draggingIdx = items.indexOfFirst { it.id == draggingId }
    val targetIdx = if (draggingIdx < 0 || slotPx == 0) -1
    else (draggingIdx + (dragOffsetY / (slotPx + spacingPx)).roundToInt()).coerceIn(0, items.size - 1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { idx, item ->
            key(item.id) {
                val isDragging = item.id == draggingId
                val displace by animateFloatAsState(
                    targetValue = when {
                        draggingIdx < 0 || isDragging -> 0f
                        draggingIdx < targetIdx && idx in (draggingIdx + 1)..targetIdx ->
                            -(slotPx + spacingPx)
                        draggingIdx > targetIdx && idx in targetIdx until draggingIdx ->
                            (slotPx + spacingPx)
                        else -> 0f
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "petite_dy"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { if (it.height > 0 && !isDragging) slotPx = it.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer(
                            translationY = if (isDragging) dragOffsetY else displace,
                            shadowElevation = if (isDragging) 10f else 0f,
                            alpha = if (isDragging) 0.93f else 1f,
                            scaleX = if (isDragging) 1.02f else 1f,
                            scaleY = if (isDragging) 1.02f else 1f
                        )
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingId = item.id
                                    dragOffsetY = 0f
                                },
                                onDrag = { _, d ->
                                    dragOffsetY += d.y
                                },
                                onDragEnd = {
                                    val from = items.indexOfFirst { it.id == draggingId }
                                    val to = if (from < 0 || slotPx == 0) from
                                    else (from + (dragOffsetY / (slotPx + spacingPx)).roundToInt())
                                        .coerceIn(0, items.size - 1)
                                    draggingId = null; dragOffsetY = 0f
                                    if (to >= 0 && to != from) onReorder(from, to)
                                },
                                onDragCancel = {
                                    draggingId = null; dragOffsetY = 0f
                                }
                            )
                        }
                ) {
                    content(item, isDragging, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DragDropTodoColumn(
    items: List<TodoItem>,
    onReorder: (Int, Int) -> Unit,
    spacing: Dp = 6.dp,
    modifier: Modifier = Modifier,
    content: @Composable (TodoItem, Boolean, Modifier) -> Unit
) {
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var slotPx by remember { mutableStateOf(0) }
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val haptic = LocalHapticFeedback.current

    val draggingIdx = items.indexOfFirst { it.id == draggingId }
    val targetIdx = if (draggingIdx < 0 || slotPx == 0) -1
    else (draggingIdx + (dragOffsetY / (slotPx + spacingPx)).roundToInt()).coerceIn(0, items.size - 1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        items.forEachIndexed { idx, item ->
            key(item.id) {
                val isDragging = item.id == draggingId
                val displace by animateFloatAsState(
                    targetValue = when {
                        draggingIdx < 0 || isDragging -> 0f
                        draggingIdx < targetIdx && idx in (draggingIdx + 1)..targetIdx -> -(slotPx + spacingPx)
                        draggingIdx > targetIdx && idx in targetIdx until draggingIdx -> (slotPx + spacingPx)
                        else -> 0f
                    },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "todo_dy"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { if (it.height > 0 && !isDragging) slotPx = it.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer(
                            translationY = if (isDragging) dragOffsetY else displace,
                            shadowElevation = if (isDragging) 10f else 0f,
                            alpha = if (isDragging) 0.93f else 1f,
                            scaleX = if (isDragging) 1.02f else 1f,
                            scaleY = if (isDragging) 1.02f else 1f
                        )
                        .pointerInput(item.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingId = item.id
                                    dragOffsetY = 0f
                                },
                                onDrag = { _, d -> dragOffsetY += d.y },
                                onDragEnd = {
                                    val from = items.indexOfFirst { it.id == draggingId }
                                    val to = if (from < 0 || slotPx == 0) from
                                    else (from + (dragOffsetY / (slotPx + spacingPx)).roundToInt())
                                        .coerceIn(0, items.size - 1)
                                    draggingId = null; dragOffsetY = 0f
                                    if (to >= 0 && to != from) onReorder(from, to)
                                },
                                onDragCancel = { draggingId = null; dragOffsetY = 0f }
                            )
                        }
                ) {
                    content(item, isDragging, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DragDropTwoColumnGrid(
    items: List<DailyCueItem>,
    onReorder: (Int, Int) -> Unit,
    rowSpacing: Dp = 8.dp,
    colSpacing: Dp = 8.dp,
    modifier: Modifier = Modifier,
    content: @Composable (DailyCueItem, Boolean, Modifier) -> Unit
) {
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var cellW by remember { mutableStateOf(0) }
    var cellH by remember { mutableStateOf(0) }
    val rsP = with(LocalDensity.current) { rowSpacing.toPx() }
    val csP = with(LocalDensity.current) { colSpacing.toPx() }
    val haptic = LocalHapticFeedback.current

    val draggingIdx = items.indexOfFirst { it.id == draggingId }
    val targetIdx = if (draggingIdx < 0 || cellW == 0 || cellH == 0) -1 else {
        val origRow = draggingIdx / 2; val origCol = draggingIdx % 2
        val dRow = (dragY / (cellH + rsP)).roundToInt()
        val dCol = (dragX / (cellW + csP)).roundToInt()
        val newRow = (origRow + dRow).coerceIn(0, (items.size - 1) / 2)
        val maxCol = if (newRow * 2 + 1 < items.size) 1 else 0
        val newCol = (origCol + dCol).coerceIn(0, maxCol)
        (newRow * 2 + newCol).coerceIn(0, items.size - 1)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(rowSpacing)) {
        items.chunked(2).forEachIndexed { rowIdx, rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(colSpacing)) {
                rowItems.forEachIndexed { colIdx, item ->
                    val flatIdx = rowIdx * 2 + colIdx
                    key(item.id) {
                        val isDragging = item.id == draggingId
                        val newFlat = when {
                            draggingIdx < 0 || isDragging -> flatIdx
                            draggingIdx < targetIdx && flatIdx in (draggingIdx + 1)..targetIdx -> flatIdx - 1
                            draggingIdx > targetIdx && flatIdx in targetIdx until draggingIdx -> flatIdx + 1
                            else -> flatIdx
                        }
                        val dX by animateFloatAsState(
                            targetValue = if (draggingIdx < 0 || isDragging) 0f
                            else (newFlat % 2 - flatIdx % 2) * (cellW + csP),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "cue_dx"
                        )
                        val dY by animateFloatAsState(
                            targetValue = if (draggingIdx < 0 || isDragging) 0f
                            else (newFlat / 2 - flatIdx / 2) * (cellH + rsP),
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "cue_dy"
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .onSizeChanged {
                                    if (!isDragging) {
                                        if (it.width > 0) cellW = it.width
                                        if (it.height > 0) cellH = it.height
                                    }
                                }
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer(
                                    translationX = if (isDragging) dragX else dX,
                                    translationY = if (isDragging) dragY else dY,
                                    shadowElevation = if (isDragging) 10f else 0f,
                                    alpha = if (isDragging) 0.93f else 1f
                                )
                                .pointerInput(item.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            draggingId = item.id; dragX = 0f; dragY = 0f
                                        },
                                        onDrag = { _, d -> dragX += d.x; dragY += d.y },
                                        onDragEnd = {
                                            val from = items.indexOfFirst { it.id == draggingId }
                                            val to = if (from < 0 || cellW == 0 || cellH == 0) from else {
                                                val oR = from / 2; val oC = from % 2
                                                val dr = (dragY / (cellH + rsP)).roundToInt()
                                                val dc = (dragX / (cellW + csP)).roundToInt()
                                                val nR = (oR + dr).coerceIn(0, (items.size - 1) / 2)
                                                val mC = if (nR * 2 + 1 < items.size) 1 else 0
                                                val nC = (oC + dc).coerceIn(0, mC)
                                                (nR * 2 + nC).coerceIn(0, items.size - 1)
                                            }
                                            draggingId = null; dragX = 0f; dragY = 0f
                                            if (to >= 0 && to != from) onReorder(from, to)
                                        },
                                        onDragCancel = { draggingId = null; dragX = 0f; dragY = 0f }
                                    )
                                }
                        ) {
                            content(item, isDragging, Modifier.fillMaxWidth())
                        }
                    }
                }
                if (rowItems.size < 2) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun OrganizedPetiteCard(
    item: OrganizedPetite,
    modifier: Modifier = Modifier,
    onStart: () -> Unit,
    onComplete: () -> Unit
) {
    var showDetail by remember(item.id) { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = organizedPetiteAccent(item.sourceType)
    val label = organizedPetiteLabel(item.sourceType)
    val metaItems = organizedPetiteMetaItems(item)

    Card(
        modifier = modifier
            .height(78.dp)
            .clickable { showDetail = true },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("·", fontSize = 12.sp, color = TextMuted)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        item.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (metaItems.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        metaItems.joinToString(" · "),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GreenSoft)
                    .clickable(onClick = onComplete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CheckCircle, "check", tint = GreenTint, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.10f))
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, "start", tint = accent, modifier = Modifier.size(18.dp))
            }
        }
    }
    if (showDetail) {
        ModalBottomSheet(
            onDismissRequest = { showDetail = false },
            sheetState = sheetState,
            containerColor = Color.White,
            contentColor = TextPrimary
        ) {
            OrganizedPetiteDetailSheet(
                item = item,
                label = label,
                accent = accent,
                metaItems = metaItems,
                onClose = { showDetail = false },
                onComplete = {
                    onComplete()
                    showDetail = false
                },
                onStart = {
                    onStart()
                    showDetail = false
                }
            )
        }
    }
}

@Composable
private fun OrganizedPetiteDetailSheet(
    item: OrganizedPetite,
    label: String,
    accent: Color,
    metaItems: List<String>,
    onClose: () -> Unit,
    onComplete: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            modifier = Modifier
                .background(accent.copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
        Text(
            item.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            lineHeight = 25.sp
        )
        if (metaItems.isNotEmpty()) {
            Text(metaItems.joinToString(" · "), fontSize = 13.sp, color = TextMuted)
        }
        item.estimatedMinutes?.let { minutes ->
            DetailInfoRow(label = "Estimated", value = "${minutes}분")
        }
        item.linkedActivityName?.let { name ->
            DetailInfoRow(label = "Source", value = name)
        }
        item.aiComment?.let { comment ->
            DetailSection(title = "AI Comment", body = comment)
        }
        if (item.steps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Steps", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                item.steps.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. $step",
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        color = TextMuted
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
            ) {
                Text("닫기", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onComplete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenTint, contentColor = Color.White)
            ) {
                Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("체크", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onStart,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("시작", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Text(value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailSection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(body, fontSize = 14.sp, lineHeight = 20.sp, color = TextMuted)
    }
}

private fun organizedPetiteAccent(sourceType: PetiteSourceType): Color = when (sourceType) {
    PetiteSourceType.EXAM -> Color(0xFF1565C0)
    PetiteSourceType.ROUTINE -> Purple
    PetiteSourceType.TODO -> Color(0xFFE35B5B)
    PetiteSourceType.PETITE -> Purple
    PetiteSourceType.CALENDAR -> Color(0xFF00897B)
}

private fun organizedPetiteLabel(sourceType: PetiteSourceType): String = when (sourceType) {
    PetiteSourceType.EXAM -> "Exam"
    PetiteSourceType.ROUTINE -> "Routine"
    PetiteSourceType.TODO -> "Todo"
    PetiteSourceType.PETITE -> "Petite"
    PetiteSourceType.CALENDAR -> "Calendar"
}

private fun organizedPetiteMetaItems(item: OrganizedPetite): List<String> = buildList {
    item.dateMillis?.let { add(if (item.sourceType == PetiteSourceType.EXAM) examDdayLabel(item.examDValue) else fmtDate(it)) }
    item.activityCategory?.let { add(it) }
    item.routineTimerCategory?.let { add(it) }
    item.category
        ?.takeIf { it != TodoCategory.TODAY && it != TodoCategory.NORMAL && item.activityCategory == null }
        ?.let { add(it.name) }
}

private fun examDdayLabel(dValue: Int?): String = when (dValue) {
    0 -> "D-Day"
    null -> "Exam"
    else -> "D-$dValue"
}
@Composable
private fun DailyCuesSection(
    cues: List<DailyCueItem>,
    onToggleCue: (Long) -> Unit,
    onAddCue: (String, String, Long?, String) -> Unit,
    onUpdateCue: (Long, String, String, Long?, String) -> Unit,
    onDeleteCue: (Long) -> Unit,
    onStartRoutine: (Long, String, Long, String) -> Unit,
    onReorderCue: (Int, Int) -> Unit,
    timerCategories: List<String>
) {
    var isShowingAll by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCue by remember { mutableStateOf<DailyCueItem?>(null) }
    val visibleCues = remember(cues, isShowingAll) {
        if (isShowingAll) cues else cues.take(4)
    }

    if (showAddDialog) {
        DailyCueEditorDialog(
            cue = null,
            timerCategories = timerCategories,
            onDismiss = { showAddDialog = false },
            onDelete = null,
            onSave = { title, label, timerDurationMillis, timerCategory ->
                onAddCue(title, label, timerDurationMillis, timerCategory)
                showAddDialog = false
                isShowingAll = true
            }
        )
    }
    editingCue?.let { cue ->
        DailyCueEditorDialog(
            cue = cue,
            timerCategories = timerCategories,
            onDismiss = { editingCue = null },
            onDelete = {
                onDeleteCue(cue.id)
                editingCue = null
            },
            onSave = { title, label, timerDurationMillis, timerCategory ->
                onUpdateCue(cue.id, title, label, timerDurationMillis, timerCategory)
                editingCue = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Daily Cues",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Purple
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("✧", fontSize = 14.sp, color = Purple.copy(alpha = 0.7f))
                }
            }
            TextButton(
                onClick = { isShowingAll = !isShowingAll },
                enabled = cues.size > 4,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    if (isShowingAll) "접기" else "더보기",
                    fontSize = 13.sp,
                    color = if (cues.size > 4) Purple else TextMuted.copy(alpha = 0.45f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = { showAddDialog = true },
                shape = ChipShape,
                border = BorderStroke(1.dp, BorderLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("추가", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        DragDropTwoColumnGrid(
            items = visibleCues,
            onReorder = { fromVis, toVis ->
                val fromId = visibleCues.getOrNull(fromVis)?.id ?: return@DragDropTwoColumnGrid
                val toId   = visibleCues.getOrNull(toVis)?.id  ?: return@DragDropTwoColumnGrid
                val from = cues.indexOfFirst { it.id == fromId }
                val to   = cues.indexOfFirst { it.id == toId }
                if (from >= 0 && to >= 0) onReorderCue(from, to)
            },
            rowSpacing = 8.dp,
            colSpacing = 8.dp
        ) { cue, _, cellModifier ->
            DailyCueCard(
                cue = cue,
                onToggle = { onToggleCue(cue.id) },
                onEdit = { editingCue = cue },
                onStartRoutine = onStartRoutine,
                modifier = cellModifier
            )
        }
    }
}

@Composable
private fun DailyCueEditorDialog(
    cue: DailyCueItem?,
    timerCategories: List<String>,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (String, String, Long?, String) -> Unit
) {
    var title by remember(cue?.id) { mutableStateOf(cue?.title ?: "") }
    var label by remember(cue?.id) { mutableStateOf(cue?.label ?: "Routine") }
    var timerDurationMillis by remember(cue?.id) { mutableStateOf(cue?.timerDurationMillis) }
    val categoryOptions = remember(timerCategories) {
        timerCategories.distinct().filter { it.isNotBlank() }.ifEmpty { DefaultDailyCueTimerCategories }
    }
    var timerCategory by remember(cue?.id, categoryOptions) {
        mutableStateOf((cue?.timerCategory ?: categoryOptions.first()).takeIf { it in categoryOptions } ?: categoryOptions.first())
    }
    var showDurationPicker by remember { mutableStateOf(false) }
    val isEditing = cue != null
    val isRoutine = label == "Routine"

    if (showDurationPicker) {
        RoutineDurationPickerSheet(
            initialDurationMillis = timerDurationMillis,
            onDismiss = { showDurationPicker = false },
            onConfirm = { durationMillis ->
                timerDurationMillis = durationMillis.takeIf { it > 0L }
                showDurationPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (isEditing) "Daily Cue 수정" else "Daily Cue 추가",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isEditing) "작은 신호의 이름과 종류를 바꿀 수 있어요." else "오늘 붙잡아둘 작은 신호를 적어주세요.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("예: 물 마시기", color = TextMuted) },
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = BorderLight,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedPlaceholderColor = TextMuted,
                        unfocusedPlaceholderColor = TextMuted,
                        cursorColor = Purple
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DailyCueTypeButton(
                        label = "Routine",
                        helper = "매일 유지",
                        isSelected = label == "Routine",
                        modifier = Modifier.weight(1f)
                    ) { label = "Routine" }
                    DailyCueTypeButton(
                        label = "Memo",
                        helper = "오늘만",
                        isSelected = label == "Memo",
                        modifier = Modifier.weight(1f)
                    ) {
                        label = "Memo"
                        timerDurationMillis = null
                    }
                }
                if (isRoutine) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Timer",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "활동 카테고리",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CategoryPicker(
                            categories = categoryOptions,
                            selectedCategory = timerCategory,
                            onSelect = { timerCategory = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PurpleSoft.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, BorderLight), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    timerDurationMillis?.let { formatCueDuration(it) } ?: "No timer",
                                    color = if (timerDurationMillis == null) TextMuted else TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Routine 카드에서 실행할 목표 시간",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                            if (timerDurationMillis != null) {
                                TextButton(onClick = { timerDurationMillis = null }) {
                                    Text("없음", color = TextMuted, fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { showDurationPicker = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Purple.copy(alpha = 0.35f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("설정", color = Purple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = timerDurationMillis?.takeIf { label == "Routine" && it > 0L }
                    onSave(title, label, duration, timerCategory)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple,
                    contentColor = Color.White,
                    disabledContainerColor = PurpleSoft,
                    disabledContentColor = TextMuted
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(if (isEditing) "저장" else "추가", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("삭제", color = Color(0xFFE35B5B), fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("취소", color = TextMuted)
                }
            }
        }
    )
}

@Composable
private fun DailyCueTypeButton(
    label: String,
    helper: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (isSelected) PurpleSoft else Color.White
    val fg = if (isSelected) Purple else TextPrimary
    val bdr = if (isSelected) Purple.copy(alpha = 0.5f) else BorderLight
    Column(
        modifier = modifier
            .height(66.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, bdr), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, color = fg, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(helper, color = TextMuted, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DailyCueCard(
    cue: DailyCueItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onStartRoutine: (Long, String, Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMemo = cue.label == "Memo"
    val labelColor = if (isMemo) Color(0xFFE85C7A) else Purple
    val labelBg = if (isMemo) Color(0xFFFFEEF3) else PurpleSoft
    val borderColor = if (cue.isCompleted) GreenTint.copy(alpha = 0.35f) else PurpleSoft
    val cardBg = if (cue.isCompleted) GreenSoft.copy(alpha = 0.45f) else Color.White
    val titleFontSize = when {
        !isMemo -> 13.sp
        cue.title.length > 18 -> 10.sp
        cue.title.length > 14 -> 11.sp
        cue.title.length > 10 -> 12.sp
        else -> 13.sp
    }

    Card(
        modifier = modifier
            .height(if (isMemo) 88.dp else 82.dp)
            .combinedClickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    cue.label,
                    color = labelColor,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(labelBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    cue.title,
                    color = if (cue.isCompleted) TextMuted else TextPrimary,
                    fontSize = titleFontSize,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 16.sp,
                    maxLines = if (isMemo) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!isMemo) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        displayCategory(cue.timerCategory),
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isMemo) {
                IconButton(
                    onClick = {
                        onStartRoutine(cue.id, cue.title, cue.timerDurationMillis ?: 0L, cue.timerCategory)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Purple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(if (cue.isCompleted) GreenTint else Color.Transparent, CircleShape)
                    .border(
                        BorderStroke(2.dp, if (cue.isCompleted) GreenTint else BorderLight),
                        CircleShape
                    )
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (cue.isCompleted) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

// ── 새 할 일 입력 카드 ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineDurationPickerSheet(
    initialDurationMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialMinutes = TimeUnit.MILLISECONDS.toMinutes(initialDurationMillis ?: 0L)
        .coerceIn(0L, 23L * 60L + 59L)
        .toInt()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hour by remember(initialDurationMillis) { mutableStateOf(initialMinutes / 60) }
    var minute by remember(initialDurationMillis) { mutableStateOf(initialMinutes % 60) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        dragHandle = null,
        containerColor = Color.White,
        contentColor = TextPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
        ) {
            PickerWaveBackground(
                color = PurpleSoft.copy(alpha = 0.54f),
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
                    "Timer duration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    "Routine 카드에서 실행할 목표 시간을 선택하세요.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    formatCueDuration(TimeUnit.HOURS.toMillis(hour.toLong()) + TimeUnit.MINUTES.toMillis(minute.toLong())),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF27324D),
                    modifier = Modifier.padding(top = 28.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WheelPickerColumn(
                        values = (0..23).toList(),
                        selectedValue = hour,
                        formatter = { "${it}h" },
                        onSelect = { hour = it },
                        selectedHighlightColor = Purple.copy(alpha = 0.44f),
                        unselectedTextColor = TextMuted.copy(alpha = 0.45f)
                    )
                    WheelPickerColumn(
                        values = (0..59).toList(),
                        selectedValue = minute,
                        formatter = { "${it}m" },
                        onSelect = { minute = it },
                        selectedHighlightColor = Purple.copy(alpha = 0.44f),
                        unselectedTextColor = TextMuted.copy(alpha = 0.45f)
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
                        colors = ButtonDefaults.textButtonColors(contentColor = TextPrimary)
                    ) {
                        Text("취소", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = {
                            onConfirm(TimeUnit.HOURS.toMillis(hour.toLong()) + TimeUnit.MINUTES.toMillis(minute.toLong()))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("확인", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private fun formatCueDuration(durationMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
        hours > 0 -> "${hours}시간"
        minutes > 0 -> "${minutes}분"
        else -> "No timer"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewTodoCard(
    title: String,
    onTitleChange: (String) -> Unit,
    isExpanded: Boolean,
    category: TodoCategory?,
    onCategoryChange: (TodoCategory?) -> Unit,
    date: Long?,
    onDateClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    onAdd: () -> Unit,
    onAddToCalendar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = CardShape
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.weight(1f).height(56.dp),
                    interactionSource = interactionSource,
                    singleLine = true,
                    placeholder = { Text("새 할 일") },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = BorderLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedPlaceholderColor = TextMuted,
                        unfocusedPlaceholderColor = TextMuted,
                        cursorColor = Purple
                    )
                )
                Button(
                    onClick = onAdd,
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleSoft, contentColor = Purple,
                        disabledContainerColor = Color(0xFFF4F2FA), disabledContentColor = Color(0xFFB8B2C9)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("추가", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(tween(260)) + fadeIn(tween(200)),
                exit  = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 14.dp)
                ) {
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.45f))
                    Spacer(Modifier.height(12.dp))
                    // 1행: 타입 칩
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeChip("오늘 할 일", category == TodoCategory.TODAY) {
                            onCategoryChange(if (category == TodoCategory.TODAY) null else TodoCategory.TODAY)
                        }
                        TypeChip("복습", category == TodoCategory.REVIEW) {
                            onCategoryChange(if (category == TodoCategory.REVIEW) null else TodoCategory.REVIEW)
                        }
                        TypeChip("과제", category == TodoCategory.ASSIGNMENT) {
                            onCategoryChange(if (category == TodoCategory.ASSIGNMENT) null else TodoCategory.ASSIGNMENT)
                        }
                        TypeChip("대학 시험", category == TodoCategory.UNIVERSITY_EXAM) {
                            onCategoryChange(if (category == TodoCategory.UNIVERSITY_EXAM) null else TodoCategory.UNIVERSITY_EXAM)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // 2행: 날짜 선택 + 캘린더 추가
                    val datePlaceholder = when (category) {
                        TodoCategory.ASSIGNMENT -> "마감일 선택"
                        TodoCategory.UNIVERSITY_EXAM -> "시험일 선택"
                        else -> "날짜 선택"
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateChipButton(date, datePlaceholder, onDateClick)
                        OutlinedButton(
                            onClick = onAddToCalendar,
                            enabled = title.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (title.isNotBlank()) BorderLight else BorderLight.copy(alpha = 0.4f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextMuted,
                                disabledContentColor = TextMuted.copy(alpha = 0.4f)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("캘린더에 추가", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

// ── Todo 카드 ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TodoCard(
    todo: TodoItem,
    modifier: Modifier = Modifier,
    isEditing: Boolean,
    isCompleting: Boolean,
    isFocus: Boolean,
    onStartTodo: () -> Unit,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onEditToggle: () -> Unit,
    onSave: (title: String, category: TodoCategory, date: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var editTitle     by remember(todo.id, isEditing) { mutableStateOf(todo.title) }
    var editCategory  by remember(todo.id, isEditing) { mutableStateOf(todo.category) }
    var editDate      by remember(todo.id, isEditing) { mutableStateOf(todo.selectedDate) }
    var confirmDelete by remember(todo.id, isEditing) { mutableStateOf(false) }
    var showEditDatePick by remember { mutableStateOf(false) }

    val cardBg by animateColorAsState(
        targetValue = when {
            isCompleting   -> GreenSoft
            todo.isCompleted && isFocus -> Color(0xFFF8F7FC)
            else           -> Color.White
        },
        animationSpec = tween(250), label = "cardBg"
    )

    if (showEditDatePick) {
        val state = rememberDatePickerState(initialSelectedDateMillis = editDate)
        DatePickerDialog(
            onDismissRequest = { showEditDatePick = false },
            confirmButton = {
                TextButton(onClick = { editDate = state.selectedDateMillis; showEditDatePick = false }) {
                    Text("확인", color = Purple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDatePick = false }) { Text("취소", color = TextMuted) }
            }
        ) { DatePicker(state = state) }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = if (isFocus) CardDefaults.cardElevation(2.dp) else CardDefaults.cardElevation(0.dp),
        border = if (isFocus) null else BorderStroke(1.dp, Color(0xFFF0EFF5)),
        shape = CardShape
    ) {
        Column(Modifier.fillMaxWidth()) {
            // 완료된 오늘의 목표 항목: 간소화 레이아웃
            if (todo.isCompleted && isFocus) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = GreenTint.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        if (todo.category != TodoCategory.NORMAL && todo.category != TodoCategory.TODAY) {
                            CategoryTag(todo.category, muted = true)
                            Spacer(Modifier.height(3.dp))
                        }
                        Text(
                            text = todo.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            textDecoration = TextDecoration.LineThrough,
                            lineHeight = 18.sp
                        )
                    }
                    TextButton(
                        onClick = onUncomplete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Purple)
                    ) {
                        Text("되돌리기", fontSize = 12.sp)
                    }
                }
            } else {
                // 메인 콘텐츠 행 (미완료)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 14.dp,
                            end = 6.dp,
                            top = if (isFocus) 9.dp else 14.dp,
                            bottom = if (isFocus) 9.dp else 14.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 태그 + 제목 (탭하면 수정 패널 토글)
                    Column(
                        Modifier
                            .weight(1f)
                            .combinedClickable(onClick = onEditToggle)
                            .padding(end = 8.dp)
                    ) {
                        if (todo.category != TodoCategory.NORMAL && todo.category != TodoCategory.TODAY) {
                            CategoryTag(todo.category)
                            Spacer(Modifier.height(if (isFocus) 3.dp else 5.dp))
                        }
                        Text(
                            text = todo.title,
                            fontSize = 15.sp,
                            fontWeight = if (isFocus) FontWeight.Bold else FontWeight.SemiBold,
                            color = TextPrimary,
                            lineHeight = if (isFocus) 18.sp else 20.sp
                        )
                        if (todo.accumulatedSeconds > 0 || todo.selectedDate != null) {
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (todo.accumulatedSeconds > 0) {
                                    Text(
                                        formatDuration(todo.accumulatedSeconds * 1000L),
                                        fontSize = 12.sp, color = TextMuted
                                    )
                                }
                                if (todo.selectedDate != null) {
                                    val datePrefix = if (todo.category == TodoCategory.UNIVERSITY_EXAM) "시험일 " else "~"
                                    Text(
                                        "$datePrefix${fmtDate(todo.selectedDate)}",
                                        fontSize = 12.sp, color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // 액션 버튼 (완료 + 시작)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isFocus) 4.dp else 6.dp),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        // 완료 (연초록 원) — UNIVERSITY_EXAM은 완료 불가
                        if (todo.category != TodoCategory.UNIVERSITY_EXAM) {
                            Box(
                                modifier = Modifier
                                    .size(if (isFocus) 30.dp else 34.dp)
                                    .clip(CircleShape)
                                    .background(GreenSoft)
                                    .clickable(onClick = onComplete),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.CheckCircle, "완료", tint = GreenTint, modifier = Modifier.size(18.dp))
                            }
                        }
                        // 시작 (연보라 원, 주요 액션)
                        Box(
                            modifier = Modifier
                                .size(if (isFocus) 32.dp else 36.dp)
                                .clip(CircleShape)
                                .background(PurpleSoft)
                                .clickable(onClick = onStartTodo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PlayArrow, "시작", tint = Purple, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // 인라인 수정 패널 (완료된 항목은 숨김)
            AnimatedVisibility(
                visible = isEditing && !todo.isCompleted,
                enter = expandVertically(tween(250)) + fadeIn(tween(200)),
                exit  = shrinkVertically(tween(200)) + fadeOut(tween(150))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp).padding(bottom = 14.dp)
                ) {
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple, unfocusedBorderColor = BorderLight, cursorColor = Purple,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeChip("오늘 할 일", editCategory == TodoCategory.TODAY) {
                            editCategory = if (editCategory == TodoCategory.TODAY) TodoCategory.NORMAL else TodoCategory.TODAY
                        }
                        TypeChip("복습", editCategory == TodoCategory.REVIEW) {
                            editCategory = if (editCategory == TodoCategory.REVIEW) TodoCategory.NORMAL else TodoCategory.REVIEW
                        }
                        TypeChip("과제", editCategory == TodoCategory.ASSIGNMENT) {
                            editCategory = if (editCategory == TodoCategory.ASSIGNMENT) TodoCategory.NORMAL else TodoCategory.ASSIGNMENT
                        }
                        TypeChip("대학 시험", editCategory == TodoCategory.UNIVERSITY_EXAM) {
                            editCategory = if (editCategory == TodoCategory.UNIVERSITY_EXAM) TodoCategory.NORMAL else TodoCategory.UNIVERSITY_EXAM
                        }
                        Spacer(Modifier.weight(1f))
                        val editDatePlaceholder = when (editCategory) {
                            TodoCategory.ASSIGNMENT -> "마감일 선택"
                            TodoCategory.UNIVERSITY_EXAM -> "시험일 선택"
                            else -> "날짜 선택"
                        }
                        DateChipButton(editDate, editDatePlaceholder) { showEditDatePick = true }
                    }
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (!confirmDelete) {
                            TextButton(
                                onClick = { confirmDelete = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE35B5B))
                            ) {
                                Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("삭제", fontSize = 13.sp)
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = onEditToggle) {
                                Text("취소", fontSize = 13.sp, color = TextMuted)
                            }
                        } else {
                            Text(
                                "정말 삭제할까요?",
                                fontSize = 13.sp,
                                color = TextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE35B5B)),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("삭제", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = { confirmDelete = false },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("취소", fontSize = 13.sp, color = TextMuted)
                            }
                        }
                        Button(
                            onClick = { onSave(editTitle.trim(), editCategory, editDate) },
                            enabled = editTitle.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text("저장", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ── 완료됨 카드 ───────────────────────────────────────────────────────────────
@Composable
private fun CompletedCard(
    todo: TodoItem,
    onUndo: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F5FA)),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = CardShape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                if (todo.category != TodoCategory.NORMAL && todo.category != TodoCategory.TODAY) {
                    CategoryTag(todo.category, muted = true)
                    Spacer(Modifier.height(3.dp))
                }
                Text(
                    text = todo.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    textDecoration = TextDecoration.LineThrough
                )
                todo.completedAt?.let {
                    Spacer(Modifier.height(2.dp))
                    Text("완료 ${fmtDate(it)}", fontSize = 11.sp, color = TextMuted.copy(alpha = 0.7f))
                }
            }
            TextButton(
                onClick = onUndo,
                colors = ButtonDefaults.textButtonColors(contentColor = Purple)
            ) {
                Text("되돌리기", fontSize = 12.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, "삭제", tint = BorderLight, modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ── 섹션 헤더 ─────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(
    title: String,
    badge: String? = null,
    color: Color = TextPrimary
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 0.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        if (badge != null) {
            Spacer(Modifier.width(5.dp))
            Text(badge, fontSize = 10.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

// ── 더 보기 버튼 ──────────────────────────────────────────────────────────────
@Composable
private fun MoreButton(hiddenCount: Int, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = Purple)
        ) {
            Text("더 보기 (${hiddenCount}개)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.size(16.dp))
        }
    }
}

// ── 공용 소형 컴포넌트 ────────────────────────────────────────────────────────
@Composable
private fun CategoryTag(category: TodoCategory, muted: Boolean = false) {
    val (bg, fg, label) = when (category) {
        TodoCategory.REVIEW          -> Triple(if (muted) Color(0xFFF0EEFF) else PurpleSoft, if (muted) Purple.copy(.5f) else Purple, "복습")
        TodoCategory.ASSIGNMENT      -> Triple(if (muted) Color(0xFFFFF0F0) else Color(0xFFFFEFF0), if (muted) Color(0xFFE35B5B).copy(.5f) else Color(0xFFE35B5B), "과제")
        TodoCategory.UNIVERSITY_EXAM -> Triple(if (muted) Color(0xFFE3F2FD) else Color(0xFFE8F4FF), if (muted) Color(0xFF1565C0).copy(.5f) else Color(0xFF1565C0), "대학 시험")
        TodoCategory.NORMAL, TodoCategory.TODAY -> return
    }
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun TypeChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val bg  by animateColorAsState(if (isSelected) Purple else Color.Transparent, tween(180), label = "chipBg")
    val fg  by animateColorAsState(if (isSelected) Color.White else TextMuted, tween(180), label = "chipFg")
    val bdr by animateColorAsState(if (isSelected) Purple else BorderLight, tween(180), label = "chipBdr")
    OutlinedButton(
        onClick = onClick,
        shape = ChipShape,
        border = BorderStroke(1.dp, bdr),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = fg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DateChipButton(date: Long?, placeholder: String = "날짜 선택", onClick: () -> Unit) {
    val hasDate = date != null
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (hasDate) Purple else BorderLight),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (hasDate) Purple else TextMuted),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(date?.let { fmtDate(it) } ?: placeholder, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── 날짜 포맷 헬퍼 ────────────────────────────────────────────────────────────
private fun fmtDate(millis: Long): String {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return "${c.get(java.util.Calendar.MONTH) + 1}/${c.get(java.util.Calendar.DAY_OF_MONTH)}"
}

private fun startOfDay(millis: Long): Long {
    val c = java.util.Calendar.getInstance().apply {
        timeInMillis = millis
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
