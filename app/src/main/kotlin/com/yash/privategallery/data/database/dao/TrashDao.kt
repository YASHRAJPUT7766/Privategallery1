package com.yash.privategallery.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yash.privategallery.data.database.entity.TrashEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash ORDER BY trashedAt DESC")
    fun observeAll(): Flow<List<TrashEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TrashEntity>)

    @Query("DELETE FROM trash WHERE mediaStoreId IN (:mediaStoreIds)")
    suspend fun deleteByIds(mediaStoreIds: List<Long>)

    @Query("SELECT mediaStoreId FROM trash WHERE trashedAt < :cutoffEpochMillis")
    suspend fun getExpiredIds(cutoffEpochMillis: Long): List<Long>
}
