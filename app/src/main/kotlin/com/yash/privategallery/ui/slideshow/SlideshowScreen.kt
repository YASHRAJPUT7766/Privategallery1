package com.yash.privategallery.ui.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

/**
 * Section 34: full-screen slideshow over the current collection. Interval
 * is one of com.yash.privategallery.domain.model.SlideshowInterval (3/5/10s),
 * sourced from the user's default in Settings unless overridden per-launch.
 * Never exposes private content outside an authenticated Private Gallery
 * context — enforced the same way as the viewer/video player:
 * [isPrivateContext] gates SecureScreenEffect.
 */
@Composable
fun SlideshowScreen(
    onExit: () -> Unit,
    isPrivateContext: Boolean,
    viewModel: SlideshowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (isPrivateContext) {
        com.yash.privategallery.ui.common.SecureScreenEffect()
    }

    LaunchedEffect(uiState.isPlaying, uiState.currentIndex, uiState.intervalSeconds) {
        if (uiState.isPlaying && uiState.items.isNotEmpty()) {
            kotlinx.coroutines.delay(uiState.intervalSeconds * 1000L)
            viewModel.next()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        uiState.currentItem?.let { item ->
            AsyncImage(
                model = item.contentUri ?: item.filePath,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { viewModel.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
            }
            IconButton(onClick = { viewModel.togglePlayPause() }) {
                Icon(
                    if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                    tint = Color.White
                )
            }
            IconButton(onClick = { viewModel.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
            }
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = "Exit slideshow", tint = Color.White)
            }
        }
    }
}
