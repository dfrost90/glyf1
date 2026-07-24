package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind

/**
 * Which qualifying segment (Q1..Q3 / SQ1..SQ3) is running at a given elapsed
 * time, and how far through it is. Timing data is not in any free API, so
 * this uses the sporting-regulation segment lengths with typical
 * inter-segment breaks; during a break the upcoming segment shows at 0%.
 */
object QualiSegments {

    data class Segment(val name: String, val fraction: Float)

    private data class Window(val name: String, val startMin: Int, val endMin: Int)

    // Q1 18 + ~7 break, Q2 15 + ~8 break, Q3 12.
    private val quali = listOf(
        Window("Q1", 0, 18),
        Window("Q2", 25, 40),
        Window("Q3", 48, 60)
    )

    // SQ1 12 + ~7 break, SQ2 10 + ~7 break, SQ3 8.
    private val sprintQuali = listOf(
        Window("SQ1", 0, 12),
        Window("SQ2", 19, 29),
        Window("SQ3", 36, 44)
    )

    /** Null for non-qualifying session kinds. */
    fun at(kind: SessionKind, elapsedMillis: Long): Segment? {
        val windows = when (kind) {
            SessionKind.QUALIFYING -> quali
            SessionKind.SPRINT_QUALIFYING -> sprintQuali
            else -> return null
        }
        val minutes = elapsedMillis / 60_000f
        for (w in windows) {
            if (minutes < w.endMin) {
                val fraction = (minutes - w.startMin) / (w.endMin - w.startMin)
                return Segment(w.name, fraction.coerceIn(0f, 1f))
            }
        }
        return Segment(windows.last().name, 1f)
    }
}
