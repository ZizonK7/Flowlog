package com.example.flowlog.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.TodoViewModel
import com.example.flowlog.ui.viewmodel.YesterdayFlowSuggestion
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

// ── 메인 화면 ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    onStartTodo: (TodoItem) -> Unit,
    onStartSuggestion: (String) -> Unit,
    isDeveloperMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val todos         by viewModel.todos.collectAsState()
    val focusTodos    by viewModel.todayFocusItems.collectAsState()
    val yesterdaySuggestion by viewModel.yesterdaySuggestion.collectAsState()
    val focusIds      = remember(focusTodos) { focusTodos.map { it.id }.toSet() }
    // 완료 여부 무관하게 오늘의 목표에 있는 항목은 전체 할 일에서 제외
    val activeTodos   = remember(todos, focusIds) { todos.filter { !it.isCompleted && it.id !in focusIds } }
    // 전체 할 일에서 완료된 항목: 최대 1개만 존재 (completeTodo가 이전 항목 삭제)
    val completedRegular = remember(todos, focusIds) {
        todos.filter { it.isCompleted && it.id !in focusIds }
            .maxByOrNull { it.completedAt ?: 0L }
    }

    // 입력 카드 상태
    val focusManager = LocalFocusManager.current
    var newTitle          by remember { mutableStateOf("") }
    var isInputExpanded   by remember { mutableStateOf(false) }
    var inputCategory     by remember { mutableStateOf<TodoCategory?>(null) }
    var inputDate         by remember { mutableStateOf<Long?>(null) }
    var showInputDatePick by remember { mutableStateOf(false) }
    val titleSrc = remember { MutableInteractionSource() }
    val isTitleFocused by titleSrc.collectIsFocusedAsState()
    LaunchedEffect(isTitleFocused) { if (isTitleFocused) isInputExpanded = true }

    // 카드 인터랙션 상태
    var editingId        by remember { mutableStateOf<Long?>(null) }
    var completingId     by remember { mutableStateOf<Long?>(null) }
    var isActiveExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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

    LazyColumn(
        modifier = modifier.fillMaxSize().background(BgPage),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── 제목 ──────────────────────────────────────────────────────────────
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Todo",
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
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
                }
            )
        }

        // ── 오늘의 목표 ───────────────────────────────────────────────────────
        if (focusTodos.isNotEmpty()) {
            item(key = "focus_header") {
                SectionHeader(title = "오늘의 목표", badge = "✦", color = Purple)
            }
            items(focusTodos, key = { "focus_${it.id}" }) { todo ->
                TodoCard(
                    todo         = todo,
                    isEditing    = editingId == todo.id,
                    isCompleting = completingId == todo.id,
                    isFocus      = true,
                    onStartTodo  = {
                        viewModel.startFocusTodo(todo)
                        onStartTodo(todo)
                    },
                    onComplete   = { onCompleteFocus(todo) },
                    onUncomplete = { viewModel.uncompleteTodo(todo) },
                    onEditToggle = { editingId = if (editingId == todo.id) null else todo.id },
                    onSave       = { t, c, d -> viewModel.updateTodo(todo.copy(title = t, category = c, selectedDate = d)); editingId = null },
                    onDelete     = { viewModel.deleteTodo(todo); editingId = null }
                )
            }
        }

        yesterdaySuggestion?.let { suggestion ->
            item(key = "yesterday_suggestion") {
                YesterdayFlowSuggestionCard(
                    suggestion = suggestion,
                    onStart = { onStartSuggestion(suggestion.actionCategory) }
                )
            }
        }

        // ── 전체 할 일 ────────────────────────────────────────────────────────
        item(key = "active_header") {
            SectionHeader(title = "전체 할 일")
        }

        if (activeTodos.isEmpty() && focusTodos.isEmpty() && completedRegular == null) {
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

        // ── 전체 할 일 완료 항목 (1개만) ──────────────────────────────────────
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
}

