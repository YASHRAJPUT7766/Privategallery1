package com.yash.privategallery.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.yash.privategallery.domain.model.AppSettings
import com.yash.privategallery.domain.model.AppTheme
import com.yash.privategallery.domain.model.GridSize
import com.yash.privategallery.domain.model.SlideshowInterval
import com.yash.privategallery.domain.model.TrashDuration
import com.yash.privategallery.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val GRID_SIZE = stringPreferencesKey("grid_size")
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val SHOW_VIDEOS = booleanPreferencesKey("show_videos")
        val SHOW_HIDDEN_FOLDERS = booleanPreferencesKey("show_hidden_folders")
        val SHOW_FILE_NAMES = booleanPreferencesKey("show_file_names")
        val SHOW_VIDEO_DURATION = booleanPreferencesKey("show_video_duration")
        val SHOW_FAVORITE_ICON = booleanPreferencesKey("show_favorite_icon")
        val INCLUDE_DOWNLOADS = booleanPreferencesKey("include_downloads")
        val SAVE_AS_COPY = booleanPreferencesKey("save_edited_as_copy")
        val PRESERVE_METADATA = booleanPreferencesKey("preserve_metadata")
        val TRASH_DURATION = stringPreferencesKey("trash_duration")
        val SLIDESHOW_INTERVAL = stringPreferencesKey("slideshow_interval")
    }

    override fun observeSettings(): Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            gridSize = prefs[Keys.GRID_SIZE]?.let { runCatching { GridSize.valueOf(it) }.getOrNull() } ?: GridSize.DEFAULT_3_COL,
            theme = prefs[Keys.THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM_DEFAULT,
            useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            animationsEnabled = prefs[Keys.ANIMATIONS] ?: true,
            showVideosInGallery = prefs[Keys.SHOW_VIDEOS] ?: true,
            showHiddenSystemFolders = prefs[Keys.SHOW_HIDDEN_FOLDERS] ?: false,
            showFileNames = prefs[Keys.SHOW_FILE_NAMES] ?: false,
            showVideoDuration = prefs[Keys.SHOW_VIDEO_DURATION] ?: true,
            showFavoriteIcon = prefs[Keys.SHOW_FAVORITE_ICON] ?: true,
            includeDownloadsInScan = prefs[Keys.INCLUDE_DOWNLOADS] ?: true,
            saveEditedAsCopyByDefault = prefs[Keys.SAVE_AS_COPY] ?: true,
            preserveMetadataOnEdit = prefs[Keys.PRESERVE_METADATA] ?: true,
            trashDuration = prefs[Keys.TRASH_DURATION]?.let { runCatching { TrashDuration.valueOf(it) }.getOrNull() } ?: TrashDuration.DAYS_30,
            defaultSlideshowInterval = prefs[Keys.SLIDESHOW_INTERVAL]?.let { runCatching { SlideshowInterval.valueOf(it) }.getOrNull() } ?: SlideshowInterval.FIVE
        )
    }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val current = observeSettings().first()
        val updated = transform(current)
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.GRID_SIZE] = updated.gridSize.name
            prefs[Keys.THEME] = updated.theme.name
            prefs[Keys.DYNAMIC_COLOR] = updated.useDynamicColor
            prefs[Keys.ANIMATIONS] = updated.animationsEnabled
            prefs[Keys.SHOW_VIDEOS] = updated.showVideosInGallery
            prefs[Keys.SHOW_HIDDEN_FOLDERS] = updated.showHiddenSystemFolders
            prefs[Keys.SHOW_FILE_NAMES] = updated.showFileNames
            prefs[Keys.SHOW_VIDEO_DURATION] = updated.showVideoDuration
            prefs[Keys.SHOW_FAVORITE_ICON] = updated.showFavoriteIcon
            prefs[Keys.INCLUDE_DOWNLOADS] = updated.includeDownloadsInScan
            prefs[Keys.SAVE_AS_COPY] = updated.saveEditedAsCopyByDefault
            prefs[Keys.PRESERVE_METADATA] = updated.preserveMetadataOnEdit
            prefs[Keys.TRASH_DURATION] = updated.trashDuration.name
            prefs[Keys.SLIDESHOW_INTERVAL] = updated.defaultSlideshowInterval.name
        }
    }
}
