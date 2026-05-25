package com.example.flowlog.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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

@Composable
fun ActivityTitleDialog(
    isVisible: Boolean,
    category: String,
    categories: List<String>,
    onSave: (String, String, String?) -> Unit,
    initialTitle: String? = null,
    initialNote: String? = null,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(category) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(isVisible, category, initialTitle, initialNote) {
        if (isVisible) {
            selectedCategory = category
            title = initialTitle
                .orEmpty()
                .takeIf { category == "TODO" }
                .orEmpty()
            note = initialNote.orEmpty()
        }
    }

    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("활동 저장") },
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
                        placeholder = { Text(displayCategory(selectedCategory)) },
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

@Composable
fun CategoryPicker(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentPadding = PaddingValues(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories.size) { index ->
            val item = categories[index]
            FilterChip(
                selected = selectedCategory == item,
                onClick = { onSelect(item) },
                label = { Text(displayCategory(item)) }
            )
        }
    }
}
