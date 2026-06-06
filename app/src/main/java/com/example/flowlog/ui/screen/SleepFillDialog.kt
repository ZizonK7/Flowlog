package com.example.flowlog.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import kotlin.math.abs

private val SleepPurple = Color(0xFF5140D8)
private val SleepPurpleSoft = Color(0xFFEDE9FF)
private val SleepInk = Color(0xFF10182C)
private val SleepMuted = Color(0xFF697386)
private val SleepError = Color(0xFFD32F2F)

// 홀수 개 아이템 → 중앙 아이템 하나로 결정
private const val VISIBLE_ITEM_COUNT = 5
private val ITEM_HEIGHT = 40.dp
private val PICKER_HEIGHT = ITEM_HEIGHT * VISIBLE_ITEM_COUNT          // 200.dp
private val CONTENT_PADDING = ITEM_HEIGHT * ((VISIBLE_ITEM_COUNT - 1) / 2)  // 80.dp

// ─────────────────────────────────────────────────────────────────────────────
// 다이얼로그
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SleepFillConfirmDialog(
    emptyRange: EmptyRange,
    allActivities: List<ActivitySession>,
    onDismiss: () -> Unit,
    onConfirm: (startMillis: Long, endMillis: Long) -> Unit
) {
    val options = remember(emptyRange) { buildTimeOffsetOptions(emptyRange) }

    var startOffsetMin by remember(emptyRange) { mutableStateOf(options.first()) }
    var endOffsetMin by remember(emptyRange) { mutableStateOf(options.last()) }

    val currentStart = emptyRange.offsetToMillis(startOffsetMin)
    val currentEnd = emptyRange.offsetToMillis(endOffsetMin)
    val isValid = validateSleepRange(currentStart, currentEnd, emptyRange, allActivities)

    fun onStartSelect(newStart: Int) {
        startOffsetMin = newStart
        if (endOffsetMin <= newStart) {
            endOffsetMin = options.firstOrNull { it > newStart } ?: newStart
        }
    }

    fun onEndSelect(newEnd: Int) {
        endOffsetMin = newEnd
        if (startOffsetMin >= newEnd) {
            startOffsetMin = options.lastOrNull { it < newEnd } ?: newEnd
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Bedtime,
                    contentDescription = null,
                    tint = SleepPurple,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "기록이 비어 있어요.\n혹시 잠든 시간이었나요?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SleepInk,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatSleepRangeLabel(currentStart, currentEnd),
                    fontSize = 13.sp,
                    color = SleepPurple,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SleepTimePickerCard(
                        title = "시작",
                        options = options,
                        selectedValue = startOffsetMin,
                        formatter = { offset ->
                            formatTimeOption(emptyRange.offsetToMillis(offset), emptyRange.startMillis)
                        },
                        onSelect = { onStartSelect(it) },
                        modifier = Modifier.weight(1f)
                    )
                    SleepTimePickerCard(
                        title = "종료",
                        options = options,
                        selectedValue = endOffsetMin,
                        formatter = { offset ->
                            formatTimeOption(emptyRange.offsetToMillis(offset), emptyRange.startMillis)
                        },
                        onSelect = { onEndSelect(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isValid) {
                    Text(
                        text = "다른 기록과 겹칠 수 없어요.\n비어 있는 시간 안에서만 선택해 주세요.",
                        fontSize = 12.sp,
                        color = SleepError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(currentStart, currentEnd) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleepPurple,
                    disabledContainerColor = SleepPurpleSoft
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "수면으로 기록",
                    fontWeight = FontWeight.Bold,
                    color = if (isValid) Color.White else SleepMuted
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = SleepMuted)
            ) {
                Text("취소")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 피커 카드
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SleepTimePickerCard(
    title: String,
    options: List<Int>,
    selectedValue: Int,
    formatter: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = SleepMuted,
            fontWeight = FontWeight.SemiBold
        )
        SleepWheelPicker(
            pickerTitle = title,
            options = options,
            selectedValue = selectedValue,
            formatter = formatter,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 핵심 피커
// ─────────────────────────────────────────────────────────────────────────────
//
// 설계 원칙
//   centeredIndex  — 스크롤 중 강조 전용. onSelect 미호출.
//   onSelect       — 스크롤 '완전 정지(prevScrolling→!isScrolling)' 시 한 번만 호출.
//   snapInFlight   — animateScrollToItem 종료 후 isScrollInProgress false 재전환이
//                    "새로운 scroll stop"으로 오인되는 것을 막는 플래그.
//   rememberUpdatedState — LaunchedEffect(listState) 내부에서 selectedValue/onSelect가
//                          항상 최신값을 가리키도록 보장 (stale closure 방지).
//
// 좌표계
//   viewportCenter = viewportStartOffset + (viewportEndOffset - viewportStartOffset) / 2
//   item.offset    — LazyListItemInfo.offset, 같은 viewport 좌표계
//   contentPadding은 item.offset에 이미 반영되어 있으므로 별도 보정 불필요.
//
@Composable
private fun SleepWheelPicker(
    pickerTitle: String,
    options: List<Int>,
    selectedValue: Int,
    formatter: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = options.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    var centeredIndex by remember(selectedIndex) { mutableStateOf(selectedIndex) }

    // stale closure 방지: LaunchedEffect(listState) 내부에서 항상 최신값 사용
    val latestSelectedValue = rememberUpdatedState(selectedValue)
    val latestOnSelect = rememberUpdatedState(onSelect)

    // 내부 커밋 추적: onSelect로 올린 값이 selectedValue로 돌아올 때 LaunchedEffect re-animation 방지
    val lastCommittedValue = remember { mutableStateOf<Int?>(null) }

    // ── Effect 1: 외부 selectedValue 변경(다른 피커 연동) → 스크롤 동기화 ──
    LaunchedEffect(selectedValue) {
        Log.d("SleepFillPicker", "[$pickerTitle] ◆ LaunchedEffect selectedValue=$selectedValue lastCommitted=${lastCommittedValue.value}")
        if (lastCommittedValue.value == selectedValue) {
            lastCommittedValue.value = null
            Log.d("SleepFillPicker", "[$pickerTitle]   → 내부 커밋 → re-animation 스킵")
            return@LaunchedEffect
        }
        if (!listState.isScrollInProgress) {
            val idx = options.indexOf(selectedValue).coerceAtLeast(0)
            Log.d("SleepFillPicker", "[$pickerTitle]   → 외부 변경 animateScrollToItem($idx) label=${formatter(selectedValue)}")
            listState.animateScrollToItem(idx)
            centeredIndex = idx
        }
    }

    // ── Effect 2: 강조 갱신 + 스크롤 정지 시 커밋·snap ──
    LaunchedEffect(listState) {
        var prevScrolling = false
        // animateScrollToItem 실행 후 isScrollInProgress false 재전환을
        // "사용자 스크롤 정지"로 오인하지 않도록 차단하는 플래그
        var snapInFlight = false

        snapshotFlow {
            val info = listState.layoutInfo
            val viewportCenter = info.viewportStartOffset +
                (info.viewportEndOffset - info.viewportStartOffset) / 2
            val closest = info.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }
            Triple(closest?.index, listState.isScrollInProgress, viewportCenter)
        }.collect { (closestIdx, isScrolling, viewportCenter) ->

            val clamped = closestIdx?.coerceIn(0, options.lastIndex)

            // 강조 인덱스 실시간 갱신
            if (clamped != null && clamped != centeredIndex) {
                Log.d("SleepFillPicker", "[$pickerTitle] centeredIndex $centeredIndex → $clamped (scrolling=$isScrolling)")
                centeredIndex = clamped
            }

            // 스크롤 정지 감지 (prevScrolling=true → isScrolling=false)
            if (prevScrolling && !isScrolling) {
                if (snapInFlight) {
                    // snap 애니메이션이 끝나서 발생한 false → 재커밋 차단
                    Log.d("SleepFillPicker", "[$pickerTitle] snap 완료 이벤트 → 재커밋 차단, snapInFlight 해제")
                    snapInFlight = false
                } else if (clamped != null) {
                    // 사용자 스크롤 정지 → 커밋 + snap
                    val info = listState.layoutInfo
                    Log.d("SleepFillPicker", "[$pickerTitle] ★ scroll stopped")
                    Log.d("SleepFillPicker", "[$pickerTitle]   viewportStartOffset=${info.viewportStartOffset} viewportEndOffset=${info.viewportEndOffset} viewportCenter=$viewportCenter")
                    info.visibleItemsInfo.forEach { item ->
                        val itemCenter = item.offset + item.size / 2
                        val label = options.getOrNull(item.index)?.let { formatter(it) } ?: "?"
                        Log.d("SleepFillPicker", "[$pickerTitle]   item[${item.index}] offset=${item.offset} size=${item.size} center=$itemCenter label=$label dist=${abs(itemCenter - viewportCenter)}")
                    }
                    Log.d("SleepFillPicker", "[$pickerTitle]   committedIndex=$clamped label=${formatter(options[clamped])}")

                    // snap: animateScrollToItem(N, 0)은 item N의 offset = contentPadding → 뷰포트 중앙
                    snapInFlight = true
                    listState.animateScrollToItem(clamped)
                    // animateScrollToItem 이 suspend로 완료된 뒤 commit 수행
                    // (이 시점에서 true→false 전환은 이미 buffer에 쌓였고 snapInFlight로 차단됨)

                    val newValue = options[clamped]
                    val curSelected = latestSelectedValue.value
                    Log.d("SleepFillPicker", "[$pickerTitle]   committedValue=${formatter(newValue)} selectedValue(before)=${formatter(curSelected)}")
                    if (newValue != curSelected) {
                        lastCommittedValue.value = newValue
                        Log.d("SleepFillPicker", "[$pickerTitle]   onValueChange(${formatter(newValue)}) 호출")
                        latestOnSelect.value(newValue)
                    } else {
                        Log.d("SleepFillPicker", "[$pickerTitle]   onValueChange 스킵 (동일값)")
                    }
                }
            }

            prevScrolling = isScrolling
        }
    }

    // ── 렌더링 ──
    Box(
        modifier = modifier
            .height(PICKER_HEIGHT)
            .background(Color(0xFFF7F6FF), RoundedCornerShape(14.dp))
    ) {
        // 선택 영역 하이라이트 — Box.align(Center)로 항상 뷰포트 정중앙에 고정
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .padding(horizontal = 6.dp)
                .background(SleepPurpleSoft, RoundedCornerShape(10.dp))
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = CONTENT_PADDING),
            userScrollEnabled = true
        ) {
            itemsIndexed(options) { index, value ->
                val isSelected = index == centeredIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatter(value),
                        fontSize = if (isSelected) 16.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) SleepPurple else SleepMuted.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
