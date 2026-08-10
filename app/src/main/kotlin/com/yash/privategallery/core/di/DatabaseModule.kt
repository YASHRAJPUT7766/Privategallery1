package com.yash.privategallery.core.di

import android.content.Context
import androidx.room.Room
import com.yash.privategallery.data.database.AppDatabase
import com.yash.privategallery.data.vault.PrivateDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration() // acceptable pre-v1; replace with real migrations post-release
            .build()

    /**
     * The private vault database lives inside the app's private "no_backup"
     * files directory rather than the default databases directory (Section
     * 41, 53) — no_backup is guaranteed by the OS to be excluded from both
     * classic auto-backup AND the newer data-extraction-rules backup path,
     * giving belt-and-suspenders protection alongside the manifest-level
     * exclusions already declared in data_extraction_rules.xml.
     */
    @Provides
    @Singleton
    fun providePrivateDatabase(@ApplicationContext context: Context): PrivateDatabase {
        val vaultDir = File(context.noBackupFilesDir, "private_vault").apply { mkdirs() }
        val dbFile = File(vaultDir, PrivateDatabase.DATABASE_NAME)
        return Room.databaseBuilder(context, PrivateDatabase::class.java, dbFile.absolutePath)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFavoriteDao(db: AppDatabase) = db.favoriteDao()

    @Provides
    fun provideAlbumDao(db: AppDatabase) = db.albumDao()

    @Provides
    fun provideTrashDao(db: AppDatabase) = db.trashDao()

    @Provides
    fun providePrivateMediaDao(db: PrivateDatabase) = db.privateMediaDao()
}
