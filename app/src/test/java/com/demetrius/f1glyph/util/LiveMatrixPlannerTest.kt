package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.UpcomingSession
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveMatrixPlannerTest {

    private val hour = 60 * 60 * 1000L
    private val minute = 60 * 1000L

    private fun state(sessionEpoch: Long?, kind: SessionKind = SessionKind.RACE) = F1WidgetState(
        weekend = RaceWeekend(
            round = 13,
            gpName = "Belgian Grand Prix",
            circuitName = "Spa",
            country = "Belgium",
            nextSession = sessionEpoch?.let { UpcomingSession(kind, it) },
            isSessionLiveNow = false // planner derives the window itself
        ),
        leader = null,
        fetchedAtMillis = 0L
    )

    @Test
    fun `far before a session it shows the countdown face and rechecks every few minutes`() {
        val start = 10 * hour
        assertEquals(
            LiveMatrixPlanner.Decision.PushCountdown(3 * minute),
            LiveMatrixPlanner.decide(state(start), nowMillis = 2 * hour)
        )
    }

    @Test
    fun `inside the countdown hour it rechecks every few minutes`() {
        val start = 10 * hour
        assertEquals(
            LiveMatrixPlanner.Decision.PushCountdown(3 * minute),
            LiveMatrixPlanner.decide(state(start), nowMillis = start - 30 * minute)
        )
    }

    @Test
    fun `countdown recheck is capped at session start`() {
        val start = 10 * hour
        assertEquals(
            LiveMatrixPlanner.Decision.PushCountdown(2 * minute),
            LiveMatrixPlanner.decide(state(start), nowMillis = start - 2 * minute)
        )
    }

    @Test
    fun `pushes and rechecks inside the live window`() {
        val start = 10 * hour
        assertEquals(
            LiveMatrixPlanner.Decision.PushLive(3 * minute),
            LiveMatrixPlanner.decide(state(start), nowMillis = start + 30 * minute)
        )
    }

    @Test
    fun `recheck delay is capped at window end`() {
        val start = 10 * hour
        // RACE window is 135 min; 2 minutes before it closes
        val now = start + 135 * minute - 2 * minute
        assertEquals(
            LiveMatrixPlanner.Decision.PushLive(2 * minute),
            LiveMatrixPlanner.decide(state(start), nowMillis = now)
        )
    }

    @Test
    fun `releases after the window`() {
        val start = 10 * hour
        assertEquals(
            LiveMatrixPlanner.Decision.Release,
            LiveMatrixPlanner.decide(state(start), nowMillis = start + 135 * minute + minute)
        )
    }

    @Test
    fun `short sessions release sooner`() {
        val start = 10 * hour
        // SPRINT_QUALIFYING window is 60 min
        assertEquals(
            LiveMatrixPlanner.Decision.Release,
            LiveMatrixPlanner.decide(
                state(start, kind = SessionKind.SPRINT_QUALIFYING),
                nowMillis = start + 61 * minute
            )
        )
    }

    @Test
    fun `idle without any session`() {
        assertEquals(
            LiveMatrixPlanner.Decision.Idle,
            LiveMatrixPlanner.decide(state(null), nowMillis = 0L)
        )
    }

    @Test
    fun `idle without weekend`() {
        val empty = F1WidgetState(weekend = null, leader = null, fetchedAtMillis = 0L)
        assertEquals(LiveMatrixPlanner.Decision.Idle, LiveMatrixPlanner.decide(empty, nowMillis = 0L))
    }
}
