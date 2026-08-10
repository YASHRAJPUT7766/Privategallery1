package com.yash.privategallery.core.di

import com.yash.privategallery.data.repository.AlbumRepositoryImpl
import com.yash.privategallery.data.repository.MediaRepositoryImpl
import com.yash.privategallery.data.repository.PrivateMediaRepositoryImpl
import com.yash.privategallery.data.repository.SecurityRepositoryImpl
import com.yash.privategallery.data.repository.SettingsRepositoryImpl
import com.yash.privategallery.domain.repository.AlbumRepository
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SecurityRepository
import com.yash.privategallery.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindPrivateMediaRepository(impl: PrivateMediaRepositoryImpl): PrivateMediaRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindSecurityRepository(impl: SecurityRepositoryImpl): SecurityRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
