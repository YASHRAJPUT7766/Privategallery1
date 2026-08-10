package com.yash.privategallery.domain.model

/**
 * Type of a media item. Kept separate from MIME string so UI logic (thumbnails,
 * viewer routing, filters — Section 8) can switch on a small closed set instead
 * of parsing MIME strings everywhere.
 */
enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * How a media item is currently stored. NORMAL items live in the public MediaStore.
 * PRIVATE items live only in the app's encrypted private storage (Section 2) and are
 * never present in MediaStore. A MediaItem is one or the other, never both at once —
 * the move operations (Sections 21/23) atomically transition an item from one
 * representation to the other, never leave duplicates in both stores.
 */
enum class StorageLocation {
    NORMAL,
    PRIVATE
}

/**
 * A single photo or video, normalized across both the public MediaStore-backed
 * gallery and the private encrypted vault.
 *
 * For NORMAL items: [id] is the MediaStore row id, [contentUri] is a content:// URI
 * MediaStore resolves, [filePath] may be null (not guaranteed on modern Android).
 *
 * For PRIVATE items: [id] is the private database primary key, [contentUri] is null
 * (private media is never exposed via a content:// URI reachable by other apps —
 * Section 2), and [filePath] points at an encrypted file inside app-private storage
 * that only this app's own code can decrypt (Section 41/42).
 */
data class MediaItem(
    val id: Long,
    val displayName: String,
    val mediaType: MediaType,
    val storageLocation: StorageLocation,
    val contentUri: String?,
    val filePath: String?,
    val dateTaken: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long? = null,
    val mimeType: String,
    val isFavorite: Boolean = false,
    val bucketName: String? = null,
    val albumId: Long? = null,
    val isTrashed: Boolean = false,
    val trashedAt: Long? = null
)

/**
 * Media grouped under a date-section header for the home timeline (Section 6, 7).
 * [label] is a pre-resolved display string ("Today", "Yesterday", "August 5, 2026")
 * rather than a raw date, since the exact "Today/Yesterday/This Week" bucketing rule
 * is a display-time decision the UI layer/use case computes, not something the
 * repository should hardcode.
 */
data class MediaDateGroup(
    val label: String,
    val items: List<MediaItem>
)
