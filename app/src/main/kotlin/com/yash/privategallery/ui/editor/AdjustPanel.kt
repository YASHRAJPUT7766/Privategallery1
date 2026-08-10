package com.yash.privategallery.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yash.privategallery.domain.model.EditOperation

private data class AdjustSlider(
    val label: String,
    val range: ClosedFloatingPointRange<Float>,
    val getValue: (EditOperation.Adjust) -> Float,
    val withValue: (EditOperation.Adjust, Float) -> EditOperation.Adjust
)

private val SLIDERS = listOf(
    AdjustSlider("Brightness", -1f..1f, { it.brightness }, { a, v -> a.copy(brightness = v) }),
    AdjustSlider("Contrast", -1f..1f, { it.contrast }, { a, v -> a.copy(contrast = v) }),
    AdjustSlider("Saturation", -1f..1f, { it.saturation }, { a, v -> a.copy(saturation = v) }),
    AdjustSlider("Exposure", -1f..1f, { it.exposure }, { a, v -> a.copy(exposure = v) }),
    AdjustSlider("Highlights", -1f..1f, { it.highlights }, { a, v -> a.copy(highlights = v) }),
    AdjustSlider("Shadows", -1f..1f, { it.shadows }, { a, v -> a.copy(shadows = v) }),
    AdjustSlider("Temperature", -1f..1f, { it.temperature }, { a, v -> a.copy(temperature = v) }),
    AdjustSlider("Tint", -1f..1f, { it.tint }, { a, v -> a.copy(tint = v) }),
    AdjustSlider("Sharpness", 0f..1f, { it.sharpness }, { a, v -> a.copy(sharpness = v) }),
    AdjustSlider("Clarity", -1f..1f, { it.clarity }, { a, v -> a.copy(clarity = v) }),
    AdjustSlider("Fade", 0f..1f, { it.fade }, { a, v -> a.copy(fade = v) }),
    AdjustSlider("Vignette", 0f..1f, { it.vignette }, { a, v -> a.copy(vignette = v) }),
    AdjustSlider("Grain", 0f..1f, { it.grain }, { a, v -> a.copy(grain = v) })
)

/**
 * Section 11's Adjust panel — one slider per parameter. Dragging calls
 * [onPreview] continuously (cheap, non-undo-stack-polluting re-render via
 * EditorViewModel.previewOperation); releasing calls [onCommit] once to
 * create a real undo checkpoint.
 */
@Composable
fun AdjustPanel(
    currentAdjust: EditOperation.Adjust,
    onPreview: (EditOperation.Adjust) -> Unit,
    onCommit: (EditOperation.Adjust) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(horizontal = 16.dp)) {
        items(SLIDERS) { slider ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(slider.label)
                Slider(
                    value = slider.getValue(currentAdjust),
                    valueRange = slider.range,
                    onValueChange = { newValue -> onPreview(slider.withValue(currentAdjust, newValue)) },
                    onValueChangeFinished = { onCommit(currentAdjust) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
