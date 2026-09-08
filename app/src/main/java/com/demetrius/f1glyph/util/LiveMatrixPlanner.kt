package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState

/**
 * Decides what the Glyph Matrix takeover should do next. While the QS tile
 * is on, the matrix is held continuously: the pre-event face (session code +
 * countdown bar) until the session starts, the live face during it, then a
 * release until the cache learns the next session. Pure logic so it stays
 * unit-testable; the worker executes the decision.
 */
object LiveMatrixPlanner {

    /** Refetch/redraw cadence while a face is being shown. */
    private const val RECHECK_MILLIS = 3 * 60 * 1000L

    /** How far before a session start to begin showing the pre-event face. */
    const val COUNTDOWN_HORIZON_MILLIS = 60 * 60 * 1000L

    sealed interface Decision {
        /** Show the pre-event face; cache-only, no network needed. */
        data class PushCountdown(val recheckDelayMillis: Long) : Decision

        /** Session live: fetch fresh data, push the live face, recheck. */
        data class PushLive(val recheckDelayMillis: Long) : Decision

        /** Live window over: release the matrix back to the system. */
        data object Release : Decision

        /** Nothing to do (no schedule data). */
        data object Idle : Decision
    }

    fun decide(state: F1WidgetState, nowMillis: Long): Decision {
        val resolved = state.resolvedAt(nowMillis)
        if (resolved.todayResult != null) return Decision.Release
        val session = resolved.weekend?.nextSession ?: return if (
            state.weekend?.nextSession != null || state.weekend?.sessions?.isNotEmpty() == true
        ) Decision.Release else Decision.Idle
        val start = session.epochMillis
        val windowEnd = start + SessionWindow.liveWindowMillis(session.kind)
        val toSession = start - nowMillis
        return when {
            // Before session: push countdown face and recheck every minute so the
            // displayed countdown stays current. Cap at RECHECK_MILLIS (3 min) so
            // we don't drift — the face is static but freshness still matters.
            toSession > 0 -> Decision.PushCountdown(minOf(RECHECK_MILLIS, toSession))
            nowMillis < windowEnd -> Decision.PushLive(minOf(RECHECK_MILLIS, windowEnd - nowMillis))
            else -> Decision.Release
        }
    }
}
