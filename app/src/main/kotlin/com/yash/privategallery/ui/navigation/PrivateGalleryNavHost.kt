package com.yash.privategallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yash.privategallery.core.permissions.MediaPermissionState
import com.yash.privategallery.core.permissions.MediaPermissions
import com.yash.privategallery.ui.home.HomeScreen
import com.yash.privategallery.ui.home.PermissionScreen
import com.yash.privategallery.ui.home.SplashScreen
import com.yash.privategallery.ui.home.WelcomeScreen
import com.yash.privategallery.ui.lock.PrivateLockScreen
import com.yash.privategallery.ui.editor.EditorScreen
import com.yash.privategallery.ui.duplicates.DuplicatesScreen
import com.yash.privategallery.ui.favorites.FavoritesScreen
import com.yash.privategallery.ui.info.ImageInfoScreen
import com.yash.privategallery.ui.settings.AboutScreen
import com.yash.privategallery.ui.settings.AppearanceSettingsScreen
import com.yash.privategallery.ui.settings.SecuritySettingsScreen
import com.yash.privategallery.ui.settings.SettingsScreen
import com.yash.privategallery.ui.settings.TrashSettingsScreen
import com.yash.privategallery.ui.slideshow.SlideshowScreen
import com.yash.privategallery.ui.trash.PrivateTrashScreen
import com.yash.privategallery.ui.trash.TrashScreen
import com.yash.privategallery.ui.search.SearchScreen
import com.yash.privategallery.ui.albums.AlbumDetailScreen
import com.yash.privategallery.ui.albums.AlbumLockScreen
import com.yash.privategallery.ui.albums.AlbumsScreen
import com.yash.privategallery.ui.vault.PrivateAlbumDetailScreen
import com.yash.privategallery.ui.vault.PrivateAlbumsScreen
import com.yash.privategallery.ui.vault.PrivateGalleryScreen
import com.yash.privategallery.ui.search.PrivateSearchScreen
import com.yash.privategallery.ui.video.VideoPlayerScreen
import com.yash.privategallery.ui.viewer.ImageViewerScreen

/**
 * Root nav graph. Only the first-launch flow (Section 54) + Home are wired
 * to real screens in this build stage; every other [Screen] route is a
 * placeholder pending implementation — see PROGRESS.md for exactly which.
 * Routes not yet implemented deliberately fall back to Home rather than
 * crashing on an unmapped route, so partially-built navigation never breaks
 * the app for routes a not-yet-built screen would call into.
 */
