package com.example.flowlog.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shower
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.flowlog.R

fun categoryAppIcon(category: String): ImageVector? = when (category) {
    "MEAL"        -> Icons.Filled.Restaurant
    "STUDY"       -> Icons.AutoMirrored.Filled.MenuBook
    "WORK"        -> Icons.Filled.Work
    "DEVELOPMENT" -> Icons.Filled.Code
    "WASH"        -> Icons.Filled.Shower
    "SCHOOL"      -> Icons.Filled.School
    "EXERCISE"    -> Icons.AutoMirrored.Filled.DirectionsRun
    "SLEEP"       -> Icons.Filled.Bedtime
    "REST"        -> Icons.Filled.LocalCafe
    "ETC"         -> Icons.Filled.MoreHoriz
    "SNACK"       -> Icons.Filled.Cookie
    "EXPERIMENT_1", "EXPERIMENT_2", "EXPERIMENT_3" -> Icons.Filled.Science
    else -> null
}

@DrawableRes
fun categoryNotificationIconRes(category: String): Int = when (category) {
    "MEAL"        -> R.drawable.ic_activity_meal
    "STUDY"       -> R.drawable.ic_activity_study
    "WORK"        -> R.drawable.ic_activity_work
    "DEVELOPMENT" -> R.drawable.ic_activity_development
    "WASH"        -> R.drawable.ic_activity_wash
    "SCHOOL"      -> R.drawable.ic_activity_school
    "EXERCISE"    -> R.drawable.ic_activity_exercise
    "SLEEP"       -> R.drawable.ic_activity_sleep
    "REST"        -> R.drawable.ic_activity_rest
    "ETC"         -> R.drawable.ic_activity_etc
    "SNACK"       -> R.drawable.ic_activity_snack
    "TOOTHBRUSH"  -> R.drawable.ic_activity_toothbrush
    "TODO"        -> R.drawable.ic_activity_todo
    else          -> R.drawable.ic_activity_generic
}
