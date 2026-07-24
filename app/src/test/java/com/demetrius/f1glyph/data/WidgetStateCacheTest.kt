package com.demetrius.f1glyph.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Exercises the pure DataStore mapping ([WidgetStateCache.writeState] /
 * [WidgetStateCache.readState]) against an in-memory preferences bag, so no
 * Context or device is needed.
 */
class WidgetStateCacheTest {

    private fun weekendState() = F1WidgetState(
        weekend = RaceWeekend(
            round = 11,
            gpName = "Hungarian Grand Prix",
            circuitName = "Hungaroring",
            country = "Hungary",
            nextSession = UpcomingSession(SessionKind.RACE, 1_000L),
            isSessionLiveNow = false,
            sessions = listOf(UpcomingSession(SessionKind.RACE, 1_000L))
        ),
        leader = LeaderInfo("VER"),
        fetchedAtMillis = 42L,
        topStandings = listOf(StandingEntry("VER", "204", "red_bull"))
    )

    @Test
    fun `round-trips a weekend`() {
        val p = mutablePreferencesOf()
        WidgetStateCache.writeState(p, weekendState())
        val loaded = WidgetStateCache.readState(p)
        assertEquals("Hungarian Grand Prix", loaded.weekend?.gpName)
        assertEquals(SessionKind.RACE, loaded.weekend?.nextSession?.kind)
        assertEquals(1, loaded.weekend?.sessions?.size)
    }

    @Test
    fun `null weekend clears a previously-cached weekend`() {
        val p = mutablePreferencesOf()
        WidgetStateCache.writeState(p, weekendState())
        // End of season: a later successful fetch returns no upcoming weekend.
        WidgetStateCache.writeState(p, weekendState().copy(weekend = null))
        val loaded = WidgetStateCache.readState(p)
        assertNull("stale weekend must not linger", loaded.weekend)
        assertNull(loaded.weekend?.nextSession)
    }
}
