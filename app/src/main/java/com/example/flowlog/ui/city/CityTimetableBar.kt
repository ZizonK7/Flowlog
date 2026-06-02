package com.example.flowlog.ui.city

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flowlog.data.model.ActivitySession
import java.util.concurrent.TimeUnit

private const val TAG = "CityTimetable"

// ── 색상 ──────────────────────────────────────────────────────────────
private val CityInk     = Color(0xFF10182C)
private val CityMuted   = Color(0xFF697386)
private val SkyColor    = Color(0xFFB3E5FC)
private val RiverColor  = Color(0xFF29B6F6)
private val GroundColor = Color(0xFF8BC34A)

// ── 레이아웃 상수 ────────────────────────────────────────────────────
private const val DP_PER_MINUTE = 1.8f

/**
 * 레이어 분리 (위 → 아래):
 *   SKY    : 0 ~ riverTop          ← 하늘
 *   RIVER  : riverTop ~ groundTop  ← 보트
 *   GROUND : groundTop ~ stripH    ← 건물 + 카펫
 */
private val STRIP_HEIGHT_DP  = 210.dp
private val GROUND_HEIGHT_DP = 45.dp
private val RIVER_HEIGHT_DP  = 55.dp

/** 건물 기본 렌더링 크기 (BUILDING / UPGRADED_BUILDING 모드) */
private val BUILDING_BASE_DP  = 52.dp
/** 소품/아이콘 기본 렌더링 크기 (SMALL_ICON 모드) */
private val SMALL_ICON_BASE_DP = 24.dp
/** 이미지가 표시될 때 최소 크기 */
private val MIN_ICON_DP = 12.dp

/**
 * 폭 기반 downgrade 임계값:
 *   segment 폭이 이보다 좁으면 UPGRADED_BUILDING → BUILDING
 */
private val THRESHOLD_UPGRADED_DP = 84.dp
/**
 *   segment 폭이 이보다 좁으면 BUILDING → SMALL_ICON
 */
private val THRESHOLD_BUILDING_DP = 60.dp

const val BOAT_DP_NORMAL = 56
const val BOAT_DP_BIG    = 76

// ── 안전한 coerceIn 헬퍼 ─────────────────────────────────────────────
/** max < min 이면 min 을 반환해 IllegalArgumentException 을 방지합니다. */
private fun Float.safeCoerceIn(min: Float, max: Float): Float =
    if (max < min) min else this.coerceIn(min, max)

// ── 유틸 ──────────────────────────────────────────────────────────────

private fun shortLabel(category: String) = when (category) {
    "SLEEP"       -> "수면"
    "MEAL"        -> "식사"
    "STUDY"       -> "공부"
    "WORK"        -> "작업"
    "DEVELOPMENT" -> "개발"
    "SCHOOL"      -> "학교"
    "COMPANY"     -> "회사"
    "EXERCISE"    -> "운동"
    "WASH"        -> "씻기"
    "REST"        -> "휴식"
    "ETC"         -> "기타"
    else          -> category.take(3)
}

/** 카테고리 배지 Android ARGB int (alpha=FF) */
fun categoryBadgeColor(category: String): Int = when (category) {
    "SLEEP"       -> 0xFF5E35B1.toInt()
    "MEAL"        -> 0xFFEF6C00.toInt()
    "STUDY"       -> 0xFF2E7D32.toInt()
    "WORK"        -> 0xFF6D4C41.toInt()
    "DEVELOPMENT" -> 0xFF0277BD.toInt()
    "SCHOOL"      -> 0xFF37474F.toInt()
    "COMPANY"     -> 0xFF1565C0.toInt()
    "EXERCISE"    -> 0xFF558B2F.toInt()
    "WASH"        -> 0xFF00838F.toInt()
    "REST"        -> 0xFF00897B.toInt()
    "ETC"         -> 0xFF78909C.toInt()
    else          -> 0xFF9E9E9E.toInt()
}

// ── 에셋 이미지 로더 ─────────────────────────────────────────────────

