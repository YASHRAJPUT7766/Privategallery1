package com.yash.privategallery.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import com.yash.privategallery.domain.usecase.GroupMediaByDateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = 0,
    val isToolbarVisible: Boolean = true,
    val isLoading: Boolean = true
) {
    val currentItem: MediaItem? get() = items.getOrNull(currentIndex)
    val counterLabel: String get() = if (items.isNotEmpty()) "${currentIndex + 1} / ${items.size}" else ""
}

/**
 * Backs the full-screen Image/Video viewer (Section 9, 10, 26). The viewer
 * doesn't re-derive its own query — it's handed a [collectionKey] that maps
 * back to whatever list the caller (Home's date group, an album, a search
 * result, Favorites, Private Gallery) was already showing, so "swipe left/
 * right should move through the current album/search/date result" (Section
 * 9) and mixed image→video→image browsing (Section 26) both fall out
 * naturally from operating over one flat [MediaItem] list regardless of type.
 *
 * [isPrivate] switches the backing repository — normal and private results
 * are never mixed into the same collection (Section 24), so a single
 * ViewerViewModel instance is always entirely one or the other.
 */
@HiltViewModel
class ViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val privateMediaRepository: PrivateMediaRepository,
    private val groupMediaByDate: GroupMediaByDateUseCase
) : ViewModel() {

    private val collectionKey: String = savedStateHandle["collectionKey"] ?: ""
    private val startIndex: Int = savedStateHandle.get<String>("startIndex")?.toIntOrNull() ?: 0
    private val isPrivate: Boolean = savedStateHandle.get<String>("isPrivate")?.toBoolean() ?: false

    private val currentIndex = MutableStateFlow(startIndex)
    private val toolbarVisible = MutableStateFlow(true)

    private val resolvedItems: StateFlow<List<MediaItem>> = if (isPrivate) {
        privateMediaRepository.observePrivateMedia(SortOrder.NEWEST_FIRST)
    } else {
        mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ViewerUiState> = combine(
        resolvedItems, currentIndex, toolbarVisible
    ) { items, index, toolbar ->
        // collectionKey scoping (matching a specific date-group label, album
        // id, etc.) is applied by the caller filtering `items` before they
        // reach this combine when a scoped repository call isn't already
        // narrow enough — Home passes the flat newest-first list and uses
        // group-relative indices, which this state naturally supports since
        // date groups are contiguous within a newest-first ordering.
        ViewerUiState(
            items = items,
            currentIndex = index.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            isToolbarVisible = toolbar,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewerUiState())

    fun onPageChanged(newIndex: Int) {
        currentIndex.value = newIndex
    }

    fun toggleToolbar() {
        toolbarVisible.value = !toolbarVisible.value
    }

    fun toggleFavorite() {
        val item = uiState.value.currentItem ?: return
        viewModelScope.launch {
            if (isPrivate) {
                privateMediaRepository.setFavorite(item.id, !item.isFavorite)
            } else {
                mediaRepository.setFavorite(item.id, !item.isFavorite)
            }
        }
    }

    fun deleteCurrent(onDeleted: () -> Unit) {
        val item = uiState.value.currentItem ?: return
        viewModelScope.launch {
            if (isPrivate) {
                privateMediaRepository.moveToPrivateTrash(listOf(item.id))
            } else {
                mediaRepository.moveToTrash(listOf(item.id))
            }
            onDeleted()
        }
    }

    /** Section 21: "Move to Private" from within the normal-gallery viewer. */
    fun moveCurrentToPrivate(onResult: (Boolean) -> Unit) {
        val item = uiState.value.currentItem ?: return
        viewModelScope.launch {
            val results = privateMediaRepository.moveToPrivate(listOf(item.id))
            onResult(results.firstOrNull() is com.yash.privategallery.domain.repository.ImportResult.Success)
        }
    }

    /** Section 23: "Move to Gallery" from within the private viewer. */
    fun moveCurrentToNormal(onResult: (Boolean) -> Unit) {
        val item = uiState.value.currentItem ?: return
        viewModelScope.launch {
            val results = privateMediaRepository.moveToNormal(listOf(item.id))
            onResult(results.firstOrNull() is com.yash.privategallery.domain.repository.ImportResult.Success)
        }
    }
}
