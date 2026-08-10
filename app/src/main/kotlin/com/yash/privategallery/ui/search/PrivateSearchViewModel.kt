package com.yash.privategallery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.repository.PrivateMediaRepository
import com.yash.privategallery.domain.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

data class PrivateSearchUiState(
    val query: String = "",
    val results: List<MediaItem> = emptyList()
)

@HiltViewModel
class PrivateSearchViewModel @Inject constructor(
    repository: PrivateMediaRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val uiState: StateFlow<PrivateSearchUiState> = combine(
        repository.observePrivateMedia(SortOrder.NEWEST_FIRST),
        query
    ) { items, q ->
        val trimmed = q.trim()
        val filtered = if (trimmed.isBlank()) {
            emptyList()
        } else {
            items.filter { item ->
                item.displayName.contains(trimmed, ignoreCase = true) ||
                    item.mediaType.name.contains(trimmed, ignoreCase = true) ||
                    monthYearFormat.format(java.util.Date(item.dateTaken)).contains(trimmed, ignoreCase = true)
            }
        }
        PrivateSearchUiState(query = q, results = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivateSearchUiState())

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }
}
