package com.yash.privategallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.AppSettings
import com.yash.privategallery.domain.model.AppTheme
import com.yash.privategallery.domain.model.GridSize
import com.yash.privategallery.domain.model.SlideshowInterval
import com.yash.privategallery.domain.model.TrashDuration
import com.yash.privategallery.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Section 35: backs every settings sub-screen. All mutations go through
 * SettingsRepository.updateSettings' functional-update pattern so
 * concurrent changes from different sub-screens never clobber each other's
 * fields.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setGridSize(size: GridSize) = update { it.copy(gridSize = size) }
    fun setTheme(theme: AppTheme) = update { it.copy(theme = theme) }
    fun setUseDynamicColor(enabled: Boolean) = update { it.copy(useDynamicColor = enabled) }
    fun setAnimationsEnabled(enabled: Boolean) = update { it.copy(animationsEnabled = enabled) }
    fun setShowVideosInGallery(enabled: Boolean) = update { it.copy(showVideosInGallery = enabled) }
    fun setShowHiddenSystemFolders(enabled: Boolean) = update { it.copy(showHiddenSystemFolders = enabled) }
    fun setShowFileNames(enabled: Boolean) = update { it.copy(showFileNames = enabled) }
    fun setShowVideoDuration(enabled: Boolean) = update { it.copy(showVideoDuration = enabled) }
    fun setShowFavoriteIcon(enabled: Boolean) = update { it.copy(showFavoriteIcon = enabled) }
    fun setIncludeDownloadsInScan(enabled: Boolean) = update { it.copy(includeDownloadsInScan = enabled) }
    fun setSaveEditedAsCopyByDefault(enabled: Boolean) = update { it.copy(saveEditedAsCopyByDefault = enabled) }
    fun setPreserveMetadataOnEdit(enabled: Boolean) = update { it.copy(preserveMetadataOnEdit = enabled) }
    fun setTrashDuration(duration: TrashDuration) = update { it.copy(trashDuration = duration) }
    fun setDefaultSlideshowInterval(interval: SlideshowInterval) = update { it.copy(defaultSlideshowInterval = interval) }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.updateSettings(transform) }
    }
}
