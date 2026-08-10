package com.yash.privategallery.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yash.privategallery.data.database.dao.AlbumDao
import com.yash.privategallery.data.database.dao.FavoriteDao
import com.yash.privategallery.data.database.dao.TrashDao
import com.yash.privategallery.data.database.entity.AlbumEntity
import com.yash.privategallery.data.database.entity.AlbumMediaCrossRef
import com.yash.privategallery.data.database.entity.FavoriteEntity
import com.yash.privategallery.data.database.entity.TrashEntity

/**
 * Normal-gallery metadata database (Section 40): favorites, custom albums,
 * album membership, and trash tracking for public MediaStore-backed media.
 * MediaStore itself remains the source of truth for the media rows — this
 * database only stores app-specific overlay metadata, never duplicating the
 * full public gallery.
 *
 * Physically separate .db file from [com.yash.privategallery.data.vault.PrivateDatabase]
 * (Section 41: "Private metadata should be isolated from normal gallery metadata").
 */
@Database(
    entities = [FavoriteEntity::class, AlbumEntity::class, AlbumMediaCrossRef::class, TrashEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun albumDao(): AlbumDao
    abstract fun trashDao(): TrashDao

    companion object {
        const val DATABASE_NAME = "gallery_app.db"
    }
}
