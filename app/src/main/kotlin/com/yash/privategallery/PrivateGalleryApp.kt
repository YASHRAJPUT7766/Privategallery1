package com.yash.privategallery

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.yash.privategallery.core.security.AppProcessLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PrivateGalleryApp : Application() {

    @Inject
    lateinit var appProcessLifecycleObserver: AppProcessLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        // Section 5/45: observe whole-process foreground/background transitions
        // so auto-lock timing is evaluated app-wide, not per-screen.
        ProcessLifecycleOwner.get().lifecycle.addObserver(appProcessLifecycleObserver)
    }
}
