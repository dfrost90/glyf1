package com.demetrius.f1glyph.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TeamColorsTest {

    @Test
    fun `known constructor gets its color`() {
        assertNotEquals(TeamColors.FALLBACK, TeamColors.of("ferrari"))
        assertNotEquals(TeamColors.FALLBACK, TeamColors.of("red_bull"))
        assertNotEquals(TeamColors.FALLBACK, TeamColors.of("mclaren"))
    }

    @Test
    fun `lookup is case insensitive`() {
        assertEquals(TeamColors.of("ferrari"), TeamColors.of("Ferrari"))
    }

    @Test
    fun `unknown constructor falls back to grey`() {
        assertEquals(TeamColors.FALLBACK, TeamColors.of("brawn_gp"))
        assertEquals(TeamColors.FALLBACK, TeamColors.of(""))
    }

    @Test
    fun `openf1 team names map to constructor ids`() {
        assertEquals("red_bull", TeamColors.idFromTeamName("Red Bull Racing"))
        assertEquals("racing_bulls", TeamColors.idFromTeamName("Racing Bulls"))
        assertEquals("mclaren", TeamColors.idFromTeamName("McLaren"))
        assertEquals("aston_martin", TeamColors.idFromTeamName("Aston Martin Aramco"))
        assertEquals("sauber", TeamColors.idFromTeamName("Kick Sauber"))
        assertEquals("", TeamColors.idFromTeamName("Unknown Racing"))
    }
}
