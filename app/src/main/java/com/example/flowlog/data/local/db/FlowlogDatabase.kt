package com.example.flowlog.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.flowlog.data.local.dao.ActivityDao
import com.example.flowlog.data.local.dao.AutoButtonScheduleDao
import com.example.flowlog.data.local.dao.DailyGoalDao
import com.example.flowlog.data.local.dao.EventLogDao
import com.example.flowlog.data.local.dao.MigrationErrorDao
import com.example.flowlog.data.local.dao.SyncBatchDao
import com.example.flowlog.data.local.dao.TodoDao
import com.example.flowlog.data.local.entity.ActivityEntity
import com.example.flowlog.data.local.entity.AutoButtonScheduleEntity
import com.example.flowlog.data.local.entity.AutoButtonSkipDateEntity
import com.example.flowlog.data.local.entity.AutoButtonUndoSnapshotEntity
import com.example.flowlog.data.local.entity.DailyGoalItemEntity
import com.example.flowlog.data.local.entity.DailyGoalRecommendationEntity
import com.example.flowlog.data.local.entity.DailySummaryEntity
import com.example.flowlog.data.local.entity.EventLogEntity
import com.example.flowlog.data.local.entity.InstallationEntity
import com.example.flowlog.data.local.entity.MigrationErrorEntity
import com.example.flowlog.data.local.entity.NotificationLogEntity
import com.example.flowlog.data.local.entity.SyncBatchEntity
import com.example.flowlog.data.local.entity.TodoEntity
import com.example.flowlog.data.local.entity.TodoWorkSessionEntity
import com.example.flowlog.data.local.entity.UserEntity

@Database(
    entities = [
        // Phase 1 — 핵심
        ActivityEntity::class,
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
    ],
    version = 5,
    // 장기적으로는 schema export + Migration 검증을 붙이는 것이 바람직하지만,
    // 현재 단계에서는 개발 편의상 schema 파일 생성을 보류한다.
    exportSchema = false
)
abstract class FlowlogDatabase : RoomDatabase() {

    // Phase 1 DAOs
    abstract fun activityDao(): ActivityDao
    abstract fun autoButtonScheduleDao(): AutoButtonScheduleDao
    abstract fun todoDao(): TodoDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun migrationErrorDao(): MigrationErrorDao

    // Phase 2 DAOs
    abstract fun dailyGoalDao(): DailyGoalDao

    // Phase 3 DAOs
    abstract fun syncBatchDao(): SyncBatchDao

    companion object {
        @Volatile
        private var INSTANCE: FlowlogDatabase? = null

        fun getInstance(context: Context): FlowlogDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlowlogDatabase::class.java,
                    "flowlog.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
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
    }
}
