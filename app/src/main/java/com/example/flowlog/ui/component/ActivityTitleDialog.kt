package com.example.flowlog.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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

private val EtcCardPurple     = Color(0xFF5140D8)
private val EtcCardPurpleSoft = Color(0xFFEDE9FF)
private val EtcCardInk        = Color(0xFF10182C)
private val EtcCardMuted      = Color(0xFF697386)
private val EtcCardBorder     = Color(0xFFE8E8EE)

@Composable
fun ActivityTitleDialog(
    isVisible: Boolean,
    category: String,
    suggestions: List<String> = emptyList(),
    onSave: (String, String) -> Unit,
    initialTitle: String? = null,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }

    LaunchedEffect(isVisible, initialTitle) {
        if (isVisible) {
            title = initialTitle.orEmpty()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 0.dp, bottom = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "방금 기타 활동, 무엇을 했나요?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = EtcCardInk
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "닫기",
                        tint = EtcCardMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions.size) { i ->
                        SuggestionChip(
                            onClick = { title = suggestions[i] },
                            label = { Text(suggestions[i], fontSize = 13.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = EtcCardPurpleSoft,
                                labelColor = EtcCardPurple
                            ),
                            border = null
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            "예: 휴식, 청소 등",
                            color = EtcCardMuted,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EtcCardPurple,
                        unfocusedBorderColor = EtcCardBorder,
                        focusedTextColor = EtcCardInk,
                        unfocusedTextColor = EtcCardInk,
                        cursorColor = EtcCardPurple
                    )
                )
                Button(
                    onClick = { onSave(category, title.trim()) },
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EtcCardPurpleSoft,
                        contentColor = EtcCardPurple,
                        disabledContainerColor = Color(0xFFF4F2FA),
                        disabledContentColor = Color(0xFFB8B2C9)
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Text("저장", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
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
