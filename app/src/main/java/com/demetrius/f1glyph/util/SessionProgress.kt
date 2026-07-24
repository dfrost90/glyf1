package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind

/**
 * Progress of a live session for the wide widget's top dot row: laps for
 * race/sprint (elapsed-time fallback when lap data is missing), elapsed time
 * for the qualifying sessions. Practice gets no progress bar. The fill
 * fraction is elapsed time over the session's nominal duration.
 */
object SessionProgress {

    data class Info(val fraction: Float, val label: String)

    private val nominalMinutes = mapOf(
        SessionKind.RACE to 105,
        SessionKind.SPRINT to 40,
        SessionKind.QUALIFYING to 60,
        SessionKind.SPRINT_QUALIFYING to 45
    )

    fun of(kind: SessionKind, startMillis: Long, nowMillis: Long, liveLap: Int?): Info? {
        val nominal = nominalMinutes[kind] ?: return null
        val elapsed = nowMillis - startMillis
        if (elapsed < 0) return null

        val label = when (kind) {
            SessionKind.QUALIFYING, SessionKind.SPRINT_QUALIFYING ->
                QualiSegments.at(kind, elapsed)?.name
                    ?: if (kind == SessionKind.QUALIFYING) "QUALI" else "SQ"
            SessionKind.RACE -> "RACE"
            SessionKind.SPRINT -> "SPRINT"
            else -> kind.name
        }
        val fraction = (elapsed.toFloat() / (nominal * 60_000f)).coerceIn(0f, 1f)
        return Info(fraction, label)
    }
}
