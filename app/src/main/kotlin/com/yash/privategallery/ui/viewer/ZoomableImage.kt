package com.yash.privategallery.ui.viewer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen zoomable image (Section 9): pinch to zoom, double-tap to
 * zoom in/out, single tap toggles the toolbar (delegated to the caller via
 * [onTap]), and a vertical drag past [dismissThresholdPx] while unzoomed
 * triggers [onSwipeDownToClose] — the classic "swipe down to close" viewer
 * gesture. Panning is clamped so the image can't be dragged fully off-screen
 * once zoomed, and zoom/pan both reset when the displayed [model] changes
 * (i.e. when the pager moves to a new item) so state never leaks between
 * photos.
 */
@Composable
fun ZoomableImage(
    model: Any?,
    contentDescription: String?,
    onTap: () -> Unit,
    onSwipeDownToClose: () -> Unit,
    modifier: Modifier = Modifier,
    dismissThresholdPx: Float = 300f
) {
    var scale by remember(model) { mutableFloatStateOf(1f) }
    var offsetX by remember(model) { mutableFloatStateOf(0f) }
    var offsetY by remember(model) { mutableFloatStateOf(0f) }
    var dragAccumulator by remember(model) { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(model) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 6f)
                    scale = newScale
                    if (newScale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(model) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(model) {
                // Swipe-to-close only engages when not zoomed in — otherwise a
                // pan-to-inspect-a-zoomed-photo gesture would accidentally
                // dismiss the viewer.
                if (scale <= 1f) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount > 0) {
                                dragAccumulator += dragAmount
                                offsetY = dragAccumulator
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (dragAccumulator > dismissThresholdPx) {
                                onSwipeDownToClose()
                            } else {
                                offsetY = 0f
                            }
                            dragAccumulator = 0f
                        }
                    )
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

/** Clamps a candidate offset so a zoomed image never pans fully off-screen. */
internal fun clampOffset(offset: Float, scale: Float, containerSize: Float): Float {
    val maxOffset = max(0f, (scale - 1f) * containerSize / 2f)
    return min(max(offset, -maxOffset), maxOffset)
}
