package com.yash.privategallery.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yash.privategallery.domain.model.MediaItem
import com.yash.privategallery.domain.model.MediaType
import java.util.concurrent.TimeUnit

/**
 * A single grid cell in the media grid (Section 6, 9). Handles both images
 * and videos (thumbnail + play icon + duration badge per Section 1), an
 * optional favorite indicator (Section 14), and multi-select checkmark
 * overlay (Section 29). Tap vs. long-press are both exposed so callers
 * implement "tap opens viewer" / "long-press enters selection mode"
 * (Section 29) without this composable knowing which screen it's in.
 */
@Composable
fun MediaThumbnail(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    showFavoriteIcon: Boolean,
    showDuration: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = item.contentUri ?: item.filePath,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(120.dp) // caller overrides actual size via modifier constraints in practice
        )

        if (item.mediaType == MediaType.VIDEO) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp)
            )
            if (showDuration && item.durationMs != null) {
                Text(
                    text = formatDuration(item.durationMs),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        if (showFavoriteIcon && item.isFavorite) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Favorite",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }

        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = if (isSelected) "Selected" else "Not selected",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
