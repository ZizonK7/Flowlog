package com.example.flowlog.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryButton(
    category: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (category) {
        "STUDY" -> Color(0xFF4CAF50)
        "MEAL" -> Color(0xFFFF9800)
        "SNACK" -> Color(0xFFFFC107)
        "TOOTHBRUSH" -> Color(0xFF26A69A)
        "EXERCISE" -> Color(0xFF2196F3)
        "SLEEP" -> Color(0xFF9C27B0)
        "REST" -> Color(0xFF00BCD4)
        "SCHOOL" -> Color(0xFFE91E63)
        "TODO" -> Color(0xFF5E6AD2)
        "EXPERIMENT_1" -> Color(0xFF00897B)
        "EXPERIMENT_2" -> Color(0xFF7E57C2)
        else -> Color(0xFF757575)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .size(80.dp)
                .padding(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) backgroundColor.copy(alpha = 0.8f) else backgroundColor
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = if (isSelected) ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp) else ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = displayCategory(category),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}

