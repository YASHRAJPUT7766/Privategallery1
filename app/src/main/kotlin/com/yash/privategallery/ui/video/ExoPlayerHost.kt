package com.yash.privategallery.ui.video

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Snapshot of player state exposed to Compose for building custom controls (Section 25). */
data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val isBuffering: Boolean = false
)

/**
 * Hosts a Media3 ExoPlayer instance for one media source, wired into the
 * Compose/Activity lifecycle: pauses on lifecycle STOP, releases on
 * DisposableEffect exit (Section 25: "When leaving the private video player,
 * private content must be protected again according to the lock policy" —
 * releasing playback here ensures nothing keeps rendering in the
 * background once the screen is left, which the lock-on-background policy
 * in core/security builds on).
 *
 * Renders as a [PlayerView] with useController = false — this app builds its
 * own custom control overlay (play/pause, seek, speed, gestures) in
 * [VideoPlayerScreen] rather than using ExoPlayer's default chrome, to match
 * Section 37's "real production" custom UI requirement rather than Media3's
 * generic default skin.
 */
@Composable
fun ExoPlayerHost(
    mediaUri: String,
    modifier: Modifier = Modifier,
    onStateChanged: (PlayerUiState) -> Unit,
    controlRequests: PlayerControlRequests
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val exoPlayer = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(Uri.parse(mediaUri)))
            prepare()
            playWhenReady = true
        }
    }

    // Surface player state changes back to Compose so the custom control
    // overlay (seek bar position, play/pause icon, duration label) stays in
    // sync without polling.
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onStateChanged(currentSnapshot(exoPlayer, isPlaying))
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                onStateChanged(currentSnapshot(exoPlayer, exoPlayer.isPlaying))
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Lifecycle-aware pause/release (Section 25/45: protect content when
    // backgrounded; never leak a playing surface past the screen's lifetime).
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // Apply imperative control requests (play/pause/seek/speed) issued from
    // the custom overlay's button/gesture handlers.
    LaunchedEffect(controlRequests.version) {
        when (val action = controlRequests.pendingAction) {
            is PlayerAction.PlayPause -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            is PlayerAction.SeekTo -> exoPlayer.seekTo(action.positionMs)
            is PlayerAction.SetSpeed -> exoPlayer.setPlaybackSpeed(action.speed)
            is PlayerAction.SeekRelative -> exoPlayer.seekTo((exoPlayer.currentPosition + action.deltaMs).coerceAtLeast(0))
            null -> Unit
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = false
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
        }
    )
}

private fun currentSnapshot(player: ExoPlayer, isPlaying: Boolean) = PlayerUiState(
    isPlaying = isPlaying,
    currentPositionMs = player.currentPosition,
    durationMs = player.duration.coerceAtLeast(0),
    playbackSpeed = player.playbackParameters.speed,
    isBuffering = player.playbackState == Player.STATE_BUFFERING
)

sealed class PlayerAction {
    data object PlayPause : PlayerAction()
    data class SeekTo(val positionMs: Long) : PlayerAction()
    data class SeekRelative(val deltaMs: Long) : PlayerAction()
    data class SetSpeed(val speed: Float) : PlayerAction()
}

/**
 * A tiny "command bus" so imperative player actions (from button clicks/
 * gestures in the custom overlay) can be issued into the composable above
 * without exposing the raw ExoPlayer instance to the rest of the UI layer.
 * [version] increments on every request so LaunchedEffect(controlRequests.version)
 * re-fires even for repeated identical actions (e.g. tapping play/pause
 * rapidly).
 */
class PlayerControlRequests {
    var pendingAction: PlayerAction? = null
        private set
    var version by mutableStateOf(0)
        private set

    fun request(action: PlayerAction) {
        pendingAction = action
        version++
    }
}
