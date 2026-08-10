package com.yash.privategallery.ui.common

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Section 5: "Use appropriate Android window security such as preventing
 * screenshots/screen capture for Private Gallery." Section 46: "Do not show
 * private photos in the [recent apps] preview."
 *
 * [WindowManager.LayoutParams.FLAG_SECURE] does both at once on Android: it
 * blocks screenshots/screen recording AND blanks the Recents preview thumbnail
 * for as long as the flag is set. Applied via [DisposableEffect] so it's
 * added when a private screen enters composition and removed when it leaves
 * — critically, removed rather than left set globally, so normal-gallery
 * screens are never accidentally screenshot-blocked too.
 */
@Composable
fun SecureScreenEffect() {
    val context = LocalContext.current
    val activity = context as? Activity ?: return

    DisposableEffect(Unit) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
