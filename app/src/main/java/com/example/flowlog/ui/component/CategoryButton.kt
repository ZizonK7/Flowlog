package com.example.flowlog.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryButton(
    category: String,
    isSelected: Boolean = false,
    label: String = displayCategory(category),
    iconCategory: String = category,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val accentColor = categoryColor(category)
    val iconBackground = categoryPastelColor(category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 5.dp else 3.dp),
        border = BorderStroke(
            width = if (isSelected) 1.4.dp else 0.6.dp,
            color = if (isSelected) Color(0xFF7D68EA) else Color(0xFFE7E8EF)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = iconCategory,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C2437),
                    maxLines = 1
                )
                if (category == "TOOTHBRUSH") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "3분",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B4FE8),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryGlyph(
    category: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (category) {
        "TOOTHBRUSH" -> ToothbrushGlyph(tint = tint, modifier = modifier)
        else -> categoryAppIcon(category)?.let { icon ->
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier
            )
        } ?: Text(
            text = displayCategory(category).take(1),
            color = tint,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun categoryPastelColor(category: String): Color {
    return when (category) {
        "TOOTHBRUSH" -> Color(0xFFEDE8FF)
        "SNACK" -> Color(0xFFFFF1DD)
        "MEAL" -> Color(0xFFFFEDE4)
        "STUDY" -> Color(0xFFE6F6E8)
        "WORK" -> Color(0xFFE9EAF0)
        "COMPANY" -> Color(0xFFE6EEF2)
        "DEVELOPMENT" -> Color(0xFFE9E7FF)
        "WASH" -> Color(0xFFE4F5FF)
        "SCHOOL" -> Color(0xFFFCE4ED)
        "EXERCISE" -> Color(0xFFE4F1FF)
        "SLEEP" -> Color(0xFFF0E4FF)
        "REST" -> Color(0xFFE2F5F5)
        "ETC" -> Color(0xFFEDEDF1)
        "EXPERIMENT_1" -> Color(0xFFE2F5F1)
        "EXPERIMENT_2" -> Color(0xFFECE7FF)
        "EXPERIMENT_3" -> Color(0xFFFFE8D8)
        else -> Color(0xFFEDEDF1)
    }
}

@Composable
private fun ToothbrushGlyph(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val handleWidth = size.width * 0.14f
        val handleHeight = size.height * 0.62f
        val handleLeft = size.width * 0.42f
        val handleTop = size.height * 0.34f
        val radius = CornerRadius(size.width * 0.07f, size.width * 0.07f)

        drawRoundRect(
            color = tint,
            topLeft = Offset(handleLeft, handleTop),
            size = Size(handleWidth, handleHeight),
            cornerRadius = radius
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.24f, size.height * 0.2f),
            size = Size(size.width * 0.42f, size.height * 0.18f),
            cornerRadius = CornerRadius(size.width * 0.05f, size.width * 0.05f)
        )
        repeat(4) { index ->
            drawRoundRect(
                color = tint.copy(alpha = 0.72f),
                topLeft = Offset(size.width * (0.27f + index * 0.1f), size.height * 0.04f),
                size = Size(size.width * 0.055f, size.height * 0.2f),
                cornerRadius = CornerRadius(size.width * 0.03f, size.width * 0.03f)
            )
        }
        drawCircle(
            color = tint.copy(alpha = 0.62f),
            radius = size.width * 0.055f,
            center = Offset(size.width * 0.7f, size.height * 0.25f)
        )
    }
}
