package com.yash.privategallery.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks normal-gallery items that have been soft-deleted (Section 27).
 * The underlying MediaStore row is left in place (Android's own Trash
 * mechanism on API 30+ can additionally be used at the repository layer),
 * this table exists to drive the "days remaining" countdown UI and the
 * app's own retention-policy sweep independent of OS trash behavior/OEM
 * differences.
 */
@Entity(tableName = "trash")
data class TrashEntity(
    @PrimaryKey val mediaStoreId: Long,
    val trashedAt: Long,
    val originalBucketId: Long?
)
