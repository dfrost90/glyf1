package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.SessionKind

/**
 * Scheduled lap totals for the 2026 calendar, so the Glyph Matrix progress
 * row can show current lap / total laps. Not available from the free APIs;
 * keyed loosely on GP/circuit names. Sprint counts for first-time 2026
 * sprint venues are ~100 km estimates. Null falls back to time-based
 * progress.
 */
object RaceLaps {

    private class Track(val keys: List<String>, val race: Int, val sprint: Int? = null)

    private val tracks = listOf(
        Track(listOf("australia", "albert park", "melbourne"), 58),
        Track(listOf("china", "chinese", "shanghai"), 56, sprint = 19),
        Track(listOf("japan", "suzuka"), 53),
        Track(listOf("bahrain", "sakhir"), 57),
        Track(listOf("saudi", "jeddah"), 50),
        Track(listOf("miami"), 57, sprint = 19),
        Track(listOf("canad", "montreal", "villeneuve"), 70, sprint = 23),
        Track(listOf("monaco", "monte carlo"), 78),
        Track(listOf("spanish", "spain", "barcelona", "catalunya"), 66),
        Track(listOf("austria", "red bull ring", "spielberg"), 71),
        Track(listOf("british", "britain", "silverstone"), 52, sprint = 17),
        Track(listOf("belgi", "spa"), 44),
        Track(listOf("hungar"), 70),
        Track(listOf("dutch", "netherlands", "zandvoort"), 72, sprint = 24),
        Track(listOf("italian", "italy", "monza"), 53),
        Track(listOf("madrid", "madring"), 57),
        Track(listOf("azerbaijan", "baku"), 51),
        Track(listOf("singapore", "marina bay"), 62, sprint = 21),
        Track(listOf("united states", "austin", "americas"), 56),
        Track(listOf("mexic"), 71),
        Track(listOf("paulo", "brazil", "interlagos"), 71),
        Track(listOf("vegas"), 50),
        Track(listOf("qatar", "lusail"), 57),
        Track(listOf("abu dhabi", "yas"), 58)
    )

    fun total(kind: SessionKind, gpName: String?, circuitName: String?): Int? {
        val hay = "${gpName.orEmpty()} ${circuitName.orEmpty()}".lowercase()
        val track = tracks.firstOrNull { t -> t.keys.any { hay.contains(it) } } ?: return null
        return when (kind) {
            SessionKind.RACE -> track.race
            SessionKind.SPRINT -> track.sprint
            else -> null
        }
    }
}
