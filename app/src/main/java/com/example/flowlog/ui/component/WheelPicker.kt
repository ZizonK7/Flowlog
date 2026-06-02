package com.example.flowlog.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPickerColumn(
    values: List<Int>,
    selectedValue: Int,
    formatter: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    selectedHighlightColor: Color = Color(0xFF5140D8).copy(alpha = 0.44f),
    selectedTextColor: Color = Color(0xFF27324D),
    unselectedTextColor: Color = Color(0xFF697386).copy(alpha = 0.45f),
    backgroundColor: Color = Color(0xFFFAFAFD)
) {
    val itemHeight = 42.dp
    val cycleCount = 101
    val middleCycle = cycleCount / 2
    val totalItems = values.size * cycleCount
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val initialIndex = (middleCycle * values.size + selectedIndex).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    var centeredItemIndex by remember { mutableStateOf(initialIndex) }

    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestSelectedValue by rememberUpdatedState(selectedValue)

    LaunchedEffect(listState, values) {
        var prevScrolling = false
        snapshotFlow { listState.centeredVisibleItemIndex(totalItems) to listState.isScrollInProgress }
            .collect { (centeredIndex, isScrollInProgress) ->
                if (centeredIndex == null) return@collect
                centeredItemIndex = centeredIndex
                val centeredValue = values[centeredIndex.floorMod(values.size)]
                if (centeredValue != latestSelectedValue) {
                    latestOnSelect(centeredValue)
                }
                if (prevScrolling && !isScrollInProgress) {
                    val targetIndex = centeredIndex.coerceIn(0, totalItems - 1)
                    if (listState.firstVisibleItemIndex != targetIndex || listState.firstVisibleItemScrollOffset != 0) {
                        listState.animateScrollToItem(index = targetIndex)
                    }
                }
                prevScrolling = isScrollInProgress
            }
    }

    Box(
        modifier = modifier
            .width(132.dp)
            .height(246.dp)
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .padding(vertical = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .padding(horizontal = 5.dp)
                .background(selectedHighlightColor, RoundedCornerShape(13.dp))
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight * 2)
        ) {
            items(totalItems) { index ->
                val value = values[index.floorMod(values.size)]
                val selected = index == centeredItemIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .combinedClickable(onClick = { onSelect(value) }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatter(value),
                        fontSize = if (selected) 23.sp else 22.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) selectedTextColor else unselectedTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun PickerWaveBackground(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val first = Path().apply {
            moveTo(0f, size.height * 0.35f)
            cubicTo(
                size.width * 0.28f,
                size.height * 0.18f,
                size.width * 0.52f,
                size.height * 0.74f,
                size.width,
                size.height * 0.34f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(first, color)
    }
}

private fun Int.floorMod(other: Int): Int = ((this % other) + other) % other

private fun LazyListState.centeredVisibleItemIndex(totalItems: Int): Int? {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return visibleItems
        .minByOrNull { item -> abs((item.offset + item.size / 2) - viewportCenter) }
        ?.index
        ?.coerceIn(0, totalItems - 1)
}
