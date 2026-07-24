package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionProgressTest {

    private val minute = 60_000L

    @Test
    fun `race shows RACE label`() {
        val p = SessionProgress.of(SessionKind.RACE, 0L, 30 * minute, liveLap = 23)!!
        assertEquals("RACE", p.label)
        // 30 of 105 nominal minutes
        assertEquals(30f / 105f, p.fraction, 0.001f)
    }

    @Test
    fun `sprint shows SPRINT label`() {
        val p = SessionProgress.of(SessionKind.SPRINT, 0L, 20 * minute, liveLap = 8)!!
        assertEquals("SPRINT", p.label)
        assertEquals(0.5f, p.fraction, 0.001f)
    }

    @Test
    fun `race without lap data still shows RACE label`() {
        val p = SessionProgress.of(SessionKind.RACE, 0L, 30 * minute, liveLap = null)!!
        assertEquals("RACE", p.label)
    }

    @Test
    fun `quali shows current segment`() {
        // 30 min into qualifying lands in Q2 (window 25–40 min)
        val p = SessionProgress.of(SessionKind.QUALIFYING, 0L, 30 * minute, liveLap = null)!!
        assertEquals("Q2", p.label)
        assertEquals(0.5f, p.fraction, 0.001f)
    }

    @Test
    fun `sprint quali shows current segment`() {
        // 20 min into sprint qualifying lands in SQ2 (window 19–29 min)
        val p = SessionProgress.of(SessionKind.SPRINT_QUALIFYING, 0L, 20 * minute, liveLap = null)!!
        assertEquals("SQ2", p.label)
        assertEquals(20f / 45f, p.fraction, 0.001f)
    }

    @Test
    fun `fraction clamps at one when a session runs long`() {
        val p = SessionProgress.of(SessionKind.QUALIFYING, 0L, 90 * minute, liveLap = null)!!
        assertEquals(1f, p.fraction, 0.001f)
    }

    @Test
    fun `practice sessions have no progress`() {
        assertNull(SessionProgress.of(SessionKind.FP1, 0L, 30 * minute, liveLap = null))
    }

    @Test
    fun `no progress before the session starts`() {
        assertNull(SessionProgress.of(SessionKind.RACE, 10 * minute, 5 * minute, liveLap = 1))
    }
}
