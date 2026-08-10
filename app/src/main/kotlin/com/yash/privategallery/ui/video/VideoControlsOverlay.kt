package com.yash.privategallery.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

/**
 * Section 25's full control set built as a custom overlay: play/pause, seek
 * (via [Slider]), forward/backward 10s, playback speed menu, fullscreen
 * toggle, and left/right vertical swipe zones for brightness/volume — a
 * common video-player UX pattern (left half = brightness, right half =
 * volume) implemented here as thin invisible gesture layers so they don't
 * interfere with the visible controls stacked on top.
 */
@Composable
fun VideoControlsOverlay(
    isVisible: Boolean,
    playerState: PlayerUiState,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    onBrightnessChange: (delta: Float) -> Unit,
    onVolumeChange: (delta: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var speedMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Gesture zones — always active regardless of control visibility, so
        // brightness/volume can be adjusted even with controls hidden, same
        // as most video players.
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            onBrightnessChange(-dragAmount / size.height)
                            change.consume()
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            onVolumeChange(-dragAmount / size.height)
                            change.consume()
                        }
                    }
            )
        }

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) {
                // Center transport controls.
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSeekRelative(-10_000L) }) {
                        Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10 seconds", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(24.dp))
                    IconButton(onClick = { onSeekRelative(10_000L) }) {
                        Icon(Icons.Filled.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White)
                    }
                }

                // Bottom bar: seek slider, timeline, speed, fullscreen.
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatTime(playerState.currentPositionMs), color = Color.White)
                        Slider(
                            value = playerState.currentPositionMs.toFloat(),
                            valueRange = 0f..playerState.durationMs.coerceAtLeast(1L).toFloat(),
                            onValueChange = { onSeekTo(it.toLong()) },
                            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White),
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        Text(formatTime(playerState.durationMs), color = Color.White)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                    ) {
                        Box {
                            IconButton(onClick = { speedMenuExpanded = true }) {
                                Icon(Icons.Filled.Speed, contentDescription = "Playback speed", tint = Color.White)
                            }
                            DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                                SPEED_OPTIONS.forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = { onSetSpeed(speed); speedMenuExpanded = false }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onToggleFullscreen) {
                            Icon(Icons.Filled.Fullscreen, contentDescription = "Fullscreen", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
