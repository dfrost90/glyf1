package com.demetrius.f1glyph.util

/**
 * Decides when the widget needs its next off-schedule re-render so the
 * pre-session countdown ("-9M", Ndot bitmap — can't tick natively) counts
 * down by the minute. Pure logic; CountdownTickWorker executes it.
 */
object CountdownTickPlanner {

    /** The countdown only shows inside this window before a session start. */
    const val WINDOW_MILLIS = 10 * 60 * 1000L

    /**
     * Millis until the next tick, or null when no tick is needed: before the
     * window it waits for the window to open; inside it wakes each time the
     * remaining minute flips; the tick landing at/after start renders the
     * live view once and stops (live re-renders are LiveMatrixWorker's job).
     */
    fun nextDelayMillis(sessionStartMillis: Long?, nowMillis: Long): Long? {
        if (sessionStartMillis == null) return null
        val remaining = sessionStartMillis - nowMillis
        return when {
            remaining <= 0 -> null
            remaining > WINDOW_MILLIS -> remaining - WINDOW_MILLIS
            else -> (remaining % 60_000L).let { if (it == 0L) 60_000L else it }
        }
    }
}
