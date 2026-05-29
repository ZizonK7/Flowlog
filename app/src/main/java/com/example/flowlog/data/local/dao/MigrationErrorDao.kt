package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.flowlog.data.local.entity.MigrationErrorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MigrationErrorDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMigrationError(error: MigrationErrorEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMigrationErrors(errors: List<MigrationErrorEntity>)

    @Query("""
        SELECT * FROM migration_errors
        WHERE sourceType = :sourceType
        ORDER BY createdAt ASC
    """)
    suspend fun getErrorsBySourceType(sourceType: String): List<MigrationErrorEntity>

    @Query("SELECT * FROM migration_errors ORDER BY createdAt DESC")
    fun observeAllMigrationErrors(): Flow<List<MigrationErrorEntity>>

    @Query("SELECT COUNT(*) FROM migration_errors")
    suspend fun getMigrationErrorCount(): Int
}
