package com.yash.privategallery.ui.albums

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.AuthMethod
import com.yash.privategallery.domain.repository.AlbumRepository
import com.yash.privategallery.domain.repository.AuthResult
import com.yash.privategallery.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumLockUiState(
    val isVerifyMode: Boolean = true,
    val enteredPin: String = "",
    val firstPinEntry: String? = null,
    val isConfirmStep: Boolean = false,
    val showError: Boolean = false,
    val lockoutSecondsRemaining: Long = 0,
    val isSetupComplete: Boolean = false,
    val isVerified: Boolean = false
)

/**
 * Backs [AlbumLockScreen]. [setMode] is called by the caller right after
 * construction, based on which nav route reached this screen (Lock a new
 * album → setup mode; opening an already-locked album → verify mode).
 */
@HiltViewModel
class AlbumLockViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val securityRepository: SecurityRepository,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<String>("albumId")?.toLongOrNull() ?: -1L

    private val _uiState = MutableStateFlow(AlbumLockUiState())
    val uiState: StateFlow<AlbumLockUiState> = _uiState.asStateFlow()

    fun setMode(isVerify: Boolean) {
        _uiState.value = _uiState.value.copy(isVerifyMode = isVerify)
    }

    fun onDigitEntered(digit: Int) {
        if (_uiState.value.lockoutSecondsRemaining > 0) return
        val newPin = _uiState.value.enteredPin + digit
        _uiState.value = _uiState.value.copy(enteredPin = newPin, showError = false)
        if (newPin.length >= PIN_LENGTH) {
            if (_uiState.value.isVerifyMode) verifyExisting(newPin) else handleSetupStep(newPin)
        }
    }

    fun onBackspace() {
        _uiState.value = _uiState.value.copy(enteredPin = _uiState.value.enteredPin.dropLast(1), showError = false)
    }

    private fun verifyExisting(pin: String) {
        viewModelScope.launch {
            when (val result = securityRepository.verifyAlbumSecret(albumId, pin)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(isVerified = true)
                is AuthResult.Failed -> _uiState.value = _uiState.value.copy(enteredPin = "", showError = true)
                is AuthResult.TooManyAttempts -> _uiState.value = _uiState.value.copy(
                    enteredPin = "", lockoutSecondsRemaining = result.retryAfterSeconds
                )
            }
        }
    }

    private fun handleSetupStep(pin: String) {
        val firstEntry = _uiState.value.firstPinEntry
        if (firstEntry == null) {
            _uiState.value = _uiState.value.copy(firstPinEntry = pin, enteredPin = "", isConfirmStep = true)
        } else if (pin == firstEntry) {
            viewModelScope.launch {
                securityRepository.setAlbumSecret(albumId, AuthMethod.PIN, pin)
                albumRepository.lockAlbum(albumId, AuthMethod.PIN)
                _uiState.value = _uiState.value.copy(isSetupComplete = true)
            }
        } else {
            _uiState.value = _uiState.value.copy(firstPinEntry = null, enteredPin = "", isConfirmStep = false, showError = true)
        }
    }

    private companion object {
        const val PIN_LENGTH = 6
    }
}
