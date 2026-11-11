package com.example.flowlog.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v24 → v25 (비파괴)
 * - external_samples 신규: 폰의 다른 곳(Health Connect, 앱 사용 통계)에서 읽은 샘플
 * - exam 잔재 정리 (2026-11-09, 사용자 결정):
 *   1. exam_strategy_checks 표 삭제 (ExamStrategyCheckEntity 제거).
 *   2. organized_petites에서 exam 칸 5개(isSeverelyBehind, totalStudyMinutesSinceD7,
 *      studiedDaysSinceD7, missedDaysSinceD7, examDValue) 제거. SQLite의 DROP COLUMN은
 *      Android 버전에 따라 없으므로 표를 다시 만들어 나머지 칸을 옮긴다. 칸·타입·NOT NULL·
 *      인덱스 이름은 OrganizedPetiteEntity에서 Room이 만드는 스키마와 같아야 한다
 *      (Room이 마이그레이션 뒤 대조한다). 엔티티에 SQL 기본값이 없어 기본값은 붙이지 않는다.
 *   3. 남은 UNIVERSITY_EXAM Todo를 한 번 NORMAL로 내리고 PENDING으로 표시해 다시 올린다
 *      (지운 TodoDao.downgradeRetiredExamTodos가 동기화마다 하던 일).
 * 지난 마이그레이션의 exam SQL은 옛 버전에서 올라오는 기기를 위해 그대로 둔다.
 */
object FlowlogMigration24To25 : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `external_samples` (`sampleId` TEXT NOT NULL, `userId` TEXT NOT NULL, `provider` TEXT NOT NULL, `kind` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `value` REAL NOT NULL, `unit` TEXT NOT NULL, `sourcePackage` TEXT, `payloadJson` TEXT, `fetchedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', PRIMARY KEY(`sampleId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_external_samples_userId` ON `external_samples` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_external_samples_userId_provider_kind_startTime` ON `external_samples` (`userId`, `provider`, `kind`, `startTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_external_samples_syncStatus` ON `external_samples` (`syncStatus`)")

        // 1. exam strategy checks
        db.execSQL("DROP TABLE IF EXISTS `exam_strategy_checks`")

        // 2. organized_petites without the exam columns
        db.execSQL("DROP TABLE IF EXISTS `organized_petites_new`")
        db.execSQL("CREATE TABLE IF NOT EXISTS `organized_petites_new` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT, `category` TEXT, `dateMillis` INTEGER, `linkedActivityName` TEXT, `activityCategory` TEXT, `isCompleted` INTEGER NOT NULL, `priorityScore` INTEGER NOT NULL, `burdenScore` INTEGER, `aiComment` TEXT, `estimatedMinutes` INTEGER, `stepsJson` TEXT NOT NULL, `routineTimerDurationMillis` INTEGER, `routineTimerCategory` TEXT, `rank` INTEGER NOT NULL, `isDismissed` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `calendarTaskType` TEXT, `autoStartEnabled` INTEGER NOT NULL, `autoStartTime24` TEXT NOT NULL, `autoStartEndTime24` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL(
            "INSERT INTO `organized_petites_new` ($ORGANIZED_PETITE_COLUMNS) " +
                "SELECT $ORGANIZED_PETITE_COLUMNS FROM `organized_petites`"
        )
        db.execSQL("DROP TABLE `organized_petites`")
        db.execSQL("ALTER TABLE `organized_petites_new` RENAME TO `organized_petites`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organized_petites_userId` ON `organized_petites` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organized_petites_isDismissed` ON `organized_petites` (`isDismissed`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_organized_petites_userId_sourceType_sourceId` ON `organized_petites` (`userId`, `sourceType`, `sourceId`)")

        // 3. retired exam Todos, once
        db.execSQL(
            "UPDATE `todos` SET `category` = 'NORMAL', " +
                "`updatedAt` = (strftime('%s','now') * 1000), `syncStatus` = 'PENDING' " +
                "WHERE `category` = 'UNIVERSITY_EXAM'"
        )
    }

    /** The 25 columns of [com.example.flowlog.data.local.entity.OrganizedPetiteEntity], in entity order. */
    private const val ORGANIZED_PETITE_COLUMNS =
        "`id`, `userId`, `title`, `sourceType`, `sourceId`, `category`, `dateMillis`, " +
            "`linkedActivityName`, `activityCategory`, `isCompleted`, `priorityScore`, `burdenScore`, " +
            "`aiComment`, `estimatedMinutes`, `stepsJson`, `routineTimerDurationMillis`, " +
            "`routineTimerCategory`, `rank`, `isDismissed`, `createdAt`, `updatedAt`, " +
            "`calendarTaskType`, `autoStartEnabled`, `autoStartTime24`, `autoStartEndTime24`"
}
