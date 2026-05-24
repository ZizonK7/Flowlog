package com.example.flowlog.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = categoryColor(category)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (category == "TOOTHBRUSH") {
            ToothbrushButton(
                isSelected = isSelected,
                onClick = onClick
            )
            return@Column
        }

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

@Composable
private fun ToothbrushButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = categoryColor("TOOTHBRUSH")
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(80.dp)
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF00897B) else Color(0xFFE8FFF9),
            contentColor = if (isSelected) Color.White else Color(0xFF00796B)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isSelected) 0.9f else 0.55f)),
        elevation = if (isSelected) {
            ButtonDefaults.elevatedButtonElevation(defaultElevation = 8.dp)
        } else {
            ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
        },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ToothbrushGlyph(
                tint = if (isSelected) Color.White else accentColor,
                highlight = if (isSelected) Color(0xFFB2DFDB) else Color(0xFF4DB6AC)
            )
            Text(
                text = displayCategory("TOOTHBRUSH"),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.White else Color(0xFF00695C),
                maxLines = 1
            )
            Text(
                text = "3\uBD84",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFFE0F2F1) else Color(0xFF4DB6AC),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToothbrushGlyph(
    tint: Color,
    highlight: Color
) {
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp, 24.dp)) {
            val handleWidth = 5.dp.toPx()
            val radius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            drawRoundRect(
                color = tint,
                topLeft = Offset(12.dp.toPx(), 7.dp.toPx()),
                size = Size(handleWidth, 15.dp.toPx()),
                cornerRadius = radius
            )
            drawRoundRect(
                color = tint,
                topLeft = Offset(8.dp.toPx(), 3.dp.toPx()),
                size = Size(13.dp.toPx(), 6.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            repeat(4) { index ->
                drawRoundRect(
                    color = highlight,
                    topLeft = Offset((9 + index * 3).dp.toPx(), 0.dp.toPx()),
                    size = Size(2.dp.toPx(), 5.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
            drawCircle(
                color = highlight.copy(alpha = 0.75f),
                radius = 1.5.dp.toPx(),
                center = Offset(22.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}
