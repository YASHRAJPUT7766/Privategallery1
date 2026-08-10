package com.yash.privategallery.data.repository

import com.yash.privategallery.data.database.dao.FavoriteDao
import com.yash.privategallery.data.database.dao.TrashDao
import com.yash.privategallery.data.database.entity.FavoriteEntity
import com.yash.privategallery.data.database.entity.TrashEntity
import com.yash.privategallery.data.media.MediaStoreDataSource
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val favoriteDao: FavoriteDao,
    private val trashDao: TrashDao
) : MediaRepository {

    override fun observeAllMedia(sortOrder: SortOrder): Flow<List<MediaItem>> =
        combineWithOverlays(mediaStoreDataSource.observeMedia(sortOrder = sortOrder))

    override fun observeMediaForBucket(bucketId: Long, sortOrder: SortOrder): Flow<List<MediaItem>> =
        combineWithOverlays(mediaStoreDataSource.observeMedia(bucketId = bucketId, sortOrder = sortOrder))

    /**
     * Section 27: joins the raw MediaStore snapshot against trash entries so
     * the Trash screen gets both metadata AND trashedAt in one query, rather
     * than one lookup per trashed item.
     */
    override fun observeTrashedMedia(): Flow<List<Pair<MediaItem, Long>>> =
        combine(
            mediaStoreDataSource.observeMedia(sortOrder = SortOrder.NEWEST_FIRST),
            trashDao.observeAll()
        ) { items, trashEntries ->
            val itemsById = items.associateBy { it.id }
            trashEntries
                .sortedByDescending { it.trashedAt }
                .mapNotNull { entry -> itemsById[entry.mediaStoreId]?.let { it to entry.trashedAt } }
        }

    /**
     * Merges the raw MediaStore stream with the Room-backed favorite/trash
     * overlays and filters out anything currently trashed from the "live"
     * view (trashed items only appear via the dedicated Trash screen's own
     * query — Section 27).
     */
    private fun combineWithOverlays(mediaFlow: Flow<List<MediaItem>>): Flow<List<MediaItem>> =
        combine(
            mediaFlow,
            favoriteDao.observeFavoriteIds(),
            trashDao.observeAll()
        ) { items, favoriteIds, trashEntries ->
            val trashedIds = trashEntries.map { it.mediaStoreId }.toSet()
            val favoriteIdSet = favoriteIds.toSet()
            items
                .filter { it.id !in trashedIds }
                .map { it.copy(isFavorite = it.id in favoriteIdSet) }
        }

    override suspend fun getMediaById(id: Long): MediaItem? =
        mediaStoreDataSource.queryMedia().find { it.id == id }

    override suspend fun rescan() {
        // Triggering a query is sufficient here since observeMedia() is already
        // content-observer-driven; an explicit MediaScannerConnection.scanFile
        // sweep would be added at the call site for newly-written files if this
        // repository ever writes files directly (it currently doesn't — imports
        // go through the system photo picker or the private vault flow).
        mediaStoreDataSource.queryMedia()
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteDao.insert(FavoriteEntity(mediaStoreId = id, favoritedAt = System.currentTimeMillis()))
        } else {
            favoriteDao.deleteById(id)
        }
    }

    override suspend fun moveToTrash(ids: List<Long>) {
        val now = System.currentTimeMillis()
        trashDao.insertAll(ids.map { TrashEntity(mediaStoreId = it, trashedAt = now, originalBucketId = null) })
    }

    override suspend fun restoreFromTrash(ids: List<Long>) {
        trashDao.deleteByIds(ids)
    }

    override suspend fun permanentlyDelete(ids: List<Long>) {
        mediaStoreDataSource.deleteMedia(ids)
        trashDao.deleteByIds(ids)
        ids.forEach { favoriteDao.deleteById(it) }
    }

    override suspend fun rename(id: Long, newDisplayName: String) {
        // Renaming requires a ContentValues update via MediaStoreDataSource;
        // deferred to keep this checkpoint's scope focused — tracked as a
        // follow-up alongside the Image Info screen's "rename" action.
        throw NotImplementedError("Rename will be implemented alongside the Image Info screen")
    }
}
