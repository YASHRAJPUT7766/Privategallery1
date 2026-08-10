package com.yash.privategallery.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
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
import com.yash.privategallery.ui.common.SecureScreenEffect

/**
 * Section 43: beautiful lock screen. Never displays private photos while
 * locked (this screen has no access to any private MediaItem at all — the
 * ViewModel it's backed by only knows auth state, never vault contents,
 * which structurally guarantees "the lock screen must not display private
 * photos" and "do not reveal whether a particular private file exists while
 * locked").
 *
 * Also doubles as the Section 54/3 lock SETUP flow (two-step enter+confirm)
 * when [isSetupMode] is true — same keypad UI, different completion
 * callback ([onSetupComplete] instead of [onUnlocked]), matching how
 * [com.yash.privategallery.ui.albums.AlbumLockScreen] handles the same
 * dual-purpose need for album locks.
 *
 * Wrong-PIN handling and lockout messaging come straight from
 * [LockScreenViewModel]'s AuthResult states, backed by the real rate
 * limiter from core/security (Section 43).
 */
@Composable
fun PrivateLockScreen(
    isSetupMode: Boolean = false,
    target: com.yash.privategallery.domain.repository.LockTarget = com.yash.privategallery.domain.repository.LockTarget.PRIVATE_GALLERY,
    onUnlocked: () -> Unit = {},
    onSetupComplete: () -> Unit = {},
    onCancel: () -> Unit,
    viewModel: LockScreenViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setTarget(target)
        viewModel.setSetupMode(isSetupMode)
    }

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) onSetupComplete()
    }

    LaunchedEffect(uiState.isUnlocked) {
        if (uiState.isUnlocked) onUnlocked()
    }

    LaunchedEffect(Unit) {
        if (!isSetupMode) viewModel.tryBiometricIfAvailable()
    }

    SecureScreenEffect()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    !isSetupMode -> if (target == com.yash.privategallery.domain.repository.LockTarget.PRIVATE_GALLERY) "Private Gallery" else "Gallery Locked"
                    uiState.isConfirmStep -> "Confirm PIN"
                    else -> "Set PIN"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.lockoutSecondsRemaining > 0) {
                Text(
                    text = "Too many attempts. Try again in ${uiState.lockoutSecondsRemaining}s",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            } else if (uiState.showError) {
                Text(text = "Incorrect PIN", color = MaterialTheme.colorScheme.error)
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

            PinKeypad(
                onDigit = { viewModel.onDigitEntered(it) },
                onBackspace = { viewModel.onBackspace() },
                onBiometric = if (!isSetupMode && uiState.biometricAvailable) ({ viewModel.tryBiometricIfAvailable(forceShow = true) }) else null,
                enabled = uiState.lockoutSecondsRemaining <= 0
            )

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?,
    enabled: Boolean
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { digit ->
                    KeypadButton(label = digit.toString(), enabled = enabled) { onDigit(digit) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (onBiometric != null) {
                IconButton(onClick = onBiometric, enabled = enabled, modifier = Modifier.size(64.dp)) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = "Use biometric")
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }
            KeypadButton(label = "0", enabled = enabled) { onDigit(0) }
            IconButton(onClick = onBackspace, enabled = enabled, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.Backspace, contentDescription = "Backspace")
            }
        }
    }
}

@Composable
private fun KeypadButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(64.dp)
    ) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxSize()) {
            Text(label, style = MaterialTheme.typography.titleLarge)
        }
    }
}
