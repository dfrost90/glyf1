package com.demetrius.f1glyph.util

/**
 * Estimates the aspect ratio of the compact widget's standings panel from the
 * widget cell size the launcher reports, so the bitmap can be rendered at the
 * panel's real shape and fill its full width under fitCenter scaling.
 */
object WidgetPanelGeometry {

    /** Must match the root padding in widget_compact.xml. */
    private const val PADDING_DP = 20

    /** Approximate height of the text block above the panel (two-line name + countdown + label). */
    private const val TEXT_BLOCK_DP = 100

    /** Rendered slightly wider than estimated so fitCenter snaps to full width. */
    private const val WIDTH_BIAS = 1.1f

    fun standingsPanelAspect(minWidthDp: Int, maxHeightDp: Int): Float {
        val w = (minWidthDp - 2 * PADDING_DP).coerceAtLeast(60)
        val h = (maxHeightDp - 2 * PADDING_DP - TEXT_BLOCK_DP).coerceAtLeast(40)
        return (w.toFloat() / h * WIDTH_BIAS).coerceIn(1f, 5f)
    }

    /**
     * The wide layout's right-hand pane: weight 1 of 2.1 total, minus the
     * 1dp divider and the 16dp marginStart on the right column.
     */
    fun widePanelAspect(minWidthDp: Int, maxHeightDp: Int): Float {
        val weighted = (minWidthDp - 2 * PADDING_DP - 1).toFloat()  // subtract padding + divider
        val w = (weighted / 2.1f - 16f).coerceAtLeast(40f)           // weight 1/2.1, minus marginStart
        val h = (maxHeightDp - 2 * PADDING_DP).coerceAtLeast(40).toFloat()
        return (w / h).coerceIn(0.5f, 5f)
    }
}
