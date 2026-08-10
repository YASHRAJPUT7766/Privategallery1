package com.yash.privategallery.domain.model

/**
 * A single reversible edit operation (Section 11). The editor keeps an
 * ordered list of applied operations rather than mutating a bitmap in place
 * step-by-step and discarding history, so "support multiple undo/redo
 * operations" (Section 11) is just popping/pushing this list — the actual
 * pixel result is always re-derived by replaying the list from the original
 * decoded bitmap, which also means undo is exact (no accumulated
 * re-compression artifacts from repeated apply/undo cycles).
 */
sealed class EditOperation {
    data class Crop(val leftFraction: Float, val topFraction: Float, val rightFraction: Float, val bottomFraction: Float) : EditOperation()
    data class Rotate(val degrees: Float) : EditOperation()
    data object FlipHorizontal : EditOperation()
    data object FlipVertical : EditOperation()
    data class Straighten(val degrees: Float) : EditOperation()
    data class Resize(val widthPx: Int, val heightPx: Int) : EditOperation()

    data class Adjust(
        val brightness: Float = 0f,
        val contrast: Float = 0f,
        val saturation: Float = 0f,
        val exposure: Float = 0f,
        val highlights: Float = 0f,
        val shadows: Float = 0f,
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val sharpness: Float = 0f,
        val clarity: Float = 0f,
        val fade: Float = 0f,
        val vignette: Float = 0f,
        val grain: Float = 0f
    ) : EditOperation()

    data class ApplyFilter(val filterId: String) : EditOperation()

    sealed class Markup : EditOperation() {
        data class PenStroke(val points: List<Pair<Float, Float>>, val colorArgb: Int, val strokeWidth: Float) : Markup()
        data class Highlight(val points: List<Pair<Float, Float>>, val colorArgb: Int, val strokeWidth: Float) : Markup()
        data class TextAnnotation(val text: String, val xFraction: Float, val yFraction: Float, val colorArgb: Int, val fontSizeSp: Float) : Markup()
        data class Shape(val type: ShapeType, val startX: Float, val startY: Float, val endX: Float, val endY: Float, val colorArgb: Int, val strokeWidth: Float) : Markup()
        data class Arrow(val startX: Float, val startY: Float, val endX: Float, val endY: Float, val colorArgb: Int, val strokeWidth: Float) : Markup()
    }
}

enum class ShapeType { RECTANGLE, OVAL, LINE }

/** Section 11's fixed crop ratio presets. */
enum class CropRatio(val label: String, val widthRatio: Float?, val heightRatio: Float?) {
    ORIGINAL("Original", null, null),
    FREE("Free", null, null),
    SQUARE("1:1", 1f, 1f),
    FOUR_THREE("4:3", 4f, 3f),
    THREE_FOUR("3:4", 3f, 4f),
    SIXTEEN_NINE("16:9", 16f, 9f),
    NINE_SIXTEEN("9:16", 9f, 16f)
}

/** A built-in filter preset (Section 11's "Filters" section). */
data class FilterPreset(
    val id: String,
    val displayName: String
)

/** Section 12: how the user chooses to save an edited image. */
enum class SaveMode { REPLACE_ORIGINAL, SAVE_AS_NEW }
