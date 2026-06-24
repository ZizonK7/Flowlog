package com.example.flowlog.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.local.entity.CalendarEventEntity
import com.example.flowlog.data.local.entity.LectureCalendarInfoEntity
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.notification.AutoButtonScheduler
import com.example.flowlog.data.remote.awaitResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Firestore calendarEvents 컬렉션에서 오늘 범위 문서를 pull하고
 * type에 따라 로컬 Room 테이블에 분배한다.
 *
 * Firestore 경로: users/{uid}/flowlog/data/calendarEvents
 * 조회 조건: startTime >= 오늘 00:00, startTime < 내일 00:00 (앱 로컬 타임존)
 *
 * type 분기:
 *   CALENDAR_PETITE        → todos (NORMAL 카테고리 NORMAL 할 일로 자동 추가; calendarSourceId로 중복 방지)
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
    private val todoDao = db.todoDao()
    private val scheduleDao = db.autoButtonScheduleDao()
    private val calendarEventDao = db.calendarEventDao()
    private val lectureInfoDao = db.lectureCalendarInfoDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val autoButtonScheduler = AutoButtonScheduler(appContext)
    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val calendarApplyMutex = Mutex()

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
        calendarApplyMutex.withLock {
            applyTodayCalendarDocuments(userId, dayStart, dayEnd, docs)
        }
    }

    fun listenTodayCalendar(
        userId: String,
        onOutcome: (CalendarPullOutcome) -> Unit = {}
    ): CalendarSubscription {
        val (dayStart, dayEnd) = todayRange()
        val eventRegistration = firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("calendarEvents")
            .whereGreaterThanOrEqualTo("startTime", dayStart)
            .whereLessThan("startTime", dayEnd)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Calendar listener failed: ${error.message}", error)
                    onOutcome(CalendarPullOutcome(failed = true))
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                listenerScope.launch {
                    val outcome = runCatching {
                        calendarApplyMutex.withLock {
                            applyTodayCalendarDocuments(userId, dayStart, dayEnd, docs)
                        }
                    }.getOrElse { e ->
                        Log.w(TAG, "Calendar listener apply failed: ${e.message}", e)
                        CalendarPullOutcome(failed = true)
                    }
                    onOutcome(outcome)
                }
            }

        val todoRegistration = firestore.collection("users").document(userId)
            .collection("flowlog").document("data")
            .collection("calendarEvents")
            .whereEqualTo("type", "FLOWLOG_TODO")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Todo calendar listener failed: ${error.message}", error)
                    onOutcome(CalendarPullOutcome(failed = true))
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                listenerScope.launch {
                    val failed = runCatching {
                        calendarApplyMutex.withLock {
                            applyTodoCalendarDocuments(userId, docs)
                        }
                    }.isFailure
                    if (failed) onOutcome(CalendarPullOutcome(failed = true))
                }
            }

        val syllabusInitialSnapshot = AtomicBoolean(true)
        val syllabusRegistration = firestore.collection("users").document(userId)
            .collection("flowlog").document("calendar")
            .addSnapshotListener { _, error ->
                if (error != null) {
                    Log.w(TAG, "Syllabus listener failed: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (syllabusInitialSnapshot.getAndSet(false)) return@addSnapshotListener
                listenerScope.launch {
                    onOutcome(pullTodayCalendar(userId))
                }
            }

        return CalendarSubscription(listOf(eventRegistration, todoRegistration, syllabusRegistration))
    }

    private suspend fun applyTodoCalendarDocuments(
        userId: String,
        docs: List<DocumentSnapshot>
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        docs.forEach { doc ->
            runCatching {
                if (doc.getString("type") != "FLOWLOG_TODO") return@runCatching
                val eventId = doc.getString("eventId") ?: doc.id
                val updatedAt = doc.getLong("updatedAt") ?: now
                val deletedAt = doc.getLong("deletedAt")
                val remoteTodoId = doc.getLong("todoId")
                    ?: doc.getLong("id")
                    ?: eventId.toLongOrNull()
                val todoId = remoteTodoId?.let { "legacy_todo_$it" } ?: "calendar_todo_$eventId"

                if (deletedAt != null) {
                    todoDao.markTodoDeletedFromRemote(todoId, deletedAt, updatedAt)
                    remoteTodoId?.let {
                        petiteDao.dismissTodoPetitesBySourceId(
                            userId = userId,
                            sourceId = it.toString(),
                            updatedAt = updatedAt
                        )
                    }
                    return@runCatching
                }

                val title = doc.getString("title") ?: return@runCatching
                val startTime = doc.getLong("startTime") ?: return@runCatching
                val category = doc.getString("category") ?: doc.getString("calendarTaskType") ?: "TODAY"
                val entity = TodoEntity(
                    todoId = todoId,
                    userId = userId,
                    title = title,
                    category = category,
                    selectedDate = startTime,
                    isCompleted = doc.getBoolean("isCompleted") ?: false,
                    completedAt = doc.getLong("completedAt"),
                    accumulatedWorkMillis = (doc.getLong("accumulatedSeconds") ?: 0L) * 1000L,
                    burdenLevel = doc.getString("burdenLevel"),
                    burdenGroupKey = doc.getString("burdenGroupKey"),
                    burdenScore = (doc.getLong("burdenScore") ?: 0L).toInt(),
                    burdenReasonJson = doc.getString("burdenReasonJson"),
                    reviewStage = (doc.getLong("reviewStage") ?: 0L).toInt(),
                    reviewStage1CompletedAt = doc.getLong("reviewStage1CompletedAt"),
                    legacyId = remoteTodoId,
                    createdAt = doc.getLong("createdAt") ?: now,
                    updatedAt = updatedAt,
                    syncStatus = "SYNCED"
                )
                val existing = todoDao.getTodoById(entity.todoId)
                if (existing == null || existing.updatedAt <= entity.updatedAt) {
                    todoDao.insertTodo(entity)
                }
            }.onFailure { e ->
                Log.w(TAG, "Todo calendar doc apply failed: ${doc.id} — ${e.message}")
            }
        }
    }

    private suspend fun applyTodayCalendarDocuments(
        userId: String,
        dayStart: Long,
        dayEnd: Long,
        docs: List<DocumentSnapshot>
    ): CalendarPullOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val calendarPetiteEntities = mutableListOf<Pair<String, TodoEntity>>()  // eventId to entity
        val calendarPetiteDeletedIds = mutableListOf<String>()                   // deletedAt 있는 CALENDAR_PETITE
        val lectureEntities = mutableListOf<LectureCalendarInfoEntity>()
        val generalEventEntities = mutableListOf<CalendarEventEntity>()
        val todoEntities = mutableListOf<TodoEntity>()
        val todoDeletedIds = mutableListOf<Pair<String, Long?>>()

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
                            calendarPetiteDeletedIds.add(eventId)
                            return@runCatching
                        }
                        val title = doc.getString("title") ?: return@runCatching
                        val origin = doc.getString("origin")
                        val calendarTaskType = doc.getString("calendarTaskType")
                            ?: doc.getString("taskType")
                            ?: if (origin.equals("studyPlan", ignoreCase = true)) "ACADEMIC" else null
                        val calendarPlanId = doc.getString("planGroupId")?.takeIf { it.isNotBlank() }
                        calendarPetiteEntities.add(
                            eventId to TodoEntity(
                                todoId = "calendar_petite_$eventId",
                                userId = userId,
                                title = title,
                                category = "NORMAL",
                                selectedDate = dayStart,
                                calendarSourceId = eventId,
                                calendarSourceType = calendarTaskType,
                                calendarPlanId = calendarPlanId,
                                createdAt = doc.getLong("createdAt") ?: now,
                                updatedAt = updatedAt,
                                syncStatus = "SYNCED"
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

                    "FLOWLOG_TODO" -> {
                        val remoteTodoId = doc.getLong("todoId")
                            ?: doc.getLong("id")
                            ?: eventId.toLongOrNull()
                        val todoId = remoteTodoId?.let { "legacy_todo_$it" } ?: "calendar_todo_$eventId"
                        if (deletedAt != null) {
                            todoDeletedIds.add(todoId to remoteTodoId)
                            return@runCatching
                        }
                        val title = doc.getString("title") ?: return@runCatching
                        val category = doc.getString("category") ?: doc.getString("calendarTaskType") ?: "TODAY"
                        val createdAt = doc.getLong("createdAt") ?: now
                        todoEntities.add(
                            TodoEntity(
                                todoId = todoId,
                                userId = userId,
                                title = title,
                                category = category,
                                selectedDate = startTime,
                                isCompleted = doc.getBoolean("isCompleted") ?: false,
                                completedAt = doc.getLong("completedAt"),
                                accumulatedWorkMillis = (doc.getLong("accumulatedSeconds") ?: 0L) * 1000L,
                                burdenLevel = doc.getString("burdenLevel"),
                                burdenGroupKey = doc.getString("burdenGroupKey"),
                                burdenScore = (doc.getLong("burdenScore") ?: 0L).toInt(),
                                burdenReasonJson = doc.getString("burdenReasonJson"),
                                reviewStage = (doc.getLong("reviewStage") ?: 0L).toInt(),
                                reviewStage1CompletedAt = doc.getLong("reviewStage1CompletedAt"),
                                legacyId = remoteTodoId,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                syncStatus = "SYNCED"
                            )
                        )
                    }

                    else -> Log.w(TAG, "Unknown calendar event type=$type eventId=$eventId")
                }
            }.onFailure { e ->
                Log.w(TAG, "Calendar doc processing failed: ${doc.id} — ${e.message}")
            }
        }

        // ── 3. CALENDAR_PETITE: 삭제된 이벤트 → todos soft delete ──────────
        calendarPetiteDeletedIds.forEach { eventId ->
            runCatching {
                todoDao.softDeleteByCalendarSourceId(userId, eventId, now)
            }.onFailure { e ->
                Log.w(TAG, "Calendar petite todo delete failed for eventId=$eventId: ${e.message}")
            }
        }

        todoDeletedIds.forEach { (todoId, remoteTodoId) ->
            runCatching {
                todoDao.markTodoDeletedFromRemote(todoId, now, now)
                remoteTodoId?.let {
                    petiteDao.dismissTodoPetitesBySourceId(
                        userId = userId,
                        sourceId = it.toString(),
                        updatedAt = now
                    )
                }
            }.onFailure { e ->
                Log.w(TAG, "Todo soft delete failed for todoId=$todoId: ${e.message}")
            }
        }

        var todoCount = 0
        todoEntities.forEach { entity ->
            runCatching {
                val existing = todoDao.getTodoById(entity.todoId)
                if (existing == null || existing.updatedAt <= entity.updatedAt) {
                    todoDao.insertTodo(entity)
                    todoCount++
                }
            }.onFailure { e ->
                Log.w(TAG, "Todo upsert failed: ${entity.todoId} — ${e.message}")
            }
        }

        // ── 4. CALENDAR_PETITE: todos에 NORMAL 할 일로 upsert (isCompleted 보존) ──
        var calendarPetiteCount = 0
        calendarPetiteEntities.forEach { (eventId, entity) ->
            runCatching {
                val existing = todoDao.getTodoByCalendarSourceId(userId, eventId)
                when {
                    existing == null -> todoDao.insertTodo(entity)
                    existing.isDeleted -> { /* 사용자가 앱에서 삭제한 항목 — 재생성 금지 */ }
                    existing.updatedAt < entity.updatedAt ->
                        todoDao.updateCalendarTodoContent(userId, eventId, entity.title, entity.calendarSourceType, entity.calendarPlanId, entity.updatedAt)
                }
                calendarPetiteCount++
            }.onFailure { e ->
                Log.w(TAG, "Calendar petite todo upsert failed: $eventId — ${e.message}")
            }
        }

        // ── 4b. 기존 CALENDAR/STUDY_PLAN organized petites 정리 (NORMAL 할 일로 전환됨) ──
        runCatching {
            petiteDao.deleteAllForUserBySource(userId, "CALENDAR")
            petiteDao.deleteAllForUserBySource(userId, "STUDY_PLAN")
        }.onFailure { e ->
            Log.w(TAG, "Calendar organized petites cleanup failed: ${e.message}")
        }

        // ── 4c. 기존 CALENDAR auto-start 스케줄 정리 (더 이상 생성하지 않음) ──
        runCatching {
            scheduleDao.deleteCalendarSourcedForUser(userId)
            autoButtonScheduler.rescheduleAll()
        }.onFailure { e ->
            Log.w(TAG, "Calendar schedule cleanup failed: ${e.message}")
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
            "Calendar pull complete — calendarTodos=$calendarPetiteCount lectureInfos=$lectureInfoCount generalEvents=$generalEventCount syllabusLessons=$syllabusCount"
        )

        CalendarPullOutcome(
            pulledCalendarTodoCount = calendarPetiteCount,
            pulledLectureInfoCount = lectureInfoCount,
            pulledGeneralEventCount = generalEventCount,
            failed = false
        )
    }

    // users/{uid}/flowlog/calendar 문서의 lessons 배열에서 오늘 날짜 항목을 lecture_calendar_infos에 저장
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

    class CalendarSubscription internal constructor(
        private val registrations: List<ListenerRegistration>
    ) {
        fun remove() {
            registrations.forEach { it.remove() }
        }
    }

    companion object {
        private const val TAG = "FlowlogCalendarPull"
    }
}
