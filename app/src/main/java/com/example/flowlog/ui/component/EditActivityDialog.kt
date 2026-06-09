package com.example.flowlog.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.ExerciseSetRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditActivityDialog(
    activity: ActivitySession,
    categories: List<String>,
    isVisible: Boolean,
    onSave: (String, String, String?, List<ExerciseSetRecord>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(activity.category) }
    var title by remember { mutableStateOf(activity.title) }
    var note by remember { mutableStateOf(activity.note.orEmpty()) }
    var exerciseSets by remember { mutableStateOf(activity.exerciseSets) }
    val violet = Color(0xFF5E6AD2)
    val violetSoft = Color(0xFFF3F1FF)
    val strongText = Color(0xFF20243A)
    val readableMuted = Color(0xFF4F5568)

    LaunchedEffect(activity.id, isVisible) {
        if (isVisible) {
            selectedCategory = activity.category
            title = activity.title
            note = activity.note.orEmpty()
            exerciseSets = activity.exerciseSets
        }
    }

    if (!isVisible) return

    val editableCategories = remember(categories) {
        categories.filter { it != "SNACK" && it != "TOOTHBRUSH" }
    }
    val isExerciseEdit = selectedCategory == "EXERCISE"
    val canSave = if (isExerciseEdit) exerciseSets.isNotEmpty() else title.isNotBlank() || selectedCategory.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(categoryColor(selectedCategory).copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryGlyph(
                        category = selectedCategory,
                        tint = categoryColor(selectedCategory),
                        modifier = Modifier.size(23.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "기록 수정",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = strongText
                    )
                    Text(
                        text = displayCategory(selectedCategory),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = readableMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isExerciseEdit) {
                    ExerciseActivityEditContent(
                        activity = activity,
                        sets = exerciseSets,
                        note = note,
                        onSetsChange = { exerciseSets = it },
                        onNoteChange = { note = it },
                        strongText = strongText,
                        readableMuted = readableMuted,
                        violet = violet
                    )
                } else {
                    ActivityEditSummary(activity = activity)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "카테고리",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = strongText
                        )
                        EditCategoryPicker(
                            categories = editableCategories,
                            selectedCategory = selectedCategory,
                            onSelect = { selectedCategory = it }
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("제목") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.EditNote,
                                contentDescription = null
                            )
                        },
                        placeholder = { Text(displayCategory(selectedCategory)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = editTextFieldColors(violet, strongText, readableMuted)
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("메모") },
                        placeholder = { Text("남겨둘 내용을 적어주세요") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(16.dp),
                        colors = editTextFieldColors(violet, strongText, readableMuted)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = if (isExerciseEdit) {
                        exerciseSets.firstOrNull()?.name ?: title.trim()
                    } else {
                        title.trim()
                    }
                    onSave(
                        selectedCategory,
                        cleanTitle,
                        note.trim().ifBlank { null },
                        if (isExerciseEdit) exerciseSets else emptyList()
                    )
                },
                enabled = canSave,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = violet,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE7E8F0),
                    disabledContentColor = Color(0xFF9EA3B5)
                )
            ) {
                Text("저장", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = readableMuted, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ExerciseActivityEditContent(
    activity: ActivitySession,
    sets: List<ExerciseSetRecord>,
    note: String,
    onSetsChange: (List<ExerciseSetRecord>) -> Unit,
    onNoteChange: (String) -> Unit,
    strongText: Color,
    readableMuted: Color,
    violet: Color
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var addingPrefill by remember { mutableStateOf<ExerciseSetRecord?>(null) }
    var showAddSetDialog by remember { mutableStateOf(false) }

    ActivityEditSummary(activity = activity)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "최근 기록",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = strongText
        )
        if (sets.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF7F6FC),
                border = BorderStroke(1.dp, Color(0xFFE4E2F4))
            ) {
                Text(
                    text = "아직 세트 기록이 없습니다.",
                    modifier = Modifier.padding(14.dp),
                    color = readableMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sets.forEachIndexed { index, set ->
                    ExerciseSetEditRow(
                        index = index,
                        set = set,
                        onEdit = { editingIndex = index },
                        onDelete = { onSetsChange(sets.filterIndexed { i, _ -> i != index }) },
                        violet = violet,
                        strongText = strongText,
                        readableMuted = readableMuted
                    )
                }
            }
        }
    }

    OutlinedButton(
        onClick = {
            addingPrefill = sets.lastOrNull()
            showAddSetDialog = true
        },
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, violet)
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = violet)
        Spacer(modifier = Modifier.width(8.dp))
        Text("같은 운동 세트 추가", color = violet, fontWeight = FontWeight.ExtraBold)
    }

    OutlinedButton(
        onClick = {
            addingPrefill = null
            showAddSetDialog = true
        },
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, Color(0xFFE4E2F4))
    ) {
        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = violet)
        Spacer(modifier = Modifier.width(8.dp))
        Text("다른 운동 추가", color = violet, fontWeight = FontWeight.ExtraBold)
    }

    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        label = { Text("메모") },
        placeholder = { Text("오늘 운동 느낌이나 메모를 남겨보세요") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(16.dp),
        colors = editTextFieldColors(violet, strongText, readableMuted)
    )

    val targetEditIndex = editingIndex
    if (targetEditIndex != null) {
        ExerciseSetEditDialog(
            title = "세트 수정",
            initialRecord = sets.getOrNull(targetEditIndex) ?: ExerciseSetRecord("팔굽혀펴기", 12, "힘듦"),
            onDismiss = { editingIndex = null },
            onSave = { updated ->
                onSetsChange(sets.toMutableList().also { if (targetEditIndex in it.indices) it[targetEditIndex] = updated })
                editingIndex = null
            },
            violet = violet,
            strongText = strongText,
            readableMuted = readableMuted
        )
    }

    if (showAddSetDialog) {
        ExerciseSetEditDialog(
            title = "세트 추가",
            initialRecord = addingPrefill ?: ExerciseSetRecord("팔굽혀펴기", 12, "힘듦"),
            onDismiss = { showAddSetDialog = false },
            onSave = { added ->
                onSetsChange(sets + added)
                showAddSetDialog = false
            },
            violet = violet,
            strongText = strongText,
            readableMuted = readableMuted
        )
    }
}

