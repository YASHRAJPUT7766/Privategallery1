package com.yash.privategallery.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yash.privategallery.domain.model.CropRatio

/**
 * Section 11's crop tool: a draggable rect over the image, with ratio preset
 * chips. Drag state is purely local UI state — it doesn't become a real
 * EditOperation.Crop until the caller commits it (an explicit crop change
 * callback fires on drag end / ratio selection), so repeatedly nudging the
 * crop box before committing doesn't spam the undo stack.
 *
 * Whole-box dragging is implemented; per-corner resize handles are flagged
 * as a natural follow-up rather than faked with non-functional handles.
 */
@Composable
fun CropOverlay(
    selectedRatio: CropRatio,
    onRatioSelected: (CropRatio) -> Unit,
    onCropChanged: (leftFraction: Float, topFraction: Float, rightFraction: Float, bottomFraction: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var cropRect by remember { mutableStateOf(Rect(0.1f, 0.1f, 0.9f, 0.9f)) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            onCropChanged(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x / size.width
                        val dy = dragAmount.y / size.height
                        val width = cropRect.right - cropRect.left
                        val height = cropRect.bottom - cropRect.top
                        val newLeft = (cropRect.left + dx).coerceIn(0f, 1f - width)
                        val newTop = (cropRect.top + dy).coerceIn(0f, 1f - height)
                        cropRect = Rect(newLeft, newTop, newLeft + width, newTop + height)
                    }
                }
        ) {
            val rectPx = Rect(
                cropRect.left * size.width,
                cropRect.top * size.height,
                cropRect.right * size.width,
                cropRect.bottom * size.height
            )
            drawRect(color = Color.Black.copy(alpha = 0.5f))
            drawRect(
                color = Color.Transparent,
                topLeft = rectPx.topLeft,
                size = Size(rectPx.width, rectPx.height),
                blendMode = BlendMode.Clear
            )
            drawRect(
                color = Color.White,
                topLeft = rectPx.topLeft,
                size = Size(rectPx.width, rectPx.height),
                style = Stroke(width = 2f)
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CropRatio.values().toList()) { ratio ->
                FilterChip(
                    selected = ratio == selectedRatio,
                    onClick = {
                        onRatioSelected(ratio)
                        if (ratio.widthRatio != null && ratio.heightRatio != null) {
                            val targetAspect = ratio.widthRatio / ratio.heightRatio
                            cropRect = fitRatioToCenter(targetAspect)
                            onCropChanged(cropRect.left, cropRect.top, cropRect.right, cropRect.bottom)
                        }
                    },
                    label = { Text(ratio.label) }
                )
            }
        }
    }
}

private fun fitRatioToCenter(targetAspect: Float): Rect {
    return if (targetAspect >= 1f) {
        val height = 1f / targetAspect
        val top = (1f - height) / 2f
        Rect(0f, top, 1f, top + height)
    } else {
        val width = targetAspect
        val left = (1f - width) / 2f
        Rect(left, 0f, left + width, 1f)
    }
}
