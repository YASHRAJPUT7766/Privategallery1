package com.yash.privategallery.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the private vault's on-disk directory layout. Everything here lives
 * under [Context.getNoBackupFilesDir] rather than getExternalFilesDir/DCIM/
 * Pictures/Movies/Downloads/public cache (Section 42: "Never put private
 * thumbnails in DCIM, Pictures, Movies, Downloads, public cache, or external
 * public storage"). no_backup is:
 *   - App-private (not readable by other apps without root)
 *   - Not scanned by MediaStore (so encrypted blobs never surface as "photos"
 *     in other gallery apps, satisfying Section 2's core requirement)
 *   - Excluded from Android's backup systems by definition
 *
 * File names on disk are random UUIDs, never the original display name
 * (Section 41: encryptedFileName is non-guessable; original name is kept
 * only in the encrypted database row for display after auth).
 */
@Singleton
class VaultStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vaultRoot: File by lazy {
        File(context.noBackupFilesDir, "private_vault").apply { mkdirs() }
    }

    private val mediaDir: File by lazy {
        File(vaultRoot, "media").apply { mkdirs() }
    }

    private val thumbnailDir: File by lazy {
        File(vaultRoot, "thumbnails").apply { mkdirs() }
    }

    /** Directory used only for ephemeral, decrypted share-export files (Section 30, 52). */
    private val shareExportDir: File by lazy {
        File(context.cacheDir, "share_exports").apply { mkdirs() }
    }

    fun newEncryptedMediaFile(): File = File(mediaDir, "${UUID.randomUUID()}.enc")

    fun newEncryptedThumbnailFile(): File = File(thumbnailDir, "${UUID.randomUUID()}.enc")

    fun resolveMediaFile(encryptedFileName: String): File = File(mediaDir, encryptedFileName)

    fun resolveThumbnailFile(encryptedFileName: String): File = File(thumbnailDir, encryptedFileName)

    fun newShareExportFile(originalDisplayName: String): File =
        File(shareExportDir, "${UUID.randomUUID()}_$originalDisplayName")

    /** Wipes all ephemeral share-export files (Section 30: "After sharing, temporary files should be cleaned up"). */
    fun clearShareExports() {
        shareExportDir.listFiles()?.forEach { it.delete() }
    }

    fun deleteMediaFile(encryptedFileName: String): Boolean = resolveMediaFile(encryptedFileName).delete()

    fun deleteThumbnailFile(encryptedFileName: String): Boolean = resolveThumbnailFile(encryptedFileName).delete()
}
