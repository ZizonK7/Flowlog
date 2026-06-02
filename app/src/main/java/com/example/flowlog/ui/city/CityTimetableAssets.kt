package com.example.flowlog.ui.city

import java.util.concurrent.TimeUnit

/**
 * segment 하나를 어떤 시각적 수준으로 렌더링할지 결정하는 모드.
 *
 * CARPET_ONLY       : 타일 이미지 없음. 카테고리 색상 카펫만 표시.
 * SMALL_ICON        : 소품/아이콘 크기(24dp). 너비가 좁은 segment 또는 짧은 활동.
 * BUILDING          : 일반 건물(52dp). 중간 duration.
 * UPGRADED_BUILDING : 업그레이드 건물(52dp, 다른 타일). 생산 활동 2h 이상 / COMPANY 8h 이상.
 */
enum class CitySegmentVisualMode {
    CARPET_ONLY,
    SMALL_ICON,
    BUILDING,
    UPGRADED_BUILDING;

    /** 디버그 라벨에 표시할 짧은 문자열 */
    fun label(): String = when (this) {
        CARPET_ONLY       -> "카펫"
        SMALL_ICON        -> "아이콘"
        BUILDING          -> "건물"
        UPGRADED_BUILDING -> "업그레이드"
    }
}

/**
 * Kenney Tiny Battle 타일셋 (tile_0000.png ~ tile_0197.png) 기반 매핑.
 *
 * 타일 선택 기준 요약:
 *   - 0000: 잔디 (기본 바닥 / fallback)
 *   - 0002: 교차로 광장 (ETC)
 *   - 0005: 덤불 소품 (STUDY 소품)
 *   - 0006: 돌 소품 (WORK 소품)
 *   - 0007: 파이프 소품 (DEVELOPMENT 소품)
 *   - 0008: 회색 소형 건물 (WORK 기본)
 *   - 0009: 회색 건물 변형 (SLEEP 소형)
 *   - 0011: 대형 회색 건물 (WORK 업그레이드)
 *   - 0013: 어두운 대형 건물 (SLEEP 장시간)
 *   - 0022: 밝은 파란 물 (WASH)
 *   - 0026: 초록 건물 창문 2개 (STUDY 기본 / SCHOOL 소형)
 *   - 0027: 초록 건물 간판 (REST 카페)
 *   - 0029: 초록 식생 (EXERCISE / REST 정원 소품)
 *   - 0044: 파란 다창문 건물 (DEVELOPMENT 기본 / COMPANY 소형)
 *   - 0045: 파란 고층 건물 (COMPANY 4h)
 *   - 0062: 붉은 건물 다창문 (MEAL 기본)
 *   - 0097: 회색 공장/실험실 (DEVELOPMENT 업그레이드)
 *   - 0115: 초록 공장 건물 (STUDY 업그레이드)
 *   - 0133: 파란 대형 건물 (COMPANY 8h 본사)
 *   - 0151: 붉은 건물 변형 (MEAL 업그레이드)
 */
object CityTimetableAssets {

    const val CHARACTER_BOAT = "main_character_boat.png"
    const val GROUND_TILE    = "tile_0000.png"
    const val WATER_TILE     = "tile_0022.png"
    private  const val FALLBACK_TILE = "tile_0000.png"

    // ── visual mode 결정 ────────────────────────────────────────────────
    /**
     * 카테고리와 duration 으로 렌더링 모드를 결정합니다.
     *
     * 이 함수가 반환하는 모드는 CityTimetableBar 에서 segment 너비와
     * 비교해 추가로 downgrade 될 수 있습니다.
     * (예: BUILDING 인데 segment 가 60dp 미만이면 → SMALL_ICON)
     *
     * 카테고리별 예외 규칙:
     *   MEAL / EXERCISE / WASH : 10분 이상이면 SMALL_ICON (이 활동은 BUILDING 없음)
     *   SLEEP                  : 30분 미만 → CARPET_ONLY, 2h 미만 → SMALL_ICON, 이상 → BUILDING
     *   STUDY / WORK / DEVELOPMENT (생산 활동):
     *       30분 미만 → CARPET_ONLY
     *       30–60분   → SMALL_ICON
     *       60–120분  → BUILDING
     *       120분 이상 → UPGRADED_BUILDING
     *   SCHOOL : 60분 미만 → SMALL_ICON, 이상 → BUILDING
     *   COMPANY: 60분 미만 → SMALL_ICON, 60–480분 → BUILDING, 480분 이상 → UPGRADED_BUILDING
     *   REST / ETC: 30분 미만 → SMALL_ICON, 이상 → BUILDING
     *   기타 카테고리: 30분 미만 → CARPET_ONLY, 30–60분 → SMALL_ICON, 이상 → BUILDING
     */
    fun visualModeFor(category: String, durationMillis: Long): CitySegmentVisualMode {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        return when (category) {
            "MEAL", "EXERCISE", "WASH" ->
                if (minutes < 10) CitySegmentVisualMode.CARPET_ONLY
                else CitySegmentVisualMode.SMALL_ICON

            "SLEEP" -> when {
                minutes < 30  -> CitySegmentVisualMode.CARPET_ONLY
                minutes < 120 -> CitySegmentVisualMode.SMALL_ICON
                else          -> CitySegmentVisualMode.BUILDING
            }

            "STUDY", "WORK", "DEVELOPMENT" -> when {
                minutes < 30  -> CitySegmentVisualMode.CARPET_ONLY
                minutes < 60  -> CitySegmentVisualMode.SMALL_ICON
                minutes < 120 -> CitySegmentVisualMode.BUILDING
                else          -> CitySegmentVisualMode.UPGRADED_BUILDING
            }

            "SCHOOL" ->
                if (minutes < 60) CitySegmentVisualMode.SMALL_ICON
                else CitySegmentVisualMode.BUILDING

            "COMPANY" -> when {
                minutes < 60  -> CitySegmentVisualMode.SMALL_ICON
                minutes < 480 -> CitySegmentVisualMode.BUILDING
                else          -> CitySegmentVisualMode.UPGRADED_BUILDING
            }

            "REST", "ETC" ->
                if (minutes < 30) CitySegmentVisualMode.SMALL_ICON
                else CitySegmentVisualMode.BUILDING

            else -> when {
                minutes < 30  -> CitySegmentVisualMode.CARPET_ONLY
                minutes < 60  -> CitySegmentVisualMode.SMALL_ICON
                else          -> CitySegmentVisualMode.BUILDING
            }
        }
    }

