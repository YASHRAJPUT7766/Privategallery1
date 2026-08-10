package com.yash.privategallery.domain.repository

import com.yash.privategallery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Result of a move-to-private / move-to-normal operation. Modeled as a sealed
 * result rather than a boolean or a thrown exception, because callers (Sections
 * 21, 23, 57) must be able to distinguish "fully succeeded", "failed before
 * anything was touched" (safe, no-op), and "partially failed" (which must NEVER
 * happen by construction — see [ImportResult.Failed] docs below — but the type
 * exists so a failure can never be silently swallowed as success).
 */
sealed class ImportResult {
    data class Success(val newItem: MediaItem) : ImportResult()

    /**
     * The operation did not complete. Because of the copy→verify→delete ordering
     * this repository enforces (Section 57), a [Failed] result guarantees the
     * ORIGINAL is untouched — the private copy (if partially written) is cleaned
     * up, and the public MediaStore entry was never deleted. Callers should show
     * [message] to the user and take no further action; nothing needs undoing.
     */
    data class Failed(val message: String, val cause: Throwable? = null) : ImportResult()
}

/**
 * Access to the app's encrypted private vault. Every implementation of this
 * interface MUST follow the data-safety flow mandated by Section 57:
 *
 *     source → secure destination → VERIFY → update database → remove source → refresh UI
 *
 * and must NEVER delete a source item before the private copy has been written
 * AND verified (re-read + integrity check). This ordering is not an optimization
 * detail — it is the core safety guarantee of the whole app (Section 49: "Never
 * silently delete the user's media... If anything fails: Keep the original, show
 * an error, do not risk data loss").
 */
interface PrivateMediaRepository {

    /** Live stream of private media. Never mixed with normal gallery results (Section 24). */
    fun observePrivateMedia(sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<List<MediaItem>>

    fun observePrivateMediaForAlbum(albumId: Long, sortOrder: SortOrder = SortOrder.NEWEST_FIRST): Flow<List<MediaItem>>

    /** Section 27: private trash is a dedicated view, isolated from normal trash and from the live private grid. */
    fun observePrivateTrash(): Flow<List<MediaItem>>

    suspend fun getPrivateMediaById(id: Long): MediaItem?

    /**
     * Moves normal MediaStore item(s) into the encrypted private vault (Section 21).
     * Implementations must, per item and in this order:
     *  1. Encrypt + write a private copy.
     *  2. Re-open and verify the private copy (readable, correct size/hash).
     *  3. Only on verification success: insert private DB row, then delete the
     *     public MediaStore entry, then trigger a MediaStore rescan.
     *  4. On any failure at steps 1-2: delete the partial private copy if one was
     *     written, leave the public original completely untouched, and return
     *     [ImportResult.Failed] for that item.
     *
     * One [ImportResult] per input id, in the same order as [publicMediaIds].
     */
    suspend fun moveToPrivate(publicMediaIds: List<Long>): List<ImportResult>

    /**
     * Imports new media directly into the vault via the system photo picker,
     * bypassing MediaStore/public storage entirely (Section 22) — the source
     * file handed to this function (from the picker's URI) is read once,
     * encrypted, verified, then the temporary read handle is released.
     */
    suspend fun importToPrivate(sourceUris: List<String>): List<ImportResult>

    /**
     * Moves private item(s) back to the normal public gallery (Section 23).
     * Mirrors [moveToPrivate]'s safety ordering in reverse: decrypt to a public
     * MediaStore entry, verify, THEN remove the private copy — never the other
     * way around.
     */
    suspend fun moveToNormal(privateMediaIds: List<Long>): List<ImportResult>

    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    /** Private trash is separate from normal trash (Section 27: "private deleted items should have a separate private trash"). */
    suspend fun moveToPrivateTrash(ids: List<Long>)

    suspend fun restoreFromPrivateTrash(ids: List<Long>)

    suspend fun permanentlyDeleteFromPrivateTrash(ids: List<Long>)

    /**
     * Produces a short-lived, decrypted, scoped file for sharing via Android's
     * Sharesheet (Section 30) — written only to the app's own cache share-export
     * directory (never public storage), and the caller is responsible for
     * invoking [cleanupShareExports] once the share intent has been dispatched.
     */
    suspend fun prepareShareExport(id: Long): String

    suspend fun cleanupShareExports()
}
