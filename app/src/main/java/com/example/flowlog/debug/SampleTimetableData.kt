package com.example.flowlog.debug

import com.example.flowlog.data.model.ActivitySession
import java.util.Calendar

enum class TimetablePreset { STUDENT, WORKER, HOME, FOCUS }

object SampleTimetableData {

    val presetCount: Int = 4

    // 개발자 모드 전용. DB와 완전히 무관하며 읽기/쓰기 없음.
    fun activitiesForIndex(index: Int): List<ActivitySession> = when (index % presetCount) {
        0    -> buildStudentActivities()
        1    -> buildWorkerActivities()
        2    -> buildHomeActivities()
        else -> buildFocusActivities()
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
        val end   = todayAt(endH, endM)
        return ActivitySession(
            id            = id,
            category      = category,
            title         = title,
            startTime     = start,
            endTime       = end,
            durationMillis = end - start
        )
    }

    // 학생 일과
    private fun buildStudentActivities() = listOf(
        session( 1, "SLEEP",    "수면",           0, 30,  7, 20),
        session( 2, "MEAL",     "아침 식사",      7, 40,  8,  5),
        session( 3, "SCHOOL",   "학교",           9,  0, 13,  0),
        session( 4, "MEAL",     "점심 식사",     13, 10, 13, 45),
        session( 5, "REST",     "휴식",          14,  0, 14, 40),
        session( 6, "STUDY",    "공학수학 공부", 15,  0, 17, 30),
        session( 7, "EXERCISE", "운동",          18,  0, 18, 40),
        session( 8, "MEAL",     "저녁 식사",     19,  0, 19, 35),
        session( 9, "WORK",     "작업",          20,  0, 22, 15),
        session(10, "REST",     "가벼운 휴식",   22, 30, 23, 10)
    )

    // 직장인 일과
    private fun buildWorkerActivities() = listOf(
        session( 1, "SLEEP",       "수면",         0, 10,  7, 10),
        session( 2, "MEAL",        "아침",         7, 30,  7, 55),
        session( 3, "COMPANY",     "회사",         9,  0, 17,  0),
        session( 4, "REST",        "퇴근 후 휴식", 17, 30, 18, 20),
        session( 5, "MEAL",        "저녁",         18, 40, 19, 20),
        session( 6, "STUDY",       "짧은 복습",    20,  0, 20, 45),
        session( 7, "DEVELOPMENT", "앱 개발",      21,  0, 23, 20)
    )

    // 집에 있는 날 (짧은 블록 위주)
    private fun buildHomeActivities() = listOf(
        session( 1, "SLEEP",       "수면",        1,  0,  8,  0),
        session( 2, "MEAL",        "아침",        8, 20,  8, 35),
        session( 3, "WASH",        "씻기",        8, 40,  8, 55),
        session( 4, "STUDY",       "짧은 공부",   9, 10,  9, 45),
        session( 5, "REST",        "휴식",       10,  0, 10, 25),
        session( 6, "DEVELOPMENT", "짧은 개발",  10, 40, 11, 20),
        session( 7, "MEAL",        "점심",       12, 10, 12, 50),
        session( 8, "EXERCISE",    "가벼운 운동", 15,  0, 15, 25),
        session( 9, "WORK",        "짧은 작업",  16,  0, 16, 50),
        session(10, "REST",        "카페 휴식",  17, 20, 18,  0)
    )

    // 집중 작업일 (긴 블록 위주)
    private fun buildFocusActivities() = listOf(
        session( 1, "SLEEP",       "수면",      0,  0,  7,  0),
        session( 2, "MEAL",        "아침",      7, 20,  7, 45),
        session( 3, "STUDY",       "긴 공부",   8, 30, 11, 10),
        session( 4, "REST",        "휴식",     11, 20, 12,  0),
        session( 5, "MEAL",        "점심",     12, 10, 12, 45),
        session( 6, "DEVELOPMENT", "긴 개발",  13, 30, 16,  0),
        session( 7, "EXERCISE",    "운동",     16, 30, 17, 20),
        session( 8, "WORK",        "긴 작업",  19,  0, 21, 30)
    )
}
