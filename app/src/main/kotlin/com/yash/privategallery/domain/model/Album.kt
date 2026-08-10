package com.yash.privategallery.domain.model

/** Distinguishes system-provided albums from user-created ones (Section 15). */
enum class AlbumKind {
    ALL_IMAGES,
    CAMERA,
    SCREENSHOTS,
    DOWNLOADS,
    FAVORITES,
    VIDEOS,
    RECENTLY_ADDED,
    RECENTLY_DELETED,
    PRIVATE,
    CUSTOM
}

/** Authentication method a locked album (or the app's normal/private lock) uses. */
enum class AuthMethod {
    NONE,
    PIN,
    PASSWORD,
    PATTERN,
    BIOMETRIC
}

/**
 * An album — either a default/system album backed by a MediaStore bucket query,
 * or a user-created custom album backed by Room-tracked membership (Section 40:
 * "Do not duplicate the entire public gallery unnecessarily into Room" — only
 * CUSTOM album membership and lock metadata are stored in Room; default albums
 * are computed queries over MediaStore).
 */
data class Album(
    val id: Long,
    val name: String,
    val kind: AlbumKind,
    val coverUri: String?,
    val itemCount: Int,
    val isLocked: Boolean = false,
    val authMethod: AuthMethod = AuthMethod.NONE,
    val iconKey: String? = null,
    val isPrivate: Boolean = false
)
