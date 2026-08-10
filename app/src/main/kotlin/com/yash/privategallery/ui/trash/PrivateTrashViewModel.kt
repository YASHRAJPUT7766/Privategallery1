package com.yash.privategallery.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.usecase.daysBetween
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Section 27: "For privacy, private deleted items should have a separate
 * private trash." Mirrors [TrashViewModel] but is backed entirely by
 * [PrivateMediaRepository.observePrivateTrash] — private and normal trash
 * are never the same list, same isolation principle as everywhere else
 * private data is handled in this app.
 */
@HiltViewModel
class PrivateTrashViewModel @Inject constructor(
    private val privateMediaRepository: PrivateMediaRepository
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val retentionDays = MutableStateFlow(DEFAULT_RETENTION_DAYS)

    val uiState: StateFlow<TrashUiState> = combine(
        privateMediaRepository.observePrivateTrash(),
        selectedIds,
        retentionDays
    ) { trashed, selected, retention ->
        val now = System.currentTimeMillis()
        val items = trashed.map { media ->
            val trashedAt = media.trashedAt ?: now
            val daysElapsed = daysBetween(trashedAt, now).toInt()
            TrashUiItem(media = media, trashedAt = trashedAt, daysRemaining = (retention - daysElapsed).coerceAtLeast(0))
        }
        TrashUiState(items = items, selectedIds = selected, isSelectionMode = selected.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TrashUiState())

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
            privateMediaRepository.restoreFromPrivateTrash(ids)
            clearSelection()
        }
    }

    fun permanentlyDeleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            privateMediaRepository.permanentlyDeleteFromPrivateTrash(ids)
            clearSelection()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val allIds = uiState.value.items.map { it.media.id }
            if (allIds.isNotEmpty()) {
                privateMediaRepository.permanentlyDeleteFromPrivateTrash(allIds)
            }
            clearSelection()
        }
    }

    private companion object {
        const val DEFAULT_RETENTION_DAYS = 30
    }
}
