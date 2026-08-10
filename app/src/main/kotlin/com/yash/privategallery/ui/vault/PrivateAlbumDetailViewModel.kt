package com.yash.privategallery.ui.vault

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.AlbumRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PrivateAlbumDetailUiState(
    val albumName: String = "",
    val items: List<MediaItem> = emptyList()
)

@HiltViewModel
class PrivateAlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val privateMediaRepository: PrivateMediaRepository
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<String>("albumId")?.toLongOrNull() ?: -1L

    val uiState: StateFlow<PrivateAlbumDetailUiState> = combine(
        albumRepository.observePrivateAlbums(),
        privateMediaRepository.observePrivateMediaForAlbum(albumId, SortOrder.NEWEST_FIRST)
    ) { albums, items ->
        PrivateAlbumDetailUiState(
            albumName = albums.find { it.id == albumId }?.name ?: "Album",
            items = items
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivateAlbumDetailUiState())
}
