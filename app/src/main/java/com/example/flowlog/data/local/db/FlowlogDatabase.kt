package com.example.flowlog.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.flowlog.data.local.dao.ActivityDao
import com.example.flowlog.data.local.dao.DailyGoalDao
import com.example.flowlog.data.local.dao.EventLogDao
import com.example.flowlog.data.local.dao.MigrationErrorDao
import com.example.flowlog.data.local.dao.SyncBatchDao
import com.example.flowlog.data.local.dao.TodoDao
import com.example.flowlog.data.local.entity.ActivityEntity
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
    version = 1,
    // 장기적으로는 schema export + Migration 검증을 붙이는 것이 바람직하지만,
    // 현재 단계에서는 개발 편의상 schema 파일 생성을 보류한다.
    exportSchema = false
)
abstract class FlowlogDatabase : RoomDatabase() {

    // Phase 1 DAOs
    abstract fun activityDao(): ActivityDao
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
                    // version = 1 유지. 앞으로 스키마 변경 시에는 version bump + Migration 추가로 관리.
                    .build().also { INSTANCE = it }
            }
        }
    }
}