@Composable
private fun ExerciseSetEditRow(
    index: Int,
    set: ExerciseSetRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    violet: Color,
    strongText: Color,
    readableMuted: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF7F6FC),
        border = BorderStroke(1.dp, Color(0xFFE4E2F4))
    ) {
        Row(
            modifier = Modifier.padding(start = 13.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = set.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = strongText
                )
                Text(
                    text = "${index + 1}세트 · ${formatExerciseEditSetValue(set)} · ${set.intensity}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = readableMuted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "세트 수정", tint = violet, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "세트 삭제", tint = Color(0xFFE04F5F), modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun ExerciseSetEditDialog(
    title: String,
    initialRecord: ExerciseSetRecord,
    onDismiss: () -> Unit,
    onSave: (ExerciseSetRecord) -> Unit,
    violet: Color,
    strongText: Color,
    readableMuted: Color
) {
    var selectedExercise by remember(initialRecord) { mutableStateOf(initialRecord.name) }
    var reps by remember(initialRecord) { mutableStateOf(initialRecord.reps) }
    var recordMode by remember(initialRecord) { mutableStateOf(initialRecord.mode) }
    var durationMillis by remember(initialRecord) { mutableStateOf(initialRecord.durationMillis ?: 40_000L) }
    var intensity by remember(initialRecord) { mutableStateOf(initialRecord.intensity) }
    val defaultExerciseOptions = remember { listOf("팔굽혀펴기", "스쿼트", "플랭크") }
    var options by remember(initialRecord) {
        mutableStateOf((defaultExerciseOptions + initialRecord.name).distinct())
    }
    var customExercise by remember { mutableStateOf("") }
    var showExerciseAddCard by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = Color.White,
        title = {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = strongText)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("운동", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = strongText)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(options) { option ->
                            val canDelete = option !in defaultExerciseOptions
                            FilterChip(
                                selected = selectedExercise == option,
                                onClick = { selectedExercise = option },
                                label = { Text(option, fontWeight = FontWeight.Bold) },
                                trailingIcon = if (canDelete) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "운동 삭제",
                                            modifier = Modifier
                                                .size(15.dp)
                                                .clickable {
                                                    options = options - option
                                                    if (selectedExercise == option) {
                                                        selectedExercise = options.firstOrNull { it != option } ?: "팔굽혀펴기"
                                                    }
                                                }
                                        )
                                    }
                                } else {
                                    null
                                }
                            )
                        }
                    }
                    if (showExerciseAddCard) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF7F6FC), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customExercise,
                                onValueChange = { customExercise = it },
                                placeholder = { Text("운동 이름") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = editTextFieldColors(violet, strongText, readableMuted)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = {
                                        customExercise = ""
                                        showExerciseAddCard = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("취소", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        val clean = customExercise.trim()
                                        if (clean.isNotBlank()) {
                                            options = (options + clean).distinct()
                                            selectedExercise = clean
                                            customExercise = ""
                                            showExerciseAddCard = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = violet, contentColor = Color.White)
                                ) {
                                    Text("추가", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showExerciseAddCard = true },
                            modifier = Modifier.height(38.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = violet)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("운동 추가", color = violet, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("기록 방식", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = strongText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { recordMode = "COUNT" },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordMode == "COUNT") violet else Color(0xFFF1F0F7),
                                contentColor = if (recordMode == "COUNT") Color.White else strongText
                            )
                        ) {
                            Text("개수", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { recordMode = "TIME" },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recordMode == "TIME") violet else Color(0xFFF1F0F7),
                                contentColor = if (recordMode == "TIME") Color.White else strongText
                            )
                        ) {
                            Text("시간", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (recordMode == "TIME") "시간" else "개수", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = strongText)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (recordMode == "TIME") {
                            ExerciseCountButton("-", { durationMillis = (durationMillis - 5_000L).coerceAtLeast(5_000L) }, violet)
                            Text(formatExerciseEditDuration(durationMillis), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = strongText)
                            ExerciseCountButton("+", { durationMillis += 5_000L }, violet)
                        } else {
                            ExerciseCountButton("-", { reps = (reps - 1).coerceAtLeast(1) }, violet)
                            Text(reps.toString(), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = strongText)
                            ExerciseCountButton("+", { reps += 1 }, violet)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("강도 (RPE 느낌)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = strongText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("가벼움", "보통", "힘듦").forEach { option ->
                            ExerciseIntensityButton(
                                label = option,
                                selected = intensity == option,
                                onClick = { intensity = option },
                                modifier = Modifier.weight(1f),
                                violet = violet,
                                strongText = strongText
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", color = readableMuted, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        ExerciseSetRecord(
                            name = selectedExercise.trim().ifBlank { "운동" },
                            reps = reps,
                            intensity = intensity,
                            mode = recordMode,
                            durationMillis = if (recordMode == "TIME") durationMillis else null
                        )
                    )
                },
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = violet, contentColor = Color.White)
            ) {
                Text("저장", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ExerciseCountButton(label: String, onClick: () -> Unit, violet: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFF3F1FF),
            contentColor = violet
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun ExerciseIntensityButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    violet: Color,
    strongText: Color
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) violet else Color(0xFFF1F0F7),
            contentColor = if (selected) Color.White else strongText.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun formatExerciseEditSetValue(record: ExerciseSetRecord): String {
    return if (record.mode == "TIME") {
        formatExerciseEditDuration(record.durationMillis ?: 0L)
    } else {
        "${record.reps}개"
    }
}

private fun formatExerciseEditDuration(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
private fun ActivityEditSummary(activity: ActivitySession) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val startTimeText = timeFormat.format(Date(activity.startTime))
    val endTimeText = timeFormat.format(Date(activity.endTime))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE4E2F4))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.AccessTime,
                contentDescription = null,
                tint = Color(0xFF5E6AD2),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$startTimeText - $endTimeText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF20243A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formatDuration(activity.durationMillis),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4F5568)
                )
            }
        }
    }
}

@Composable
private fun EditCategoryPicker(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { it }) { category ->
            val selected = selectedCategory == category
            CategoryChoiceChip(
                category = category,
                selected = selected,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun CategoryChoiceChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val violet = Color(0xFF5E6AD2)
    val violetSoft = Color(0xFFF3F1FF)
    val borderColor = if (selected) violet else Color(0xFFE2DFF4)
    val textColor = if (selected) violet else Color(0xFF3E4356)

    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) violetSoft else Color.White,
        border = BorderStroke(if (selected) 1.4.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (selected) Color.White else violetSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = category,
                    tint = violet,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = displayCategory(category),
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                color = textColor
            )
        }
    }
}

@Composable
private fun editTextFieldColors(
    violet: Color,
    strongText: Color,
    readableMuted: Color
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = strongText,
    unfocusedTextColor = strongText,
    focusedLabelColor = violet,
    unfocusedLabelColor = readableMuted,
    focusedPlaceholderColor = readableMuted,
    unfocusedPlaceholderColor = readableMuted,
    focusedLeadingIconColor = violet,
    unfocusedLeadingIconColor = readableMuted,
    focusedBorderColor = violet,
    unfocusedBorderColor = Color(0xFFD7D5E8),
    cursorColor = violet
)
