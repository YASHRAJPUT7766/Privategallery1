package com.yash.privategallery.ui.duplicates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.domain.usecase.DuplicateGroup
import com.yash.privategallery.ui.common.EmptyState
import com.yash.privategallery.ui.common.MediaThumbnail

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!uiState.hasScanned) viewModel.scanForDuplicates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duplicates") },
                actions = {
                    if (uiState.selectedForDeletion.isNotEmpty()) {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Delete (${uiState.selectedForDeletion.size})")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isScanning -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text("Scanning for duplicates…", modifier = Modifier.padding(top = 12.dp))
                }
            }
            uiState.groups.isEmpty() -> EmptyState(message = "No duplicates found.", modifier = Modifier.padding(paddingValues))
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                androidx.compose.foundation.lazy.items(uiState.groups) { group ->
                    DuplicateGroupCard(
                        group = group,
                        selectedIds = uiState.selectedForDeletion,
                        onToggleItem = { viewModel.toggleSelectionForDeletion(it) },
                        onKeepOne = { viewModel.selectAllButFirstInGroup(group) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Duplicates") },
            text = { Text("${uiState.selectedForDeletion.size} item(s) will be moved to Recently Deleted.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; viewModel.confirmDeleteSelected { } }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedIds: Set<Long>,
    onToggleItem: (Long) -> Unit,
    onKeepOne: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${group.items.size} similar photos", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onKeepOne) { Text("Keep one") }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                userScrollEnabled = false
            ) {
                androidx.compose.foundation.lazy.grid.items(group.items, key = { it.id }) { item ->
                    MediaThumbnail(
                        item = item,
                        isSelected = item.id in selectedIds,
                        isSelectionMode = true,
                        showFavoriteIcon = false,
                        showDuration = false,
                        onClick = { onToggleItem(item.id) },
                        onLongClick = { onToggleItem(item.id) },
                        modifier = Modifier.aspectRatio(1f).padding(2.dp)
                    )
                }
            }
        }
    }
}
