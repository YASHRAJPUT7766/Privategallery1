package com.yash.privategallery.ui.private

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun PrivateAlbumDetailScreen(
    onBack: () -> Unit,
    onOpenViewer: (index: Int) -> Unit,
    viewModel: PrivateAlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    SecureScreenEffect()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.albumName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.items.isEmpty()) {
            EmptyState(message = "No items in this album yet.", modifier = Modifier.padding(paddingValues))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    val index = uiState.items.indexOf(item)
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
