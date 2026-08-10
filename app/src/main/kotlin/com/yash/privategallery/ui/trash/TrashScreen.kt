package com.yash.privategallery.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yash.privategallery.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    TrashScreenContent(
        uiState = uiState,
        title = "Recently Deleted",
        onToggleSelection = { viewModel.toggleSelection(it) },
        onClearSelection = { viewModel.clearSelection() },
        onRestoreSelected = { viewModel.restoreSelected() },
        onDeleteSelected = { viewModel.permanentlyDeleteSelected() },
        onEmptyTrash = { viewModel.emptyTrash() }
    )
}

/**
 * Section 27: the private-vault trash reuses this exact UI shape via
 * [PrivateTrashViewModel], which is backed entirely by
 * [com.yash.privategallery.domain.repository.PrivateMediaRepository] — kept
 * as a distinct entry point (not a boolean flag on [TrashScreen]) so the two
 * screens can never accidentally share a ViewModel instance or route.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateTrashScreen(
    viewModel: PrivateTrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    com.yash.privategallery.ui.common.SecureScreenEffect()
    TrashScreenContent(
        uiState = uiState,
        title = "Private Trash",
        onToggleSelection = { viewModel.toggleSelection(it) },
        onClearSelection = { viewModel.clearSelection() },
        onRestoreSelected = { viewModel.restoreSelected() },
        onDeleteSelected = { viewModel.permanentlyDeleteSelected() },
        onEmptyTrash = { viewModel.emptyTrash() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashScreenContent(
    uiState: TrashUiState,
    title: String,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onRestoreSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEmptyTrash: () -> Unit
) {
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.isSelectionMode) "${uiState.selectedIds.size} selected" else title) },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = onRestoreSelected) {
                            Icon(Icons.Filled.Restore, contentDescription = "Restore")
                        }
                        IconButton(onClick = onDeleteSelected) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Delete permanently")
                        }
                    } else if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashConfirm = true }) {
                            Text("Empty Trash")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        if (uiState.items.isEmpty()) {
            EmptyState(message = "Trash is empty.", modifier = Modifier.padding(paddingValues))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(2.dp)
            ) {
                items(uiState.items, key = { it.media.id }) { trashItem ->
                    TrashThumbnail(
                        trashItem = trashItem,
                        isSelected = trashItem.media.id in uiState.selectedIds,
                        onClick = { onToggleSelection(trashItem.media.id) },
                        onLongClick = { onToggleSelection(trashItem.media.id) }
                    )
                }
            }
        }
    }

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = { Text("Empty Trash") },
            text = { Text("All ${uiState.items.size} item(s) will be permanently deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false; onEmptyTrash() }) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TrashThumbnail(
    trashItem: TrashUiItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = trashItem.media.contentUri ?: trashItem.media.filePath,
            contentDescription = trashItem.media.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }
        Text(
            text = "${trashItem.daysRemaining}d left",
            color = Color.White,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
