package com.yash.privategallery.ui.albums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Section 18/19: used both to SET a new lock on a previously-unlocked album
 * (two-step: enter PIN, confirm PIN) and to VERIFY an existing lock before
 * opening a locked album. [AlbumLockViewModel]'s mode determines which flow
 * renders — the two share the same keypad UI since the interaction pattern
 * (enter digits, see dots fill) is identical either way.
 */
@Composable
fun AlbumLockScreen(
    isVerifyMode: Boolean,
    onSetupComplete: () -> Unit,
    onVerified: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AlbumLockViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setMode(isVerifyMode)
    }

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) onSetupComplete()
    }
    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) onVerified()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    uiState.isVerifyMode -> "Enter Album PIN"
                    uiState.isConfirmStep -> "Confirm PIN"
                    else -> "Set Album PIN"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.lockoutSecondsRemaining > 0) {
                Text("Too many attempts. Try again in ${uiState.lockoutSecondsRemaining}s", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            } else if (uiState.showError) {
                Text(if (uiState.isVerifyMode) "Incorrect PIN" else "PINs didn't match — try again", color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { index ->
                    val filled = index < uiState.enteredPin.length
                    Surface(
                        shape = CircleShape,
                        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(14.dp)
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            AlbumPinKeypad(
                onDigit = { viewModel.onDigitEntered(it) },
                onBackspace = { viewModel.onBackspace() },
                enabled = uiState.lockoutSecondsRemaining <= 0
            )

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun AlbumPinKeypad(onDigit: (Int) -> Unit, onBackspace: () -> Unit, enabled: Boolean) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit ->
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(64.dp)) {
                        IconButton(onClick = { onDigit(digit) }, enabled = enabled, modifier = Modifier.fillMaxSize()) {
                            Text(digit.toString(), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Spacer(modifier = Modifier.size(64.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(64.dp)) {
                IconButton(onClick = { onDigit(0) }, enabled = enabled, modifier = Modifier.fillMaxSize()) {
                    Text("0", style = MaterialTheme.typography.titleLarge)
                }
            }
            IconButton(onClick = onBackspace, enabled = enabled, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.Backspace, contentDescription = "Backspace")
            }
        }
    }
}
