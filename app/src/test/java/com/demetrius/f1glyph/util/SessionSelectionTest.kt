package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.UpcomingSession
import com.demetrius.f1glyph.data.SessionResult
import java.time.Instant
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

    @Test
    fun `result expires at midnight UTC even without a weekend`() {
        val start = Instant.parse("2026-07-05T13:00:00Z").toEpochMilli()
        val state = F1WidgetState(null, null, start,
            todayResult = SessionResult("VER", "GP", start))
        assertEquals(state.todayResult,
            state.resolvedAt(Instant.parse("2026-07-05T23:59:59Z").toEpochMilli()).todayResult)
        assertNull(state.resolvedAt(Instant.parse("2026-07-06T00:00:00Z").toEpochMilli()).todayResult)
    }

    @Test
    fun `a later live session replaces an earlier result across all labels`() {
        val stale = stateWith(fp1, live = false).copy(
            todayResult = SessionResult("VER", "SQ3", fp1.epochMillis))
        val resolved = stale.resolvedAt(fp2.epochMillis + minute)
        assertNull(resolved.todayResult)
        assertTrue(resolved.weekend!!.isSessionLiveNow)
        assertEquals("PRACTICE 2", DisplayFormat.countdownCaption(resolved))
        assertEquals("LIVE", DisplayFormat.widgetTimer(resolved, fp2.epochMillis + minute))
    }

    @Test
    fun `published result ends the estimated live state for the same session`() {
        val state = stateWith(fp1, live = true).copy(
            todayResult = SessionResult("VER", "Q3", fp1.epochMillis))
        val resolved = state.resolvedAt(fp1.epochMillis + minute)
        assertEquals(state.todayResult, resolved.todayResult)
        assertFalse(resolved.weekend!!.isSessionLiveNow)
        assertEquals("FINISHED", DisplayFormat.widgetTimer(resolved, fp1.epochMillis + minute))
    }

    @Test
    fun `legacy single session snapshot also expires`() {
        val state = stateWith(fp1, live = true).let {
            it.copy(weekend = it.weekend!!.copy(sessions = emptyList()))
        }
        val resolved = state.resolvedAt(fp1.epochMillis + SessionWindow.liveWindowMillis(fp1.kind))
        assertNull(resolved.weekend!!.nextSession)
        assertFalse(resolved.weekend!!.isSessionLiveNow)
    }

    @Test
    fun `widget counts down through final ten minutes and switches to live`() {
        val state = stateWith(fp1, live = false)
        for ((remaining, label) in listOf(600_000L to "-10M", 60_001L to "-2M", 1L to "-1M")) {
            val now = fp1.epochMillis - remaining
            assertEquals(label, DisplayFormat.widgetTimer(state.resolvedAt(now), now))
        }
        assertEquals("LIVE", DisplayFormat.widgetTimer(state.resolvedAt(fp1.epochMillis), fp1.epochMillis))
    }

    @Test
    fun `sprint result describes a win rather than pole`() {
        assertEquals("WINS", SessionResult("VER", "S", 0).action)
        assertEquals("WINS", SessionResult("VER", "GP", 0).action)
        assertEquals("POLE", SessionResult("VER", "SQ3", 0).action)
        assertEquals("POLE", SessionResult("VER", "Q3", 0).action)
    }
}
