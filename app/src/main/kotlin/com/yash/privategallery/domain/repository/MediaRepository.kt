package com.yash.privategallery.domain.repository

import com.yash.privategallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/** Sort orders applied across images, albums, and (separately) private gallery — Section 32. */
enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    NAME_A_Z,
    NAME_Z_A,
    LARGEST_FIRST,
    SMALLEST_FIRST
}

/**
 * Read/write access to the device's public media library via MediaStore.
 * Implementations must perform all scanning/querying off the main thread
 * (Section 39) and must never load full-resolution bitmaps here — this layer
 * deals in [MediaItem] metadata and URIs; actual pixel loading is Coil's job
 * in the UI layer.
 */
interface MediaRepository {

    /** Live stream of all non-trashed normal media, re-emitting on MediaStore changes. */
    fun observeAllMedia(sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<List<MediaItem>>

    /** Live stream scoped to a single bucket/album id (e.g. Camera, Screenshots, a custom album). */
    fun observeMediaForBucket(bucketId: Long, sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<List<MediaItem>>

    /**
     * Live stream of trashed items (Section 27), each paired with the epoch-
     * millis timestamp it was trashed at, newest-trashed first. Deliberately
     * separate from [observeAllMedia], which excludes trashed items — trash
     * is a dedicated view, not a filter toggle on the main gallery stream.
     */
    fun observeTrashedMedia(): Flow<List<Pair<MediaItem, Long>>>

    suspend fun getMediaById(id: Long): MediaItem?

    /** Forces a fresh MediaStore scan (Section 39: "Perform MediaStore scanning off the main thread"). */
    suspend fun rescan()

    /** Toggles the OS-level "favorite" flag via MediaStore, backed also by Room for cross-check (Section 14). */
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    /** Moves an item to Recently Deleted (soft delete) — never a hard delete directly (Section 27). */
    suspend fun moveToTrash(ids: List<Long>)

    suspend fun restoreFromTrash(ids: List<Long>)

    /** Hard delete — only ever called from the Trash screen's "Permanently Delete" action. */
    suspend fun permanentlyDelete(ids: List<Long>)

    /** Renames the underlying file/MediaStore display name. */
    suspend fun rename(id: Long, newDisplayName: String)
}
