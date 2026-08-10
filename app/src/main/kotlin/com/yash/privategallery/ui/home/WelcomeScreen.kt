package com.yash.privategallery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yash.privategallery.R

/**
 * Section 54: Welcome step of the first-launch flow. Purely informational —
 * sets expectations before the permission rationale/request that follows.
 */
@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
        }
    }
}
