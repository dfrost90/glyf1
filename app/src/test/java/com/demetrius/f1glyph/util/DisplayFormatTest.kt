package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.LeaderInfo
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.UpcomingSession
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayFormatTest {

    private fun state(
        kind: SessionKind? = SessionKind.QUALIFYING,
        sessionEpoch: Long = 1_000_000L,
        live: Boolean = false,
        leader: LeaderInfo? = LeaderInfo("VER"),
        hasWeekend: Boolean = true
    ) = F1WidgetState(
        weekend = if (hasWeekend) RaceWeekend(
            round = 13,
            gpName = "Belgian Grand Prix",
            circuitName = "Spa",
            country = "Belgium",
            nextSession = kind?.let { UpcomingSession(it, sessionEpoch) },
            isSessionLiveNow = live
        ) else null,
        leader = leader,
        fetchedAtMillis = 0L
    )

    @Test
    fun `matrix panel shows session and leader`() {
        assertEquals("QUALI" to "VER", DisplayFormat.matrixPanelText(state()))
    }

    @Test
    fun `matrix panel falls back when empty`() {
        assertEquals("F1" to "--", DisplayFormat.matrixPanelText(state(hasWeekend = false, leader = null)))
    }

    @Test
    fun `caption is the bare event name`() {
        assertEquals("QUALIFYING", DisplayFormat.countdownCaption(state()))
        assertEquals("QUALIFYING", DisplayFormat.countdownCaption(state(live = true)))
    }

    @Test
    fun `elapsed label in hours and minutes`() {
        assertEquals("+1H12M", DisplayFormat.elapsedCompact(0L, 72 * 60_000L))
    }

    @Test
    fun `elapsed label in minutes`() {
        assertEquals("+42M", DisplayFormat.elapsedCompact(0L, 42 * 60_000L))
    }

    @Test
    fun `caption without data`() {
        assertEquals("NO DATA", DisplayFormat.countdownCaption(state(hasWeekend = false)))
    }

    // --- compactCountdown ---

    @Test
    fun `compact countdown in days`() {
        val now = 0L
        val epoch = (2 * 24 * 60 + 5 * 60) * 60_000L // 2d 5h ahead
        assertEquals("-2D5H", DisplayFormat.compactCountdown(state(sessionEpoch = epoch), now))
    }

    @Test
    fun `compact countdown in hours`() {
        val now = 0L
        val epoch = (3 * 60 + 12) * 60_000L // 3h12m ahead
        assertEquals("-3H12M", DisplayFormat.compactCountdown(state(sessionEpoch = epoch), now))
    }

    @Test
    fun `compact countdown in minutes`() {
        assertEquals("-42M", DisplayFormat.compactCountdown(state(sessionEpoch = 42 * 60_000L), 0L))
    }

    @Test
    fun `compact countdown live`() {
        assertEquals("LIVE", DisplayFormat.compactCountdown(state(live = true), 0L))
    }

    @Test
    fun `compact countdown session started but not flagged live`() {
        assertEquals("NOW", DisplayFormat.compactCountdown(state(sessionEpoch = 0L), 10L))
    }

    @Test
    fun `compact countdown without data`() {
        assertEquals("--", DisplayFormat.compactCountdown(state(hasWeekend = false), 0L))
    }

    // --- sessionWhen ---

    private val utc = java.time.ZoneId.of("UTC")

    @Test
    fun `sessionWhen same day shows time`() {
        // 2026-07-05 13:00 UTC; now = same day at 10:00
        val event = 1783256400000L // 2026-07-05 13:00:00 UTC
        val now = 1783245600000L   // 2026-07-05 10:00:00 UTC
        assertEquals("13:00", DisplayFormat.sessionWhen(event, now, utc))
    }

    @Test
    fun `sessionWhen within week shows weekday`() {
        val event = 1783256400000L // 2026-07-05 13:00:00 UTC (Sunday)
        val now = event - 2 * 24 * 60 * 60 * 1000L // 2 days before
        assertEquals("SUN 13:00", DisplayFormat.sessionWhen(event, now, utc))
    }

    @Test
    fun `sessionWhen further out shows day and month`() {
        val event = 1783256400000L // 2026-07-05 13:00:00 UTC
        val now = event - 20L * 24 * 60 * 60 * 1000L
        assertEquals("5 JUL", DisplayFormat.sessionWhen(event, now, utc))
    }

    @Test
    fun `sessionWhen exactly 7 days out shows day and month`() {
        val event = 1783256400000L // 2026-07-05 13:00:00 UTC
        val now = event - 7L * 24 * 60 * 60 * 1000L
        assertEquals("5 JUL", DisplayFormat.sessionWhen(event, now, utc))
    }

    // --- eventDateTime ---

    @Test
    fun `event within the week shows weekday and time`() {
        // 2026-07-05 is a Sunday; now = two days before
        val event = 1783256400000L // 2026-07-05 13:00:00 UTC
        val now = event - 2 * 24 * 60 * 60 * 1000L
        assertEquals("SUN 13:00", DisplayFormat.eventDateTime(event, now, utc))
    }

    @Test
    fun `event further out shows date and time`() {
        val event = 1783256400000L // 2026-07-05 13:00:00 UTC
        val now = event - 20L * 24 * 60 * 60 * 1000L
        assertEquals("5 JUL 13:00", DisplayFormat.eventDateTime(event, now, utc))
    }

    @Test
    fun `session codes for the glyph face`() {
        assertEquals("SQ", DisplayFormat.sessionCode(SessionKind.SPRINT_QUALIFYING))
        assertEquals("S", DisplayFormat.sessionCode(SessionKind.SPRINT))
        assertEquals("Q", DisplayFormat.sessionCode(SessionKind.QUALIFYING))
        assertEquals("R", DisplayFormat.sessionCode(SessionKind.RACE))
        assertEquals("FP2", DisplayFormat.sessionCode(SessionKind.FP2))
    }

    // --- countdownCompact (widget timer row, last 10 minutes) ---

    @Test
    fun `countdown shows minutes left, rounding up`() {
        val start = 100 * 60_000L
        assertEquals("-10M", DisplayFormat.countdownCompact(start, start - 10 * 60_000L))
        assertEquals("-10M", DisplayFormat.countdownCompact(start, start - 9 * 60_000L - 30_000L))
        assertEquals("-9M", DisplayFormat.countdownCompact(start, start - 9 * 60_000L))
        assertEquals("-1M", DisplayFormat.countdownCompact(start, start - 20_000L))
        assertEquals("-0M", DisplayFormat.countdownCompact(start, start))
    }
}
