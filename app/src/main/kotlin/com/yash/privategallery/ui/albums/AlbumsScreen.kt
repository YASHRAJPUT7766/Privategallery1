package com.yash.privategallery.ui.albums

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.domain.model.Album
import com.yash.privategallery.domain.model.AlbumKind

/**
 * Section 15: Albums screen. Default albums (All Images, Camera,
 * Screenshots, Downloads, Favorites, Videos, Recently Added) render first,
 * followed by "My Albums" (Section 17). Long-pressing a CUSTOM album opens
 * the management menu (Section 18-19: Open/Rename/Lock/Unlock/Delete);
 * default albums have no such menu since they're computed, not user-owned.
 */
@Composable
fun AlbumsScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenLockedAlbum: (Album) -> Unit,
    onOpenLockSetup: (Album) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumForMenu by remember { mutableStateOf<Album?>(null) }

    val defaultAlbums = albums.filter { it.kind != AlbumKind.CUSTOM }
    val customAlbums = albums.filter { it.kind == AlbumKind.CUSTOM }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create album")
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(12.dp)
        ) {
            items(defaultAlbums, key = { "default_${it.kind}" }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onOpenAlbum(album) },
                    onLongClick = {}
                )
            }

            if (customAlbums.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "My Albums",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(customAlbums, key = { "custom_${it.id}" }) { album ->
                    AlbumCard(
                        album = album,
                        onClick = { if (album.isLocked) onOpenLockedAlbum(album) else onOpenAlbum(album) },
                        onLongClick = { albumForMenu = album }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createAlbum(name)
                showCreateDialog = false
            }
        )
    }

    albumForMenu?.let { album ->
        AlbumManagementMenu(
            album = album,
            onDismiss = { albumForMenu = null },
            onOpen = { onOpenAlbum(album); albumForMenu = null },
            onRename = { newName -> viewModel.renameAlbum(album.id, newName); albumForMenu = null },
            onDelete = { viewModel.deleteAlbum(album.id); albumForMenu = null },
            onRequestUnlock = { albumForMenu = null; onOpenLockedAlbum(album) },
            onRequestLock = { albumForMenu = null; onOpenLockSetup(album) }
        )
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit, onLongClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(6.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(album.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                if (album.isLocked) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text("${album.itemCount} items", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateAlbumDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Album") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Album name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Section 18: long-press menu — Open/Rename/Lock/Unlock/Delete/Album info. */
@Composable
private fun AlbumManagementMenu(
    album: Album,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRequestUnlock: () -> Unit,
    onRequestLock: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (!showRenameDialog && !showDeleteConfirm) {
        DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
            DropdownMenuItem(text = { Text("Open") }, onClick = onOpen)
            DropdownMenuItem(text = { Text("Rename") }, onClick = { showRenameDialog = true })
            if (album.isLocked) {
                DropdownMenuItem(text = { Text("Unlock") }, onClick = onRequestUnlock)
            } else {
                DropdownMenuItem(text = { Text("Lock") }, onClick = onRequestLock)
            }
            DropdownMenuItem(text = { Text("Delete") }, onClick = { showDeleteConfirm = true })
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(album.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false; onDismiss() },
            title = { Text("Rename Album") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { if (newName.isNotBlank()) onRename(newName.trim()) }, enabled = newName.isNotBlank()) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false; onDismiss() }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; onDismiss() },
            title = { Text("Delete Album") },
            text = { Text("\"${album.name}\" will be deleted. The photos inside it are not deleted, only the album itself.") },
            confirmButton = {
                TextButton(onClick = onDelete) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDismiss() }) { Text("Cancel") }
            }
        )
    }
}