@Composable
private fun YesterdayFlowSuggestionCard(
    suggestion: YesterdayFlowSuggestion,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FB)),
        border = BorderStroke(1.dp, Color(0xFFE5E7EF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = suggestion.message,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(
                onClick = onStart,
                shape = ChipShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(suggestion.actionLabel, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── 새 할 일 입력 카드 ────────────────────────────────────────────────────────
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
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = CardShape
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.weight(1f),
                    interactionSource = interactionSource,
                    singleLine = true,
                    placeholder = { Text("새 할 일") },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
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
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TypeChip("복습", category == TodoCategory.REVIEW) {
                            onCategoryChange(if (category == TodoCategory.REVIEW) null else TodoCategory.REVIEW)
                        }
                        TypeChip("과제", category == TodoCategory.ASSIGNMENT) {
                            onCategoryChange(if (category == TodoCategory.ASSIGNMENT) null else TodoCategory.ASSIGNMENT)
                        }
                        Spacer(Modifier.weight(1f))
                        DateChipButton(date, onDateClick)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Info, null, Modifier.size(13.dp), tint = TextMuted)
                        Text("선택 사항은 나중에 태그랑 볼 수 있어요", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

// ── Todo 카드 ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoCard(
    todo: TodoItem,
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = if (isFocus) CardDefaults.cardElevation(4.dp) else CardDefaults.cardElevation(0.dp),
        border = if (isFocus) null else BorderStroke(1.dp, Color(0xFFF0EFF5)),
        shape = CardShape
    ) {
        Column(Modifier.fillMaxWidth()) {
            // 완료된 오늘의 목표 항목: 간소화 레이아웃
            if (todo.isCompleted && isFocus) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
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
                        if (todo.category != TodoCategory.NORMAL) {
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
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 태그 + 제목
                    Column(Modifier.weight(1f)) {
                        if (todo.category != TodoCategory.NORMAL) {
                            CategoryTag(todo.category)
                            Spacer(Modifier.height(5.dp))
                        }
                        Text(
                            text = todo.title,
                            fontSize = if (isFocus) 16.sp else 15.sp,
                            fontWeight = if (isFocus) FontWeight.Bold else FontWeight.SemiBold,
                            color = TextPrimary,
                            lineHeight = 20.sp
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
                                    Text(
                                        "~${fmtDate(todo.selectedDate)}",
                                        fontSize = 12.sp, color = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    // 액션 버튼 3개
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        // 수정 / 닫기 (배경 없음)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onEditToggle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isEditing) Icons.Filled.Close else Icons.Filled.Edit,
                                if (isEditing) "닫기" else "수정",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // 완료 (연초록 원)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GreenSoft)
                                .clickable(onClick = onComplete),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.CheckCircle, "완료", tint = GreenTint, modifier = Modifier.size(18.dp))
                        }
                        // 시작 (연보라 원, 주요 액션)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
                        TypeChip("복습", editCategory == TodoCategory.REVIEW) {
                            editCategory = if (editCategory == TodoCategory.REVIEW) TodoCategory.NORMAL else TodoCategory.REVIEW
                        }
                        TypeChip("과제", editCategory == TodoCategory.ASSIGNMENT) {
                            editCategory = if (editCategory == TodoCategory.ASSIGNMENT) TodoCategory.NORMAL else TodoCategory.ASSIGNMENT
                        }
                        Spacer(Modifier.weight(1f))
                        DateChipButton(editDate) { showEditDatePick = true }
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
                        } else {
                            Text("정말 삭제할까요?", fontSize = 13.sp, color = TextMuted)
                            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE35B5B))) {
                                Text("삭제", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { confirmDelete = false }) {
                                Text("취소", fontSize = 13.sp, color = TextMuted)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onEditToggle) {
                            Text("취소", fontSize = 13.sp, color = TextMuted)
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
                if (todo.category != TodoCategory.NORMAL) {
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
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        if (badge != null) {
            Spacer(Modifier.width(5.dp))
            Text(badge, fontSize = 11.sp, color = color.copy(alpha = 0.7f))
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
        TodoCategory.REVIEW     -> Triple(if (muted) Color(0xFFF0EEFF) else PurpleSoft, if (muted) Purple.copy(.5f) else Purple, "복습")
        TodoCategory.ASSIGNMENT -> Triple(if (muted) Color(0xFFFFF0F0) else Color(0xFFFFEFF0), if (muted) Color(0xFFE35B5B).copy(.5f) else Color(0xFFE35B5B), "과제")
        TodoCategory.NORMAL     -> return
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
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DateChipButton(date: Long?, onClick: () -> Unit) {
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
        Text(date?.let { fmtDate(it) } ?: "날짜 선택", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── 날짜 포맷 헬퍼 ────────────────────────────────────────────────────────────
private fun fmtDate(millis: Long): String {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = millis }
    return "${c.get(java.util.Calendar.MONTH) + 1}/${c.get(java.util.Calendar.DAY_OF_MONTH)}"
}
