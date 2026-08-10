package com.yash.privategallery.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.core.security.AppLockStateManager
import com.yash.privategallery.domain.model.BiometricClass
import com.yash.privategallery.domain.repository.AuthResult
import com.yash.privategallery.domain.repository.LockTarget
import com.yash.privategallery.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LockScreenUiState(
    val isSetupMode: Boolean = false,
    val enteredPin: String = "",
    val firstPinEntry: String? = null,
    val isConfirmStep: Boolean = false,
    val showError: Boolean = false,
    val lockoutSecondsRemaining: Long = 0,
    val biometricAvailable: Boolean = false,
    val isUnlocked: Boolean = false,
    val isSetupComplete: Boolean = false,
    val requestBiometricPromptTrigger: Int = 0
)

/**
 * Drives [PrivateLockScreen] and (via [target]) is reused for the Normal
 * Gallery lock screen too — same PIN/biometric flow, different [LockTarget]
 * (Section 3: fully independent configuration, same UI mechanism).
 *
 * PIN entry is buffered digit-by-digit and auto-submitted once it reaches
 * [PIN_LENGTH], matching common OS lock-screen UX. Biometric availability is
 * reported by the caller (via a FragmentActivity-scoped check, since
 * BiometricManager needs an Activity) — this ViewModel only reacts to a
 * boolean already resolved for it, keeping it Activity-agnostic.
 */
@HiltViewModel
class LockScreenViewModel @Inject constructor(
    private val securityRepository: SecurityRepository,
    private val lockStateManager: AppLockStateManager
) : ViewModel() {

    var target: LockTarget = LockTarget.PRIVATE_GALLERY
        private set

    private val _uiState = MutableStateFlow(LockScreenUiState())
    val uiState: StateFlow<LockScreenUiState> = _uiState.asStateFlow()

    fun setTarget(newTarget: LockTarget) {
        target = newTarget
    }

    fun setSetupMode(isSetup: Boolean) {
        _uiState.value = _uiState.value.copy(isSetupMode = isSetup)
    }

    fun onDigitEntered(digit: Int) {
        if (_uiState.value.lockoutSecondsRemaining > 0) return
        val newPin = _uiState.value.enteredPin + digit
        _uiState.value = _uiState.value.copy(enteredPin = newPin, showError = false)
        if (newPin.length >= PIN_LENGTH) {
            if (_uiState.value.isSetupMode) handleSetupStep(newPin) else verify(newPin)
        }
    }

    private fun handleSetupStep(pin: String) {
        val firstEntry = _uiState.value.firstPinEntry
        if (firstEntry == null) {
            _uiState.value = _uiState.value.copy(firstPinEntry = pin, enteredPin = "", isConfirmStep = true)
        } else if (pin == firstEntry) {
            viewModelScope.launch {
                securityRepository.setSecret(target, com.yash.privategallery.domain.model.AuthMethod.PIN, pin)
                securityRepository.updateLockConfiguration(
                    target,
                    securityRepository.observeLockConfiguration(target).first().copy(isEnabled = true, authMethod = com.yash.privategallery.domain.model.AuthMethod.PIN)
                )
                lockStateManager.markUnlocked(target)
                _uiState.value = _uiState.value.copy(isSetupComplete = true)
            }
        } else {
            _uiState.value = _uiState.value.copy(firstPinEntry = null, enteredPin = "", isConfirmStep = false, showError = true)
        }
    }

    fun onBackspace() {
        _uiState.value = _uiState.value.copy(
            enteredPin = _uiState.value.enteredPin.dropLast(1),
            showError = false
        )
    }

    private fun verify(pin: String) {
        viewModelScope.launch {
            when (val result = securityRepository.verifySecret(target, pin)) {
                is AuthResult.Success -> {
                    lockStateManager.markUnlocked(target)
                    _uiState.value = _uiState.value.copy(isUnlocked = true)
                }
                is AuthResult.Failed -> _uiState.value = _uiState.value.copy(enteredPin = "", showError = true)
                is AuthResult.TooManyAttempts -> _uiState.value = _uiState.value.copy(
                    enteredPin = "",
                    lockoutSecondsRemaining = result.retryAfterSeconds
                )
            }
        }
    }

    /**
     * Marks biometric as available (set by the calling screen after checking
     * hardware via BiometricAuthenticator against an Activity) and, if
     * [forceShow] is true or this is the first automatic attempt, bumps
     * [requestBiometricPromptTrigger] so the UI layer knows to actually show
     * the system BiometricPrompt sheet — the prompt call itself must happen
     * in the Composable/Activity layer since it needs a FragmentActivity.
     */
    fun tryBiometricIfAvailable(forceShow: Boolean = false) {
        if (_uiState.value.biometricAvailable || forceShow) {
            _uiState.value = _uiState.value.copy(
                requestBiometricPromptTrigger = _uiState.value.requestBiometricPromptTrigger + 1
            )
        }
    }

    fun setBiometricAvailable(available: Boolean) {
        _uiState.value = _uiState.value.copy(biometricAvailable = available)
    }

    fun onBiometricSuccess() {
        lockStateManager.markUnlocked(target)
        _uiState.value = _uiState.value.copy(isUnlocked = true)
    }

    companion object {
        const val PIN_LENGTH = 6
    }
}
