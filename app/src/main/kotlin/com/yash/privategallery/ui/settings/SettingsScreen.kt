package com.yash.privategallery.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Section 35's top-level Settings tree. Grouped into: Gallery (inline
 * toggles, since those are single quick flips — Section 33), Privacy &
 * Security / Appearance / Trash (each a sub-screen, since they carry
 * multiple related choices), Editing (inline, two toggles), and About.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenTrashSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by settingsViewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item { SettingsSectionHeader("Gallery") }
            item {
                SettingsChoiceRow(
                    title = "Grid size",
                    currentValueLabel = settings.gridSize.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { settingsViewModel.setGridSize(nextGridSize(settings.gridSize)) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Show videos",
                    checked = settings.showVideosInGallery,
                    onCheckedChange = { settingsViewModel.setShowVideosInGallery(it) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Show hidden system folders",
                    checked = settings.showHiddenSystemFolders,
                    onCheckedChange = { settingsViewModel.setShowHiddenSystemFolders(it) }
                )
            }

            item { SettingsSectionHeader("Privacy & Security") }
            item { SettingsNavigationRow(title = "Security settings", subtitle = "Locks, biometric, auto-lock", onClick = onOpenSecuritySettings) }

            item { SettingsSectionHeader("Media") }
            item {
                SettingsToggleRow(
                    title = "Include downloads in scan",
                    checked = settings.includeDownloadsInScan,
                    onCheckedChange = { settingsViewModel.setIncludeDownloadsInScan(it) }
                )
            }

            item { SettingsSectionHeader("Trash") }
            item { SettingsNavigationRow(title = "Trash settings", subtitle = "Retention period", onClick = onOpenTrashSettings) }

            item { SettingsSectionHeader("Editing") }
            item {
                SettingsToggleRow(
                    title = "Save edited image as copy by default",
                    checked = settings.saveEditedAsCopyByDefault,
                    onCheckedChange = { settingsViewModel.setSaveEditedAsCopyByDefault(it) }
                )
            }
            item {
                SettingsToggleRow(
                    title = "Preserve metadata",
                    checked = settings.preserveMetadataOnEdit,
                    onCheckedChange = { settingsViewModel.setPreserveMetadataOnEdit(it) }
                )
            }

            item { SettingsSectionHeader("Appearance") }
            item { SettingsNavigationRow(title = "Appearance settings", subtitle = "Theme, dynamic color, animations", onClick = onOpenAppearanceSettings) }

            item { SettingsSectionHeader("About") }
            item { SettingsNavigationRow(title = "About", subtitle = "Version, privacy policy, licenses", onClick = onOpenAbout) }
        }
    }
}

private fun nextGridSize(current: com.yash.privategallery.domain.model.GridSize): com.yash.privategallery.domain.model.GridSize {
    val values = com.yash.privategallery.domain.model.GridSize.values()
    return values[(current.ordinal + 1) % values.size]
}
