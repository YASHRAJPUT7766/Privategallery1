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
import com.yash.privategallery.domain.model.AutoLockDelay
import com.yash.privategallery.domain.repository.LockTarget

/**
 * Section 3, 5, 35: normal and private gallery locks configured
 * independently (each its own [LockTarget]), plus per-target auto-lock
 * delay and screenshot protection toggles. Setting/changing the actual
 * PIN/password/pattern secret routes to the lock setup flow (reused rather
 * than duplicated) via [onSetupNormalLock]/[onSetupPrivateLock].
 */
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    onSetupNormalLock: () -> Unit,
    onSetupPrivateLock: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val normalConfig by viewModel.normalConfig.collectAsState()
    val privateConfig by viewModel.privateConfig.collectAsState()
    var autoLockMenuTarget by remember { mutableStateOf<LockTarget?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item { SettingsSectionHeader("Normal Gallery Lock") }
            item {
                SettingsToggleRow(
                    title = "Require lock to open gallery",
                    checked = normalConfig.isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) onSetupNormalLock() else viewModel.disableLock(LockTarget.NORMAL_GALLERY)
                    }
                )
            }
            if (normalConfig.isEnabled) {
                item {
                    SettingsChoiceRow(
                        title = "Auto lock",
                        currentValueLabel = normalConfig.autoLockDelay.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { autoLockMenuTarget = LockTarget.NORMAL_GALLERY }
                    )
                }
                item {
                    SettingsToggleRow(
                        title = "Screenshot protection",
                        checked = normalConfig.screenshotProtectionEnabled,
                        onCheckedChange = { viewModel.setScreenshotProtection(LockTarget.NORMAL_GALLERY, it) }
                    )
                }
            }

            item { SettingsSectionHeader("Private Gallery Lock") }
            item {
                SettingsToggleRow(
                    title = "Require lock to open Private Gallery",
                    checked = privateConfig.isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) onSetupPrivateLock() else viewModel.disableLock(LockTarget.PRIVATE_GALLERY)
                    }
                )
            }
            if (privateConfig.isEnabled) {
                item {
                    SettingsChoiceRow(
                        title = "Auto lock",
                        currentValueLabel = privateConfig.autoLockDelay.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                        onClick = { autoLockMenuTarget = LockTarget.PRIVATE_GALLERY }
                    )
                }
                item {
                    SettingsToggleRow(
                        title = "Screenshot protection",
                        checked = privateConfig.screenshotProtectionEnabled,
                        onCheckedChange = { viewModel.setScreenshotProtection(LockTarget.PRIVATE_GALLERY, it) }
                    )
                }
                item {
                    SettingsToggleRow(
                        title = "Hide from recents",
                        checked = privateConfig.hideFromRecents,
                        onCheckedChange = { viewModel.setHideFromRecents(LockTarget.PRIVATE_GALLERY, it) }
                    )
                }
            }
        }
    }

    autoLockMenuTarget?.let { target ->
        DropdownMenu(expanded = true, onDismissRequest = { autoLockMenuTarget = null }) {
            AutoLockDelay.values().forEach { delay ->
                DropdownMenuItem(
                    text = { Text(delay.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        viewModel.setAutoLockDelay(target, delay)
                        autoLockMenuTarget = null
                    }
                )
            }
        }
    }
}
