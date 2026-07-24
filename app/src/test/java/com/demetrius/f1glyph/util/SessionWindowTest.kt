package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionWindowTest {

    private val minute = 60_000L

    @Test
    fun `windows cover nominal duration plus buffer`() {
        assertEquals(135 * minute, SessionWindow.liveWindowMillis(SessionKind.RACE))
        assertEquals(60 * minute, SessionWindow.liveWindowMillis(SessionKind.SPRINT))
        assertEquals(75 * minute, SessionWindow.liveWindowMillis(SessionKind.QUALIFYING))
        assertEquals(60 * minute, SessionWindow.liveWindowMillis(SessionKind.SPRINT_QUALIFYING))
        assertEquals(75 * minute, SessionWindow.liveWindowMillis(SessionKind.FP1))
    }
}
