package com.yash.privategallery.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registered against androidx.lifecycle.ProcessLifecycleOwner (see
 * PrivateGalleryApp), this fires exactly on whole-app foreground/background
 * transitions — not per-Activity, which matters because a single
 * configuration change (rotation) or transient Activity recreation must
 * NOT be treated as "the user left the app" (Section 5). ON_STOP here means
 * every Activity of the process has stopped; ON_START means the process is
 * visible again.
 */
@Singleton
class AppProcessLifecycleObserver @Inject constructor(
    private val lockStateManager: AppLockStateManager
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        lockStateManager.onAppBackgrounded()
    }

    override fun onStart(owner: LifecycleOwner) {
        lockStateManager.onAppForegrounded()
    }
}
