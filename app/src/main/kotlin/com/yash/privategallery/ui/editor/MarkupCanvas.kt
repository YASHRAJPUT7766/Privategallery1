package com.yash.privategallery.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import com.yash.privategallery.domain.model.EditOperation
import com.yash.privategallery.domain.model.ShapeType

enum class MarkupTool { PEN, HIGHLIGHT, TEXT, SHAPE, ARROW }

/** Section 11's Draw/Markup toolbar: Pen, Highlight, Text, Shapes, Arrow. */
@Composable
fun MarkupToolbar(
    selectedTool: MarkupTool?,
    onToolSelected: (MarkupTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        MarkupToolButton(Icons.Filled.Edit, "Pen", selectedTool == MarkupTool.PEN) { onToolSelected(MarkupTool.PEN) }
        MarkupToolButton(Icons.Filled.Crop169, "Highlight", selectedTool == MarkupTool.HIGHLIGHT) { onToolSelected(MarkupTool.HIGHLIGHT) }
        MarkupToolButton(Icons.Filled.TextFields, "Text", selectedTool == MarkupTool.TEXT) { onToolSelected(MarkupTool.TEXT) }
        MarkupToolButton(Icons.Filled.PanoramaFishEye, "Shape", selectedTool == MarkupTool.SHAPE) { onToolSelected(MarkupTool.SHAPE) }
        MarkupToolButton(Icons.Filled.ArrowForward, "Arrow", selectedTool == MarkupTool.ARROW) { onToolSelected(MarkupTool.ARROW) }
    }
}

@Composable
private fun MarkupToolButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * The actual drawing surface — captures drag gestures and emits a finished
 * EditOperation.Markup once a stroke/shape is completed (pointer up), which
 * the caller pushes via EditorViewModel.applyOperation for a real undo
 * checkpoint per gesture (Section 11's per-stroke undo granularity).
 * Text markup is placed via tap + a separate text-entry dialog rather than
 * drag, since a drag gesture doesn't map naturally onto placing a label —
 * handled by the caller (EditorScreen), not this composable.
 */
@Composable
fun MarkupCanvas(
    activeTool: MarkupTool,
    color: Color,
    strokeWidth: Float,
    onStrokeComplete: (EditOperation.Markup) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPoints by remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
    var shapeStart by remember { mutableStateOf(0f to 0f) }
    var shapeEnd by remember { mutableStateOf(0f to 0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(activeTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPoints = listOf(offset.x to offset.y)
                        shapeStart = offset.x to offset.y
                        shapeEnd = offset.x to offset.y
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPoints = currentPoints + (change.position.x to change.position.y)
                        shapeEnd = change.position.x to change.position.y
                    },
                    onDragEnd = {
                        val colorArgb = color.toArgb()
                        val operation: EditOperation.Markup? = when (activeTool) {
                            MarkupTool.PEN -> EditOperation.Markup.PenStroke(currentPoints, colorArgb, strokeWidth)
                            MarkupTool.HIGHLIGHT -> EditOperation.Markup.Highlight(currentPoints, colorArgb, strokeWidth * 3)
                            MarkupTool.SHAPE -> EditOperation.Markup.Shape(
                                ShapeType.RECTANGLE, shapeStart.first, shapeStart.second, shapeEnd.first, shapeEnd.second, colorArgb, strokeWidth
                            )
                            MarkupTool.ARROW -> EditOperation.Markup.Arrow(
                                shapeStart.first, shapeStart.second, shapeEnd.first, shapeEnd.second, colorArgb, strokeWidth
                            )
                            MarkupTool.TEXT -> null
                        }
                        operation?.let { onStrokeComplete(it) }
                        currentPoints = emptyList()
                    }
                )
            }
    ) {
        if (currentPoints.size > 1) {
            val path = Path().apply {
                moveTo(currentPoints[0].first, currentPoints[0].second)
                currentPoints.drop(1).forEach { lineTo(it.first, it.second) }
            }
            drawPath(path = path, color = color, style = Stroke(width = strokeWidth))
        }
    }
}
