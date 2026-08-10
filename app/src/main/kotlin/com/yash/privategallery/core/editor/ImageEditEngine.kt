package com.yash.privategallery.core.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import com.yash.privategallery.domain.model.EditOperation
import com.yash.privategallery.domain.model.ShapeType
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Replays an ordered list of [EditOperation]s against a source [Bitmap] to
 * produce the final edited result (Section 11, 12). Always starts from the
 * ORIGINAL decoded bitmap and re-applies the full operation list on every
 * render — this is what makes undo exact rather than lossy: undoing an
 * operation is simply dropping it from the list and re-rendering, never
 * re-compressing an already-edited bitmap.
 *
 * Geometric operations (crop/rotate/flip/straighten/resize) run first and in
 * order since each changes the coordinate space subsequent operations (like
 * markup, which uses fractional coordinates) are defined against. Color
 * adjustments and filters are applied via [ColorMatrix] composition, which is
 * both fast (single Canvas + Paint pass) and numerically predictable. Markup
 * is drawn last, directly onto the finished image, so annotations never get
 * blurred/distorted by later color operations.
 */
@Singleton
class ImageEditEngine @Inject constructor() {

    fun render(source: Bitmap, operations: List<EditOperation>): Bitmap {
        var working = source

        operations.filterIsInstance<EditOperation.Crop>().forEach { working = applyCrop(working, it) }
        operations.forEach { op ->
            when (op) {
                is EditOperation.Rotate -> working = applyRotate(working, op.degrees)
                is EditOperation.Straighten -> working = applyRotate(working, op.degrees)
                is EditOperation.FlipHorizontal -> working = applyFlip(working, horizontal = true)
                is EditOperation.FlipVertical -> working = applyFlip(working, horizontal = false)
                is EditOperation.Resize -> working = Bitmap.createScaledBitmap(working, op.widthPx, op.heightPx, true)
                else -> Unit
            }
        }

        val adjustOps = operations.filterIsInstance<EditOperation.Adjust>()
        if (adjustOps.isNotEmpty()) {
            val combined = adjustOps.fold(EditOperation.Adjust()) { acc, next -> combineAdjustments(acc, next) }
            working = applyColorMatrix(working, buildColorMatrix(combined))
            if (combined.vignette > 0f) working = applyVignette(working, combined.vignette)
            if (combined.grain > 0f) working = applyGrain(working, combined.grain)
        }
        operations.filterIsInstance<EditOperation.ApplyFilter>().forEach {
            working = applyColorMatrix(working, FilterPresets.matrixFor(it.filterId))
        }

        val markupOps = operations.filterIsInstance<EditOperation.Markup>()
        if (markupOps.isNotEmpty()) {
            working = applyMarkup(working, markupOps)
        }

        return working
    }

