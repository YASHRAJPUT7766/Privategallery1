package com.yash.privategallery.ui.private

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.domain.model.Album
import com.yash.privategallery.ui.common.EmptyState
import com.yash.privategallery.ui.common.SecureScreenEffect

@Composable
fun PrivateAlbumsScreen(
    onBack: () -> Unit,
    onOpenAlbum: (Long) -> Unit,
    viewModel: PrivateAlbumsViewModel = hiltViewModel()
) {
    val albums by viewModel.albums.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    SecureScreenEffect()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Albums") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create album")
            }
        }
    ) { paddingValues ->
        if (albums.isEmpty()) {
            EmptyState(message = "Create your first album.", modifier = Modifier.padding(paddingValues))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    PrivateAlbumCard(album = album, onClick = { onOpenAlbum(album.id) })
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePrivateAlbumDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createAlbum(name)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun PrivateAlbumCard(album: Album, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(album.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            Text("${album.itemCount} items", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreatePrivateAlbumDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
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
