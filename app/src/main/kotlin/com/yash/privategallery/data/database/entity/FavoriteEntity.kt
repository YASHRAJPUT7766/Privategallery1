package com.yash.privategallery.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks favorite state for normal media. Kept in Room rather than relying
 * solely on MediaStore's own favorite flag (Section 40) because: (a) not all
 * MediaStore versions/OEMs reliably support the favorite column, and (b) this
 * table is also the join point private favorites use a parallel table for
 * ([PrivateFavoriteEntity]), keeping the two concerns symmetric.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaStoreId: Long,
    val favoritedAt: Long
)
