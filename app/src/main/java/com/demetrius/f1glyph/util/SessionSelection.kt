package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.UpcomingSession

/**
 * Chooses which weekend session the widget/toy should show *right now*, from
 * the full list of the weekend's sessions. Pure and clock-driven so any redraw
 * (widget tap, countdown tick, session-boundary re-render) self-corrects
 * without a network fetch — the whole schedule is fetched once and kept.
 */
object SessionSelection {

    /** [session] is the live one if any, else the next upcoming; null if none left. */
    data class Active(val session: UpcomingSession?, val isLive: Boolean)

    fun select(sessions: List<UpcomingSession>, now: Long): Active {
        val sorted = sessions.sortedBy { it.epochMillis }
        val live = sorted.lastOrNull {
            it.epochMillis <= now && now - it.epochMillis < SessionWindow.liveWindowMillis(it.kind)
        }
        val next = live ?: sorted.firstOrNull { it.epochMillis > now }
        return Active(next, live != null)
    }
}

/**
 * Returns a copy of this state with the weekend's [nextSession] and
 * [isSessionLiveNow] recomputed for [now] from the cached session list, so
 * every render reflects the current clock without a network fetch. A no-op
 * when there is no weekend or no session list.
 */
fun F1WidgetState.resolvedAt(now: Long): F1WidgetState {
    val w = weekend ?: return this
    if (w.sessions.isEmpty()) return this
    val active = SessionSelection.select(w.sessions, now)
    return copy(
        weekend = w.copy(nextSession = active.session, isSessionLiveNow = active.isLive)
    )
}
