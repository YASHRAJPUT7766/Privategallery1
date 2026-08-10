package com.yash.privategallery.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** Result of checking the app's current media access level (Section 50). */
enum class MediaPermissionState {
    /** Full access to images and videos. */
    FULL_ACCESS,
    /** Android 14+ "select photos" partial access — some items granted, not all. */
    PARTIAL_ACCESS,
    /** No access granted yet. */
    DENIED
}

object MediaPermissions {

    /** The permission set to request, resolved for the running OS version. */
    val requiredPermissions: Array<String> = buildList {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        add(Manifest.permission.READ_MEDIA_VIDEO)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }.toTypedArray()

    fun currentState(context: Context): MediaPermissionState {
        val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

        if (hasImages && hasVideo) return MediaPermissionState.FULL_ACCESS

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            val hasPartial = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPartial) return MediaPermissionState.PARTIAL_ACCESS
        }

        return MediaPermissionState.DENIED
    }
}
