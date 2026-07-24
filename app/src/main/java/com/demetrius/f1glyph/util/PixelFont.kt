package com.demetrius.f1glyph.util

/**
 * Hand-built 3x5 dot font for the Glyph Matrix face. The 4a Pro matrix is
 * only 13x13, so downsampled vector text turns to mush — crisp pixel glyphs
 * are the only thing that stays readable at this size.
 */
object PixelFont {

    const val GLYPH_W = 3
    const val GLYPH_H = 5
    const val TRACKING = 1

    /** Width in cells of [text] rendered in this font. */
    fun widthOf(text: String): Int =
        if (text.isEmpty()) 0 else text.length * GLYPH_W + (text.length - 1) * TRACKING

    /**
     * Stamps [text] into [grid] (brightness 1f), centered horizontally, with
     * its top edge at [topRow]. Cells outside the grid are dropped silently.
     */
    fun stamp(grid: Array<FloatArray>, text: String, topRow: Int) {
        val cols = grid.firstOrNull()?.size ?: return
        var x = ((cols - widthOf(text)) / 2).coerceAtLeast(0)
        for (ch in text.uppercase()) {
            val glyph = glyphs[ch] ?: glyphs.getValue(' ')
            for (dy in 0 until GLYPH_H) {
                val y = topRow + dy
                if (y !in grid.indices) continue
                for (dx in 0 until GLYPH_W) {
                    if (glyph[dy][dx] == '#' && x + dx in 0 until cols) {
                        grid[y][x + dx] = 1f
                    }
                }
            }
            x += GLYPH_W + TRACKING
        }
    }

    /**
     * Like [stamp] but scrolled: text starts at x = -scrollOffset so it slides
     * left as scrollOffset grows. Pixels outside [0, grid width) are silently
     * dropped, enabling a simple marquee.
     */
    fun stampScrolled(grid: Array<FloatArray>, text: String, topRow: Int, scrollOffset: Int) {
        val cols = grid.firstOrNull()?.size ?: return
        var x = -scrollOffset
        for (ch in text.uppercase()) {
            if (x >= cols) break
            val glyph = glyphs[ch] ?: glyphs.getValue(' ')
            for (dy in 0 until GLYPH_H) {
                val y = topRow + dy
                if (y !in grid.indices) continue
                for (dx in 0 until GLYPH_W) {
                    val col = x + dx
                    if (col in 0 until cols && glyph[dy][dx] == '#') {
                        grid[y][col] = 1f
                    }
                }
            }
            x += GLYPH_W + TRACKING
        }
    }

    private val glyphs: Map<Char, List<String>> = mapOf(
        'A' to listOf(".#.", "#.#", "###", "#.#", "#.#"),
        'B' to listOf("##.", "#.#", "##.", "#.#", "##."),
        'C' to listOf(".##", "#..", "#..", "#..", ".##"),
        'D' to listOf("##.", "#.#", "#.#", "#.#", "##."),
        'E' to listOf("###", "#..", "##.", "#..", "###"),
        'F' to listOf("###", "#..", "##.", "#..", "#.."),
        'G' to listOf(".##", "#..", "#.#", "#.#", ".##"),
        'H' to listOf("#.#", "#.#", "###", "#.#", "#.#"),
        'I' to listOf("###", ".#.", ".#.", ".#.", "###"),
        'J' to listOf("..#", "..#", "..#", "#.#", ".#."),
        'K' to listOf("#.#", "#.#", "##.", "#.#", "#.#"),
        'L' to listOf("#..", "#..", "#..", "#..", "###"),
        'M' to listOf("#.#", "###", "###", "#.#", "#.#"),
        'N' to listOf("##.", "#.#", "#.#", "#.#", "#.#"),
        'O' to listOf(".#.", "#.#", "#.#", "#.#", ".#."),
        'P' to listOf("##.", "#.#", "##.", "#..", "#.."),
        'Q' to listOf(".#.", "#.#", "#.#", ".#.", "..#"),
        'R' to listOf("##.", "#.#", "##.", "#.#", "#.#"),
        'S' to listOf(".##", "#..", ".#.", "..#", "##."),
        'T' to listOf("###", ".#.", ".#.", ".#.", ".#."),
        'U' to listOf("#.#", "#.#", "#.#", "#.#", "###"),
        'V' to listOf("#.#", "#.#", "#.#", "#.#", ".#."),
        'W' to listOf("#.#", "#.#", "###", "###", "#.#"),
        'X' to listOf("#.#", "#.#", ".#.", "#.#", "#.#"),
        'Y' to listOf("#.#", "#.#", ".#.", ".#.", ".#."),
        'Z' to listOf("###", "..#", ".#.", "#..", "###"),
        '0' to listOf(".#.", "#.#", "#.#", "#.#", ".#."),
        '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
        '2' to listOf("##.", "..#", ".#.", "#..", "###"),
        '3' to listOf("##.", "..#", ".#.", "..#", "##."),
        '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
        '5' to listOf("###", "#..", "##.", "..#", "##."),
        '6' to listOf(".##", "#..", "##.", "#.#", ".#."),
        '7' to listOf("###", "..#", "..#", ".#.", ".#."),
        '8' to listOf(".#.", "#.#", ".#.", "#.#", ".#."),
        '9' to listOf(".#.", "#.#", ".##", "..#", "##."),
        '-' to listOf("...", "...", "###", "...", "..."),
        ' ' to listOf("...", "...", "...", "...", "...")
    )
}
