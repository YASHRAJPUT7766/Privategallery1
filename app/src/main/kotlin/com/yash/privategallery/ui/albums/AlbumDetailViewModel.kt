package com.yash.privategallery.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.AlbumKind
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.domain.repository.AlbumRepository
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumName: String = "",
    val items: List<MediaItem> = emptyList()
)

/**
 * Section 15/21: resolves one album's contents regardless of whether it's a
 * default album (filtered from the full library by [AlbumKind], synthetic
 * negative id — see AlbumRepositoryImpl.computedAlbum) or a CUSTOM album
 * (Room-tracked membership, positive autogen id, resolved via
 * [AlbumRepository.observeCustomAlbumMedia] — NOT a MediaStore bucket
 * query, since custom albums have no corresponding MediaStore bucket).
 */
@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<String>("albumId")?.toLongOrNull() ?: 0L

    val uiState: StateFlow<AlbumDetailUiState> = albumRepository.observeAlbums()
        .flatMapLatest { albums ->
            val album = albums.find { it.id == albumId }
            val albumName = album?.name ?: "Album"

            if (album?.kind == AlbumKind.CUSTOM) {
                albumRepository.observeCustomAlbumMedia(albumId).map { items ->
                    AlbumDetailUiState(albumName, items)
                }
            } else {
                mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST).map { items ->
                    val filtered = when (album?.kind) {
                        AlbumKind.CAMERA -> items.filter { it.bucketName == "Camera" }
                        AlbumKind.SCREENSHOTS -> items.filter { it.bucketName?.contains("Screenshot", ignoreCase = true) == true }
                        AlbumKind.DOWNLOADS -> items.filter { it.bucketName == "Download" || it.bucketName == "Downloads" }
                        AlbumKind.FAVORITES -> items.filter { it.isFavorite }
                        AlbumKind.VIDEOS -> items.filter { it.mediaType == MediaType.VIDEO }
                        AlbumKind.RECENTLY_ADDED -> items.sortedByDescending { it.dateAdded }.take(50)
                        else -> items // ALL_IMAGES and unresolved fall through to the full set
                    }
                    AlbumDetailUiState(albumName, filtered)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumDetailUiState())
}
