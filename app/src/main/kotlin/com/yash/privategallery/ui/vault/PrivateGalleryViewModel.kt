package com.yash.privategallery.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.core.security.AppLockStateManager
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.ImportResult
import com.yash.privategallery.domain.repository.LockTarget
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivateGalleryUiState(
    val items: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val isImporting: Boolean = false,
    val lastImportMessage: String? = null
)

/**
 * Backs Section 20's Private Gallery home. All reads/writes go through
 * [PrivateMediaRepository], which already enforces the copy→verify→delete
 * safety flow (Section 57) — this ViewModel is intentionally thin, since the
 * actual safety-critical logic lives one layer down where it can't be
 * bypassed by a UI mistake.
 */
@HiltViewModel
class PrivateGalleryViewModel @Inject constructor(
    private val repository: PrivateMediaRepository,
    private val lockStateManager: AppLockStateManager
) : ViewModel() {

    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val importing = MutableStateFlow(false)
    private val lastMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PrivateGalleryUiState> = combine(
        repository.observePrivateMedia(SortOrder.NEWEST_FIRST),
        selectedIds,
        importing,
        lastMessage
    ) { items, selected, isImporting, message ->
        PrivateGalleryUiState(
            items = items,
            selectedIds = selected,
            isSelectionMode = selected.isNotEmpty(),
            isImporting = isImporting,
            lastImportMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivateGalleryUiState())

    fun toggleSelection(id: Long) {
        selectedIds.value = if (id in selectedIds.value) selectedIds.value - id else selectedIds.value + id
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    /** Section 22: import via system photo picker — [sourceUris] come from the picker result. */
    fun importFromPicker(sourceUris: List<String>) {
        if (sourceUris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            val results = repository.importToPrivate(sourceUris)
            reportResults(results, actionLabel = "Import")
            importing.value = false
        }
    }

    /** Section 23: "Move to Gallery" for the current selection. */
    fun moveSelectedToNormal() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            val results = repository.moveToNormal(ids)
            reportResults(results, actionLabel = "Move to Gallery")
            importing.value = false
            clearSelection()
        }
    }

    fun toggleFavoriteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val allFavorited = uiState.value.items.filter { it.id in ids }.all { it.isFavorite }
            ids.forEach { repository.setFavorite(it, !allFavorited) }
            clearSelection()
        }
    }

    fun moveSelectedToTrash() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.moveToPrivateTrash(ids)
            clearSelection()
        }
    }

    fun dismissMessage() {
        lastMessage.value = null
    }

    /** Section 45: "Lock Now" — clears UI-visible state's protection immediately. */
    fun lockNow() {
        lockStateManager.lockNow(LockTarget.PRIVATE_GALLERY)
    }

    private fun reportResults(results: List<ImportResult>, actionLabel: String) {
        val successCount = results.count { it is ImportResult.Success }
        val failCount = results.size - successCount
        lastMessage.value = when {
            failCount == 0 -> "$actionLabel complete — $successCount item(s)."
            successCount == 0 -> "$actionLabel failed for all items. Nothing was lost."
            else -> "$actionLabel: $successCount succeeded, $failCount failed (originals kept for failed items)."
        }
    }
}
