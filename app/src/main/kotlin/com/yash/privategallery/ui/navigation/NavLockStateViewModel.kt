package com.yash.privategallery.ui.navigation

import androidx.lifecycle.ViewModel
import com.yash.privategallery.core.security.AppLockStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * [AppLockStateManager] is a plain `@Singleton`, not a ViewModel — it must
 * outlive any single screen and is fed by process-level lifecycle callbacks
 * (see AppProcessLifecycleObserver), not a screen's lifecycle. This thin
 * wrapper is the standard way to surface a Hilt singleton's state into
 * Compose via `hiltViewModel()` without giving the singleton itself
 * ViewModel semantics it shouldn't have (it must NOT be cleared when one
 * screen's ViewModel scope ends).
 */
@HiltViewModel
class NavLockStateViewModel @Inject constructor(
    lockStateManager: AppLockStateManager
) : ViewModel() {
    val isPrivateLocked: StateFlow<Boolean> = lockStateManager.isPrivateLocked
}
