package com.yash.privategallery.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yash.privategallery.data.database.entity.PrivateAlbumEntity
import com.yash.privategallery.data.database.entity.PrivateMediaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrivateMediaDao {
    @Query("SELECT * FROM private_media WHERE isTrashed = 0 ORDER BY dateTaken DESC")
    fun observeAll(): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE isTrashed = 0 AND albumId = :albumId ORDER BY dateTaken DESC")
    fun observeForAlbum(albumId: Long): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE isTrashed = 1 ORDER BY trashedAt DESC")
    fun observeTrash(): Flow<List<PrivateMediaEntity>>

    @Query("SELECT * FROM private_media WHERE id = :id")
    suspend fun getById(id: Long): PrivateMediaEntity?

    @Insert
    suspend fun insert(entity: PrivateMediaEntity): Long

    @Update
    suspend fun update(entity: PrivateMediaEntity)

    @Query("DELETE FROM private_media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE private_media SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE private_media SET isTrashed = 1, trashedAt = :trashedAt WHERE id = :id")
    suspend fun moveToTrash(id: Long, trashedAt: Long)

    @Query("UPDATE private_media SET isTrashed = 0, trashedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("SELECT * FROM private_albums ORDER BY createdAt DESC")
    fun observeAlbums(): Flow<List<PrivateAlbumEntity>>

    @Insert
    suspend fun insertAlbum(album: PrivateAlbumEntity): Long

    @Query("SELECT COUNT(*) FROM private_media WHERE albumId = :albumId AND isTrashed = 0")
    suspend fun getAlbumItemCount(albumId: Long): Int
}
