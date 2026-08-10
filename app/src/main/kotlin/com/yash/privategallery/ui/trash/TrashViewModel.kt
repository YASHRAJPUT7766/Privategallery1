package com.yash.privategallery.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.MediaRepository
import com.yash.privategallery.domain.usecase.daysBetween
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One trashed item plus its computed days-remaining before permanent deletion (Section 27). */
data class TrashUiItem(
    val media: MediaItem,
    val trashedAt: Long,
    val daysRemaining: Int
)

data class TrashUiState(
    val items: List<TrashUiItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false
)

/**
 * Section 27: Recently Deleted for the normal gallery. Days-remaining is
 * computed client-side from a retention window (Section 35's Trash
 * Duration setting) rather than stored per-row, so changing the retention
 * setting immediately re-computes remaining days for every item already in
 * trash.
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val retentionDays = MutableStateFlow(DEFAULT_RETENTION_DAYS)

    val uiState: StateFlow<TrashUiState> = combine(
        mediaRepository.observeTrashedMedia(),
        selectedIds,
        retentionDays
    ) { trashedPairs, selected, retention ->
        val now = System.currentTimeMillis()
        val items = trashedPairs.map { (media, trashedAt) ->
            val daysElapsed = daysBetween(trashedAt, now).toInt()
            TrashUiItem(media = media, trashedAt = trashedAt, daysRemaining = (retention - daysElapsed).coerceAtLeast(0))
        }
        TrashUiState(items = items, selectedIds = selected, isSelectionMode = selected.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrashUiState())

    fun setRetentionDays(days: Int) {
        retentionDays.value = days
    }

    fun toggleSelection(id: Long) {
        selectedIds.value = if (id in selectedIds.value) selectedIds.value - id else selectedIds.value + id
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun restoreSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.restoreFromTrash(ids)
            clearSelection()
        }
    }

    fun permanentlyDeleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            mediaRepository.permanentlyDelete(ids)
            clearSelection()
        }
    }

    /** Section 27: "Empty Trash" — permanently deletes everything currently in trash. */
    fun emptyTrash() {
        viewModelScope.launch {
            val allIds = uiState.value.items.map { it.media.id }
            if (allIds.isNotEmpty()) {
                mediaRepository.permanentlyDelete(allIds)
            }
            clearSelection()
        }
    }

    private companion object {
        const val DEFAULT_RETENTION_DAYS = 30
    }
}
