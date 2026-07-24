package com.demetrius.f1glyph.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPanelGeometryTest {

    @Test
    fun `aspect stays within sane bounds`() {
        val typical2x2 = WidgetPanelGeometry.standingsPanelAspect(minWidthDp = 155, maxHeightDp = 180)
        assertTrue("got $typical2x2", typical2x2 in 1f..5f)
    }

    @Test
    fun `wider widget yields wider panel`() {
        val narrow = WidgetPanelGeometry.standingsPanelAspect(minWidthDp = 155, maxHeightDp = 180)
        val wide = WidgetPanelGeometry.standingsPanelAspect(minWidthDp = 300, maxHeightDp = 180)
        assertTrue(wide > narrow)
    }

    @Test
    fun `degenerate sizes clamp instead of exploding`() {
        assertEquals(5f, WidgetPanelGeometry.standingsPanelAspect(minWidthDp = 800, maxHeightDp = 120))
        assertEquals(1f, WidgetPanelGeometry.standingsPanelAspect(minWidthDp = 0, maxHeightDp = 2000))
    }

    @Test
    fun `wide pane aspect is roughly half the widget width over its height`() {
        // 4x2-ish widget: pane gets ~half the width, full height
        val aspect = WidgetPanelGeometry.widePanelAspect(minWidthDp = 320, maxHeightDp = 180)
        assertTrue("got $aspect", aspect in 0.8f..2f)
    }

    @Test
    fun `wide pane aspect clamps on degenerate sizes`() {
        assertEquals(0.8f, WidgetPanelGeometry.widePanelAspect(minWidthDp = 0, maxHeightDp = 2000))
        assertEquals(5f, WidgetPanelGeometry.widePanelAspect(minWidthDp = 4000, maxHeightDp = 100))
    }
}