/**
 * assets 에서 ImageBitmap 을 로드합니다.
 * 실패 시 null 반환 + Log.w. 절대 !! 강제 unwrap 하지 않습니다.
 */
@Composable
fun rememberAssetBitmap(fileName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(fileName) {
        loadAssetImageOrNull(context.assets, fileName)
    }
}

private fun loadAssetImageOrNull(
    assets: android.content.res.AssetManager,
    assetName: String
): ImageBitmap? = runCatching {
    assets.open(assetName).use { stream ->
        BitmapFactory.decodeStream(stream)?.asImageBitmap()
            ?: run { Log.w(TAG, "BitmapFactory null for $assetName"); null }
    }
}.getOrElse { e ->
    Log.w(TAG, "Asset load failed: $assetName — ${e.message}")
    null
}

@Composable
private fun rememberTileCache(names: Set<String>): Map<String, ImageBitmap> {
    val context = LocalContext.current
    return remember(names) {
        names.mapNotNull { name ->
            loadAssetImageOrNull(context.assets, name)?.let { name to it }
        }.toMap()
    }
}

// ── segment 당 계산 데이터 ────────────────────────────────────────────

/**
 * 세그먼트 하나의 렌더링에 필요한 사전 계산 데이터.
 *
 * @param rawMode      duration 기반 시각 모드 (width 보정 전)
 * @param effectiveMode width 보정 후 실제 렌더링 모드 (rawMode ≥ effectiveMode)
 * @param iconPx       이미지를 그릴 때 사용할 실제 픽셀 크기 (CARPET_ONLY = 0)
 */
private data class SegmentRenderData(
    val rawMode: CitySegmentVisualMode,
    val effectiveMode: CitySegmentVisualMode,
    val iconPx: Float
)

/** density 기반으로 세그먼트별 렌더 데이터를 계산합니다 (remember 밖에서 호출 가능). */
private fun computeSegmentRenderData(
    category: String,
    durationMillis: Long,
    segWPx: Float,
    buildingBasePx: Float,
    smallIconBasePx: Float,
    minIconPx: Float,
    thresholdBuildingPx: Float,
    thresholdUpgradedPx: Float,
    marginPx: Float
): SegmentRenderData {
    val rawMode = CityTimetableAssets.visualModeFor(category, durationMillis)

    // width 기반 downgrade: segment 폭이 임계값 미만이면 한 단계 내림
    val effectiveMode = when {
        rawMode == CitySegmentVisualMode.UPGRADED_BUILDING && segWPx < thresholdUpgradedPx ->
            CitySegmentVisualMode.BUILDING
        rawMode == CitySegmentVisualMode.BUILDING && segWPx < thresholdBuildingPx ->
            CitySegmentVisualMode.SMALL_ICON
        else -> rawMode
    }

    val iconPx = when (effectiveMode) {
        CitySegmentVisualMode.CARPET_ONLY -> 0f
        CitySegmentVisualMode.SMALL_ICON ->
            (smallIconBasePx.coerceAtMost(segWPx - marginPx)).coerceAtLeast(minIconPx)
        CitySegmentVisualMode.BUILDING,
        CitySegmentVisualMode.UPGRADED_BUILDING ->
            (buildingBasePx.coerceAtMost(segWPx - marginPx)).coerceAtLeast(minIconPx)
    }

    return SegmentRenderData(rawMode, effectiveMode, iconPx)
}

// ── 핵심 Composable ────────────────────────────────────────────────────

/**
 * 도시 타임테이블 바.
 *
 * @param activities        활동 세션 목록
 * @param currentTimeMillis 보트 위치 기준 시각 (null → 스트립 중앙)
 * @param showLabels        세그먼트 아래 라벨 (카테고리·duration·visual mode) 표시
 * @param showBadges        건물 위 카테고리 배지 표시
 * @param bigBoat           보트 크기 확대
 */
