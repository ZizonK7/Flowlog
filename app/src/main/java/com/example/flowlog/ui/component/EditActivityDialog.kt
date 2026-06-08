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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditActivityDialog(
    activity: ActivitySession,
    categories: List<String>,
    isVisible: Boolean,
    onSave: (String, String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(activity.category) }
    var title by remember { mutableStateOf(activity.title) }
    var note by remember { mutableStateOf(activity.note.orEmpty()) }
    val violet = Color(0xFF5E6AD2)
    val violetSoft = Color(0xFFF3F1FF)
    val strongText = Color(0xFF20243A)
    val readableMuted = Color(0xFF4F5568)

    LaunchedEffect(activity.id, isVisible) {
        if (isVisible) {
            selectedCategory = activity.category
            title = activity.title
            note = activity.note.orEmpty()
        }
    }

    if (!isVisible) return

    val editableCategories = remember(categories) {
        categories.filter { it != "SNACK" && it != "TOOTHBRUSH" }
    }
    val canSave = title.isNotBlank() || selectedCategory.isNotBlank()

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
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(selectedCategory, title.trim(), note.trim().ifBlank { null })
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
