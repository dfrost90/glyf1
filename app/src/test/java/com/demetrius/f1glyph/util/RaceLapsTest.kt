package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RaceLapsTest {

    @Test
    fun `matches on GP name`() {
        assertEquals(52, RaceLaps.total(SessionKind.RACE, "British Grand Prix", "Silverstone Circuit"))
    }

    @Test
    fun `matches on circuit name alone`() {
        assertEquals(44, RaceLaps.total(SessionKind.RACE, null, "Circuit de Spa-Francorchamps"))
    }

    @Test
    fun `sprint venues have sprint laps`() {
        assertEquals(17, RaceLaps.total(SessionKind.SPRINT, "British Grand Prix", "Silverstone Circuit"))
    }

    @Test
    fun `no sprint laps for non-sprint venues`() {
        assertNull(RaceLaps.total(SessionKind.SPRINT, "Monaco Grand Prix", "Circuit de Monaco"))
    }

    @Test
    fun `only race and sprint kinds have lap totals`() {
        assertNull(RaceLaps.total(SessionKind.QUALIFYING, "British Grand Prix", "Silverstone"))
    }

    @Test
    fun `unknown venue falls through`() {
        assertNull(RaceLaps.total(SessionKind.RACE, "Nowhere Grand Prix", "Nowhere"))
    }
}
