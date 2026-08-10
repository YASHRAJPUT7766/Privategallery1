package com.yash.privategallery.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.GridSize
import com.yash.privategallery.domain.model.MediaDateGroup
import com.yash.privategallery.domain.repository.ImportResult
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SettingsRepository
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

data class HomeUiState(
    val dateGroups: List<MediaDateGroup> = emptyList(),
    val gridSize: GridSize = GridSize.DEFAULT_3_COL,
    val isLoading: Boolean = true,
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val lastActionMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val privateMediaRepository: PrivateMediaRepository,
    private val settingsRepository: SettingsRepository,
    private val groupMediaByDate: GroupMediaByDateUseCase
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val lastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST),
        settingsRepository.observeSettings(),
        selectedIds,
        lastMessage
    ) { media, settings, selected, message ->
        HomeUiState(
            dateGroups = groupMediaByDate(media),
            gridSize = settings.gridSize,
            isLoading = false,
            selectedIds = selected,
            isSelectionMode = selected.isNotEmpty(),
            lastActionMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleSelection(mediaId: Long) {
        selectedIds.value = if (mediaId in selectedIds.value) {
            selectedIds.value - mediaId
        } else {
            selectedIds.value + mediaId
        }
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun toggleFavorite(mediaId: Long, isFavorite: Boolean) {
        viewModelScope.launch { mediaRepository.setFavorite(mediaId, isFavorite) }
    }

    fun moveSelectedToTrash() {
        viewModelScope.launch {
            mediaRepository.moveToTrash(selectedIds.value.toList())
            clearSelection()
        }
    }

    /** Section 21: confirmed by [com.yash.privategallery.ui.common.MoveToPrivateConfirmDialog] before this is called. */
    fun moveSelectedToPrivate() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val results = privateMediaRepository.moveToPrivate(ids)
            val successCount = results.count { it is ImportResult.Success }
            val failCount = results.size - successCount
            lastMessage.value = if (failCount == 0) {
                "Moved $successCount item(s) to Private Gallery."
            } else {
                "$successCount moved, $failCount failed (originals kept for failed items)."
            }
            clearSelection()
        }
    }

    fun dismissMessage() {
        lastMessage.value = null
    }

    fun refresh() {
        viewModelScope.launch { mediaRepository.rescan() }
    }
}
