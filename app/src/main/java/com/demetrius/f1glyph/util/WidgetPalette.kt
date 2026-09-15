package com.demetrius.f1glyph.util

/**
 * Colors for the bitmap-rendered widget parts. XML views get theirs from
 * values/values-night resources; bitmap panels are rendered in both palettes
 * so the launcher can select the current theme without an app refresh.
 * Must stay in sync with colors.xml.
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
    }
}