    // ── 타일 선택 ────────────────────────────────────────────────────────
    /**
     * 카테고리와 duration 으로 대표 타일 파일명을 반환합니다.
     * CARPET_ONLY 모드여도 타일을 미리 캐시할 수 있도록 항상 유효한 파일명을 반환합니다.
     * 실제 렌더링 여부는 CityTimetableBar 의 visualModeFor 결과에 따릅니다.
     *
     * 파일명은 app/src/main/assets/ 기준 상대 경로입니다.
     */
    fun tileFor(category: String, durationMillis: Long): String {
        val hours   = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis)
        return when (category) {
            "SLEEP" -> if (hours >= 2) "tile_0013.png" else "tile_0009.png"

            "MEAL" ->
                if (minutes >= 60) "tile_0151.png"    // 업그레이드 식당
                else "tile_0062.png"                   // 기본 식당

            "STUDY" -> when {
                hours >= 2 -> "tile_0115.png"          // UPGRADED: 업그레이드 도서관
                hours >= 1 -> "tile_0026.png"          // BUILDING: 도서관
                else       -> "tile_0005.png"          // SMALL_ICON: 공부 소품
            }

            "WORK" -> when {
                hours >= 2 -> "tile_0011.png"          // UPGRADED: 업그레이드 작업실
                hours >= 1 -> "tile_0008.png"          // BUILDING: 작업실
                else       -> "tile_0006.png"          // SMALL_ICON: 작업 소품
            }

            "DEVELOPMENT" -> when {
                hours >= 2 -> "tile_0097.png"          // UPGRADED: 테크랩
                hours >= 1 -> "tile_0044.png"          // BUILDING: 개발 공방
                else       -> "tile_0007.png"          // SMALL_ICON: 개발 소품
            }

            "SCHOOL" ->
                if (hours >= 4) "tile_0044.png"        // BUILDING: 대형 학교
                else "tile_0026.png"                   // SMALL_ICON/BUILDING: 소형 학교

            "COMPANY" -> when {
                hours >= 8 -> "tile_0133.png"          // UPGRADED: 본사
                hours >= 4 -> "tile_0045.png"          // BUILDING: 오피스
                else       -> "tile_0044.png"          // SMALL_ICON/BUILDING: 소규모
            }

            "EXERCISE" -> "tile_0029.png"              // 운동장/녹지
            "WASH"     -> "tile_0022.png"              // 물/온천
            "REST"     -> "tile_0027.png"              // 카페/정원
            "ETC"      -> "tile_0002.png"              // 광장/교차로
            else       -> FALLBACK_TILE
        }
    }

    /**
     * 카테고리별 카펫 오버레이 색상 (ARGB Long).
     * alpha 0x40 = 약 25%. CityTimetableBar 에서 0.80f 로 override됨.
     */
    fun overlayColorFor(category: String): Long = when (category) {
        "SLEEP"       -> 0x40_7B61C8L
        "MEAL"        -> 0x40_FF8C42L
        "STUDY"       -> 0x40_4CAF50L
        "WORK"        -> 0x40_78909CL
        "DEVELOPMENT" -> 0x40_5C6BC0L
        "SCHOOL"      -> 0x40_26C6DAL
        "COMPANY"     -> 0x40_1565C0L
        "EXERCISE"    -> 0x40_29B6F6L
        "WASH"        -> 0x40_00BCD4L
        "REST"        -> 0x40_66BB6AL
        "ETC"         -> 0x40_BDBDBDL
        else          -> 0x20_9E9E9EL
    }
}
