package com.yash.privategallery.core.security

import com.yash.privategallery.domain.model.AutoLockDelay
import com.yash.privategallery.domain.repository.LockTarget
import com.yash.privategallery.domain.repository.SecurityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether each [LockTarget] is currently considered "locked" for the
 * purposes of gating navigation (Section 5, 45). This is process-wide,
 * singleton state — not tied to any one screen's lifecycle — because
 * "as soon as the user leaves the application... put the gallery into
 * locked state" must hold true regardless of which screen happened to be on
 * top when the app backgrounded.
 *
 * [onAppBackgrounded] / [onAppForegrounded] are called from the single
 * Activity's lifecycle (see MainActivity) rather than per-screen, matching
 * the single-activity architecture (Section 58). Timing math uses the
 * configured [AutoLockDelay] per target — Private Gallery defaults to
 * IMMEDIATELY (Section 5: "For Private Gallery, default should be
 * Immediately"), enforced already by SecurityRepositoryImpl's default.
 */
@Singleton
class AppLockStateManager @Inject constructor(
    private val securityRepository: SecurityRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _isPrivateLocked = MutableStateFlow(true) // private starts locked until first unlock
    val isPrivateLocked: StateFlow<Boolean> = _isPrivateLocked

    private val _isNormalLocked = MutableStateFlow(false) // reflects normal-gallery lock config, resolved below
    val isNormalLocked: StateFlow<Boolean> = _isNormalLocked

    private var backgroundedAtMs: Long? = null

    fun markUnlocked(target: LockTarget) {
        when (target) {
            LockTarget.PRIVATE_GALLERY -> _isPrivateLocked.value = false
            LockTarget.NORMAL_GALLERY -> _isNormalLocked.value = false
        }
    }

    /** Section 45 "Lock Now": immediately re-locks a target regardless of its auto-lock delay. */
    fun lockNow(target: LockTarget) {
        when (target) {
            LockTarget.PRIVATE_GALLERY -> _isPrivateLocked.value = true
            LockTarget.NORMAL_GALLERY -> _isNormalLocked.value = true
        }
    }

    fun onAppBackgrounded() {
        backgroundedAtMs = System.currentTimeMillis()
    }

    /**
     * Called when the app returns to foreground. Re-evaluates both lock
     * targets against how long the app was backgrounded vs. each target's
     * configured [AutoLockDelay] (Section 5's delay options). A target whose
     * delay is NEVER is only ever locked by an explicit "Lock Now" or the
     * next process start.
     */
    fun onAppForegrounded() {
        val backgroundedAt = backgroundedAtMs ?: return
        val elapsedSeconds = (System.currentTimeMillis() - backgroundedAt) / 1000L
        backgroundedAtMs = null

        scope.launch {
            evaluateReLock(LockTarget.PRIVATE_GALLERY, elapsedSeconds) { _isPrivateLocked.value = it }
            evaluateReLock(LockTarget.NORMAL_GALLERY, elapsedSeconds) { _isNormalLocked.value = it }
        }
    }

    private suspend fun evaluateReLock(target: LockTarget, elapsedSeconds: Long, apply: (Boolean) -> Unit) {
        val config = securityRepository.observeLockConfiguration(target).first()
        if (!config.isEnabled) {
            apply(false) // lock disabled entirely for this target — never gate navigation
            return
        }
        val shouldRelock = when (config.autoLockDelay) {
            AutoLockDelay.NEVER -> false
            AutoLockDelay.IMMEDIATELY -> true
            else -> elapsedSeconds >= config.autoLockDelay.seconds
        }
        if (shouldRelock) apply(true)
    }
}
