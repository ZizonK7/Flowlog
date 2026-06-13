package com.example.flowlog.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun ActivityItem(
    activity: ActivitySession,
    onDelete: (ActivitySession) -> Unit,
    onEdit: (Long) -> Unit,
    onToggleFavorite: (ActivitySession) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startTimeText = timeFormat.format(Date(activity.startTime))
    val endTimeText = timeFormat.format(Date(activity.endTime))
    val durationText = formatDuration(activity.durationMillis)
    val categoryColor = categoryColor(activity.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = categoryColor)) {
                Text(
                    text = displayCategory(activity.category),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activity.title.ifBlank { displayCategory(activity.category) },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "$startTimeText ~ $endTimeText ($durationText)",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (activity.category == "EXERCISE" && activity.exerciseSets.isNotEmpty()) {
                    val setSummary = activity.exerciseSets
                        .take(3)
                        .joinToString(" · ") { "${it.name} ${formatExerciseSetItemValue(it)} ${it.intensity}" }
                    val moreText = if (activity.exerciseSets.size > 3) " 외 ${activity.exerciseSets.size - 3}세트" else ""
                    Text(
                        text = setSummary + moreText,
                        fontSize = 12.sp,
                        color = Color(0xFF5E6AD2),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
                if (!activity.note.isNullOrBlank()) {
                    Text(
                        text = activity.note,
                        fontSize = 12.sp,
                        color = Color(0xFF616161),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2
                    )
                }
            }

            IconButton(onClick = { onToggleFavorite(activity) }) {
                Icon(
                    imageVector = if (activity.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "\uC990\uACA8\uCC3E\uAE30",
                    tint = if (activity.isFavorite) Color(0xFFFFB300) else Color.Gray
                )
            }

            IconButton(onClick = { onEdit(activity.id) }) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "\uC218\uC815",
                    tint = Color(0xFF1976D2)
                )
            }

            IconButton(onClick = { onDelete(activity) }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "\uC0AD\uC81C",
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}

private fun formatExerciseSetItemValue(record: com.example.flowlog.data.model.ExerciseSetRecord): String {
    if (record.mode != "TIME") return "${record.reps}개"
    val totalSeconds = ((record.durationMillis ?: 0L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

fun formatDuration(durationMillis: Long): String {
    val durationHours = durationMillis / (1000 * 3600)
    val durationMinutes = (durationMillis % (1000 * 3600)) / (1000 * 60)
    val durationSeconds = (durationMillis % (1000 * 60)) / 1000

    return when {
        durationHours > 0 -> "${durationHours}\uC2DC\uAC04 ${durationMinutes}\uBD84"
        durationMinutes > 0 -> "${durationMinutes}\uBD84 ${durationSeconds}\uCD08"
        else -> "${durationSeconds}\uCD08"
    }
}

fun displayCategory(category: String): String {
    return when (category) {
        "TOOTHBRUSH" -> "\uC591\uCE58"
        "SNACK" -> "\uAC04\uC2DD"
        "MEAL" -> "\uC2DD\uC0AC"
        "STUDY" -> "\uACF5\uBD80"
        "WORK" -> "\uC5C5\uBB34"
        "COMPANY" -> "\uD68C\uC0AC"
        "DEVELOPMENT" -> "\uAC1C\uBC1C"
        "READING" -> "\uB3C5\uC11C"
        "MOVE" -> "\uC774\uB3D9"
        "WASH" -> "\uC53B\uAE30"
        "SCHOOL" -> "\uD559\uAD50"
        "EXERCISE" -> "\uC6B4\uB3D9"
        "SLEEP" -> "\uC218\uBA74"
        "REST" -> "\uD734\uC2DD"
        "ETC" -> "\uAE30\uD0C0"
        "GAME" -> "\uAC8C\uC784"
        "TODO" -> "\uD560\uC77C"
        "EXPERIMENT_1" -> "1\uBC88 \uC2E4\uD5D8"
        "EXPERIMENT_2" -> "2\uBC88 \uC2E4\uD5D8"
        "EXPERIMENT_3" -> "3\uBC88 \uC2E4\uD5D8"
        else -> "\uAE30\uD0C0"
    }
}

fun categoryColor(category: String): Color {
    return when (category) {
        "STUDY" -> Color(0xFF4CAF50)
        "MEAL" -> Color(0xFFFF9800)
        "SNACK" -> Color(0xFFFFC107)
        "TOOTHBRUSH" -> Color(0xFF26A69A)
        "EXERCISE" -> Color(0xFF2196F3)
        "WORK" -> Color(0xFF546E7A)
        "COMPANY" -> Color(0xFF455A64)
        "DEVELOPMENT" -> Color(0xFF3949AB)
        "READING" -> Color(0xFF00796B)
        "MOVE" -> Color(0xFF00838F)
        "WASH" -> Color(0xFF1E88E5)
        "SLEEP" -> Color(0xFF9C27B0)
        "REST" -> Color(0xFF00BCD4)
        "SCHOOL" -> Color(0xFFE91E63)
        "GAME" -> Color(0xFF5C6BC0)
        "TODO" -> Color(0xFF5E6AD2)
        "EXPERIMENT_1" -> Color(0xFF00897B)
        "EXPERIMENT_2" -> Color(0xFF7E57C2)
        "EXPERIMENT_3" -> Color(0xFFFF6A2A)
        else -> Color(0xFF757575)
    }
}
