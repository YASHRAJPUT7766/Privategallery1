package com.yash.privategallery.data.media

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.domain.model.StorageLocation
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads media directly from Android's MediaStore. This is the ONLY class in
 * the app that queries MediaStore's Images/Video collections — kept isolated
 * here so the rest of the app depends on the [MediaItem] domain model instead
 * of Cursor/URI plumbing (Section 58 clean architecture).
 *
 * All query methods run on [Dispatchers.IO] and use a Cursor with an explicit
 * projection (never SELECT *), per Section 39's performance requirements —
 * scanning must never block the main thread, and large photo libraries
 * (10,000-50,000+ items per Section 39) make column selection matter for
 * cursor-window performance.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.DATE_MODIFIED,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Video.VideoColumns.DURATION,
        // DATE_TAKEN lives on the type-specific tables; queried via the generic
        // Files collection it is still accessible under this column name on
        // API 29+, which covers our API 33+ minimum comfortably.
        MediaStore.Files.FileColumns.DATE_TAKEN
    )

    /** Emits the current snapshot immediately, then re-emits on every MediaStore change. */
    fun observeMedia(bucketId: Long? = null, sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<List<MediaItem>> =
        callbackFlow {
            val observerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            suspend fun push() = trySend(queryMedia(bucketId, sortOrder))

            observerScope.launch { push() }

            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    observerScope.launch { push() }
                }
            }
            context.contentResolver.registerContentObserver(
                MediaStore.Files.getContentUri("external"),
                true,
                observer
            )
            awaitClose {
                context.contentResolver.unregisterContentObserver(observer)
                observerScope.cancel()
            }
        }

    suspend fun queryMedia(bucketId: Long? = null, sortOrder: SortOrder = SortOrder.NEWEST_FIRST): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val selectionParts = mutableListOf(
                "(${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?)"
            )
            val selectionArgs = mutableListOf(
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )
            if (bucketId != null) {
                selectionParts.add("${MediaStore.Files.FileColumns.BUCKET_ID} = ?")
                selectionArgs.add(bucketId.toString())
            }

            val sortColumn = when (sortOrder) {
                SortOrder.NEWEST_FIRST, SortOrder.OLDEST_FIRST -> MediaStore.Files.FileColumns.DATE_ADDED
                SortOrder.NAME_A_Z, SortOrder.NAME_Z_A -> MediaStore.Files.FileColumns.DISPLAY_NAME
                SortOrder.LARGEST_FIRST, SortOrder.SMALLEST_FIRST -> MediaStore.Files.FileColumns.SIZE
            }
            val sortDirection = when (sortOrder) {
                SortOrder.NEWEST_FIRST, SortOrder.NAME_Z_A, SortOrder.LARGEST_FIRST -> "DESC"
                else -> "ASC"
            }

            val items = mutableListOf<MediaItem>()
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selectionParts.joinToString(" AND "),
                selectionArgs.toTypedArray(),
                "$sortColumn $sortDirection"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_TAKEN)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val mediaTypeInt = cursor.getInt(typeCol)
                    val isVideo = mediaTypeInt == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE

                    val contentUri = ContentUris.withAppendedId(
                        if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val dateAddedMs = cursor.getLong(dateAddedCol) * 1000L
                    val dateModifiedMs = cursor.getLong(dateModifiedCol) * 1000L
                    // Section 7: "Use the best available timestamp" — prefer DATE_TAKEN,
                    // fall back to DATE_ADDED, then DATE_MODIFIED.
                    val dateTakenRaw = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
                    val resolvedDateTaken = when {
                        dateTakenRaw > 0L -> dateTakenRaw
                        dateAddedMs > 0L -> dateAddedMs
                        else -> dateModifiedMs
                    }

                    items.add(
                        MediaItem(
                            id = id,
                            displayName = cursor.getString(nameCol) ?: "",
                            mediaType = mediaType,
                            storageLocation = StorageLocation.NORMAL,
                            contentUri = contentUri.toString(),
                            filePath = null,
                            dateTaken = resolvedDateTaken,
                            dateAdded = dateAddedMs,
                            dateModified = dateModifiedMs,
                            sizeBytes = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            durationMs = if (isVideo && durationCol >= 0) cursor.getLong(durationCol) else null,
                            mimeType = cursor.getString(mimeCol) ?: "",
                            bucketName = cursor.getString(bucketNameCol),
                            albumId = cursor.getLong(bucketIdCol)
                        )
                    )
                }
            }
            items
        }

    suspend fun deleteMedia(ids: List<Long>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            val uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
            context.contentResolver.delete(uri, null, null)
        }
    }
}
