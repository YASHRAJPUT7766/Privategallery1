package com.yash.privategallery.ui.vault

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.ui.common.EmptyState
import com.yash.privategallery.ui.common.MediaThumbnail
import com.yash.privategallery.ui.common.MoveToNormalConfirmDialog
import com.yash.privategallery.ui.common.SecureScreenEffect

/**
 * Section 20: Private Gallery home. Reachable only after [PrivateLockScreen]
 * has succeeded — this screen itself never re-checks auth (the nav graph
 * gates entry), matching Section 44's "Switching to Private should
 * immediately authenticate" / "After switching back, Private content must
 * disappear from the UI" — leaving this screen via [onSwitchToNormal] (which
 * pops it off the back stack rather than merely hiding it) ensures the
 * screen and its ViewModel are actually torn down, not kept alive
 * underneath.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateGalleryScreen(
    onSwitchToNormal: () -> Unit,
    onOpenImportPicker: () -> Unit,
    onOpenPrivateSearch: () -> Unit,
    onOpenViewer: (index: Int) -> Unit,
    onOpenPrivateAlbums: () -> Unit,
    onOpenPrivateTrash: () -> Unit,
    onOpenPrivateSettings: () -> Unit,
    viewModel: PrivateGalleryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var showMoveToNormalDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFromPicker(uris.map { it.toString() })
        }
    }

    LaunchedEffect(uiState.lastImportMessage) {
        uiState.lastImportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    SecureScreenEffect()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (uiState.isSelectionMode) "${uiState.selectedIds.size} selected" else "Private Gallery") },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.toggleFavoriteSelected() }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
                        }
                        IconButton(onClick = { showMoveToNormalDialog = true }) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Move to Gallery")
                        }
                        IconButton(onClick = { viewModel.moveSelectedToTrash() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    } else {
                        IconButton(onClick = onOpenPrivateSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = {
                            pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Import")
                        }
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Manage private albums") }, onClick = { menuExpanded = false; onOpenPrivateAlbums() })
                            DropdownMenuItem(text = { Text("Private Trash") }, onClick = { menuExpanded = false; onOpenPrivateTrash() })
                            DropdownMenuItem(text = { Text("Private settings") }, onClick = { menuExpanded = false; onOpenPrivateSettings() })
                            DropdownMenuItem(text = { Text("Lock now") }, onClick = { menuExpanded = false; viewModel.lockNow(); onSwitchToNormal() })
                            DropdownMenuItem(text = { Text("Switch to Normal Gallery") }, onClick = { menuExpanded = false; onSwitchToNormal() })
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
            )
        }
    ) { paddingValues ->
        if (uiState.items.isEmpty()) {
            EmptyState(
                message = "Your private space is ready.",
                modifier = Modifier.padding(paddingValues)
            )
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
                        isSelected = item.id in uiState.selectedIds,
                        isSelectionMode = uiState.isSelectionMode,
                        showFavoriteIcon = true,
                        showDuration = true,
                        onClick = {
                            if (uiState.isSelectionMode) viewModel.toggleSelection(item.id)
                            else onOpenViewer(index)
                        },
                        onLongClick = { viewModel.toggleSelection(item.id) },
                        modifier = Modifier.aspectRatio(1f).padding(1.dp)
                    )
                }
            }
        }
    }

    if (showMoveToNormalDialog) {
        MoveToNormalConfirmDialog(
            itemCount = uiState.selectedIds.size,
            onConfirm = {
                showMoveToNormalDialog = false
                viewModel.moveSelectedToNormal()
            },
            onDismiss = { showMoveToNormalDialog = false }
        )
    }
}
