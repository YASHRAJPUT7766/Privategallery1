package com.yash.privategallery.ui.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yash.privategallery.ui.common.SecureScreenEffect

/** Section 13: Image Information screen. */
@Composable
fun ImageInfoScreen(
    onBack: () -> Unit,
    viewModel: ImageInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isPrivate) {
        SecureScreenEffect()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!uiState.isLoading) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                InfoRow("File name", uiState.fileName)
                InfoRow("Date", uiState.dateTimeLabel)
                InfoRow("Size", uiState.fileSizeLabel)
                InfoRow("Resolution", uiState.resolutionLabel)
                InfoRow("Type", uiState.fileType)
                InfoRow("Folder", uiState.folderLabel)
                uiState.locationLabel?.let { InfoRow("Location", it) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
    Divider()
}
