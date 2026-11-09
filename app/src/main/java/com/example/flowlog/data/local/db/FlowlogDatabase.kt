package com.example.flowlog.data.local.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flowlog.data.local.dao.ActivityDao
import com.example.flowlog.data.local.dao.ActivityRevisionDao
import com.example.flowlog.data.local.dao.StudyDao
import com.example.flowlog.data.local.entity.StudyLinkEntity
import com.example.flowlog.data.local.entity.StudyDecisionEntity
import com.example.flowlog.data.local.entity.ActivityConflictEntity
import com.example.flowlog.data.local.dao.AutoButtonScheduleDao
import com.example.flowlog.data.local.dao.CalendarEventDao
import com.example.flowlog.data.local.dao.DailyCueDao
import com.example.flowlog.data.local.dao.DailyGoalDao
import com.example.flowlog.data.local.dao.EventLogDao
import com.example.flowlog.data.local.dao.ExternalSampleDao
import com.example.flowlog.data.local.dao.LectureCalendarInfoDao
import com.example.flowlog.data.local.dao.MigrationErrorDao
import com.example.flowlog.data.local.dao.OrganizedPetiteDao
import com.example.flowlog.data.local.dao.SyncBatchDao
import com.example.flowlog.data.local.dao.TodoDao
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.entity.AutoButtonScheduleEntity
import com.example.flowlog.data.local.entity.AutoButtonSkipDateEntity
import com.example.flowlog.data.local.entity.AutoButtonUndoSnapshotEntity
import com.example.flowlog.data.local.entity.CalendarEventEntity
import com.example.flowlog.data.local.entity.DailyCueEntity
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.local.entity.DailySummaryEntity
import com.example.flowlog.data.local.entity.EventLogEntity
import com.example.flowlog.data.local.entity.ExamStrategyCheckEntity
import com.example.flowlog.data.local.entity.ExternalSampleEntity
import com.example.flowlog.data.local.entity.InstallationEntity
import com.example.flowlog.data.local.entity.LectureCalendarInfoEntity
import com.example.flowlog.data.local.entity.MigrationErrorEntity
import com.example.flowlog.data.local.entity.NotificationLogEntity
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import com.example.flowlog.data.local.entity.SyncBatchEntity
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.local.entity.TodoWorkSessionEntity
import com.example.flowlog.data.local.entity.UserEntity
import com.example.flowlog.data.constants.EventSource
import com.example.flowlog.data.constants.EventType
import com.example.flowlog.data.repository.EventLogRepository
import com.example.flowlog.data.repository.EventMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        // Phase 1 — 핵심
        ActivityEntity::class,
        StudyLinkEntity::class,
        StudyDecisionEntity::class,
        ActivityConflictEntity::class,
        AutoButtonScheduleEntity::class,
        AutoButtonSkipDateEntity::class,
        AutoButtonUndoSnapshotEntity::class,
        TodoEntity::class,
        EventLogEntity::class,
        MigrationErrorEntity::class,

        // Phase 2 — 오늘의 목표 / 작업 세션
        DailyGoalRecommendationEntity::class,
        DailyGoalItemEntity::class,
        TodoWorkSessionEntity::class,

        // Phase 3 — 동기화 / 통계 / 계정
        SyncBatchEntity::class,
        DailySummaryEntity::class,
        NotificationLogEntity::class,
        UserEntity::class,
        InstallationEntity::class,

        // Phase 4 — Exam (feature removed; entity kept until the exam cleanup migration,
        // see docs/PROJECT_CONTEXT.md "Planned Cleanup" — not done in v25)
        ExamStrategyCheckEntity::class,
        OrganizedPetiteEntity::class,

        // Phase 5 — Calendar
        CalendarEventEntity::class,
        LectureCalendarInfoEntity::class,
        DailyCueEntity::class,

        // Phase 6 — External samples (Health Connect; usage statistics later)
        ExternalSampleEntity::class,
    ],
    version = 25,
    // 장기적으로는 schema export + Migration 검증을 붙이는 것이 바람직하지만,
    // 현재 단계에서는 개발 편의상 schema 파일 생성을 보류한다.
    exportSchema = false
)
abstract class FlowlogDatabase : RoomDatabase() {

