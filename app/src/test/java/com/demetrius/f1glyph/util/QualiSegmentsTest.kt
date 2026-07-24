package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualiSegmentsTest {

    private val minute = 60 * 1000L

    @Test
    fun `quali starts in Q1`() {
        assertEquals(
            QualiSegments.Segment("Q1", 0.5f),
            QualiSegments.at(SessionKind.QUALIFYING, 9 * minute)
        )
    }

    @Test
    fun `break after Q1 shows upcoming Q2 at zero`() {
        assertEquals(
            QualiSegments.Segment("Q2", 0f),
            QualiSegments.at(SessionKind.QUALIFYING, 20 * minute)
        )
    }

    @Test
    fun `Q3 runs late in the hour`() {
        assertEquals(
            QualiSegments.Segment("Q3", 0.5f),
            QualiSegments.at(SessionKind.QUALIFYING, 54 * minute)
        )
    }

    @Test
    fun `after the last segment it pins at full`() {
        assertEquals(
            QualiSegments.Segment("Q3", 1f),
            QualiSegments.at(SessionKind.QUALIFYING, 90 * minute)
        )
    }

    @Test
    fun `sprint quali uses SQ names and lengths`() {
        assertEquals(
            QualiSegments.Segment("SQ1", 0.5f),
            QualiSegments.at(SessionKind.SPRINT_QUALIFYING, 6 * minute)
        )
        assertEquals(
            QualiSegments.Segment("SQ3", 0.5f),
            QualiSegments.at(SessionKind.SPRINT_QUALIFYING, 40 * minute)
        )
    }

    @Test
    fun `non-quali kinds have no segments`() {
        assertNull(QualiSegments.at(SessionKind.RACE, 0L))
        assertNull(QualiSegments.at(SessionKind.SPRINT, 0L))
        assertNull(QualiSegments.at(SessionKind.FP1, 0L))
    }
}
