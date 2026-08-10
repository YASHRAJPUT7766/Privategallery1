package com.yash.privategallery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.yash.privategallery.domain.model.AppTheme

/**
 * Resolves the user's [AppTheme] + dynamic-color preference (Section 36) into
 * an actual Material 3 color scheme, then applies it. SYSTEM_DEFAULT follows
 * [isSystemInDarkTheme]; dynamic color (Material You) is only attempted on
 * API 31+ where the platform API exists, silently falling back to the static
 * palette below that on older devices.
 */
@Composable
fun PrivateGalleryTheme(
    appTheme: AppTheme = AppTheme.SYSTEM_DEFAULT,
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PrivateGalleryTypography,
        content = content
    )
}
