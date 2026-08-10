package com.yash.privategallery.ui.video

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yash.privategallery.ui.common.SecureScreenEffect
import kotlinx.coroutines.launch

/**
 * Section 25/26: the dedicated video player. Swiping left/right moves
 * between videos in the current collection (Section 26), each page hosting
 * its own [ExoPlayerHost] instance so only the currently-visible page is
 * ever actually playing (off-screen pager pages are disposed by Compose's
 * default HorizontalPager beyond-bounds behavior, which naturally satisfies
 * "leaving the private video player" content-protection concerns per
 * Section 25 since a swiped-away private video's player is torn down, not
 * left running).
 *
 * Screen orientation for fullscreen landscape playback is handled by
 * directly setting the hosting Activity's requestedOrientation — the
 * simplest reliable approach for a single-purpose full-screen player screen.
 */
@Composable
fun VideoPlayerScreen(
    onBack: () -> Unit,
    viewModel: VideoPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    var isFullscreen by remember { mutableStateOf(false) }
    var playerState by remember { mutableStateOf(PlayerUiState()) }
    val controlRequests = remember { PlayerControlRequests() }

    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val maxVolume = remember { audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15 }

    if (uiState.isPrivate) {
        SecureScreenEffect()
    }

    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.videos.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onSwipeToIndex(pagerState.currentPage)
    }

    // Fullscreen landscape toggle (Section 25: "Fullscreen, Landscape, Portrait").
    DisposableEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (isFullscreen) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.videos.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = uiState.videos[page]
                val isCurrentPage = page == pagerState.currentPage

                Box(modifier = Modifier.fillMaxSize()) {
                    if (isCurrentPage) {
                        val mediaSource = video.contentUri ?: video.filePath ?: ""
                        ExoPlayerHost(
                            mediaUri = mediaSource,
                            modifier = Modifier.fillMaxSize(),
                            onStateChanged = { playerState = it },
                            controlRequests = controlRequests
                        )
                        VideoControlsOverlay(
                            isVisible = uiState.isControlsVisible,
                            playerState = playerState,
                            onPlayPause = { controlRequests.request(PlayerAction.PlayPause) },
                            onSeekTo = { controlRequests.request(PlayerAction.SeekTo(it)) },
                            onSeekRelative = { controlRequests.request(PlayerAction.SeekRelative(it)) },
                            onSetSpeed = { controlRequests.request(PlayerAction.SetSpeed(it)) },
                            onToggleFullscreen = { isFullscreen = !isFullscreen },
                            onBrightnessChange = { delta ->
                                activity?.window?.attributes = activity?.window?.attributes?.apply {
                                    screenBrightness = (screenBrightness.coerceAtLeast(0f) + delta).coerceIn(0.01f, 1f)
                                }
                            },
                            onVolumeChange = { delta ->
                                val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                val newVolume = (current + (delta * maxVolume)).toInt().coerceIn(0, maxVolume)
                                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // Single tap anywhere off the button/gesture zones toggles controls —
    // handled inside VideoControlsOverlay's gesture zones would conflict with
    // the drag detector, so tap-to-toggle is exposed via a lightweight
    // transparent tap catcher behind the gesture row instead when controls
    // are hidden, kept simple here by relying on the play/pause button and
    // system back for primary interaction, with a dedicated tap zone left as
    // a follow-up polish item once real-device gesture tuning is possible.
}
