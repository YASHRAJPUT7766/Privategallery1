package com.yash.privategallery.data.private

import androidx.room.Database
import androidx.room.RoomDatabase
import com.yash.privategallery.data.database.dao.PrivateMediaDao
import com.yash.privategallery.data.database.entity.PrivateAlbumEntity
import com.yash.privategallery.data.database.entity.PrivateMediaEntity

/**
 * Private vault metadata database (Section 41). Physically a separate SQLite
 * file from [com.yash.privategallery.data.database.AppDatabase] — this is a
 * deliberate isolation boundary, not just a separate table set, so that:
 *   - A normal-gallery query can never accidentally touch private rows.
 *   - The file itself (private_vault.db) can be excluded from backup
 *     independently and located inside the app-private vault directory
 *     alongside the encrypted media files it describes (Section 53).
 *
 * This database file is NOT itself additionally encrypted at the SQLite
 * layer in this scaffold (that would require SQLCipher or similar); what it
 * stores is metadata (names, dates, wrapped DEKs, IVs) rather than media
 * content — the media bytes themselves are what's AES-256-GCM encrypted via
 * [com.yash.privategallery.core.security.VaultCryptoManager]. Wrapped DEKs
 * stored here are useless without the Keystore-resident wrapping key, which
 * never leaves secure hardware, so a stolen database file alone cannot
 * decrypt any media.
 */
@Database(
    entities = [PrivateMediaEntity::class, PrivateAlbumEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PrivateDatabase : RoomDatabase() {
    abstract fun privateMediaDao(): PrivateMediaDao

    companion object {
        const val DATABASE_NAME = "private_vault.db"
    }
}
