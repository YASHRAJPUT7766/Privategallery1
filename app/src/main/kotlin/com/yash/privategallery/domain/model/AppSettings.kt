package com.yash.privategallery.domain.model

enum class GridSize { COMPACT_4_COL, DEFAULT_3_COL, LARGE_2_COL, LIST_VIEW }

enum class AppTheme { LIGHT, DARK, SYSTEM_DEFAULT }

enum class SlideshowInterval(val seconds: Int) { THREE(3), FIVE(5), TEN(10) }

enum class TrashDuration(val days: Int) { DAYS_7(7), DAYS_15(15), DAYS_30(30), DAYS_60(60) }

/**
 * All user-adjustable, non-security app preferences (Section 33, 35). Security-
 * related settings (locks, biometric, screenshot protection) intentionally live
 * in [LockConfiguration]/[SecurityRepository] instead, so the two concerns can
 * evolve and be tested independently.
 */
data class AppSettings(
    val gridSize: GridSize = GridSize.DEFAULT_3_COL,
    val theme: AppTheme = AppTheme.SYSTEM_DEFAULT,
    val useDynamicColor: Boolean = true,
    val animationsEnabled: Boolean = true,
    val showVideosInGallery: Boolean = true,
    val showHiddenSystemFolders: Boolean = false,
    val showFileNames: Boolean = false,
    val showVideoDuration: Boolean = true,
    val showFavoriteIcon: Boolean = true,
    val includeDownloadsInScan: Boolean = true,
    val saveEditedAsCopyByDefault: Boolean = true,
    val preserveMetadataOnEdit: Boolean = true,
    val trashDuration: TrashDuration = TrashDuration.DAYS_30,
    val defaultSlideshowInterval: SlideshowInterval = SlideshowInterval.FIVE
)
