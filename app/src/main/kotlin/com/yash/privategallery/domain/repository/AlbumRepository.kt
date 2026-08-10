package com.yash.privategallery.domain.repository

import com.yash.privategallery.domain.model.Album
import com.yash.privategallery.domain.model.AuthMethod
import kotlinx.coroutines.flow.Flow

/**
 * Access to albums — both computed default albums (All Images, Camera,
 * Screenshots, Downloads, Favorites, Videos, Recently Added/Deleted, Private)
 * and user-created custom albums tracked in Room (Section 15, 40).
 */
interface AlbumRepository {

    fun observeAlbums(): Flow<List<Album>>

    fun observePrivateAlbums(): Flow<List<Album>>

    /**
     * Media belonging to a CUSTOM album, resolved via Room membership
     * ([com.yash.privategallery.data.database.entity.AlbumMediaCrossRef]) —
     * NOT a MediaStore bucket query. Default albums (Camera, Screenshots,
     * etc.) are filtered client-side from the full library instead; this
     * method is only meaningful for [com.yash.privategallery.domain.model.AlbumKind.CUSTOM].
     */
    fun observeCustomAlbumMedia(albumId: Long): Flow<List<com.yash.privategallery.domain.model.MediaItem>>

    suspend fun createCustomAlbum(name: String, iconKey: String?): Album

    suspend fun createPrivateAlbum(name: String, iconKey: String?): Album

    suspend fun renameAlbum(albumId: Long, newName: String)

    suspend fun deleteAlbum(albumId: Long)

    suspend fun addMediaToAlbum(albumId: Long, mediaIds: List<Long>)

    suspend fun removeMediaFromAlbum(albumId: Long, mediaIds: List<Long>)

    /**
     * Locks an existing (previously unlocked) album (Section 19). The caller
     * must have already run the user through setting up [authMethod]'s secret
     * via the security layer before calling this — this method only flips the
     * album's lock metadata once a secret is confirmed to exist.
     */
    suspend fun lockAlbum(albumId: Long, authMethod: AuthMethod)

    /** Unlocks an album. Caller must have already verified authentication succeeded. */
    suspend fun unlockAlbum(albumId: Long)

    /**
     * Changes a locked album's authentication method/secret. Caller must have
     * already verified the *existing* authentication before calling this, per
     * Section 19's "Change Lock" flow.
     */
    suspend fun changeAlbumLock(albumId: Long, newAuthMethod: AuthMethod)
}
