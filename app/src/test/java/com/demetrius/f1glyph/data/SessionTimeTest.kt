package com.demetrius.f1glyph.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionTimeTest {

    @Test
    fun `parses date and zulu time`() {
        // 2026-07-26 13:00 UTC
        assertEquals(1785070800000L, parseSessionInstant("2026-07-26", "13:00:00Z"))
    }

    @Test
    fun `appends Z when missing`() {
        assertEquals(
            parseSessionInstant("2026-07-26", "13:00:00Z"),
            parseSessionInstant("2026-07-26", "13:00:00")
        )
    }
}
