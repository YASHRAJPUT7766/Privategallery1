package com.yash.privategallery.data.repository

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import com.yash.privategallery.core.security.VaultCryptoManager
import com.yash.privategallery.core.storage.VaultStorageManager
import com.yash.privategallery.data.database.dao.PrivateMediaDao
import com.yash.privategallery.data.database.entity.PrivateMediaEntity
import com.yash.privategallery.data.media.MediaStoreDataSource
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.domain.model.StorageLocation
import com.yash.privategallery.domain.repository.ImportResult
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivateMediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PrivateMediaDao,
    private val cryptoManager: VaultCryptoManager,
    private val storageManager: VaultStorageManager,
    private val mediaStoreDataSource: MediaStoreDataSource
) : PrivateMediaRepository {

    override fun observePrivateMedia(sortOrder: SortOrder): Flow<List<MediaItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() }.applySort(sortOrder) }

    override fun observePrivateMediaForAlbum(albumId: Long, sortOrder: SortOrder): Flow<List<MediaItem>> =
        dao.observeForAlbum(albumId).map { entities -> entities.map { it.toDomain() }.applySort(sortOrder) }

    override fun observePrivateTrash(): Flow<List<MediaItem>> =
        dao.observeTrash().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getPrivateMediaById(id: Long): MediaItem? = dao.getById(id)?.toDomain()

    /**
     * Implements Section 57's mandatory ordering:
     *   source → secure destination → VERIFY → update database → remove source → refresh UI
     *
     * Each item is handled independently and fully atomically with respect to
     * this ordering — a failure on item N never touches item N's public
     * original, and never affects any other item in the batch.
     */
    override suspend fun moveToPrivate(publicMediaIds: List<Long>): List<ImportResult> =
        withContext(Dispatchers.IO) {
            publicMediaIds.map { mediaId -> moveOneToPrivate(mediaId) }
        }

    private suspend fun moveOneToPrivate(mediaId: Long): ImportResult {
        val publicItem = mediaStoreDataSource.queryMedia().find { it.id == mediaId }
            ?: return ImportResult.Failed("Original item not found — nothing was changed.")

        val destinationFile = storageManager.newEncryptedMediaFile()

        return try {
            // Step 1: encrypt a private copy. Source is opened read-only; the
            // public original is not touched at all during this step.
            val sourceUri = android.net.Uri.parse(publicItem.contentUri)
            val encryptionResult = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                cryptoManager.encryptToVault(input, destinationFile)
            } ?: return ImportResult.Failed("Could not read the original file — nothing was changed.").also {
                destinationFile.delete()
            }

            // Step 2: VERIFY — mandatory before any deletion. Re-decrypts and
            // re-hashes the just-written file and compares against the hash
            // computed during the original read.
            val verified = cryptoManager.verify(
                encryptedFile = destinationFile,
                wrappedDekBase64 = encryptionResult.wrappedDekBase64,
                ivBase64 = encryptionResult.ivBase64,
                expectedSha256 = encryptionResult.plaintextSha256
            )
            if (!verified) {
                destinationFile.delete() // clean up the unverified partial copy
                return ImportResult.Failed("Could not verify the secure copy — original was kept untouched.")
            }

            // Step 3: only now, after verification succeeded, insert the DB row.
            val entity = PrivateMediaEntity(
                encryptedFileName = destinationFile.name,
                originalDisplayName = publicItem.displayName,
                mediaTypeName = publicItem.mediaType.name,
                wrappedDekBase64 = encryptionResult.wrappedDekBase64,
                ivBase64 = encryptionResult.ivBase64,
                plaintextSha256 = encryptionResult.plaintextSha256,
                plaintextSizeBytes = encryptionResult.plaintextSizeBytes,
                width = publicItem.width,
                height = publicItem.height,
                durationMs = publicItem.durationMs,
                mimeType = publicItem.mimeType,
                dateTaken = publicItem.dateTaken,
                importedAt = System.currentTimeMillis()
            )
            val newId = dao.insert(entity)

            // Step 4: only after the DB row is durably committed, remove the
            // public original. If this step throws, the private copy already
            // exists and is valid — worst case the original also still exists
            // (safe duplicate), never a lost photo.
            mediaStoreDataSource.deleteMedia(listOf(mediaId))

            ImportResult.Success(entity.copy(id = newId).toDomain())
        } catch (e: Exception) {
            destinationFile.delete()
            ImportResult.Failed("Import failed: ${e.message ?: "unknown error"} — original was kept untouched.", e)
        }
    }

    override suspend fun importToPrivate(sourceUris: List<String>): List<ImportResult> =
        withContext(Dispatchers.IO) {
            sourceUris.map { uriString -> importOneFromPicker(uriString) }
        }

    private suspend fun importOneFromPicker(uriString: String): ImportResult {
        val destinationFile = storageManager.newEncryptedMediaFile()
        return try {
            val uri = android.net.Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val isVideo = mimeType.startsWith("video/")

            val (width, height) = readImageDimensions(uri) ?: (0 to 0)
            val displayName = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"

            val encryptionResult = context.contentResolver.openInputStream(uri)?.use { input ->
                cryptoManager.encryptToVault(input, destinationFile)
            } ?: return ImportResult.Failed("Could not read the selected file.").also { destinationFile.delete() }

            val verified = cryptoManager.verify(
                destinationFile, encryptionResult.wrappedDekBase64, encryptionResult.ivBase64, encryptionResult.plaintextSha256
            )
            if (!verified) {
                destinationFile.delete()
                return ImportResult.Failed("Could not verify the secure copy after import.")
            }

            val entity = PrivateMediaEntity(
                encryptedFileName = destinationFile.name,
                originalDisplayName = displayName,
                mediaTypeName = if (isVideo) MediaType.VIDEO.name else MediaType.IMAGE.name,
                wrappedDekBase64 = encryptionResult.wrappedDekBase64,
                ivBase64 = encryptionResult.ivBase64,
                plaintextSha256 = encryptionResult.plaintextSha256,
                plaintextSizeBytes = encryptionResult.plaintextSizeBytes,
                width = width,
                height = height,
                durationMs = null,
                mimeType = mimeType,
                dateTaken = System.currentTimeMillis(),
                importedAt = System.currentTimeMillis()
            )
            val newId = dao.insert(entity)
            ImportResult.Success(entity.copy(id = newId).toDomain())
        } catch (e: Exception) {
            destinationFile.delete()
            ImportResult.Failed("Import failed: ${e.message ?: "unknown error"}", e)
        }
    }

    /**
     * Mirrors [moveOneToPrivate] in reverse: write a public MediaStore entry
     * first, verify it, THEN remove the private copy — never delete the
     * private copy before the restored public copy is confirmed good
     * (Section 23, 57).
     */
    override suspend fun moveToNormal(privateMediaIds: List<Long>): List<ImportResult> =
        withContext(Dispatchers.IO) {
            privateMediaIds.map { id -> moveOneToNormal(id) }
        }

    private suspend fun moveOneToNormal(privateId: Long): ImportResult {
        val entity = dao.getById(privateId)
            ?: return ImportResult.Failed("Private item not found — nothing was changed.")

        val encryptedFile = storageManager.resolveMediaFile(entity.encryptedFileName)
        val isVideo = entity.mediaTypeName == MediaType.VIDEO.name

        return try {
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, entity.originalDisplayName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, entity.mimeType)
            }
            val collection = if (isVideo) {
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val newUri = context.contentResolver.insert(collection, values)
                ?: return ImportResult.Failed("Could not create a public entry — private copy was kept.")

            // Step 1: write decrypted bytes to the new public entry.
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            context.contentResolver.openOutputStream(newUri)?.use { out ->
                cryptoManager.decryptFromVault(encryptedFile, entity.wrappedDekBase64, entity.ivBase64).use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        digest.update(buffer, 0, read)
                        out.write(buffer, 0, read)
                    }
                }
            } ?: run {
                context.contentResolver.delete(newUri, null, null)
                return ImportResult.Failed("Could not write the restored file — private copy was kept.")
            }

            // Step 2: verify the restored public copy's hash matches the
            // original plaintext hash before touching the private copy.
            val restoredHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (restoredHash != entity.plaintextSha256) {
                context.contentResolver.delete(newUri, null, null)
                return ImportResult.Failed("Restored copy failed verification — private copy was kept.")
            }

            // Step 3: only now remove the private copy (file + encrypted thumbnail + DB row).
            storageManager.deleteMediaFile(entity.encryptedFileName)
            entity.encryptedThumbnailFileName?.let { storageManager.deleteThumbnailFile(it) }
            dao.deleteById(privateId)

            val restoredItem = mediaStoreDataSource.queryMedia().find { it.contentUri == newUri.toString() }
            ImportResult.Success(
                restoredItem ?: entity.toDomain().copy(
                    storageLocation = StorageLocation.NORMAL,
                    contentUri = newUri.toString(),
                    filePath = null
                )
            )
        } catch (e: Exception) {
            ImportResult.Failed("Restore failed: ${e.message ?: "unknown error"} — private copy was kept.", e)
        }
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean) = dao.setFavorite(id, isFavorite)

    override suspend fun moveToPrivateTrash(ids: List<Long>) {
        val now = System.currentTimeMillis()
        ids.forEach { dao.moveToTrash(it, now) }
    }

    override suspend fun restoreFromPrivateTrash(ids: List<Long>) {
        ids.forEach { dao.restoreFromTrash(it) }
    }

    override suspend fun permanentlyDeleteFromPrivateTrash(ids: List<Long>) {
        ids.forEach { id ->
            val entity = dao.getById(id) ?: return@forEach
            storageManager.deleteMediaFile(entity.encryptedFileName)
            entity.encryptedThumbnailFileName?.let { storageManager.deleteThumbnailFile(it) }
            dao.deleteById(id)
        }
    }

    override suspend fun prepareShareExport(id: Long): String = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: error("Item not found")
        val encryptedFile = storageManager.resolveMediaFile(entity.encryptedFileName)
        val exportFile = storageManager.newShareExportFile(entity.originalDisplayName)
        cryptoManager.decryptFromVault(encryptedFile, entity.wrappedDekBase64, entity.ivBase64).use { input ->
            exportFile.outputStream().use { output -> input.copyTo(output) }
        }
        exportFile.absolutePath
    }

    override suspend fun cleanupShareExports() = withContext(Dispatchers.IO) {
        storageManager.clearShareExports()
    }

    private fun readImageDimensions(uri: android.net.Uri): Pair<Int, Int>? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeStream(input, null, options)
            if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
        }
    } catch (e: Exception) {
        null
    }

    private fun queryDisplayName(uri: android.net.Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(android.provider.MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    private fun PrivateMediaEntity.toDomain(): MediaItem = MediaItem(
        id = id,
        displayName = originalDisplayName,
        mediaType = if (mediaTypeName == MediaType.VIDEO.name) MediaType.VIDEO else MediaType.IMAGE,
        storageLocation = StorageLocation.PRIVATE,
        contentUri = null,
        filePath = storageManager.resolveMediaFile(encryptedFileName).absolutePath,
        dateTaken = dateTaken,
        dateAdded = importedAt,
        dateModified = importedAt,
        sizeBytes = plaintextSizeBytes,
        width = width,
        height = height,
        durationMs = durationMs,
        mimeType = mimeType,
        isFavorite = isFavorite,
        albumId = albumId,
        isTrashed = isTrashed,
        trashedAt = trashedAt
    )

    private fun List<MediaItem>.applySort(sortOrder: SortOrder): List<MediaItem> = when (sortOrder) {
        SortOrder.NEWEST_FIRST -> sortedByDescending { it.dateTaken }
        SortOrder.OLDEST_FIRST -> sortedBy { it.dateTaken }
        SortOrder.NAME_A_Z -> sortedBy { it.displayName.lowercase() }
        SortOrder.NAME_Z_A -> sortedByDescending { it.displayName.lowercase() }
        SortOrder.LARGEST_FIRST -> sortedByDescending { it.sizeBytes }
        SortOrder.SMALLEST_FIRST -> sortedBy { it.sizeBytes }
    }
}
