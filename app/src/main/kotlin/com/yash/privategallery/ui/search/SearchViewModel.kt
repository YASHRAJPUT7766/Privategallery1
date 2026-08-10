package com.yash.privategallery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.domain.repository.MediaRepository
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

/** Section 8's filter chips: Images / Videos / Favorites / Screenshots. */
enum class SearchFilter(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    FAVORITES("Favorites"),
    SCREENSHOTS("Screenshots")
}

data class SearchUiState(
    val query: String = "",
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val results: List<MediaItem> = emptyList()
)

/**
 * Section 8: supports searching by file name, date, month, year, and the
 * quick filter chips. Free text and structured filters combine (matching
 * how the spec's examples mix both, e.g. "Vacation", "IMG", "Screenshot").
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    mediaRepository: MediaRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter.ALL)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val uiState: StateFlow<SearchUiState> = combine(
        mediaRepository.observeAllMedia(SortOrder.NEWEST_FIRST),
        query,
        filter
    ) { items, q, activeFilter ->
        val trimmed = q.trim()
        val textFiltered = if (trimmed.isBlank()) items else items.filter { item ->
            item.displayName.contains(trimmed, ignoreCase = true) ||
                monthYearFormat.format(java.util.Date(item.dateTaken)).contains(trimmed, ignoreCase = true) ||
                item.bucketName?.contains(trimmed, ignoreCase = true) == true
        }
        val fullyFiltered = when (activeFilter) {
            SearchFilter.ALL -> textFiltered
            SearchFilter.IMAGES -> textFiltered.filter { it.mediaType == MediaType.IMAGE }
            SearchFilter.VIDEOS -> textFiltered.filter { it.mediaType == MediaType.VIDEO }
            SearchFilter.FAVORITES -> textFiltered.filter { it.isFavorite }
            SearchFilter.SCREENSHOTS -> textFiltered.filter { it.bucketName?.contains("Screenshot", ignoreCase = true) == true }
        }
        // Section 8: only show results once the user has actually searched
        // for something (text or a non-ALL filter) — an empty query with ALL
        // selected shouldn't dump the entire library into "search results".
        val shouldShowResults = trimmed.isNotBlank() || activeFilter != SearchFilter.ALL
        SearchUiState(query = q, activeFilter = activeFilter, results = if (shouldShowResults) fullyFiltered else emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun onFilterChanged(newFilter: SearchFilter) {
        filter.value = if (filter.value == newFilter) SearchFilter.ALL else newFilter
    }
}
