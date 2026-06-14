package com.example.flowlog.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.flowlog.data.agent.PetiteSourceType
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.CalendarEventEntity
import com.example.flowlog.data.local.entity.LectureCalendarInfoEntity
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Firestore calendarEvents 컬렉션에서 오늘 범위 문서를 pull하고
 * type에 따라 로컬 Room 테이블에 분배한다.
 *
 * Firestore 경로: users/{uid}/flowlog/data/calendarEvents
 * 조회 조건: startTime >= 오늘 00:00, startTime < 내일 00:00 (앱 로컬 타임존)
 *
 * type 분기:
 *   CALENDAR_PETITE        → organized_petites (사용자 상태 보존 upsert; deletedAt 있으면 dismiss)
 *   LECTURE_PLAN / CLASS_EVENT / SYLLABUS → lecture_calendar_infos (오늘 범위 atomic replace)
 *   GENERAL_EVENT          → calendar_events (오늘 범위 atomic replace)
 *
 * 실패 처리:
 *   Firestore 오류 → 기존 로컬 데이터 손대지 않고 failed=true 반환.
 *   로컬 저장 오류 → 타입별로 개별 처리, 크래시하지 않음.
 */
class FirebaseCalendarPullDataSource(context: Context) {

    private val appContext = context.applicationContext
    private val db = FlowlogDatabase.getInstance(appContext)
    private val petiteDao = db.organizedPetiteDao()
    private val calendarEventDao = db.calendarEventDao()
    private val lectureInfoDao = db.lectureCalendarInfoDao()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun pullTodayCalendar(userId: String): CalendarPullOutcome = withContext(Dispatchers.IO) {
        val (dayStart, dayEnd) = todayRange()

        // ── 1. Firestore fetch ───────────────────────────────────────────────
        val docs = runCatching {
            firestore.collection("users").document(userId)
                .collection("flowlog").document("data")
                .collection("calendarEvents")
                .whereGreaterThanOrEqualTo("startTime", dayStart)
                .whereLessThan("startTime", dayEnd)
                .get()
                .awaitResult()
                .documents
        }.getOrElse { e ->
            Log.w(TAG, "Calendar pull failed: ${e.message}", e)
            // 실패 시 로컬 데이터 손대지 않음
            return@withContext CalendarPullOutcome(failed = true)
        }

        Log.i(TAG, "Calendar pull fetched ${docs.size} docs — userId=$userId dayStart=$dayStart")

        val now = System.currentTimeMillis()
        val petiteEntities = mutableListOf<OrganizedPetiteEntity>()
        val petiteDeletedEventIds = mutableListOf<String>()   // deletedAt 있는 CALENDAR_PETITE
        val lectureEntities = mutableListOf<LectureCalendarInfoEntity>()
        val generalEventEntities = mutableListOf<CalendarEventEntity>()

        // ── 2. 문서 파싱 ────────────────────────────────────────────────────
        docs.forEach { doc ->
            runCatching {
                val type = doc.getString("type") ?: return@runCatching
                val eventId = doc.getString("eventId") ?: doc.id      // #3: fallback to doc.id
                val startTime = doc.getLong("startTime") ?: return@runCatching
                val endTime = doc.getLong("endTime")
                val updatedAt = doc.getLong("updatedAt") ?: now
                val deletedAt = doc.getLong("deletedAt")

                when (type) {
                    "CALENDAR_PETITE" -> {
                        if (deletedAt != null) {
                            // #7: 취소된 이벤트 → 기존 Petite dismiss
                            petiteDeletedEventIds.add(eventId)
                            return@runCatching
                        }
                        val title = doc.getString("title") ?: return@runCatching
                        val durationMinutes = endTime
                            ?.let { end -> ((end - startTime) / 60_000L).toInt().coerceAtLeast(1) }
                        petiteEntities.add(
                            OrganizedPetiteEntity(
                                id = "calendar_$eventId",
                                userId = userId,
                                title = title,
                                sourceType = PetiteSourceType.CALENDAR.name,
                                sourceId = eventId,
                                category = doc.getString("category"),
                                // #6: dateMillis = 오늘 자정(day-level, 다른 Petite 타입과 일관)
                                dateMillis = dayStart,
                                linkedActivityName = null,
                                activityCategory = null,
                                isCompleted = false,          // 신규 삽입 시 초기값; 재-pull 시 보존됨
                                priorityScore = (doc.getLong("priorityScore") ?: 0L).toInt(),
                                burdenScore = null,
                                isSeverelyBehind = null,
                                totalStudyMinutesSinceD7 = null,
                                studiedDaysSinceD7 = null,
                                missedDaysSinceD7 = null,
                                aiComment = doc.getString("description"),
                                estimatedMinutes = durationMinutes,
                                stepsJson = "[]",
                                examDValue = null,
                                routineTimerDurationMillis = null,
                                routineTimerCategory = null,
                                rank = (doc.getLong("rank") ?: 0L).toInt(),
                                isDismissed = false,          // 신규 삽입 시 초기값; 재-pull 시 보존됨
                                createdAt = now,
                                updatedAt = updatedAt
                            )
                        )
                    }

                    "LECTURE_PLAN", "CLASS_EVENT", "SYLLABUS" -> {
                        // #7: deletedAt 있으면 replace 목록에서 제외 (오늘 replace로 자연 삭제됨)
                        if (deletedAt != null) return@runCatching
                        lectureEntities.add(
                            LectureCalendarInfoEntity(
                                eventId = eventId,
                                type = type,
                                courseTitle = doc.getString("courseTitle"),
                                lectureTitle = doc.getString("lectureTitle"),
                                // #4: classDate = 오늘 자정, startTime = 실제 수업 시작 millis
                                classDate = dayStart,
                                startTime = startTime,
                                endTime = endTime,
                                week = doc.getLong("week")?.toInt(),
                                syllabusText = doc.getString("syllabusText"),
                                previewCardId = doc.getString("previewCardId"),
                                location = doc.getString("location"),
                                description = doc.getString("description"),
                                sourceId = eventId,
                                updatedAt = updatedAt,
                                deletedAt = null   // deletedAt 있는 doc는 위에서 early return
                            )
                        )
                    }

                    "GENERAL_EVENT" -> {
                        // #7: deletedAt 있으면 제외
                        if (deletedAt != null) return@runCatching
                        val title = doc.getString("title") ?: return@runCatching
                        generalEventEntities.add(
                            CalendarEventEntity(
                                eventId = eventId,
                                type = type,
                                title = title,
                                startTime = startTime,
                                endTime = endTime,
                                location = doc.getString("location"),
                                description = doc.getString("description"),
                                source = doc.getString("source"),
                                updatedAt = updatedAt,
                                deletedAt = null   // deletedAt 있는 doc는 위에서 early return
                            )
                        )
                    }

                    else -> Log.w(TAG, "Unknown calendar event type=$type eventId=$eventId")
                }
            }.onFailure { e ->
                Log.w(TAG, "Calendar doc processing failed: ${doc.id} — ${e.message}")
            }
        }

        // ── 3. CALENDAR_PETITE: deletedAt 처리 (dismiss) ────────────────────
        // #7: 취소된 이벤트의 기존 Petite를 isDismissed=true로 처리
        petiteDeletedEventIds.forEach { eventId ->
            runCatching {
                petiteDao.dismissBySource(
                    userId = userId,
                    sourceType = PetiteSourceType.CALENDAR.name,
                    sourceId = eventId,
                    updatedAt = now
                )
            }.onFailure { e ->
                Log.w(TAG, "Petite dismiss failed for eventId=$eventId: ${e.message}")
            }
        }

        // ── 4. CALENDAR_PETITE: upsert (isDismissed, isCompleted 보존) ─────
        // #1: updateCalendarPetiteContent는 isDismissed, isCompleted를 건드리지 않음
        var petiteCount = 0
        petiteEntities.forEach { entity ->
            runCatching {
                petiteDao.upsertCalendarPetitePreservingUserState(entity)
                petiteCount++
            }.onFailure { e ->
                Log.w(TAG, "Petite upsert failed: ${entity.id} — ${e.message}")
            }
        }

        // ── 5. Lecture + GeneralEvent: atomic replace ──────────────────────
        // #5: pull 성공 시 항상 오늘 범위를 replace (서버 기준 — 비어있어도 로컬 정리)
        // #8: 두 테이블을 하나의 트랜잭션으로 묶어 partial 상태 방지
        var lectureInfoCount = 0
        var generalEventCount = 0
        runCatching {
            db.withTransaction {
                lectureInfoDao.replaceTodayLectureInfos(dayStart, dayEnd, lectureEntities)
                calendarEventDao.replaceEventsForDay(dayStart, dayEnd, generalEventEntities)
            }
            lectureInfoCount = lectureEntities.size
            generalEventCount = generalEventEntities.size
        }.onFailure { e ->
            Log.w(TAG, "Lecture/CalendarEvent atomic replace failed: ${e.message}")
        }

        // ── 6. calender 문서에서 오늘 강의 계획서(COURSE_LESSON) pull ──────────
        val syllabusCount = runCatching {
            pullTodaySyllabusLessons(userId, dayStart, dayEnd)
        }.getOrElse { e ->
            Log.w(TAG, "Syllabus pull threw: ${e.message}", e)
            0
        }

        Log.i(
            TAG,
            "Calendar pull complete — petites=$petiteCount lectureInfos=$lectureInfoCount generalEvents=$generalEventCount syllabusLessons=$syllabusCount"
        )

        CalendarPullOutcome(
            pulledPetiteCount = petiteCount,
            pulledLectureInfoCount = lectureInfoCount,
            pulledGeneralEventCount = generalEventCount,
            failed = false
        )
    }

