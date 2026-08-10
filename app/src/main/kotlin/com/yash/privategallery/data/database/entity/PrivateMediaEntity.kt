package com.yash.privategallery.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata row for one item in the encrypted private vault (Section 41).
 * Deliberately isolated in its own table (and, per [PrivateDatabase], its own
 * physical Room database file) from the normal-gallery Room tables, so a bug
 * or query elsewhere in the app can never accidentally join/leak private rows
 * into a normal-gallery query result set.
 *
 * [encryptedFileName] is a random, non-guessable name (never the original
 * display name) — the original name is preserved separately in
 * [originalDisplayName] purely for UI display *after* authentication, per
 * Section 41: "Original file name where appropriate".
 */
@Entity(tableName = "private_media")
data class PrivateMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val encryptedFileName: String,
    val originalDisplayName: String,
    val mediaTypeName: String, // "IMAGE" | "VIDEO"
    val wrappedDekBase64: String,
    val ivBase64: String,
    val plaintextSha256: String,
    val plaintextSizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long?,
    val mimeType: String,
    val dateTaken: Long,
    val importedAt: Long,
    val isFavorite: Boolean = false,
    val albumId: Long? = null,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null,
    val encryptedThumbnailFileName: String? = null
)

/** Private-space custom albums — separate table from [AlbumEntity] per Section 41's isolation requirement. */
@Entity(tableName = "private_albums")
data class PrivateAlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String?,
    val createdAt: Long
)
