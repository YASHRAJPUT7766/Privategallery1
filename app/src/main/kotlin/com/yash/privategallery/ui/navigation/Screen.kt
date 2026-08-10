package com.yash.privategallery.ui.navigation

/**
 * All navigation destinations (Section 60's 30 screens), defined together so
 * the graph's shape is visible in one place even though not every screen is
 * implemented yet in this build stage. Composable-level implementation status
 * is tracked in PROGRESS.md at the project root, not here.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Permission : Screen("permission")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object ImageViewer : Screen("viewer/{collectionKey}/{startIndex}") {
        fun route(collectionKey: String, startIndex: Int) = "viewer/$collectionKey/$startIndex"
    }
    data object PrivateImageViewer : Screen("private_viewer/{collectionKey}/{startIndex}") {
        fun route(collectionKey: String, startIndex: Int) = "private_viewer/$collectionKey/$startIndex"
    }
    data object VideoPlayer : Screen("video/{mediaId}/{isPrivate}") {
        fun route(mediaId: Long, isPrivate: Boolean) = "video/$mediaId/$isPrivate"
    }
    data object ImageEditor : Screen("editor/{mediaId}/{isPrivate}") {
        fun route(mediaId: Long, isPrivate: Boolean) = "editor/$mediaId/$isPrivate"
    }
    data object ImageInfo : Screen("info/{mediaId}/{isPrivate}") {
        fun route(mediaId: Long, isPrivate: Boolean) = "info/$mediaId/$isPrivate"
    }
    data object Albums : Screen("albums")
    data object AlbumDetail : Screen("album/{albumId}") {
        fun route(albumId: Long) = "album/$albumId"
    }
    data object CreateAlbum : Screen("create_album")
    data object EditAlbum : Screen("edit_album/{albumId}") {
        fun route(albumId: Long) = "edit_album/$albumId"
    }
    data object AlbumLock : Screen("album_lock/{albumId}") {
        fun route(albumId: Long) = "album_lock/$albumId"
    }
    data object Favorites : Screen("favorites")
    data object RecentlyDeleted : Screen("recently_deleted")
    data object Duplicates : Screen("duplicates")
    data object NormalGalleryLock : Screen("normal_lock")
    data object PrivateGalleryLock : Screen("private_lock")
    data object PrivateGallery : Screen("private_gallery")
    data object PrivateTrash : Screen("private_trash")
    data object PrivateAlbumsList : Screen("private_albums_list")
    data object PrivateAlbum : Screen("private_album/{albumId}") {
        fun route(albumId: Long) = "private_album/$albumId"
    }
    data object PrivateSearch : Screen("private_search")
    data object ImportToPrivate : Screen("import_to_private")
    data object ExportFromPrivate : Screen("export_from_private")
    data object Settings : Screen("settings")
    data object SecuritySettings : Screen("settings/security")
    data object AppearanceSettings : Screen("settings/appearance")
    data object TrashSettings : Screen("settings/trash")
    data object About : Screen("settings/about")
    data object Slideshow : Screen("slideshow/{isPrivate}") {
        fun route(isPrivate: Boolean) = "slideshow/$isPrivate"
    }
}