    // users/{uid}/flowlog/calender 문서의 lessons 배열에서 오늘 날짜 항목을 lecture_calendar_infos에 저장
    private suspend fun pullTodaySyllabusLessons(userId: String, dayStart: Long, dayEnd: Long): Int {
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dayStart))

        val doc = runCatching {
            firestore.collection("users").document(userId)
                .collection("flowlog").document("calendar")
                .get().awaitResult()
        }.getOrElse { e ->
            Log.w(TAG, "Syllabus doc fetch failed: ${e.message}", e)
            return 0
        }

        if (!doc.exists()) {
            Log.i(TAG, "Syllabus document not found — userId=$userId")
            return 0
        }

        val courseTitle = (doc.get("course") as? Map<*, *>)?.get("name") as? String

        @Suppress("UNCHECKED_CAST")
        val lessons = (doc.get("lessons") as? List<*>)
            ?.filterIsInstance<Map<String, Any?>>()
            ?: emptyList()

        val now = System.currentTimeMillis()

        val todayLessons = lessons.mapNotNull { lesson ->
            runCatching {
                val date = lesson["date"] as? String ?: return@runCatching null
                if (date != todayDateStr) return@runCatching null
                val eventId = lesson["id"] as? String ?: return@runCatching null
                LectureCalendarInfoEntity(
                    eventId = eventId,
                    type = "COURSE_LESSON",
                    courseTitle = courseTitle,
                    lectureTitle = lesson["title"] as? String,
                    classDate = dayStart,
                    startTime = parseTime24ToMillis(lesson["time24"] as? String, dayStart),
                    endTime = parseTime24ToMillis(lesson["endTime24"] as? String, dayStart),
                    week = extractWeekNumber(lesson["week"] as? String),
                    syllabusText = lesson["week"] as? String,
                    previewCardId = null,
                    location = lesson["location"] as? String,
                    description = null,
                    sourceId = eventId,
                    updatedAt = now,
                    deletedAt = null
                )
            }.getOrElse { e ->
                Log.w(TAG, "Syllabus lesson parse failed: ${e.message}")
                null
            }
        }

        runCatching {
            lectureInfoDao.replaceCourseLessonsForDay(dayStart, todayLessons)
        }.onFailure { e ->
            Log.w(TAG, "Syllabus lessons save failed: ${e.message}")
            return 0
        }

        // 각 강의마다 "예습하기" Petite upsert (isDismissed/isCompleted 보존)
        todayLessons.forEachIndexed { index, lesson ->
            runCatching {
                val previewId = "lecture_preview_${lesson.eventId}"
                val title = "${lesson.courseTitle ?: lesson.lectureTitle ?: "강의"} 예습하기"
                petiteDao.upsertCalendarPetitePreservingUserState(
                    OrganizedPetiteEntity(
                        id = previewId,
                        userId = userId,
                        title = title,
                        sourceType = PetiteSourceType.CALENDAR.name,
                        sourceId = previewId,
                        category = null,
                        dateMillis = dayStart,
                        linkedActivityName = null,
                        activityCategory = "STUDY",
                        isCompleted = false,
                        priorityScore = 1,
                        burdenScore = null,
                        isSeverelyBehind = null,
                        totalStudyMinutesSinceD7 = null,
                        studiedDaysSinceD7 = null,
                        missedDaysSinceD7 = null,
                        aiComment = lesson.syllabusText,
                        estimatedMinutes = 30,
                        stepsJson = "[]",
                        examDValue = null,
                        routineTimerDurationMillis = null,
                        routineTimerCategory = null,
                        rank = index,
                        isDismissed = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }.onFailure { e ->
                Log.w(TAG, "Lecture preview petite upsert failed: ${lesson.eventId} — ${e.message}")
            }
        }

        Log.i(TAG, "Syllabus pull complete — saved=${todayLessons.size} date=$todayDateStr course=$courseTitle")
        return todayLessons.size
    }

    private fun parseTime24ToMillis(time24: String?, dayStart: Long): Long? {
        if (time24.isNullOrBlank()) return null
        return runCatching {
            val parts = time24.split(":")
            if (parts.size < 2) return null
            dayStart + (parts[0].toInt() * 60 + parts[1].toInt()) * 60_000L
        }.getOrNull()
    }

    private fun extractWeekNumber(weekLabel: String?): Int? {
        if (weekLabel == null) return null
        return Regex("""(\d+)주차""").find(weekLabel)?.groupValues?.get(1)?.toIntOrNull()
    }

    // #2: Calendar.add(DAY_OF_YEAR, 1)로 dayEnd 계산 — DST 전환일에도 정확
    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dayStart = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val dayEnd = cal.timeInMillis

        return dayStart to dayEnd
    }

    companion object {
        private const val TAG = "FlowlogCalendarPull"
    }
}
