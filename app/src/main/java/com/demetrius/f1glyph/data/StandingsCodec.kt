package com.demetrius.f1glyph.data

/**
 * Flat string encoding for the top-standings list so it fits in the
 * preferences DataStore: "VER,255,red_bull;NOR,241,mclaren".
 * Driver codes, points, and constructor ids never contain ',' or ';'.
 */
object StandingsCodec {

    fun encode(entries: List<StandingEntry>): String =
        entries.joinToString(";") { "${it.code},${it.points},${it.constructorId}" }

    fun decode(raw: String): List<StandingEntry> =
        raw.split(';').mapNotNull { row ->
            val parts = row.split(',')
            if (parts.size == 3) StandingEntry(parts[0], parts[1], parts[2]) else null
        }
}
