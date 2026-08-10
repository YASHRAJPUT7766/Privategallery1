package com.yash.privategallery.domain.repository

import com.yash.privategallery.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun updateSettings(transform: (AppSettings) -> AppSettings)
}
