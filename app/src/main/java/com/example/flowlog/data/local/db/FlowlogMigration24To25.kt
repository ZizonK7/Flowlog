package com.example.flowlog.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v24 → v25 (비파괴)
 * - external_samples 신규: 폰의 다른 곳(Health Connect, 나중에 사용 통계)에서 읽은 샘플
 */
object FlowlogMigration24To25 : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `external_samples` (`sampleId` TEXT NOT NULL, `userId` TEXT NOT NULL, `provider` TEXT NOT NULL, `kind` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER NOT NULL, `value` REAL NOT NULL, `unit` TEXT NOT NULL, `sourcePackage` TEXT, `payloadJson` TEXT, `fetchedAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `syncStatus` TEXT NOT NULL DEFAULT 'PENDING', PRIMARY KEY(`sampleId`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_external_samples_userId` ON `external_samples` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_external_samples_userId_provider_kind_startTime` ON `external_samples` (`userId`, `provider`, `kind`, `startTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_external_samples_syncStatus` ON `external_samples` (`syncStatus`)")
    }
}
