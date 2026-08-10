package com.yash.privategallery.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A user-created album (Section 17: "My Albums"). Default/system albums
 * (Camera, Screenshots, etc.) are NOT rows here — they're computed queries
 * over MediaStore bucket data, per Section 40's "do not duplicate the entire
 * public gallery unnecessarily into Room".
 *
 * Lock fields mirror [com.yash.privategallery.domain.model.AuthMethod] but are
 * stored as a plain string column (Room-friendly) rather than the enum type
 * directly, converted at the repository boundary.
 */
@Entity(tableName = "custom_albums")
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String?,
    val isLocked: Boolean = false,
    val authMethodName: String = "NONE",
    val isPrivate: Boolean = false,
    val createdAt: Long
)

/** Many-to-many join between [AlbumEntity] and normal MediaStore items (by their MediaStore id). */
@Entity(
    tableName = "album_media_cross_ref",
    primaryKeys = ["albumId", "mediaStoreId"],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("albumId"), Index("mediaStoreId")]
)
data class AlbumMediaCrossRef(
    val albumId: Long,
    val mediaStoreId: Long,
    val addedAt: Long
)
