package com.yash.privategallery.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    mediaRepository: MediaRepository
) : ViewModel() {

    val favorites: StateFlow<List<MediaItem>> = mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST)
        .map { items -> items.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
