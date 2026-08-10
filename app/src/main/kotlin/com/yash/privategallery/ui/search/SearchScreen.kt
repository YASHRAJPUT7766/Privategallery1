package com.yash.privategallery.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.ui.common.EmptyState
import com.yash.privategallery.ui.common.MediaThumbnail

/**
 * Section 8: normal-gallery search. Entirely separate ViewModel/screen from
 * PrivateSearchScreen — Section 24's "Never mix private results with normal
 * gallery results" is structural here too, since this screen only ever
 * talks to MediaRepository.
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenViewer: (index: Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        placeholder = { Text("Search photos & videos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(SearchFilter.entries.toList()) { filter ->
                    FilterChip(
                        selected = uiState.activeFilter == filter,
                        onClick = { viewModel.onFilterChanged(filter) },
                        label = { Text(filter.label) }
                    )
                }
            }

            if (uiState.results.isEmpty()) {
                EmptyState(
                    message = if (uiState.query.isBlank() && uiState.activeFilter == SearchFilter.ALL) {
                        "Search by name, date, month, year, or type."
                    } else {
                        "No matches found."
                    },
                    icon = Icons.Filled.Search
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp)
                ) {
                    items(uiState.results, key = { it.id }) { item ->
                        val index = uiState.results.indexOf(item)
                        MediaThumbnail(
                            item = item,
                            isSelected = false,
                            isSelectionMode = false,
                            showFavoriteIcon = true,
                            showDuration = true,
                            onClick = { onOpenViewer(index) },
                            onLongClick = {},
                            modifier = Modifier.aspectRatio(1f).padding(1.dp)
                        )
                    }
                }
            }
        }
    }
}