@Composable
fun PrivateGalleryNavHost(
    navController: NavHostController = rememberNavController(),
    startWithPermissionCheck: Boolean = true
) {
    val lockStateViewModel: NavLockStateViewModel = hiltViewModel()
    val isPrivateLocked by lockStateViewModel.isPrivateLocked.collectAsState()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()

    // Route templates that are EXCLUSIVELY private (their whole screen only
    // ever shows private content, never shared with a normal-mode variant).
    val exclusivelyPrivateRoutes = setOf(
        Screen.PrivateGallery.route,
        Screen.PrivateAlbumsList.route,
        Screen.PrivateSearch.route,
        Screen.PrivateAlbum.route,
        Screen.PrivateImageViewer.route,
        Screen.PrivateTrash.route
    )

    // Route templates SHARED between normal and private modes, distinguished
    // only by an "isPrivate" runtime argument — checked via the actual
    // argument value below, since NavBackStackEntry.destination.route
    // exposes the route PATTERN (e.g. "info/{mediaId}/{isPrivate}"), never
    // the instantiated path, so a naive string-prefix check here would
    // incorrectly force a re-lock for a NORMAL photo's info screen too.
    val sharedRoutesWithPrivateArg = setOf(Screen.ImageInfo.route, Screen.VideoPlayer.route, Screen.ImageEditor.route)

    // Section 5/45: if auto-lock has flipped the private target back to
    // locked (app was backgrounded past its configured delay) while the user
    // is currently inside any private-scoped screen, force them back out to
    // the lock screen immediately rather than letting stale unlocked state
    // leave private content visible.
    LaunchedEffect(isPrivateLocked, currentBackStackEntry) {
        val currentRoute = currentBackStackEntry?.destination?.route
        val isOnExclusivelyPrivateRoute = currentRoute in exclusivelyPrivateRoutes
        val isOnSharedPrivateInstance = currentRoute in sharedRoutesWithPrivateArg &&
            currentBackStackEntry?.arguments?.getString("isPrivate")?.toBoolean() == true
        if (isPrivateLocked && (isOnExclusivelyPrivateRoute || isOnSharedPrivateInstance)) {
            navController.navigate(Screen.PrivateGalleryLock.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    val destination = if (startWithPermissionCheck) Screen.Welcome.route else Screen.Home.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onContinue = { navController.navigate(Screen.Permission.route) }
            )
        }

        composable(Screen.Permission.route) {
            PermissionScreen(
                onPermissionResult = { state ->
                    // FULL_ACCESS and PARTIAL_ACCESS both proceed to Home — the
                    // grid itself renders whatever MediaStore returns, which
                    // naturally reflects partial-access scoping. DENIED still
                    // proceeds (Section 54: "user must be able to skip security
                    // setup and enable it later" — same spirit applied to media
                    // permission; Home's empty state covers the no-access case).
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenSearch = { navController.navigate(Screen.Search.route) },
                onOpenPrivateGallery = { navController.navigate(Screen.PrivateGalleryLock.route) },
                onOpenAlbums = { navController.navigate(Screen.Albums.route) },
                onOpenFavorites = { navController.navigate(Screen.Favorites.route) },
                onOpenTrash = { navController.navigate(Screen.RecentlyDeleted.route) },
                onOpenDuplicates = { navController.navigate(Screen.Duplicates.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenViewer = { collectionKey, index ->
                    navController.navigate(Screen.ImageViewer.route(collectionKey, index))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenSecuritySettings = { navController.navigate(Screen.SecuritySettings.route) },
                onOpenAppearanceSettings = { navController.navigate(Screen.AppearanceSettings.route) },
                onOpenTrashSettings = { navController.navigate(Screen.TrashSettings.route) },
                onOpenAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.SecuritySettings.route) {
            SecuritySettingsScreen(
                onBack = { navController.popBackStack() },
                onSetupNormalLock = { navController.navigate(Screen.NormalGalleryLock.route) },
                onSetupPrivateLock = { navController.navigate("${Screen.PrivateGalleryLock.route}?setup=true") }
            )
        }

        composable(Screen.AppearanceSettings.route) {
            AppearanceSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.TrashSettings.route) {
            TrashSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Slideshow.route,
            arguments = listOf(navArgument("isPrivate") { type = NavType.StringType })
        ) {
            SlideshowScreen(
                onExit = { navController.popBackStack() },
                isPrivateContext = it.arguments?.getString("isPrivate")?.toBoolean() ?: false
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenViewer = { index -> navController.navigate(Screen.ImageViewer.route("search", index)) }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onOpenViewer = { index -> navController.navigate(Screen.ImageViewer.route("favorites", index)) }
            )
        }

        composable(Screen.RecentlyDeleted.route) {
            TrashScreen()
        }

        composable(Screen.Duplicates.route) {
            DuplicatesScreen()
        }

        composable(Screen.Albums.route) {
            AlbumsScreen(
                onOpenAlbum = { album -> navController.navigate(Screen.AlbumDetail.route(album.id)) },
                onOpenLockedAlbum = { album -> navController.navigate("${Screen.AlbumLock.route(album.id)}?verify=true") },
                onOpenLockSetup = { album -> navController.navigate("${Screen.AlbumLock.route(album.id)}?verify=false") }
            )
        }

        composable(
            route = "${Screen.AlbumLock.route}?verify={verify}",
            arguments = listOf(
                navArgument("albumId") { type = NavType.StringType },
                navArgument("verify") { type = NavType.StringType; defaultValue = "true" }
            )
        ) { backStackEntry ->
            val isVerify = backStackEntry.arguments?.getString("verify")?.toBoolean() ?: true
            AlbumLockScreen(
                isVerifyMode = isVerify,
                onSetupComplete = { navController.popBackStack() },
                onVerified = {
                    navController.popBackStack()
                    val albumId = backStackEntry.arguments?.getString("albumId")?.toLongOrNull() ?: 0L
                    navController.navigate(Screen.AlbumDetail.route(albumId))
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) {
            AlbumDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenViewer = { index -> navController.navigate(Screen.ImageViewer.route("album", index)) }
            )
        }

        composable(Screen.PrivateGalleryLock.route) {
            PrivateLockScreen(
                isSetupMode = false,
                target = com.yash.privategallery.domain.repository.LockTarget.PRIVATE_GALLERY,
                onUnlocked = {
                    navController.navigate(Screen.PrivateGallery.route) {
                        popUpTo(Screen.PrivateGalleryLock.route) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("${Screen.PrivateGalleryLock.route}?setup=true") {
            PrivateLockScreen(
                isSetupMode = true,
                target = com.yash.privategallery.domain.repository.LockTarget.PRIVATE_GALLERY,
                onSetupComplete = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.NormalGalleryLock.route) {
            PrivateLockScreen(
                isSetupMode = true,
                target = com.yash.privategallery.domain.repository.LockTarget.NORMAL_GALLERY,
                onSetupComplete = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.PrivateGallery.route) {
            PrivateGalleryScreen(
                onSwitchToNormal = {
                    // Section 44: pop fully off the back stack rather than just
                    // navigating away, so the private screen + its ViewModel
                    // (and everything it was observing) are actually torn down —
                    // "Never simply keep private screens alive underneath the
                    // normal screen."
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onOpenImportPicker = { /* picker launched inline in PrivateGalleryScreen */ },
                onOpenPrivateSearch = { navController.navigate(Screen.PrivateSearch.route) },
                onOpenViewer = { index ->
                    navController.navigate(Screen.PrivateImageViewer.route("private_all", index))
                },
                onOpenPrivateAlbums = { navController.navigate(Screen.PrivateAlbumsList.route) },
                onOpenPrivateTrash = { navController.navigate(Screen.PrivateTrash.route) },
                onOpenPrivateSettings = { /* wired once Private Settings sub-screen (Section 35) is built */ }
            )
        }

        composable(Screen.PrivateTrash.route) {
            PrivateTrashScreen()
        }

        composable(Screen.PrivateAlbumsList.route) {
            PrivateAlbumsScreen(
                onBack = { navController.popBackStack() },
                onOpenAlbum = { albumId -> navController.navigate(Screen.PrivateAlbum.route(albumId)) }
            )
        }

        composable(
            route = Screen.PrivateAlbum.route,
            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
        ) {
            PrivateAlbumDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenViewer = { index -> navController.navigate(Screen.PrivateImageViewer.route("private_album", index)) }
            )
        }

        composable(Screen.PrivateSearch.route) {
            PrivateSearchScreen(
                onBack = { navController.popBackStack() },
                onOpenViewer = { index -> navController.navigate(Screen.PrivateImageViewer.route("private_search", index)) }
            )
        }

        composable(
            route = Screen.ImageViewer.route,
            arguments = listOf(
                navArgument("collectionKey") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.StringType }
            )
        ) {
            ImageViewerScreen(
                onBack = { navController.popBackStack() },
                onOpenVideoPlayer = { mediaId, isPrivate ->
                    navController.navigate(Screen.VideoPlayer.route(mediaId, isPrivate))
                },
                onOpenEditor = { mediaId -> navController.navigate(Screen.ImageEditor.route(mediaId, false)) },
                onOpenInfo = { mediaId, isPrivate -> navController.navigate(Screen.ImageInfo.route(mediaId, isPrivate)) },
                onShare = { /* wired once ShareExportManager UI hookup (Stage 4/5) lands */ },
                isPrivateContext = false
            )
        }

        composable(
            route = Screen.PrivateImageViewer.route,
            arguments = listOf(
                navArgument("collectionKey") { type = NavType.StringType },
                navArgument("startIndex") { type = NavType.StringType }
            )
        ) {
            ImageViewerScreen(
                onBack = { navController.popBackStack() },
                onOpenVideoPlayer = { mediaId, isPrivate ->
                    navController.navigate(Screen.VideoPlayer.route(mediaId, isPrivate))
                },
                onOpenEditor = { mediaId -> navController.navigate(Screen.ImageEditor.route(mediaId, true)) },
                onOpenInfo = { mediaId, isPrivate -> navController.navigate(Screen.ImageInfo.route(mediaId, isPrivate)) },
                onShare = { /* wired once ShareExportManager UI hookup (Stage 4/5) lands */ },
                isPrivateContext = true
            )
        }

        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType },
                navArgument("isPrivate") { type = NavType.StringType }
            )
        ) {
            VideoPlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.ImageEditor.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType },
                navArgument("isPrivate") { type = NavType.StringType }
            )
        ) {
            EditorScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImageInfo.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType },
                navArgument("isPrivate") { type = NavType.StringType }
            )
        ) {
            ImageInfoScreen(onBack = { navController.popBackStack() })
        }
    }
}