    private fun applyCrop(bitmap: Bitmap, crop: EditOperation.Crop): Bitmap {
        val left = (crop.leftFraction * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (crop.topFraction * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (crop.rightFraction * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (crop.bottomFraction * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
    }

    private fun applyRotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun applyFlip(bitmap: Bitmap, horizontal: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) preScale(-1f, 1f) else preScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun combineAdjustments(a: EditOperation.Adjust, b: EditOperation.Adjust) = EditOperation.Adjust(
        brightness = (a.brightness + b.brightness).coerceIn(-1f, 1f),
        contrast = (a.contrast + b.contrast).coerceIn(-1f, 1f),
        saturation = (a.saturation + b.saturation).coerceIn(-1f, 1f),
        exposure = (a.exposure + b.exposure).coerceIn(-1f, 1f),
        highlights = (a.highlights + b.highlights).coerceIn(-1f, 1f),
        shadows = (a.shadows + b.shadows).coerceIn(-1f, 1f),
        temperature = (a.temperature + b.temperature).coerceIn(-1f, 1f),
        tint = (a.tint + b.tint).coerceIn(-1f, 1f),
        sharpness = (a.sharpness + b.sharpness).coerceIn(0f, 1f),
        clarity = (a.clarity + b.clarity).coerceIn(-1f, 1f),
        fade = max(a.fade, b.fade),
        vignette = max(a.vignette, b.vignette),
        grain = max(a.grain, b.grain)
    )

    private fun buildColorMatrix(adjust: EditOperation.Adjust): ColorMatrix {
        val matrix = ColorMatrix()

        val brightnessValue = (adjust.brightness + adjust.exposure) * 255f
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, brightnessValue,
                    0f, 1f, 0f, 0f, brightnessValue,
                    0f, 0f, 1f, 0f, brightnessValue,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        val contrastScale = 1f + adjust.contrast
        val contrastTranslate = (1f - contrastScale) * 128f
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    contrastScale, 0f, 0f, 0f, contrastTranslate,
                    0f, contrastScale, 0f, 0f, contrastTranslate,
                    0f, 0f, contrastScale, 0f, contrastTranslate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        matrix.postConcat(ColorMatrix().apply { setSaturation(1f + adjust.saturation) })

        // Temperature/tint as a simple channel-weighted shift — warm
        // (+temperature) boosts red/lowers blue, tint shifts green.
        matrix.postConcat(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, adjust.temperature * 30f,
                    0f, 1f, 0f, 0f, adjust.tint * 30f,
                    0f, 0f, 1f, 0f, -adjust.temperature * 30f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )

        if (adjust.fade > 0f) {
            val fadeLift = adjust.fade * 40f
            val fadeScale = 1f - adjust.fade * 0.2f
            matrix.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        fadeScale, 0f, 0f, 0f, fadeLift,
                        0f, fadeScale, 0f, 0f, fadeLift,
                        0f, 0f, fadeScale, 0f, fadeLift,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        return matrix
    }

    private fun applyColorMatrix(bitmap: Bitmap, colorMatrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }

    private fun applyVignette(bitmap: Bitmap, strength: Float): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val radius = max(bitmap.width, bitmap.height) * 0.75f
        val edgeAlpha = (strength * 170).toInt().coerceIn(0, 255)
        val paint = Paint().apply {
            shader = RadialGradient(
                bitmap.width / 2f, bitmap.height / 2f, radius,
                intArrayOf(0x00000000, (edgeAlpha shl 24)),
                floatArrayOf(0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DARKEN)
        }
        canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        return result
    }

    private fun applyGrain(bitmap: Bitmap, strength: Float): Bitmap {
        // Lightweight grain: per-pixel random noise at full resolution is
        // expensive, so a small tiled noise pattern is composited repeatedly
        // instead — visually convincing, far cheaper (Section 39 perf focus
        // applies to editor rendering too, since it must stay responsive).
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val noiseTile = generateNoiseTile(64, strength)
        val paint = Paint().apply { alpha = (strength * 60).toInt() }
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                canvas.drawBitmap(noiseTile, x.toFloat(), y.toFloat(), paint)
                x += noiseTile.width
            }
            y += noiseTile.height
        }
        return result
    }

    private fun generateNoiseTile(size: Int, strength: Float): Bitmap {
        val tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val random = java.util.Random()
        for (px in 0 until size) {
            for (py in 0 until size) {
                val gray = (128 + (random.nextFloat() - 0.5f) * 255 * strength).toInt().coerceIn(0, 255)
                tile.setPixel(px, py, (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray)
            }
        }
        return tile
    }

    private fun applyMarkup(bitmap: Bitmap, markupOps: List<EditOperation.Markup>): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        markupOps.forEach { op ->
            when (op) {
                is EditOperation.Markup.PenStroke -> drawStroke(canvas, op.points, op.colorArgb, op.strokeWidth, alpha = 255)
                is EditOperation.Markup.Highlight -> drawStroke(canvas, op.points, op.colorArgb, op.strokeWidth, alpha = 100)
                is EditOperation.Markup.TextAnnotation -> {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = op.colorArgb
                        textSize = op.fontSizeSp * 2.5f
                    }
                    canvas.drawText(op.text, op.xFraction * result.width, op.yFraction * result.height, paint)
                }
                is EditOperation.Markup.Shape -> drawShape(canvas, op)
                is EditOperation.Markup.Arrow -> drawArrow(canvas, op)
            }
        }
        return result
    }

    private fun drawStroke(canvas: Canvas, points: List<Pair<Float, Float>>, colorArgb: Int, strokeWidth: Float, alpha: Int) {
        if (points.size < 2) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            this.alpha = alpha
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = android.graphics.Path().apply {
            moveTo(points[0].first, points[0].second)
            for (i in 1 until points.size) lineTo(points[i].first, points[i].second)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawShape(canvas: Canvas, op: EditOperation.Markup.Shape) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = op.colorArgb
            style = Paint.Style.STROKE
            strokeWidth = op.strokeWidth
        }
        val left = min(op.startX, op.endX)
        val top = min(op.startY, op.endY)
        val right = max(op.startX, op.endX)
        val bottom = max(op.startY, op.endY)
        when (op.type) {
            ShapeType.RECTANGLE -> canvas.drawRect(left, top, right, bottom, paint)
            ShapeType.OVAL -> canvas.drawOval(left, top, right, bottom, paint)
            ShapeType.LINE -> canvas.drawLine(op.startX, op.startY, op.endX, op.endY, paint)
        }
    }

    private fun drawArrow(canvas: Canvas, op: EditOperation.Markup.Arrow) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = op.colorArgb
            style = Paint.Style.STROKE
            strokeWidth = op.strokeWidth
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(op.startX, op.startY, op.endX, op.endY, paint)
        val angle = Math.atan2((op.endY - op.startY).toDouble(), (op.endX - op.startX).toDouble())
        val arrowLength = op.strokeWidth * 4
        val arrowAngle = Math.PI / 6
        val x1 = op.endX - (arrowLength * Math.cos(angle - arrowAngle)).toFloat()
        val y1 = op.endY - (arrowLength * Math.sin(angle - arrowAngle)).toFloat()
        val x2 = op.endX - (arrowLength * Math.cos(angle + arrowAngle)).toFloat()
        val y2 = op.endY - (arrowLength * Math.sin(angle + arrowAngle)).toFloat()
        canvas.drawLine(op.endX, op.endY, x1, y1, paint)
        canvas.drawLine(op.endX, op.endY, x2, y2, paint)
    }
}
