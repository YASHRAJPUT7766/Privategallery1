package com.yash.privategallery.ui.favorites

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

/** Section 14: Favorites — a dedicated section for favorited normal-gallery media. */
@Composable
fun FavoritesScreen(
    onOpenViewer: (index: Int) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Favorites") }) }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            EmptyState(message = "Photos you favorite will appear here.", modifier = Modifier.padding(paddingValues))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(favorites, key = { it.id }) { item ->
                    val index = favorites.indexOf(item)
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
