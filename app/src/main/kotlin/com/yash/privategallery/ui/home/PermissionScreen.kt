package com.yash.privategallery.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yash.privategallery.R
import com.yash.privategallery.core.permissions.MediaPermissionState
import com.yash.privategallery.core.permissions.MediaPermissions

/**
 * Section 50: explains why media access is required, then requests it via the
 * modern per-media-type permissions. Handles all three post-request states —
 * full access, partial access (Android 14+ "select photos"), and denied —
 * rather than treating permission as a boolean, since partial access is a
 * fully-supported first-class state the home screen must work correctly
 * under (Section 50: "Support Android's newer photo/media access behavior,
 * including partial/selected media access where applicable").
 */
@Composable
fun PermissionScreen(
    onPermissionResult: (MediaPermissionState) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionResult(MediaPermissions.currentState(context))
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PermMedia,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.permission_rationale_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.permission_rationale_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { launcher.launch(MediaPermissions.requiredPermissions) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Allow Access")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { onPermissionResult(MediaPermissionState.DENIED) }) {
                Text("Not Now")
            }
        }
    }
}
