package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.UpcomingSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionSelectionTest {

    private val minute = 60_000L
    private val fp1 = UpcomingSession(SessionKind.FP1, 10 * 60 * minute)
    private val fp2 = UpcomingSession(SessionKind.FP2, 10 * 60 * minute + 210 * minute)
    private val sessions = listOf(fp1, fp2)

    /** The reported bug: after FP1's live window closes the widget must move on
     *  to FP2, not keep showing FP1 as if it were still upcoming. */
    @Test
    fun `advances to next session once the previous window has ended`() {
        val now = fp1.epochMillis + SessionWindow.liveWindowMillis(SessionKind.FP1) + 1
        val active = SessionSelection.select(sessions, now)
        assertEquals(fp2, active.session)
        assertFalse(active.isLive)
    }

    @Test
    fun `selects the live session during its window`() {
        val now = fp1.epochMillis + 20 * minute
        val active = SessionSelection.select(sessions, now)
        assertEquals(fp1, active.session)
        assertTrue(active.isLive)
    }

    @Test
    fun `before any session selects the first upcoming`() {
        val now = fp1.epochMillis - 30 * minute
        val active = SessionSelection.select(sessions, now)
        assertEquals(fp1, active.session)
        assertFalse(active.isLive)
    }

    @Test
    fun `after all sessions returns null`() {
        val now = fp2.epochMillis + SessionWindow.liveWindowMillis(SessionKind.FP2) + 1
        val active = SessionSelection.select(sessions, now)
        assertNull(active.session)
        assertFalse(active.isLive)
    }

    @Test
    fun `unsorted input still selects correctly`() {
        val active = SessionSelection.select(listOf(fp2, fp1), fp1.epochMillis - minute)
        assertEquals(fp1, active.session)
        assertFalse(active.isLive)
    }

    private fun stateWith(next: UpcomingSession?, live: Boolean) = F1WidgetState(
        weekend = RaceWeekend(1, "GP", "Circuit", "Country", next, live, sessions),
        leader = null,
        fetchedAtMillis = 0L
    )

    @Test
    fun `resolvedAt advances nextSession after a session's window ends`() {
        // Cache still says FP1 is the live session (the stale snapshot)...
        val stale = stateWith(fp1, live = true)
        val now = fp1.epochMillis + SessionWindow.liveWindowMillis(SessionKind.FP1) + 1
        val resolved = stale.resolvedAt(now)
        assertEquals(fp2, resolved.weekend?.nextSession)
        assertFalse(resolved.weekend?.isSessionLiveNow == true)
    }

    @Test
    fun `resolvedAt marks the session live during its window`() {
        val resolved = stateWith(fp1, live = false).resolvedAt(fp1.epochMillis + minute)
        assertEquals(fp1, resolved.weekend?.nextSession)
        assertTrue(resolved.weekend?.isSessionLiveNow == true)
    }
}
