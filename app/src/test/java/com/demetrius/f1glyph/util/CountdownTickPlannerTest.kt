package com.demetrius.f1glyph.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownTickPlannerTest {

    private val minute = 60 * 1000L
    private val start = 100 * minute

    @Test
    fun `no session means no tick`() {
        assertNull(CountdownTickPlanner.nextDelayMillis(null, nowMillis = 0L))
    }

    @Test
    fun `before the window it waits for the window to open`() {
        assertEquals(
            35 * minute,
            CountdownTickPlanner.nextDelayMillis(start, nowMillis = start - 45 * minute)
        )
    }

    @Test
    fun `at the window edge it ticks on the next minute flip`() {
        assertEquals(
            minute,
            CountdownTickPlanner.nextDelayMillis(start, nowMillis = start - 10 * minute)
        )
    }

    @Test
    fun `inside the window it wakes when the remaining minute flips`() {
        // 9.5 minutes left: next flip (10M -> 9M on display) in 30s.
        assertEquals(
            30_000L,
            CountdownTickPlanner.nextDelayMillis(start, nowMillis = start - 9 * minute - 30_000L)
        )
    }

    @Test
    fun `final tick lands exactly at session start`() {
        assertEquals(
            40_000L,
            CountdownTickPlanner.nextDelayMillis(start, nowMillis = start - 40_000L)
        )
    }

    @Test
    fun `stops once the session has started`() {
        assertNull(CountdownTickPlanner.nextDelayMillis(start, nowMillis = start))
        assertNull(CountdownTickPlanner.nextDelayMillis(start, nowMillis = start + minute))
    }
}