    // Phase 1 DAOs
    abstract fun activityDao(): ActivityDao
    abstract fun activityRevisionDao(): ActivityRevisionDao
    abstract fun studyDao(): StudyDao
    abstract fun autoButtonScheduleDao(): AutoButtonScheduleDao
    abstract fun todoDao(): TodoDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun migrationErrorDao(): MigrationErrorDao

    // Phase 2 DAOs
    abstract fun dailyGoalDao(): DailyGoalDao

    // Phase 3 DAOs
    abstract fun syncBatchDao(): SyncBatchDao

    // Phase 4 DAOs
    abstract fun organizedPetiteDao(): OrganizedPetiteDao

    // Phase 5 DAOs
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun lectureCalendarInfoDao(): LectureCalendarInfoDao
    abstract fun dailyCueDao(): DailyCueDao

    // Phase 6 DAOs
    abstract fun externalSampleDao(): ExternalSampleDao

    companion object {
        @Volatile
        private var INSTANCE: FlowlogDatabase? = null

        fun getInstance(context: Context): FlowlogDatabase {
            INSTANCE?.let { return it }
            var created = false
            val instance = synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlowlogDatabase::class.java,
                    "flowlog.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, FlowlogMigration23To24, FlowlogMigration24To25)
                    .build().also {
                        INSTANCE = it
                        created = true
                    }
            }
            // Outside the lock and after INSTANCE is set, so the getInstance() call inside
            // EventLogRepository returns at once (no recursion, no second build).
            if (created) recordMigrationIfVersionChanged(context.applicationContext, instance)
            return instance
        }

        private const val VERSION_PREFS = "flowlog_db_version"
        private const val KEY_LAST_OPENED_VERSION = "last_opened_version"

