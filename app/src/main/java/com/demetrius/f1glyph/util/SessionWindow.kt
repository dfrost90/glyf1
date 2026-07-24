package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind

/**
 * How long after its start a session counts as live. There is no free
 * "session ended" signal, so this is nominal duration plus a buffer for
 * delays and red flags. Shared by the repository, widget, planner and glyph.
 */
object SessionWindow {

    private const val MINUTE = 60_000L

    fun liveWindowMillis(kind: SessionKind): Long = when (kind) {
        SessionKind.RACE -> 135 * MINUTE
        SessionKind.SPRINT -> 60 * MINUTE
        SessionKind.QUALIFYING -> 75 * MINUTE
        SessionKind.SPRINT_QUALIFYING -> 60 * MINUTE
        SessionKind.FP1, SessionKind.FP2, SessionKind.FP3 -> 75 * MINUTE
    }
}
