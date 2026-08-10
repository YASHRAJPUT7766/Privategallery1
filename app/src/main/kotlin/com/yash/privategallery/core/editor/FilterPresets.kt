package com.yash.privategallery.core.editor

import android.graphics.ColorMatrix
import com.yash.privategallery.domain.model.FilterPreset

/**
 * Section 11: "Include a set of built-in filters." Each filter is a single
 * [ColorMatrix], the same efficient mechanism used for the manual Adjust
 * sliders — this keeps filters and manual adjustments composable (a filter
 * plus manual tweaks on top both stay single-pass Canvas operations).
 */
object FilterPresets {

    val available: List<FilterPreset> = listOf(
        FilterPreset("none", "Original"),
        FilterPreset("mono", "Mono"),
        FilterPreset("noir", "Noir"),
        FilterPreset("vivid", "Vivid"),
        FilterPreset("warm", "Warm"),
        FilterPreset("cool", "Cool"),
        FilterPreset("vintage", "Vintage"),
        FilterPreset("fade_classic", "Fade")
    )

    fun matrixFor(filterId: String): ColorMatrix = when (filterId) {
        "mono" -> ColorMatrix().apply { setSaturation(0f) }
        "noir" -> ColorMatrix().apply {
            setSaturation(0f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, -20f,
                        0f, 1.2f, 0f, 0f, -20f,
                        0f, 0f, 1.2f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        "vivid" -> ColorMatrix().apply { setSaturation(1.4f) }
        "warm" -> ColorMatrix(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, 15f,
                0f, 1.0f, 0f, 0f, 5f,
                0f, 0f, 0.9f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        "cool" -> ColorMatrix(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, -10f,
                0f, 1.0f, 0f, 0f, 0f,
                0f, 0f, 1.1f, 0f, 15f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        "vintage" -> ColorMatrix().apply {
            setSaturation(0.7f)
            postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.05f, 0.05f, 0f, 0f, 10f,
                        0f, 1.0f, 0f, 0f, 5f,
                        0f, 0.05f, 0.85f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        "fade_classic" -> ColorMatrix(
            floatArrayOf(
                0.85f, 0f, 0f, 0f, 35f,
                0f, 0.85f, 0f, 0f, 35f,
                0f, 0f, 0.85f, 0f, 35f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        else -> ColorMatrix() // "none" / unrecognized id → identity matrix, no-op
    }
}
