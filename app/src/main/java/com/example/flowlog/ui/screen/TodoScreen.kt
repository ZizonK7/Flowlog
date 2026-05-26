package com.example.flowlog.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.TodoViewModel

private val TodoBackground = Color(0xFFFAFAFC)
private val TodoText = Color(0xFF11182F)
private val TodoMutedText = Color(0xFF7D8190)
private val TodoPurple = Color(0xFF6757E7)
private val TodoPurpleSoft = Color(0xFFF0EEFF)
private val TodoBorder = Color(0xFFD8D3E2)
private val TodoDanger = Color(0xFFE3343F)
private val TodoCardShape = RoundedCornerShape(18.dp)

@Composable
fun TodoScreen(
    viewModel: TodoViewModel,
    onStartTodo: (TodoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val todos by viewModel.todos.collectAsState()
    var title by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(TodoBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Todo",
                fontSize = 31.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TodoText,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = TodoCardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("새 할 일") },
                        textStyle = TextStyle(
                            color = TodoText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TodoPurple,
                            unfocusedBorderColor = TodoBorder,
                            focusedTextColor = TodoText,
                            unfocusedTextColor = TodoText,
                            focusedPlaceholderColor = TodoMutedText,
                            unfocusedPlaceholderColor = TodoMutedText,
                            cursorColor = TodoPurple
                        )
                    )
                    Button(
                        onClick = {
                            viewModel.addTodo(title)
                            title = ""
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(22.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TodoPurpleSoft,
                            contentColor = TodoPurple,
                            disabledContainerColor = Color(0xFFF4F2FA),
                            disabledContentColor = Color(0xFFB8B2C9)
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "추가",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (todos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "아직 할 일이 없습니다.",
                        color = TodoMutedText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            items(
                items = todos,
                key = { it.id }
            ) { todo ->
                TodoRow(
                    todo = todo,
                    onToggleDone = { viewModel.toggleTodoDone(todo) },
                    onDelete = { viewModel.deleteTodo(todo) },
                    onStartTodo = { onStartTodo(todo) }
                )
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: TodoItem,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onStartTodo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F1F5)),
        shape = TodoCardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isDone,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(
                    checkedColor = TodoPurple,
                    uncheckedColor = TodoBorder,
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else null,
                    color = if (todo.isDone) Color(0xFFA0A3B1) else TodoText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "누적 작업 ${formatDuration(todo.accumulatedMillis)}",
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFA0A3B1)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onStartTodo,
                enabled = !todo.isDone,
                modifier = Modifier
                    .size(38.dp)
                    .background(if (todo.isDone) Color.Transparent else TodoPurpleSoft, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "작업 시작",
                    tint = if (todo.isDone) Color.LightGray else TodoPurple,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "삭제",
                    tint = Color(0xFFD4D4DA),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
