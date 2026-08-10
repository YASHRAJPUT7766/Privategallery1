package com.yash.privategallery.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// A restrained, premium palette — deep indigo/near-black as the anchor rather
// than a generic Material purple, avoiding the "default Compose demo" look
// (Section 37: "The gallery should feel like a real production Android
// gallery"). Private-space screens reuse this same palette (Section 36:
// "must remain consistent with the main application") rather than a separate
// neon/glow theme.

val md_theme_light_primary = Color(0xFF3A5CE0)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_secondary = Color(0xFF5B5D72)
val md_theme_light_background = Color(0xFFFDFBFF)
val md_theme_light_surface = Color(0xFFFDFBFF)
val md_theme_light_surfaceVariant = Color(0xFFE3E1EC)
val md_theme_light_onSurface = Color(0xFF1B1B1F)
val md_theme_light_error = Color(0xFFBA1A1A)

val md_theme_dark_primary = Color(0xFFB4C4FF)
val md_theme_dark_onPrimary = Color(0xFF00297A)
val md_theme_dark_secondary = Color(0xFFC4C5DD)
val md_theme_dark_background = Color(0xFF121212) // true near-black, favored by gallery/media apps for OLED + content focus
val md_theme_dark_surface = Color(0xFF141318)
val md_theme_dark_surfaceVariant = Color(0xFF46464F)
val md_theme_dark_onSurface = Color(0xFFE4E1E6)
val md_theme_dark_error = Color(0xFFFFB4AB)

val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    background = md_theme_light_background,
    surface = md_theme_light_surface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurface = md_theme_light_onSurface,
    error = md_theme_light_error
)

val DarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurface = md_theme_dark_onSurface,
    error = md_theme_dark_error
)
