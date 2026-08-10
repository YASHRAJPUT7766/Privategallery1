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
import androidx.compose.ui.Modifier

/**
 * Section 35's About screen. Version is passed in rather than read from
 * BuildConfig directly here, so this composable stays previewable without a
 * full Gradle build context — the nav host wires the real BuildConfig
 * version at the call site.
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    versionName: String = "1.0.0",
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onSendFeedback: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            item { SettingsNavigationRow(title = "Version", subtitle = versionName, onClick = {}) }
            item { SettingsNavigationRow(title = "Privacy Policy", onClick = onOpenPrivacyPolicy) }
            item { SettingsNavigationRow(title = "Open-source licenses", onClick = onOpenLicenses) }
            item { SettingsNavigationRow(title = "Send feedback", onClick = onSendFeedback) }
        }
    }
}
