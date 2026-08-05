package com.example.flowlog.ui.screen

import android.content.Context
import android.graphics.Paint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.flowlog.data.constants.ActivitySourceType
import com.example.flowlog.data.local.ExerciseLastSettings
import com.example.flowlog.data.local.ExerciseOptionsStore
import com.example.flowlog.data.local.FocusModeStore
import com.example.flowlog.data.local.TimerStateStore
import com.example.flowlog.notification.KakaoStyleAlertPlayer
import com.example.flowlog.notification.FocusDndController
import com.example.flowlog.debug.CityTimetablePreset
import com.example.flowlog.debug.CityTimetableSamples
import com.example.flowlog.debug.SampleTimetableData
import com.example.flowlog.data.model.ActivitySession
import com.example.flowlog.data.model.ExerciseSetRecord
import com.example.flowlog.ui.city.CityTimetableCard
import com.example.flowlog.data.agent.OrganizedPetite
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.model.AutoButtonSchedule
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.RecommendedTodoBlock
import com.example.flowlog.data.model.ScheduledAutoButtonBlock
import com.example.flowlog.data.model.TodoCategory
import com.example.flowlog.data.model.TodoItem
import com.example.flowlog.ui.component.CategoryButton
import com.example.flowlog.ui.component.CategoryGlyph
import com.example.flowlog.ui.component.EditActivityDialog
import com.example.flowlog.ui.component.PickerWaveBackground
import com.example.flowlog.ui.component.WheelPickerColumn
import com.example.flowlog.ui.component.categoryColor
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.component.formatDuration
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.FlowActivityRecommendation
import com.example.flowlog.ui.viewmodel.TimerDisplayState
import com.example.flowlog.ui.viewmodel.AnalyticsState
import com.example.flowlog.ui.viewmodel.CategoryStat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun MainButtonConflictDialog(
    localConfig: MainButtonConfig,
    remoteConfig: MainButtonConfig,
    onUseLocal: () -> Unit,
    onUseRemote: () -> Unit,
    onSetupNew: () -> Unit
) {
    val isOrderOnlyConflict = remember(localConfig, remoteConfig) {
        localConfig.buttons.map { it.category }.toSet() ==
            remoteConfig.buttons.map { it.category }.toSet()
    }
    val subtitle = if (isOrderOnlyConflict) {
        "이 기기와 계정의 버튼 순서가 달라요.\n어느 쪽 순서를 사용할까요?"
    } else {
        "이 기기와 계정의 버튼 설정이 달라요.\n어느 쪽을 사용할까요?"
    }
    val localLabel = if (isOrderOnlyConflict) "현재 기기 순서 사용" else "현재 기기 설정 사용"
    val remoteLabel = if (isOrderOnlyConflict) "계정 순서 사용" else "계정 설정 사용"

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "메인 버튼 설정이 달라요",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = FlowMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ConflictConfigPreview(
                        label = "현재 기기",
                        config = localConfig,
                        modifier = Modifier.weight(1f)
                    )
                    ConflictConfigPreview(
                        label = "계정",
                        config = remoteConfig,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onUseLocal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = localLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUseRemote,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, FlowPurple)
                ) {
                    Text(
                        text = remoteLabel,
                        color = FlowPurple,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(
                    onClick = onSetupNew,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "직접 다시 고르기",
                        color = FlowMuted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun ConflictConfigPreview(
    label: String,
    config: MainButtonConfig,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFFF4F4F8), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        config.buttons.sortedBy { it.order }.forEach { btn ->
            Text(
                text = displayCategory(btn.category),
                fontSize = 12.sp,
                color = FlowInk,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
internal fun MainButtonSetupPage(
    onComplete: (List<String>) -> Unit
) {
    val initialRecommended = remember {
        linkedSetOf("STUDY", "REST", "EXERCISE", "MEAL", "SLEEP", "ETC")
    }
    var selected by remember { mutableStateOf<Set<String>>(initialRecommended) }
    var showMaxWarning by remember { mutableStateOf(false) }

    val count = selected.size

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "메인 버튼을 설정해 주세요",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = FlowInk
        )
        Text(
            text = "자주 쓰는 활동을 추려봤어요. 필요하면 바꿀 수 있어요.\n나중에 Flowlog AI가 더 맞는 버튼을 추천해 드려요.",
            fontSize = 13.sp,
            color = FlowMuted,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Text(
            text = "${count}개 선택됨  •  최소 ${MainButtonConfig.MIN_BUTTONS}개, 최대 ${MainButtonConfig.MAX_BUTTONS}개",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                count < MainButtonConfig.MIN_BUTTONS -> Color(0xFFE53935)
                count >= MainButtonConfig.MAX_BUTTONS -> FlowPurple
                else -> FlowMuted
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val allCategories = MainButtonConfig.ALL_SELECTABLE_CATEGORIES
        val rowCount = (allCategories.size + 1) / 2
        val gridHeight = (rowCount * 84 + (rowCount - 1) * 12 + 8).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(items = allCategories, key = { it }) { category ->
                val isSelected = category in selected
                CategoryButton(
                    category = category,
                    label = displayCategory(category),
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected) {
                            selected = selected - category
                            showMaxWarning = false
                        } else {
                            if (selected.size >= MainButtonConfig.MAX_BUTTONS) {
                                showMaxWarning = true
                            } else {
                                selected = selected + category
                                showMaxWarning = false
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(visible = showMaxWarning) {
            Text(
                text = "메인 버튼은 최대 ${MainButtonConfig.MAX_BUTTONS}개까지 둘 수 있어요.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE53935),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { onComplete(selected.toList()) },
            enabled = count >= MainButtonConfig.MIN_BUTTONS,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = FlowPurple,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFDED9F5),
                disabledContentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "이대로 시작하기",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun FlowStartPage(
    mainButtonConfig: MainButtonConfig,
    onLongClickButton: (String) -> Unit,
    activeCategory: String?,
    onPinQuickCategory: (String) -> Unit,
    pinnedQuickCategory: String?,
    statusMessage: String?,
    onStart: (String) -> Unit,
    isMainButtonReorderMode: Boolean = false,
    selectedMainButtonForSwapId: String? = null,
    onExitReorderMode: () -> Unit = {},
    onConfirmReorderMode: () -> Unit = {},
    onSelectButtonForSwap: (String) -> Unit = {}
) {
    val displayCategories = remember(mainButtonConfig) {
        mainButtonConfig.buttons.sortedBy { it.order }.map { it.category }
    }

    val visibleCategories = remember(displayCategories, activeCategory, pinnedQuickCategory) {
        val pinIsSchoolOrCompany = pinnedQuickCategory == "SCHOOL" || pinnedQuickCategory == "COMPANY"
        displayCategories.filterNot { category ->
            category == activeCategory || (pinIsSchoolOrCompany && category == pinnedQuickCategory)
        }
    }

    val gridCategories = if (isMainButtonReorderMode) displayCategories else visibleCategories
    val rowCount = (gridCategories.size + 1) / 2
    val gridHeight = (rowCount * 84 + (rowCount - 1) * 12 + 8).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMainButtonReorderMode) "자리 바꾸기" else "활동 시작",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Text(
                    text = statusMessage ?: "기록할 활동을 선택하세요.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (isMainButtonReorderMode) {
                TextButton(onClick = onConfirmReorderMode) {
                    Text(
                        text = "완료",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlowPurple
                    )
                }
            }
        }

        Text(
            text = "활동",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = FlowMuted,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
            contentPadding = PaddingValues(0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            userScrollEnabled = false
        ) {
            items(
                items = gridCategories,
                key = { category -> category }
            ) { category ->
                if (isMainButtonReorderMode) {
                    CategoryButton(
                        category = category,
                        label = displayCategory(category),
                        isSelected = category == selectedMainButtonForSwapId,
                        onClick = { onSelectButtonForSwap(category) },
                        onLongClick = null,
                        modifier = Modifier.animateItem()
                    )
                } else {
                    CategoryButton(
                        category = category,
                        label = displayCategory(category),
                        onClick = {
                            when {
                                category == "SCHOOL" || category == "COMPANY" -> {
                                    if (pinnedQuickCategory != category) {
                                        onPinQuickCategory(category)
                                    }
                                }
                                category != activeCategory -> onStart(category)
                            }
                        },
                        onLongClick = { onLongClickButton(category) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecommendationBannerCard(
    category: String,
    activityName: String,
    reasonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(0xFF00BCD4)
    val cardBg = Color(0xFFE2F5F5)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = category,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = activityName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FlowInk
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "추천 · $reasonText",
                    fontSize = 11.sp,
                    color = FlowMuted
                )
            }
        }
    }
}

@Composable
internal fun FlowPageDots(activePage: Int) {
    Row(
        modifier = Modifier.padding(top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index == activePage) FlowPurple else Color(0xFFC8CBD4),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
internal fun ActivityRecommendationRow(
    category: String,
    activityName: String,
    isCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FlowPurpleSoft.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = if (isCompleted) Color(0xFF18A058) else FlowPurple,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "추천 흐름",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = FlowMuted
            )
            Text(
                text = when {
                    isCompleted -> "$activityName · 완료"
                    else -> activityName
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FlowMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActivityRecommendationSheet(
    category: String,
    activityName: String,
    reasonText: String?,
    onDismiss: () -> Unit,
    onStart: () -> Unit,
    onComplete: () -> Unit,
    isCompleted: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        contentColor = FlowInk
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(categoryColor(category).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryGlyph(
                    category = category,
                    tint = categoryColor(category),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = activityName,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = FlowInk
            )
            if (!reasonText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = reasonText,
                    fontSize = 14.sp,
                    color = FlowMuted,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onStart,
                enabled = !isCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FlowPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (isCompleted) "완료됨" else "시작하기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(
                onClick = onComplete,
                enabled = !isCompleted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isCompleted) "완료됨" else "완료로 표시",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FlowMuted
                )
            }
        }
    }
}


