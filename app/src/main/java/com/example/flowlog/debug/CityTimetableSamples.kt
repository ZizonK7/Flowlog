package com.example.flowlog.debug

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar

enum class CityTimetablePreset(val label: String) {
    BASIC_DAY("기본 하루"),
    SHORT_ACTIVITIES("짧은 활동 검수"),
    UPGRADE_CHECK("업그레이드 검수"),
    COMPANY_8H("회사 8시간 검수"),
}

object CityTimetableSamples {

    val presets = CityTimetablePreset.entries

    fun activitiesFor(preset: CityTimetablePreset): List<ActivitySession> = when (preset) {
        CityTimetablePreset.BASIC_DAY -> buildBasicDay()
        CityTimetablePreset.SHORT_ACTIVITIES -> buildShortActivities()
        CityTimetablePreset.UPGRADE_CHECK -> buildUpgradeCheck()
        CityTimetablePreset.COMPANY_8H -> buildCompany8h()
    }

    private fun todayAt(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun session(
        id: Long, category: String, title: String,
        startH: Int, startM: Int, endH: Int, endM: Int
    ): ActivitySession {
        val start = todayAt(startH, startM)
        val end = todayAt(endH, endM)
        return ActivitySession(
            id = id,
            category = category,
            title = title,
            startTime = start,
            endTime = end,
            durationMillis = end - start
        )
    }

    // 기본 하루 — SLEEP·MEAL·WASH·STUDY·EXERCISE·DEVELOPMENT·REST 고르게 포함
    private fun buildBasicDay() = listOf(
        session(1,  "SLEEP",       "수면",          1,  0,  7, 30),
        session(2,  "WASH",        "씻기",          7, 40,  8,  0),
        session(3,  "MEAL",        "아침",          8,  5,  8, 30),
        session(4,  "STUDY",       "공부",          9,  0, 10, 20),
        session(5,  "REST",        "휴식",         10, 30, 11,  0),
        session(6,  "DEVELOPMENT", "개발",         11, 10, 13, 10),
        session(7,  "MEAL",        "점심",         13, 20, 14,  0),
        session(8,  "WORK",        "작업",         14, 30, 16,  0),
        session(9,  "EXERCISE",    "운동",         16, 30, 17, 15),
        session(10, "MEAL",        "저녁",         18,  0, 18, 40),
        session(11, "REST",        "카페",         19,  0, 19, 45),
        session(12, "ETC",         "기타",         20,  0, 20, 30),
    )

    // 짧은 활동 검수 — 대부분 30분 미만, 소품/카펫 렌더링 위주로 확인
    private fun buildShortActivities() = listOf(
        session(1,  "SLEEP",       "수면",          0,  0,  7,  0),
        session(2,  "WASH",        "씻기",          7,  5,  7, 20),
        session(3,  "MEAL",        "아침",          7, 25,  7, 45),
        session(4,  "STUDY",       "짧은 공부",     8,  0,  8, 25),
        session(5,  "DEVELOPMENT", "짧은 개발",     8, 35,  9,  5),
        session(6,  "WORK",        "짧은 작업",     9, 15,  9, 45),
        session(7,  "REST",        "휴식",          9, 55, 10, 20),
        session(8,  "EXERCISE",    "가벼운 운동",  10, 30, 10, 55),
        session(9,  "MEAL",        "점심",         11,  5, 11, 35),
        session(10, "STUDY",       "짧은 복습",    11, 45, 12, 10),
        session(11, "ETC",         "잡무",         12, 20, 12, 45),
        session(12, "MEAL",        "간식",         13,  0, 13, 15),
        session(13, "DEVELOPMENT", "짧은 코딩",    13, 25, 13, 55),
        session(14, "REST",        "낮잠",         14,  5, 14, 30),
    )

    // 업그레이드 검수 — STUDY·WORK·DEVELOPMENT 각각 2시간 이상, 업그레이드 건물 확인
    private fun buildUpgradeCheck() = listOf(
        session(1, "SLEEP",       "수면",          0,  0,  7,  0),
        session(2, "MEAL",        "아침",          7, 20,  7, 45),
        session(3, "STUDY",       "긴 공부",       8,  0, 10, 30),   // 2.5h → 업그레이드 도서관
        session(4, "MEAL",        "점심",         10, 40, 11, 15),
        session(5, "WORK",        "긴 작업",      11, 30, 14,  0),   // 2.5h → 업그레이드 작업실
        session(6, "MEAL",        "간식",         14, 10, 14, 30),
        session(7, "DEVELOPMENT", "긴 개발",      14, 45, 17, 15),   // 2.5h → 업그레이드 테크랩
        session(8, "EXERCISE",    "운동",         17, 30, 18, 20),
        session(9, "MEAL",        "저녁",         18, 40, 19, 20),
        session(10, "REST",       "휴식",         19, 30, 20,  0),
    )

    // 회사 8시간 검수 — COMPANY 8h 블록 + 전후 일과
    private fun buildCompany8h() = listOf(
        session(1, "SLEEP",       "수면",          0,  0,  7,  0),
        session(2, "WASH",        "씻기",          7, 10,  7, 30),
        session(3, "MEAL",        "아침",          7, 35,  8,  0),
        session(4, "COMPANY",     "회사",          9,  0, 18,  0),   // 9h → 8h 이상 빌딩
        session(5, "MEAL",        "저녁",         18, 20, 19,  0),
        session(6, "REST",        "퇴근 후 휴식", 19, 10, 19, 50),
        session(7, "STUDY",       "짧은 복습",    20,  0, 20, 45),
        session(8, "DEVELOPMENT", "개발",         21,  0, 22, 30),   // 1.5h → 개발 공방
    )
}
