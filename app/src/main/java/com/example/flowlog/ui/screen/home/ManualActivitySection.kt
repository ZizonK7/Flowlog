package com.example.flowlog.ui.screen

import com.example.flowlog.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.ui.component.EditCategoryPicker
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.component.editTextFieldColors
import com.example.flowlog.ui.viewmodel.ManualActivityError
import com.example.flowlog.ui.viewmodel.ManualActivityValidator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val MANUAL_DEFAULT_LENGTH_MINUTES = 30
private const val MANUAL_OVERLAP_PREVIEW_LIMIT = 3

private enum class ManualDayChoice { TODAY, YESTERDAY, OTHER }
private enum class ManualTimeTarget { START, END }

/**
 * Dialog for adding a past activity by hand (the timer was not started).
 * Shows blocking errors and a non-blocking overlap warning from
 * [ManualActivityValidator]; saving goes through ActivityViewModel.addManualActivity.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ManualActivityDialog(
    categories: List<String>,
    activities: List<ActivitySession>,
    categoryLabels: Map<String, String>,
    onSave: (category: String, title: String, note: String?, startMillis: Long, endMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val openedAt = remember { System.currentTimeMillis() }
    val todayStart = remember(openedAt) { localStartOfDay(openedAt) }
    val nowMinute = remember(openedAt) { minuteOfDay(openedAt) }

    var selectedCategory by remember { mutableStateOf(categories.firstOrNull().orEmpty()) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dayChoice by remember { mutableStateOf(ManualDayChoice.TODAY) }
    var dayStartMillis by remember { mutableLongStateOf(todayStart) }
    var startMinute by remember { mutableIntStateOf((nowMinute - MANUAL_DEFAULT_LENGTH_MINUTES).coerceAtLeast(0)) }
    var endMinute by remember { mutableIntStateOf(nowMinute) }
    var timeTarget by remember { mutableStateOf<ManualTimeTarget?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val violet = Color(0xFF5E6AD2)
    val strongText = Color(0xFF20243A)
    val readableMuted = Color(0xFF4F5568)
    val errorColor = Color(0xFFD93848)
    val warningColor = Color(0xFFB26A00)

    val (startMillis, endMillis) = ManualActivityValidator.resolveTimes(
        dayStartMillis = dayStartMillis,
        startMinuteOfDay = startMinute,
        endMinuteOfDay = endMinute
    )
    val validation = ManualActivityValidator.validate(
        startMillis = startMillis,
        endMillis = endMillis,
        nowMillis = System.currentTimeMillis(),
        existing = activities
    )
    val titleSuggestions = remember(selectedCategory, activities) {
        if (selectedCategory.isBlank()) emptyList() else buildTitleSuggestions(
            category = selectedCategory,
            activities = activities,
            categoryLabels = categoryLabels
        )
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = stringResource(R.string.manual_activity_date_format)
    val dateText = remember(dayStartMillis, dateFormat) {
        SimpleDateFormat(dateFormat, Locale.KOREAN).format(Date(dayStartMillis))
    }
    val endsNextDay = endMinute < startMinute

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Column {
                Text(
                    text = stringResource(R.string.manual_activity_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = strongText
                )
                Text(
                    text = stringResource(R.string.manual_activity_subtitle),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = readableMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ManualSectionLabel(stringResource(R.string.edit_activity_category_label), strongText)
                EditCategoryPicker(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it }
                )

                ManualSectionLabel(stringResource(R.string.manual_activity_day_label), strongText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ManualDayChip(
                        label = stringResource(R.string.manual_activity_today),
                        selected = dayChoice == ManualDayChoice.TODAY,
                        onClick = {
                            dayChoice = ManualDayChoice.TODAY
                            dayStartMillis = todayStart
                        }
                    )
                    ManualDayChip(
                        label = stringResource(R.string.manual_activity_yesterday),
                        selected = dayChoice == ManualDayChoice.YESTERDAY,
                        onClick = {
                            dayChoice = ManualDayChoice.YESTERDAY
                            dayStartMillis = addLocalDays(todayStart, -1)
                        }
                    )
                    ManualDayChip(
                        label = stringResource(R.string.manual_activity_pick_date),
                        selected = dayChoice == ManualDayChoice.OTHER,
                        onClick = { showDatePicker = true }
                    )
                }
                Text(text = dateText, fontSize = 13.sp, color = readableMuted)

                ManualSectionLabel(stringResource(R.string.manual_activity_time_label), strongText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ManualTimeField(
                        label = stringResource(R.string.manual_activity_start),
                        value = timeFormat.format(Date(startMillis)),
                        onClick = { timeTarget = ManualTimeTarget.START },
                        modifier = Modifier.weight(1f)
                    )
                    ManualTimeField(
                        label = stringResource(R.string.manual_activity_end),
                        value = timeFormat.format(Date(endMillis)) +
                            if (endsNextDay) " " + stringResource(R.string.manual_activity_next_day) else "",
                        onClick = { timeTarget = ManualTimeTarget.END },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (endMillis > startMillis) {
                    Text(
                        text = stringResource(
                            R.string.manual_activity_duration,
                            manualDurationText(endMillis - startMillis)
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = readableMuted
                    )
                }
                validation.error?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                ManualActivityError.END_NOT_AFTER_START -> R.string.manual_activity_error_end_before_start
                                ManualActivityError.TOO_LONG -> R.string.manual_activity_error_too_long
                                ManualActivityError.IN_FUTURE -> R.string.manual_activity_error_future
                            }
                        ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = errorColor
                    )
                }
                if (validation.hasOverlapWarning) {
                    val preview = validation.overlaps
                        .take(MANUAL_OVERLAP_PREVIEW_LIMIT)
                        .joinToString("\n") { overlap ->
                            "· " + overlap.title.ifBlank { displayCategory(overlap.category) } + " " +
                                timeFormat.format(Date(overlap.startTime)) + "–" +
                                timeFormat.format(Date(overlap.endTime))
                        }
                    Text(
                        text = stringResource(R.string.manual_activity_overlap_warning, validation.overlaps.size) +
                            "\n" + preview,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = warningColor
                    )
                }

                if (titleSuggestions.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        titleSuggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = { title = suggestion },
                                label = { Text(suggestion, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = FlowPurpleSoft,
                                    labelColor = FlowPurple
                                ),
                                border = null
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.edit_activity_title_label)) },
                    placeholder = { Text(displayCategory(selectedCategory)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = editTextFieldColors(violet, strongText, readableMuted)
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.edit_activity_memo_label)) },
                    placeholder = { Text(stringResource(R.string.edit_activity_memo_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = editTextFieldColors(violet, strongText, readableMuted)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        selectedCategory,
                        title.trim(),
                        note.trim().ifBlank { null },
                        startMillis,
                        endMillis
                    )
                },
                enabled = validation.canSave && selectedCategory.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = violet,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE7E8F0),
                    disabledContentColor = Color(0xFF9EA3B5)
                )
            ) {
                Text(stringResource(R.string.edit_activity_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.edit_activity_cancel), color = readableMuted, fontWeight = FontWeight.Bold)
            }
        }
    )

    when (timeTarget) {
        ManualTimeTarget.START -> TimePickerSheet(
            title = stringResource(R.string.manual_activity_pick_start_title),
            subtitle = dateText,
            minuteOfDay = startMinute,
            onDismiss = { timeTarget = null },
            onConfirm = {
                startMinute = it
                timeTarget = null
            }
        )
        ManualTimeTarget.END -> TimePickerSheet(
            title = stringResource(R.string.manual_activity_pick_end_title),
            subtitle = dateText,
            minuteOfDay = endMinute,
            onDismiss = { timeTarget = null },
            onConfirm = {
                endMinute = it
                timeTarget = null
            }
        )
        null -> Unit
    }

    if (showDatePicker) {
        val todayUtcMidnight = remember(todayStart) { localDayToUtcMidnight(todayStart) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDayToUtcMidnight(dayStartMillis),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= todayUtcMidnight
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val picked = utcMidnightToLocalDay(utcMillis)
                            dayStartMillis = picked
                            dayChoice = when (picked) {
                                todayStart -> ManualDayChoice.TODAY
                                addLocalDays(todayStart, -1) -> ManualDayChoice.YESTERDAY
                                else -> ManualDayChoice.OTHER
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.manual_activity_confirm), color = FlowPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.edit_activity_cancel), color = FlowMuted, fontWeight = FontWeight.Bold)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ManualSectionLabel(text: String, color: Color) {
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun ManualDayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = FlowPurpleSoft,
            selectedLabelColor = FlowPurple
        )
    )
}

@Composable
private fun ManualTimeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFD7D5E8))
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FlowMuted)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = FlowInk)
        }
    }
}

@Composable
private fun manualDurationText(durationMillis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.duration_hours_minutes, hours, minutes)
        hours > 0 -> stringResource(R.string.duration_hours_only, hours)
        else -> stringResource(R.string.duration_minutes_only, minutes)
    }
}

private fun localStartOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addLocalDays(dayStartMillis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = dayStartMillis
    add(Calendar.DAY_OF_YEAR, days)
}.timeInMillis

private fun minuteOfDay(millis: Long): Int {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
}

// Material3 DatePicker works with UTC midnight for the picked calendar date.
private fun localDayToUtcMidnight(localDayMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localDayMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun utcMidnightToLocalDay(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
