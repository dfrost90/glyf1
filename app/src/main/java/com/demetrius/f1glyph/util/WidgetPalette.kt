package com.demetrius.f1glyph.util

import android.content.Context
import android.content.res.Configuration

/**
 * Colors for the bitmap-rendered widget parts. XML views get theirs from
 * values/values-night resources; bitmaps are drawn in-process, so the
 * palette is picked from the current UI mode at render time. Must stay in
 * sync with colors.xml.
 */
data class WidgetPalette(
    val primary: Int,
    val secondary: Int,
    val accent: Int,
    val yellow: Int,
    val dotDim: Int,
    val dotBright: Int
) {
    companion object {
        private const val NOTHING_YELLOW = 0xFFF5C400.toInt()

        val DARK = WidgetPalette(
            primary = 0xFFFFFFFF.toInt(),
            secondary = 0xFF8A8A8A.toInt(),
            accent = 0xFFFF1E1E.toInt(),
            yellow = NOTHING_YELLOW,
            dotDim = 0x46FFFFFF,
            dotBright = 0xEBFFFFFF.toInt()
        )
        val LIGHT = WidgetPalette(
            primary = 0xFF141414.toInt(),
            secondary = 0xFF6E6E6E.toInt(),
            accent = 0xFFD50000.toInt(),
            yellow = NOTHING_YELLOW,
            dotDim = 0x3C000000,
            dotBright = 0xE6141414.toInt()
        )

        fun of(context: Context): WidgetPalette {
            val night = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            return if (night) DARK else LIGHT
        }
    }
}
