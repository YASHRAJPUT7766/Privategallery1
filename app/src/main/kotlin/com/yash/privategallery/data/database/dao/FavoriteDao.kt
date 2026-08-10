package com.yash.privategallery.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.yash.privategallery.data.database.entity.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT mediaStoreId FROM favorites")
    fun observeFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteById(mediaStoreId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaStoreId = :mediaStoreId)")
    suspend fun isFavorite(mediaStoreId: Long): Boolean
}
