package com.demetrius.f1glyph.data

/**
 * Flat string encoding for the weekend's session list so it fits in the
 * preferences DataStore: "FP1,1719849600000;QUALIFYING,1719936000000".
 */
object SessionsCodec {

    fun encode(sessions: List<UpcomingSession>): String =
        sessions.joinToString(";") { "${it.kind.name},${it.epochMillis}" }

    fun decode(raw: String): List<UpcomingSession> =
        raw.split(';').mapNotNull { row ->
            val parts = row.split(',')
            if (parts.size != 2) return@mapNotNull null
            val kind = runCatching { SessionKind.valueOf(parts[0]) }.getOrNull()
                ?: return@mapNotNull null
            val epoch = parts[1].toLongOrNull() ?: return@mapNotNull null
            UpcomingSession(kind, epoch)
        }
}