@Composable
fun CityTimetableBar(
    activities: List<ActivitySession>,
    currentTimeMillis: Long? = System.currentTimeMillis(),
    showLabels: Boolean = true,
    showBadges: Boolean = true,
    bigBoat: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (activities.isEmpty()) {
        Text(
            text = "샘플 데이터 없음",
            fontSize = 12.sp, color = CityMuted,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    val sorted = remember(activities) {
        activities.filter { it.durationMillis > 0L }.sortedBy { it.startTime }
    }
    if (sorted.isEmpty()) {
        Text(
            text = "유효한 세그먼트 없음 (duration ≤ 0)",
            fontSize = 12.sp, color = CityMuted,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    // 세그먼트별 가로 너비 (dp), 최소 4dp 보장
    val segWidths: List<Dp> = remember(sorted) {
        sorted.map { a ->
            (TimeUnit.MILLISECONDS.toMinutes(a.durationMillis).coerceAtLeast(1L) * DP_PER_MINUTE).dp
                .coerceAtLeast(4.dp)
        }
    }
    val totalWidthDp = remember(segWidths) { segWidths.fold(0.dp) { acc, w -> acc + w } }

    val tileNames = remember(sorted) {
        sorted.map { CityTimetableAssets.tileFor(it.category, it.durationMillis) }.toSet() +
            CityTimetableAssets.CHARACTER_BOAT
    }
    val tiles = rememberTileCache(tileNames)

    val density = LocalDensity.current

    // px 상수 (Composable 내 한 번만 계산)
    val stripPx           = with(density) { STRIP_HEIGHT_DP.toPx() }
    val groundPx          = with(density) { GROUND_HEIGHT_DP.toPx() }
    val riverPx           = with(density) { RIVER_HEIGHT_DP.toPx() }
    val buildingBasePx    = with(density) { BUILDING_BASE_DP.toPx() }
    val smallIconBasePx   = with(density) { SMALL_ICON_BASE_DP.toPx() }
    val minIconPx         = with(density) { MIN_ICON_DP.toPx() }
    val thresholdBuildPx  = with(density) { THRESHOLD_BUILDING_DP.toPx() }
    val thresholdUpgdPx   = with(density) { THRESHOLD_UPGRADED_DP.toPx() }
    val boatPx            = with(density) { (if (bigBoat) BOAT_DP_BIG else BOAT_DP_NORMAL).dp.toPx() }
    val totalWidthPx      = with(density) { totalWidthDp.toPx() }
    val dpPx              = with(density) { 1.dp.toPx() }
    val marginPx          = 4f * dpPx    // 아이콘 좌우 여백

    val groundTop = stripPx - groundPx
    val riverTop  = groundTop - riverPx

    // 세그먼트 시작 X (px)
    val segStartXs: List<Float> = remember(segWidths, density) {
        val xs = mutableListOf<Float>()
        var acc = 0f
        segWidths.forEach { w -> xs += acc; acc += with(density) { w.toPx() } }
        xs
    }

    // 세그먼트별 렌더 데이터 (rawMode, effectiveMode, iconPx)
    // — 이 리스트를 Canvas 와 라벨 Row 양쪽에서 공유합니다.
    val segRenderData: List<SegmentRenderData> = remember(
        sorted, segWidths, density,
        buildingBasePx, smallIconBasePx, minIconPx, thresholdBuildPx, thresholdUpgdPx
    ) {
        sorted.mapIndexed { i, activity ->
            val w = with(density) { segWidths[i].toPx() }.coerceAtLeast(1f)
            computeSegmentRenderData(
                category          = activity.category,
                durationMillis    = activity.durationMillis,
                segWPx            = w,
                buildingBasePx    = buildingBasePx,
                smallIconBasePx   = smallIconBasePx,
                minIconPx         = minIconPx,
                thresholdBuildingPx = thresholdBuildPx,
                thresholdUpgradedPx = thresholdUpgdPx,
                marginPx          = marginPx
            )
        }
    }

    // 현재 시각 X (px)
    val windowStart = sorted.first().startTime
    val windowEnd   = sorted.last().endTime.coerceAtLeast(windowStart + 60_000L)
    val currentX: Float? = currentTimeMillis?.let { now ->
        if (now < windowStart || now > windowEnd) null
        else with(density) {
            (TimeUnit.MILLISECONDS.toMinutes(now - windowStart).toFloat() * DP_PER_MINUTE).dp.toPx()
        }
    }

    // Canvas 와 라벨 Row 가 공유하는 스크롤 상태
    val scrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {

        // ── 도시 Canvas ─────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Canvas(modifier = Modifier.width(totalWidthDp).height(STRIP_HEIGHT_DP)) {

                // 1. 하늘
                drawRect(color = SkyColor, size = Size(totalWidthPx, riverTop.coerceAtLeast(0f)))

                // 2. 강
                if (riverPx > 0f) {
                    drawRect(
                        color = RiverColor.copy(alpha = 0.78f),
                        topLeft = Offset(0f, riverTop),
                        size = Size(totalWidthPx, riverPx)
                    )
                    drawLine(
                        color = Color(0x55FFFFFF),
                        start = Offset(0f, riverTop + riverPx * 0.55f),
                        end   = Offset(totalWidthPx, riverTop + riverPx * 0.55f),
                        strokeWidth = 2f * dpPx
                    )
                }

                // 3. 지면
                drawRect(
                    color = GroundColor,
                    topLeft = Offset(0f, groundTop),
                    size = Size(totalWidthPx, groundPx)
                )
                drawLine(
                    color = Color(0x80395214),
                    start = Offset(0f, groundTop),
                    end   = Offset(totalWidthPx, groundTop),
                    strokeWidth = 1.5f * dpPx
                )

                // 4. 세그먼트
                sorted.forEachIndexed { i, activity ->
                    val segX        = segStartXs[i]
                    val segW        = with(density) { segWidths[i].toPx() }.coerceAtLeast(1f)
                    val segCenterX  = segX + segW / 2f
                    val rd          = segRenderData[i]

                    // 4a. 카펫 오버레이
                    drawRect(
                        color = Color(CityTimetableAssets.overlayColorFor(activity.category).toInt())
                            .copy(alpha = 0.80f),
                        topLeft = Offset(segX, groundTop),
                        size    = Size(segW, groundPx)
                    )

                    // 4b. 경계선
                    if (i > 0) {
                        drawLine(
                            color = Color(0x60000000),
                            start = Offset(segX, riverTop),
                            end   = Offset(segX, stripPx),
                            strokeWidth = 1f * dpPx
                        )
                    }

                    // 4c. 건물/아이콘 이미지
                    // CARPET_ONLY 모드이면 iconPx == 0 → 타일 렌더링 skip
                    if (rd.effectiveMode != CitySegmentVisualMode.CARPET_ONLY && rd.iconPx > 0f) {
                        val tileName = CityTimetableAssets.tileFor(activity.category, activity.durationMillis)
                        tiles[tileName]?.let { bmp ->
                            val iconSize = rd.iconPx
                            val bBottom  = groundTop + 4f * dpPx   // 지면에 살짝 박힘
                            val bTop     = bBottom - iconSize

                            // ★ safeCoerceIn 으로 segW < iconSize 시 범위 역전 방지
                            val bLeft = (segCenterX - iconSize / 2f)
                                .safeCoerceIn(segX, segX + segW - iconSize)

                            val drawSize = iconSize.toInt().coerceAtLeast(1)
                            drawImage(
                                image     = bmp,
                                dstOffset = IntOffset(bLeft.toInt(), bTop.toInt()),
                                dstSize   = IntSize(drawSize, drawSize),
                                filterQuality = FilterQuality.None
                            )

                            // 4d. 카테고리 배지 (showBadges)
                            if (showBadges) {
                                drawIntoCanvas { canvas ->
                                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = categoryBadgeColor(activity.category)
                                        style = Paint.Style.FILL
                                    }
                                    val txtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 8.5f * dpPx
                                        typeface = Typeface.DEFAULT_BOLD
                                        textAlign = Paint.Align.CENTER
                                    }
                                    val label = shortLabel(activity.category)
                                    val pH    = 4f * dpPx
                                    val pV    = 2.5f * dpPx
                                    val tw    = txtPaint.measureText(label)
                                    val maxBw = (segW - 4f * dpPx).coerceAtLeast(0f)
                                    val bw    = (tw + pH * 2).coerceAtMost(maxBw).coerceAtLeast(0f)

                                    if (bw > 0f) {
                                        val bh   = (txtPaint.textSize + pV * 2).coerceAtLeast(1f)
                                        val cxMin = segX + bw / 2 + 2f * dpPx
                                        val cxMax = (segX + segW - bw / 2 - 2f * dpPx).coerceAtLeast(cxMin)
                                        val cx    = segCenterX.coerceIn(cxMin, cxMax)
                                        val bdTop = bTop - bh - 3f * dpPx
                                        val rect  = RectF(cx - bw / 2, bdTop, cx + bw / 2, bdTop + bh)
                                        canvas.nativeCanvas.drawRoundRect(rect, 4f * dpPx, 4f * dpPx, bgPaint)
                                        canvas.nativeCanvas.drawText(
                                            label, cx,
                                            bdTop + pV + txtPaint.textSize - txtPaint.descent(), txtPaint
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. 현재 시각 마커
                if (currentX != null) {
                    drawLine(
                        color = Color(0xFFFF5252),
                        start = Offset(currentX, 0f),
                        end   = Offset(currentX, stripPx),
                        strokeWidth = 2.5f * dpPx
                    )
                }

                // 6. 보트 — 강 레이어 중앙
                val boatX = currentX ?: (totalWidthPx / 2f)
                tiles[CityTimetableAssets.CHARACTER_BOAT]?.let { bmp ->
                    val bx     = (boatX - boatPx / 2f).safeCoerceIn(0f, totalWidthPx - boatPx)
                    val by     = (riverTop + riverPx / 2f) - boatPx / 2f
                    val bSize  = boatPx.toInt().coerceAtLeast(1)
                    drawImage(
                        image     = bmp,
                        dstOffset = IntOffset(bx.toInt(), by.toInt()),
                        dstSize   = IntSize(bSize, bSize),
                        filterQuality = FilterQuality.None
                    )
                }
            }
        }

        // ── 라벨 Row (동일 scrollState → Canvas 와 스크롤 동기화) ────────
        if (showLabels) {
            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                Row(modifier = Modifier.width(totalWidthDp)) {
                    sorted.forEachIndexed { i, activity ->
                        val durationMins = TimeUnit.MILLISECONDS.toMinutes(activity.durationMillis)
                        val rd = segRenderData[i]
                        Box(
                            modifier = Modifier
                                .width(segWidths[i])
                                .padding(horizontal = 1.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = shortLabel(activity.category),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(categoryBadgeColor(activity.category)),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "${durationMins}분",
                                    fontSize = 8.sp,
                                    color = CityMuted,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                // ── 디버그: visual mode 표시 ──────────────
                                Text(
                                    text = rd.effectiveMode.label(),
                                    fontSize = 7.sp,
                                    color = CityMuted.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── 카드 래퍼 ────────────────────────────────────────────────────────

@Composable
fun CityTimetableCard(
    activities: List<ActivitySession>,
    currentTimeMillis: Long? = System.currentTimeMillis(),
    showLabels: Boolean = true,
    showBadges: Boolean = true,
    bigBoat: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "도시 타임테이블",
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = CityInk, modifier = Modifier.padding(bottom = 2.dp)
            )
            Text(
                text = "← 좌우 스크롤 · 보트 ${if (bigBoat) BOAT_DP_BIG else BOAT_DP_NORMAL}dp",
                fontSize = 10.sp, color = CityMuted,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            CityTimetableBar(
                activities = activities,
                currentTimeMillis = currentTimeMillis,
                showLabels = showLabels,
                showBadges = showBadges,
                bigBoat = bigBoat
            )
        }
    }
}
