package com.example.flowlog.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v23 → v24 (비파괴)
 * - activities: remoteRevision(원격 revision, 기본 0), originalCategory(첫 재분류 이전 category)
 * - activity_study_links, study_decisions 신규 (owner-scoped, 복합 PK)
 */
object FlowlogMigration23To24 : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE activities ADD COLUMN remoteRevision INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE activities ADD COLUMN originalCategory TEXT")
        db.execSQL("CREATE TABLE IF NOT EXISTS `activity_conflicts` (`userId` TEXT NOT NULL, `activityId` TEXT NOT NULL, `remoteRevision` INTEGER NOT NULL, `localUpdatedAt` INTEGER NOT NULL, `localDescription` TEXT NOT NULL, `remoteDescription` TEXT NOT NULL, `reason` TEXT NOT NULL, PRIMARY KEY(`userId`, `activityId`))")

        db.execSQL("CREATE TABLE IF NOT EXISTS `activity_study_links` (`linkId` TEXT NOT NULL, `userId` TEXT NOT NULL, `activityId` TEXT NOT NULL, `courseId` TEXT NOT NULL, `lessonId` TEXT NOT NULL, `lessonDate` TEXT NOT NULL, `phase` TEXT NOT NULL, `segmentsJson` TEXT NOT NULL, `snapshotJson` TEXT NOT NULL, `sourceVersion` TEXT NOT NULL, `confirmedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `remoteRevision` INTEGER NOT NULL DEFAULT 0, `deletedAt` INTEGER, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', PRIMARY KEY(`userId`, `linkId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_study_links_userId_activityId` ON `activity_study_links` (`userId`, `activityId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_study_links_userId_lessonId` ON `activity_study_links` (`userId`, `lessonId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_study_links_syncStatus` ON `activity_study_links` (`syncStatus`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `study_decisions` (`decisionId` TEXT NOT NULL, `userId` TEXT NOT NULL, `proposalId` TEXT NOT NULL, `kind` TEXT NOT NULL, `activityId` TEXT, `category` TEXT, `localDate` TEXT NOT NULL, `minuteOfDay` INTEGER NOT NULL, `weekday` INTEGER NOT NULL, `outcome` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `payloadJson` TEXT NOT NULL, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', PRIMARY KEY(`userId`, `decisionId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_decisions_userId_createdAt` ON `study_decisions` (`userId`, `createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_decisions_userId_proposalId` ON `study_decisions` (`userId`, `proposalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_decisions_syncStatus` ON `study_decisions` (`syncStatus`)")
    }
}
