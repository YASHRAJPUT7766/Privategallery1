package com.yash.privategallery.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Section 10's bottom toolbar. [isPrivateContext] swaps the "Move to
 * Private" action for "Move to Gallery" (Sections 21, 23), since a single
 * viewer instance is always scoped to one storage location (never mixed —
 * Section 24). The toolbar "should disappear automatically while viewing" —
 * that auto-hide-on-idle timer lives in the calling screen (which also
 * drives visibility on single-tap); this composable only renders the
 * current visibility state handed to it.
 */
@Composable
fun ViewerBottomToolbar(
    isVisible: Boolean,
    isFavorite: Boolean,
    isPrivateContext: Boolean,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onMove: () -> Unit,
    onAddToAlbum: () -> Unit,
    onTogglePrivate: () -> Unit,
    onInfo: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .navigationBarsPadding()
                .padding(vertical = 4.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarAction(Icons.Filled.Share, "Share", onShare)
            ToolbarAction(Icons.Filled.Edit, "Edit", onEdit)
            ToolbarAction(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                "Favorite",
                onToggleFavorite
            )
            ToolbarAction(Icons.Filled.Delete, "Delete", onDelete)
            ToolbarAction(
                icon = Icons.Filled.PhotoLibrary,
                label = if (isPrivateContext) "To Gallery" else "Move",
                onClick = if (isPrivateContext) onTogglePrivate else onMove
            )
            if (!isPrivateContext) {
                ToolbarAction(Icons.Filled.Lock, "Private", onTogglePrivate)
            }
            ToolbarAction(Icons.Filled.Info, "Info", onInfo)
            ToolbarAction(Icons.Filled.MoreVert, "More", onMore)
        }
    }
}

@Composable
private fun ToolbarAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}