        /**
         * MIGRATION_COMPLETED {from, to} when the version this process opened differs from
         * the one stored at the last open. Room runs migrations on the first open, not in
         * build(), so this opens the database on a background thread and reads the version
         * afterwards; if a migration throws there, nothing is recorded or stored (the same
         * exception reaches the app's own first query). No stored version (first install, or
         * the first run of a build with this check) stores the version without an event.
         * MIGRATION_STARTED / MIGRATION_FAILED are not recorded: the event table cannot be
         * written before or during a migration, and a failed one leaves it unusable.
         */
        private fun recordMigrationIfVersionChanged(appContext: Context, db: FlowlogDatabase) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                runCatching {
                    val openedVersion = db.openHelper.writableDatabase.version
                    val prefs = appContext.getSharedPreferences(VERSION_PREFS, Context.MODE_PRIVATE)
                    val stored = prefs.getInt(KEY_LAST_OPENED_VERSION, 0).takeIf { it > 0 }
                    val from = EventMetadata.migrationFromVersion(stored, openedVersion)
                    if (from != null) {
                        EventLogRepository(appContext).log(
                            eventType = EventType.MIGRATION_COMPLETED,
                            source = EventSource.SYSTEM,
                            metadataJson = EventMetadata.migrationCompleted(from, openedVersion)
                        )
                    }
                    if (stored != openedVersion) {
                        prefs.edit().putInt(KEY_LAST_OPENED_VERSION, openedVersion).apply()
                    }
                }.onFailure { Log.w("FlowlogDatabase", "version check skipped: ${it.javaClass.simpleName}") }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN sourceType TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("ALTER TABLE activities ADD COLUMN sourceId TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS auto_button_schedules (
                        scheduleId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        category TEXT NOT NULL,
                        repeatDaysMask INTEGER NOT NULL,
                        startMinuteOfDay INTEGER NOT NULL,
                        endMinuteOfDay INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL,
                        notifyOnStart INTEGER NOT NULL,
                        notifyOnEnd INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_schedules_userId ON auto_button_schedules(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_schedules_isEnabled ON auto_button_schedules(isEnabled)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_schedules_userId_isDeleted ON auto_button_schedules(userId, isDeleted)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS auto_button_skip_dates (
                        id TEXT NOT NULL PRIMARY KEY,
                        scheduleId TEXT NOT NULL,
                        dateKey INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_auto_button_skip_dates_scheduleId_dateKey ON auto_button_skip_dates(scheduleId, dateKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_skip_dates_dateKey ON auto_button_skip_dates(dateKey)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS auto_button_undo_snapshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        scheduleId TEXT NOT NULL,
                        autoActivityId TEXT NOT NULL,
                        previousActivityId TEXT,
                        previousActivityTitle TEXT,
                        previousActivityCategory TEXT,
                        previousActivityStartTime INTEGER,
                        previousActivityEndTimeBeforeAuto INTEGER,
                        triggeredAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        isUsed INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_undo_snapshots_scheduleId ON auto_button_undo_snapshots(scheduleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_undo_snapshots_autoActivityId ON auto_button_undo_snapshots(autoActivityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_undo_snapshots_expiresAt ON auto_button_undo_snapshots(expiresAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auto_button_undo_snapshots_isUsed ON auto_button_undo_snapshots(isUsed)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN recommendationMode TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN workplaceDetected INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN workplaceBlocksJson TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN selectedTodoIdsJson TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN heavyTodoId TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN heavyBurdenLevel TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN heavyReason TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN heavyDistributionSnapshotJson TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN lightTodoId TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN lightBurdenLevel TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN lightReason TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN lightDistributionSnapshotJson TEXT")
                db.execSQL("ALTER TABLE daily_goal_recommendations ADD COLUMN plannedItemsJson TEXT")

                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN burdenLevel TEXT")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN plannedStartMillis INTEGER")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN plannedEndMillis INTEGER")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN recommendedDurationMinutes INTEGER")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN userActionStatus TEXT NOT NULL DEFAULT 'PLANNED'")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN actualStartedAt INTEGER")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN actualCompletedAt INTEGER")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN linkedActivityId TEXT")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN completedTodoId TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN burdenLevel TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN burdenGroupKey TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN burdenScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE todos ADD COLUMN burdenReasonJson TEXT")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN burdenReasonJson TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN notificationScheduledAtMillis INTEGER")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auto_button_undo_snapshots ADD COLUMN previousGoalMillis INTEGER")
                db.execSQL("ALTER TABLE auto_button_undo_snapshots ADD COLUMN previousLinkedTodoId INTEGER")
                db.execSQL("ALTER TABLE auto_button_undo_snapshots ADD COLUMN previousLinkedTodoTitle TEXT")
                db.execSQL("ALTER TABLE auto_button_undo_snapshots ADD COLUMN previousSourceType TEXT")
                db.execSQL("ALTER TABLE auto_button_undo_snapshots ADD COLUMN previousSourceId TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exam_strategy_checks ADD COLUMN undoneAtMillis INTEGER")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN notificationDeliveredAt INTEGER")
                db.execSQL("ALTER TABLE daily_goal_items ADD COLUMN notificationClickedAt INTEGER")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS organized_petites (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        sourceType TEXT NOT NULL,
                        sourceId TEXT,
                        category TEXT,
                        dateMillis INTEGER,
                        linkedActivityName TEXT,
                        activityCategory TEXT,
                        isCompleted INTEGER NOT NULL,
                        priorityScore INTEGER NOT NULL,
                        burdenScore INTEGER,
                        isSeverelyBehind INTEGER,
                        totalStudyMinutesSinceD7 INTEGER,
                        studiedDaysSinceD7 INTEGER,
                        missedDaysSinceD7 INTEGER,
                        aiComment TEXT,
                        estimatedMinutes INTEGER,
                        stepsJson TEXT NOT NULL,
                        examDValue INTEGER,
                        routineTimerDurationMillis INTEGER,
                        routineTimerCategory TEXT,
                        rank INTEGER NOT NULL,
                        isDismissed INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_organized_petites_userId ON organized_petites(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_organized_petites_isDismissed ON organized_petites(isDismissed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_organized_petites_userId_sourceType_sourceId ON organized_petites(userId, sourceType, sourceId)")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN exerciseSetsJson TEXT")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_events (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        location TEXT,
                        description TEXT,
                        source TEXT,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_startTime ON calendar_events(startTime)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_type ON calendar_events(type)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS lecture_calendar_infos (
                        eventId TEXT NOT NULL PRIMARY KEY,
                        type TEXT NOT NULL,
                        courseTitle TEXT,
                        lectureTitle TEXT,
                        classDate INTEGER,
                        startTime INTEGER,
                        endTime INTEGER,
                        week INTEGER,
                        syllabusText TEXT,
                        previewCardId TEXT,
                        location TEXT,
                        description TEXT,
                        sourceId TEXT,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lecture_calendar_infos_classDate ON lecture_calendar_infos(classDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_lecture_calendar_infos_type ON lecture_calendar_infos(type)")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE organized_petites ADD COLUMN calendarTaskType TEXT")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN linkedPetiteId TEXT")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE organized_petites ADD COLUMN autoStartEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE organized_petites ADD COLUMN autoStartTime24 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE organized_petites ADD COLUMN autoStartEndTime24 TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auto_button_schedules ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_cues (
                        userId TEXT NOT NULL,
                        cueId INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        title TEXT NOT NULL,
                        timerDurationMillis INTEGER,
                        timerCategory TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        archivedAt INTEGER,
                        PRIMARY KEY(userId, cueId)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_cues_userId ON daily_cues(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_cues_archivedAt ON daily_cues(archivedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_cues_userId_archivedAt ON daily_cues(userId, archivedAt)")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE daily_cues ADD COLUMN recommendationTiming TEXT NOT NULL DEFAULT 'NONE'"
                )
                db.execSQL(
                    "ALTER TABLE daily_cues ADD COLUMN note TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE activities
                    SET sourceId = COALESCE(sourceId, linkedPetiteId),
                        syncStatus = 'PENDING'
                    WHERE linkedPetiteId IS NOT NULL
                      AND sourceType IN ('LEARNING_PLAN_PREVIEW', 'LEARNING_PLAN_STUDY')
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN calendarSourceId TEXT")
                db.execSQL("ALTER TABLE todos ADD COLUMN calendarSourceType TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_todos_calendarSourceId ON todos(calendarSourceId)")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN calendarPlanId TEXT")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auto_button_schedules ADD COLUMN sourceDateKey INTEGER")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auto_button_schedules ADD COLUMN sourceDateKeysCsv TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE auto_button_schedules ADD COLUMN sourceEventIdsCsv TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS exam_strategy_checks (
                        checkId TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        examTodoLegacyId INTEGER NOT NULL,
                        subjectTitleSnapshot TEXT NOT NULL,
                        examDateMillis INTEGER NOT NULL,
                        strategyDValue INTEGER NOT NULL,
                        strategyLabelSnapshot TEXT NOT NULL,
                        checkedAtMillis INTEGER NOT NULL,
                        checkedOnDateKey INTEGER NOT NULL,
                        checkedOnDaysUntilExam INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'PENDING'
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exam_strategy_checks_userId ON exam_strategy_checks(userId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exam_strategy_checks_syncStatus ON exam_strategy_checks(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_exam_strategy_checks_examTodoLegacyId_strategyDValue ON exam_strategy_checks(examTodoLegacyId, strategyDValue)")
            }
        }
    }
}
