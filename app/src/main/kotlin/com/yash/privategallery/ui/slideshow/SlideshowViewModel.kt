package com.yash.privategallery.ui.slideshow

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SettingsRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SlideshowUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val isPlaying: Boolean = true,
    val intervalSeconds: Int = 5
) {
    val currentItem: MediaItem? get() = items.getOrNull(currentIndex)
}

@HiltViewModel
class SlideshowViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    mediaRepository: MediaRepository,
    privateMediaRepository: PrivateMediaRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val isPrivate: Boolean = savedStateHandle.get<String>("isPrivate")?.toBoolean() ?: false

    private val currentIndex = MutableStateFlow(0)
    private val isPlaying = MutableStateFlow(true)

    private val items = if (isPrivate) {
        privateMediaRepository.observePrivateMedia(SortOrder.NEWEST_FIRST)
    } else {
        mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST)
    }

    val uiState: StateFlow<SlideshowUiState> = combine(
        items, currentIndex, isPlaying, settingsRepository.observeSettings()
    ) { mediaItems, index, playing, settings ->
        SlideshowUiState(
            items = mediaItems,
            currentIndex = index.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0)),
            isPlaying = playing,
            intervalSeconds = settings.defaultSlideshowInterval.seconds
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SlideshowUiState())

    fun togglePlayPause() {
        isPlaying.value = !isPlaying.value
    }

    fun next() {
        val state = uiState.value
        if (state.items.isEmpty()) return
        currentIndex.value = (state.currentIndex + 1) % state.items.size
    }

    fun previous() {
        val state = uiState.value
        if (state.items.isEmpty()) return
        currentIndex.value = (state.currentIndex - 1 + state.items.size) % state.items.size
    }
}
