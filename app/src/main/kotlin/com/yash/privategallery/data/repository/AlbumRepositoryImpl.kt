package com.yash.privategallery.data.repository

import com.yash.privategallery.data.database.dao.AlbumDao
import com.yash.privategallery.data.database.dao.FavoriteDao
import com.yash.privategallery.data.database.dao.PrivateMediaDao
import com.yash.privategallery.data.database.entity.AlbumEntity
import com.yash.privategallery.data.database.entity.AlbumMediaCrossRef
import com.yash.privategallery.data.database.entity.PrivateAlbumEntity
import com.yash.privategallery.data.media.MediaStoreDataSource
import com.yash.privategallery.domain.model.Album
import com.yash.privategallery.domain.model.AlbumKind
import com.yash.privategallery.domain.model.AuthMethod
import com.yash.privategallery.domain.repository.AlbumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepositoryImpl @Inject constructor(
    private val albumDao: AlbumDao,
    private val favoriteDao: FavoriteDao,
    private val privateMediaDao: PrivateMediaDao,
    private val mediaStoreDataSource: MediaStoreDataSource
) : AlbumRepository {

    /**
     * Default albums (Section 15) are computed from a live MediaStore query
     * rather than stored as rows — per Section 40's "do not duplicate the
     * entire public gallery unnecessarily into Room". Custom albums come from
     * Room and are merged in alongside them.
     */
    override fun observeAlbums(): Flow<List<Album>> =
        albumDao.observeCustomAlbums().flatMapLatest { customAlbumEntities ->
            val nonPrivate = customAlbumEntities.filter { !it.isPrivate }
            if (nonPrivate.isEmpty()) {
                favoriteDao.observeFavoriteIds().map { favoriteIds -> buildDefaultAlbums(favoriteIds) }
            } else {
                // Combine each custom album's live member-count flow together
                // with favorites, so item counts stay accurate as membership
                // changes (Section 17/29's "Add to Album" reflecting live).
                val countFlows = nonPrivate.map { entity ->
                    albumDao.observeMediaIdsForAlbum(entity.id).map { it.size }
                }
                combine(favoriteDao.observeFavoriteIds(), combine(countFlows) { it.toList() }) { favoriteIds, counts ->
                    val defaultAlbums = buildDefaultAlbums(favoriteIds)
                    val customAlbums = nonPrivate.mapIndexed { index, entity ->
                        Album(
                            id = entity.id,
                            name = entity.name,
                            kind = AlbumKind.CUSTOM,
                            coverUri = null,
                            itemCount = counts.getOrElse(index) { 0 },
                            isLocked = entity.isLocked,
                            authMethod = runCatching { AuthMethod.valueOf(entity.authMethodName) }.getOrDefault(AuthMethod.NONE),
                            iconKey = entity.iconKey,
                            isPrivate = false
                        )
                    }
                    defaultAlbums + customAlbums
                }
            }
        }

    private fun buildDefaultAlbums(favoriteIds: List<Long>): List<Album> {
        val allMedia = mediaStoreDataSource.queryMedia()
        return buildList {
            add(computedAlbum(AlbumKind.ALL_IMAGES, "All Images", allMedia))
            add(computedAlbum(AlbumKind.CAMERA, "Camera", allMedia.filter { it.bucketName == "Camera" }))
            add(computedAlbum(AlbumKind.SCREENSHOTS, "Screenshots", allMedia.filter { it.bucketName?.contains("Screenshot", ignoreCase = true) == true }))
            add(computedAlbum(AlbumKind.DOWNLOADS, "Downloads", allMedia.filter { it.bucketName == "Download" || it.bucketName == "Downloads" }))
            add(computedAlbum(AlbumKind.FAVORITES, "Favorites", allMedia.filter { it.id in favoriteIds.toSet() }))
            add(computedAlbum(AlbumKind.VIDEOS, "Videos", allMedia.filter { it.mediaType == com.yash.privategallery.domain.model.MediaType.VIDEO }))
            add(computedAlbum(AlbumKind.RECENTLY_ADDED, "Recently Added", allMedia.sortedByDescending { it.dateAdded }.take(50)))
        }
    }

    override fun observeCustomAlbumMedia(albumId: Long): Flow<List<com.yash.privategallery.domain.model.MediaItem>> =
        albumDao.observeMediaIdsForAlbum(albumId).map { memberIds ->
            val memberSet = memberIds.toSet()
            mediaStoreDataSource.queryMedia().filter { it.id in memberSet }
        }

    override fun observePrivateAlbums(): Flow<List<Album>> =
        privateMediaDao.observeAlbums().map { entities ->
            entities.map { entity ->
                Album(
                    id = entity.id,
                    name = entity.name,
                    kind = AlbumKind.CUSTOM,
                    coverUri = null,
                    itemCount = privateMediaDao.getAlbumItemCount(entity.id),
                    isLocked = true, // all private albums are implicitly behind the private-gallery lock
                    authMethod = AuthMethod.NONE,
                    iconKey = entity.iconKey,
                    isPrivate = true
                )
            }
        }

    private fun computedAlbum(kind: AlbumKind, name: String, items: List<com.yash.privategallery.domain.model.MediaItem>): Album =
        Album(
            id = kind.ordinal.toLong() * -1, // negative synthetic ids so they never collide with real Room autogen ids
            name = name,
            kind = kind,
            coverUri = items.firstOrNull()?.contentUri,
            itemCount = items.size
        )

    override suspend fun createCustomAlbum(name: String, iconKey: String?): Album {
        val entity = AlbumEntity(name = name, iconKey = iconKey, createdAt = System.currentTimeMillis())
        val id = albumDao.insert(entity)
        return Album(id = id, name = name, kind = AlbumKind.CUSTOM, coverUri = null, itemCount = 0, iconKey = iconKey)
    }

    override suspend fun createPrivateAlbum(name: String, iconKey: String?): Album {
        val entity = PrivateAlbumEntity(name = name, iconKey = iconKey, createdAt = System.currentTimeMillis())
        val id = privateMediaDao.insertAlbum(entity)
        return Album(id = id, name = name, kind = AlbumKind.CUSTOM, coverUri = null, itemCount = 0, iconKey = iconKey, isPrivate = true, isLocked = true)
    }

    override suspend fun renameAlbum(albumId: Long, newName: String) {
        val existing = albumDao.getAlbum(albumId) ?: return
        albumDao.update(existing.copy(name = newName))
    }

    override suspend fun deleteAlbum(albumId: Long) {
        albumDao.deleteById(albumId)
    }

    override suspend fun addMediaToAlbum(albumId: Long, mediaIds: List<Long>) {
        val now = System.currentTimeMillis()
        albumDao.addMediaToAlbum(mediaIds.map { AlbumMediaCrossRef(albumId, it, now) })
    }

    override suspend fun removeMediaFromAlbum(albumId: Long, mediaIds: List<Long>) {
        albumDao.removeMediaFromAlbum(albumId, mediaIds)
    }

    override suspend fun lockAlbum(albumId: Long, authMethod: AuthMethod) {
        val existing = albumDao.getAlbum(albumId) ?: return
        albumDao.update(existing.copy(isLocked = true, authMethodName = authMethod.name))
    }

    override suspend fun unlockAlbum(albumId: Long) {
        val existing = albumDao.getAlbum(albumId) ?: return
        albumDao.update(existing.copy(isLocked = false, authMethodName = AuthMethod.NONE.name))
    }

    override suspend fun changeAlbumLock(albumId: Long, newAuthMethod: AuthMethod) {
        val existing = albumDao.getAlbum(albumId) ?: return
        albumDao.update(existing.copy(authMethodName = newAuthMethod.name))
    }
}
