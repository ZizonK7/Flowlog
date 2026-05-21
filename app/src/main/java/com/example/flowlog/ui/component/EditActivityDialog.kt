package com.example.flowlog.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flowlog.data.model.ActivitySession

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

    LaunchedEffect(activity.id, isVisible) {
        if (isVisible) {
            selectedCategory = activity.category
            title = activity.title
            note = activity.note.orEmpty()
        }
    }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("활동 수정") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("카테고리")
                    CategoryPicker(
                        categories = categories.filter { it != "SNACK" && it != "TOOTHBRUSH" },
                        selectedCategory = selectedCategory,
                        onSelect = { selectedCategory = it }
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("제목 (선택)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("메모 (선택)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        minLines = 3,
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(selectedCategory, title.trim(), note.trim().ifBlank { null })
                    }
                ) {
                    Text("확인")
                }
            }
        )
    }
}
