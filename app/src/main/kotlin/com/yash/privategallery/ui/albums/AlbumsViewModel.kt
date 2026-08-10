package com.yash.privategallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.Album
import com.yash.privategallery.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Section 15/16: backs the Albums tab, listing default (computed) albums
 * alongside "My Albums" (Section 17, user-created). AlbumRepository already
 * merges the two into one list, default-first — this ViewModel just exposes
 * that and the create-album action.
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = albumRepository.observeAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createAlbum(name: String) {
        viewModelScope.launch {
            albumRepository.createCustomAlbum(name, iconKey = null)
        }
    }

    fun renameAlbum(albumId: Long, newName: String) {
        viewModelScope.launch { albumRepository.renameAlbum(albumId, newName) }
    }

    fun deleteAlbum(albumId: Long) {
        viewModelScope.launch { albumRepository.deleteAlbum(albumId) }
    }

    fun unlockAlbum(albumId: Long) {
        viewModelScope.launch { albumRepository.unlockAlbum(albumId) }
    }
}
