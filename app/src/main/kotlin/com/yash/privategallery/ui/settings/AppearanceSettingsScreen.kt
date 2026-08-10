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
import com.yash.privategallery.domain.model.AppTheme

/** Section 36: Light / Dark / System default, plus Material You dynamic color and animation toggle. */
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var showThemeMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
                    title = "Theme",
                    currentValueLabel = settings.theme.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showThemeMenu = true }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Dynamic color",
                    subtitle = "Match system wallpaper colors (Android 12+)",
                    checked = settings.useDynamicColor,
                    onCheckedChange = { viewModel.setUseDynamicColor(it) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Animations",
                    checked = settings.animationsEnabled,
                    onCheckedChange = { viewModel.setAnimationsEnabled(it) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Show file names",
                    checked = settings.showFileNames,
                    onCheckedChange = { viewModel.setShowFileNames(it) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Show video duration",
                    checked = settings.showVideoDuration,
                    onCheckedChange = { viewModel.setShowVideoDuration(it) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Show favorite icon",
                    checked = settings.showFavoriteIcon,
                    onCheckedChange = { viewModel.setShowFavoriteIcon(it) }
                )
            }
        }
    }

    if (showThemeMenu) {
        DropdownMenu(expanded = true, onDismissRequest = { showThemeMenu = false }) {
            AppTheme.values().forEach { theme ->
                DropdownMenuItem(
                    text = { Text(theme.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = { viewModel.setTheme(theme); showThemeMenu = false }
                )
            }
        }
    }
}
