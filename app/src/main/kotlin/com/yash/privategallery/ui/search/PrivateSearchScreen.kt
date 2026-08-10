package com.yash.privategallery.ui.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import com.yash.privategallery.ui.common.SecureScreenEffect

/**
 * Section 24: Private Gallery's own search, entirely separate from normal
 * search (Section 8) — backed by [PrivateSearchViewModel], which only ever
 * queries [com.yash.privategallery.domain.repository.PrivateMediaRepository].
 * Results here can never include a normal-gallery item, structurally, since
 * this screen has no reference to MediaRepository at all.
 */
@Composable
fun PrivateSearchScreen(
    onBack: () -> Unit,
    onOpenViewer: (index: Int) -> Unit,
    viewModel: PrivateSearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SecureScreenEffect()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        placeholder = { Text("Search private photos & videos") },
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
        if (uiState.results.isEmpty()) {
            EmptyState(
                message = if (uiState.query.isBlank()) "Search by name, date, type, or album." else "No matches found.",
                icon = Icons.Filled.Search,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
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
