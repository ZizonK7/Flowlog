package com.example.flowlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.flowlog.data.local.entity.OrganizedPetiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizedPetiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<OrganizedPetiteEntity>)

    @Query("DELETE FROM organized_petites WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("""
        SELECT * FROM organized_petites
        WHERE userId = :userId
          AND isDismissed = 0
        ORDER BY rank ASC, priorityScore ASC, title ASC
    """)
    fun observeActive(userId: String): Flow<List<OrganizedPetiteEntity>>

    @Query("""
        SELECT * FROM organized_petites
        WHERE userId = :userId
          AND isDismissed = 1
        ORDER BY updatedAt DESC
    """)
    suspend fun getDismissed(userId: String): List<OrganizedPetiteEntity>

    @Query("""
        UPDATE organized_petites
        SET isDismissed = 1,
            updatedAt = :updatedAt
        WHERE userId = :userId
          AND sourceType = :sourceType
          AND IFNULL(sourceId, '') = IFNULL(:sourceId, '')
    """)
    suspend fun dismissBySource(userId: String, sourceType: String, sourceId: String?, updatedAt: Long)

    @Query("""
        UPDATE organized_petites
        SET isDismissed = 0,
            updatedAt = :updatedAt
        WHERE userId = :userId
          AND sourceType = :sourceType
          AND IFNULL(sourceId, '') = IFNULL(:sourceId, '')
    """)
    suspend fun restoreBySource(userId: String, sourceType: String, sourceId: String?, updatedAt: Long)

    @Transaction
    suspend fun replaceAllForUser(userId: String, items: List<OrganizedPetiteEntity>) {
        deleteAllForUser(userId)
        if (items.isNotEmpty()) insertAll(items)
    }
}
