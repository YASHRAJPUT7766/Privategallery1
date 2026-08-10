package com.yash.privategallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yash.privategallery.domain.model.AuthMethod
import com.yash.privategallery.domain.model.AutoLockDelay
import com.yash.privategallery.domain.model.LockConfiguration
import com.yash.privategallery.domain.repository.LockTarget
import com.yash.privategallery.domain.repository.SecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DEFAULT_NORMAL_CONFIG = LockConfiguration(
    isEnabled = false,
    authMethod = AuthMethod.NONE,
    biometricClass = null,
    autoLockDelay = AutoLockDelay.AFTER_1_MINUTE,
    screenshotProtectionEnabled = false,
    hideFromRecents = false
)

private val DEFAULT_PRIVATE_CONFIG = LockConfiguration(
    isEnabled = false,
    authMethod = AuthMethod.NONE,
    biometricClass = null,
    autoLockDelay = AutoLockDelay.IMMEDIATELY,
    screenshotProtectionEnabled = true,
    hideFromRecents = true
)

/**
 * Section 3, 5, 35: mutation surface for lock configuration on both
 * targets. Disabling a lock here only flips [LockConfiguration.isEnabled]
 * — it deliberately does NOT clear the stored secret (Section 19's
 * "Forgot lock" flow and simply re-enabling later both depend on the
 * secret surviving a temporary disable).
 */
@HiltViewModel
class SecuritySettingsViewModel @Inject constructor(
    private val securityRepository: SecurityRepository
) : ViewModel() {

    val normalConfig: StateFlow<LockConfiguration> = securityRepository.observeLockConfiguration(LockTarget.NORMAL_GALLERY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_NORMAL_CONFIG)

    val privateConfig: StateFlow<LockConfiguration> = securityRepository.observeLockConfiguration(LockTarget.PRIVATE_GALLERY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_PRIVATE_CONFIG)

    fun disableLock(target: LockTarget) {
        viewModelScope.launch {
            val current = if (target == LockTarget.NORMAL_GALLERY) normalConfig.value else privateConfig.value
            securityRepository.updateLockConfiguration(target, current.copy(isEnabled = false))
        }
    }

    fun setAutoLockDelay(target: LockTarget, delay: AutoLockDelay) {
        viewModelScope.launch {
            val current = if (target == LockTarget.NORMAL_GALLERY) normalConfig.value else privateConfig.value
            securityRepository.updateLockConfiguration(target, current.copy(autoLockDelay = delay))
        }
    }

    fun setScreenshotProtection(target: LockTarget, enabled: Boolean) {
        viewModelScope.launch {
            val current = if (target == LockTarget.NORMAL_GALLERY) normalConfig.value else privateConfig.value
            securityRepository.updateLockConfiguration(target, current.copy(screenshotProtectionEnabled = enabled))
        }
    }

    fun setHideFromRecents(target: LockTarget, enabled: Boolean) {
        viewModelScope.launch {
            val current = if (target == LockTarget.NORMAL_GALLERY) normalConfig.value else privateConfig.value
            securityRepository.updateLockConfiguration(target, current.copy(hideFromRecents = enabled))
        }
    }
}
