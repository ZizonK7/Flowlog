package com.example.flowlog.ui.screen

import com.example.flowlog.data.model.ActivitySession
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class EmptyRange(val startMillis: Long, val endMillis: Long)


/**
 * 롱프레스 위치(시간)가 속한 빈 구간 전체를 반환한다.
 * 누른 시간이 이미 기록된 활동 안이면 null을 반환한다.
 */
fun findEmptyRangeAroundPressedTime(
    pressedTimeMillis: Long,
    allActivities: List<ActivitySession>,
    runningTimerStartMillis: Long?
): EmptyRange? {
    val sorted = allActivities.sortedBy { it.startTime }

    // 누른 시간이 기존 기록 안이면 빈 공간이 아님
    if (sorted.any { a -> pressedTimeMillis >= a.startTime && pressedTimeMillis < a.endTime }) return null

    // 빈 구간 시작: 누른 시간 이전 마지막 활동의 종료 시간
    val rangeStart = sorted
        .filter { it.endTime <= pressedTimeMillis }
        .maxByOrNull { it.endTime }
        ?.endTime ?: 0L

    // 빈 구간 끝: 다음 활동 시작, 진행 중인 타이머 시작, 현재 시각 중 가장 이른 것
    val followingStart = sorted
        .filter { it.startTime > pressedTimeMillis }
        .minByOrNull { it.startTime }
        ?.startTime
    val now = System.currentTimeMillis()
    val rangeEnd = listOfNotNull(followingStart, runningTimerStartMillis, now).min()

    if (rangeEnd <= rangeStart) return null
    return EmptyRange(rangeStart, rangeEnd)
}

/**
 * 디버그용 완화 조건: 빈 구간이 30분 이상이면 수면 제안 후보로 본다.
 * TODO: 검증 완료 후 원래 조건(2h 이상, 16h 이하, 21:00~10:00 겹침)으로 복원
 */
fun isSleepCandidateRange(startMillis: Long, endMillis: Long): Boolean {
    return endMillis - startMillis >= 30 * 60 * 1000L
}

/**
 * 사용자가 선택한 수면 범위가 유효한지 검사한다.
 */
fun validateSleepRange(
    sleepStart: Long,
    sleepEnd: Long,
    emptyRange: EmptyRange,
    allActivities: List<ActivitySession>
): Boolean {
    if (sleepStart < emptyRange.startMillis) return false
    if (sleepEnd > emptyRange.endMillis) return false
    if (sleepStart >= sleepEnd) return false
    return allActivities.none { a -> sleepStart < a.endTime && sleepEnd > a.startTime }
}

/**
 * "6월 5일 23:30 ~ 6월 6일 07:20" 형식의 레이블을 반환한다.
 * 같은 날이면 날짜를 한 번만 표시한다.
 */
fun formatSleepRangeLabel(startMillis: Long, endMillis: Long): String {
    val dateFmt = SimpleDateFormat("M월 d일 HH:mm", Locale.getDefault())
    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
    val sameDay = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR) &&
        startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        "${dateFmt.format(Date(startMillis))} ~ ${timeFmt.format(Date(endMillis))}"
    } else {
        "${dateFmt.format(Date(startMillis))} ~ ${dateFmt.format(Date(endMillis))}"
    }
}

/**
 * emptyRange 안에서 5분 단위 옵션 목록을 분 오프셋(emptyRange.startMillis 기준)으로 생성한다.
 * 항상 0과 totalMinutes를 포함한다.
 */
fun buildTimeOffsetOptions(emptyRange: EmptyRange): List<Int> {
    val totalMinutes = ((emptyRange.endMillis - emptyRange.startMillis) / (60 * 1000L)).toInt()
    val step = 5
    val options = mutableListOf<Int>()
    var t = 0
    while (t <= totalMinutes) {
        options.add(t)
        t += step
    }
    if (options.lastOrNull() != totalMinutes) options.add(totalMinutes)
    return options
}

/** 분 오프셋을 절대 시각(밀리초)으로 변환 */
fun EmptyRange.offsetToMillis(offsetMinutes: Int): Long =
    startMillis + offsetMinutes * 60_000L

/** 밀리초를 "HH:mm" 또는 (날짜가 다를 경우) "M/d HH:mm" 로 포맷 */
fun formatTimeOption(millis: Long, referenceMillis: Long): String {
    val refCal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
    val tCal = Calendar.getInstance().apply { timeInMillis = millis }
    val sameDay = refCal.get(Calendar.YEAR) == tCal.get(Calendar.YEAR) &&
        refCal.get(Calendar.DAY_OF_YEAR) == tCal.get(Calendar.DAY_OF_YEAR)
    return if (sameDay) {
        "%02d:%02d".format(tCal.get(Calendar.HOUR_OF_DAY), tCal.get(Calendar.MINUTE))
    } else {
        val dayDiff = ((millis - referenceMillis) / (24 * 60 * 60 * 1000L)).toInt() + 1
        "+${dayDiff}일 %02d:%02d".format(tCal.get(Calendar.HOUR_OF_DAY), tCal.get(Calendar.MINUTE))
    }
}
