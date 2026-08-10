package com.yash.privategallery.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.domain.model.TrashDuration

/** Section 27/35: how long items stay in Recently Deleted before permanent removal. */
@Composable
fun TrashSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showDurationMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item {
                SettingsChoiceRow(
                    title = "Trash retention",
                    currentValueLabel = "${settings.trashDuration.days} days",
                    onClick = { showDurationMenu = true }
                )
            }
        }
    }

    if (showDurationMenu) {
        DropdownMenu(expanded = true, onDismissRequest = { showDurationMenu = false }) {
            TrashDuration.values().forEach { duration ->
                DropdownMenuItem(
                    text = { Text("${duration.days} days") },
                    onClick = { viewModel.setTrashDuration(duration); showDurationMenu = false }
                )
            }
        }
    }
}
