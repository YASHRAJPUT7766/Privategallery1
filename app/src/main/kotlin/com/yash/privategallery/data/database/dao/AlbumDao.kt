package com.yash.privategallery.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yash.privategallery.data.database.entity.AlbumEntity
import com.yash.privategallery.data.database.entity.AlbumMediaCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Query("SELECT * FROM custom_albums WHERE isPrivate = 0 ORDER BY createdAt DESC")
    fun observeCustomAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM custom_albums WHERE id = :albumId")
    suspend fun getAlbum(albumId: Long): AlbumEntity?

    @Insert
    suspend fun insert(album: AlbumEntity): Long

    @Update
    suspend fun update(album: AlbumEntity)

    @Query("DELETE FROM custom_albums WHERE id = :albumId")
    suspend fun deleteById(albumId: Long)

    @Query("SELECT COUNT(*) FROM album_media_cross_ref WHERE albumId = :albumId")
    suspend fun getItemCount(albumId: Long): Int

    @Query("SELECT mediaStoreId FROM album_media_cross_ref WHERE albumId = :albumId")
    fun observeMediaIdsForAlbum(albumId: Long): Flow<List<Long>>

    @Insert
    suspend fun addMediaToAlbum(refs: List<AlbumMediaCrossRef>)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId AND mediaStoreId IN (:mediaIds)")
    suspend fun removeMediaFromAlbum(albumId: Long, mediaIds: List<Long>)

    @Query("""
        SELECT mediaStoreId FROM album_media_cross_ref
        WHERE albumId = :albumId ORDER BY addedAt DESC LIMIT 1
    """)
    suspend fun getMostRecentMediaId(albumId: Long): Long?
}
