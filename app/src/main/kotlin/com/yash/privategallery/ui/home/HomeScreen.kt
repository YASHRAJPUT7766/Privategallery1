package com.yash.privategallery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.R
import com.yash.privategallery.domain.model.GridSize
import com.yash.privategallery.ui.common.EmptyState
import com.yash.privategallery.ui.common.MoveToPrivateConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenPrivateGallery: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenViewer: (collectionKey: String, index: Int) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMoveToPrivateDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastActionMessage) {
        uiState.lastActionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (uiState.isSelectionMode) "${uiState.selectedIds.size} selected" else stringResource(R.string.app_name))
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { showMoveToPrivateDialog = true }) {
                            Icon(Icons.Filled.Lock, contentDescription = "Move to Private")
                        }
                        IconButton(onClick = { viewModel.moveSelectedToTrash() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    } else {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onOpenPrivateGallery) {
                            Icon(Icons.Filled.Lock, contentDescription = "Private Gallery")
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        androidx.compose.material3.DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Albums") }, onClick = { menuExpanded = false; onOpenAlbums() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Favorites") }, onClick = { menuExpanded = false; onOpenFavorites() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Recently Deleted") }, onClick = { menuExpanded = false; onOpenTrash() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Duplicates") }, onClick = { menuExpanded = false; onOpenDuplicates() })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Settings") }, onClick = { menuExpanded = false; onOpenSettings() })
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> Unit // could add a loading indicator; kept minimal per Section 39 perf focus
            uiState.dateGroups.isEmpty() -> EmptyState(
                message = stringResource(R.string.empty_photos),
                modifier = Modifier.padding(paddingValues)
            )
            else -> HomeMediaGrid(
                uiState = uiState,
                paddingValues = paddingValues,
                onItemClick = { collectionKey, index -> onOpenViewer(collectionKey, index) },
                onItemLongClick = { viewModel.toggleSelection(it) }
            )
        }
    }

    if (showMoveToPrivateDialog) {
        MoveToPrivateConfirmDialog(
            itemCount = uiState.selectedIds.size,
            onConfirm = {
                showMoveToPrivateDialog = false
                viewModel.moveSelectedToPrivate()
            },
            onDismiss = { showMoveToPrivateDialog = false }
        )
    }
}

@Composable
private fun HomeMediaGrid(
    uiState: HomeUiState,
    paddingValues: PaddingValues,
    onItemClick: (String, Int) -> Unit,
    onItemLongClick: (Long) -> Unit
) {
    val columns = when (uiState.gridSize) {
        GridSize.COMPACT_4_COL -> 4
        GridSize.DEFAULT_3_COL -> 3
        GridSize.LARGE_2_COL -> 2
        GridSize.LIST_VIEW -> 1
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(2.dp)
    ) {
        var runningIndex = 0
        uiState.dateGroups.forEach { group ->
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(columns) }) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            val groupStartIndex = runningIndex
            items(group.items, key = { it.id }) { mediaItem ->
                val indexInGroup = group.items.indexOf(mediaItem)
                com.yash.privategallery.ui.common.MediaThumbnail(
                    item = mediaItem,
                    isSelected = mediaItem.id in uiState.selectedIds,
                    isSelectionMode = uiState.isSelectionMode,
                    showFavoriteIcon = true,
                    showDuration = true,
                    onClick = {
                        if (uiState.isSelectionMode) onItemLongClick(mediaItem.id)
                        else onItemClick(group.label, groupStartIndex + indexInGroup)
                    },
                    onLongClick = { onItemLongClick(mediaItem.id) },
                    modifier = Modifier.aspectRatio(1f).padding(1.dp)
                )
            }
            runningIndex += group.items.size
        }
    }
}

