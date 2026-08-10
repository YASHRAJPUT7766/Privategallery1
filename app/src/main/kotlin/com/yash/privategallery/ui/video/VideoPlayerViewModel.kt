package com.yash.privategallery.ui.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class VideoPlayerUiState(
    val videos: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val isControlsVisible: Boolean = true,
    val isPrivate: Boolean = false
) {
    val currentItem: MediaItem? get() = videos.getOrNull(currentIndex)
}

/**
 * Section 25/26: resolves the swipeable video collection (all VIDEO-type
 * items from the same storage location the player was opened from — normal
 * or private, never mixed per Section 24) and tracks which one is current
 * and whether the control overlay is showing. The actual ExoPlayer instance
 * is owned by the Composable (Section 25 recommends Media3/ExoPlayer, which
 * is most naturally lifecycle-bound to the Compose tree via
 * DisposableEffect) rather than here, so this ViewModel stays testable
 * without a Context dependency.
 */
@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    mediaRepository: MediaRepository,
    privateMediaRepository: PrivateMediaRepository
) : ViewModel() {

    private val startMediaId: Long = savedStateHandle.get<String>("mediaId")?.toLongOrNull() ?: -1L
    private val isPrivate: Boolean = savedStateHandle.get<String>("isPrivate")?.toBoolean() ?: false

    private val currentIndex = MutableStateFlow(0)
    private val controlsVisible = MutableStateFlow(true)

    private val allVideos: StateFlow<List<MediaItem>> = (
        if (isPrivate) privateMediaRepository.observePrivateMedia(SortOrder.NEWEST_FIRST)
        else mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST)
        )
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<VideoPlayerUiState> = combine(
        allVideos, currentIndex, controlsVisible
    ) { items, index, controls ->
        val videosOnly = items.filter { it.mediaType == MediaType.VIDEO }
        val resolvedStartIndex = if (index == 0 && startMediaId != -1L) {
            videosOnly.indexOfFirst { it.id == startMediaId }.takeIf { it >= 0 } ?: 0
        } else index
        VideoPlayerUiState(
            videos = videosOnly,
            currentIndex = resolvedStartIndex.coerceIn(0, (videosOnly.size - 1).coerceAtLeast(0)),
            isControlsVisible = controls,
            isPrivate = isPrivate
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VideoPlayerUiState())

    fun onSwipeToIndex(index: Int) {
        currentIndex.value = index
    }

    fun toggleControls() {
        controlsVisible.value = !controlsVisible.value
    }
}
