package com.demetrius.f1glyph.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionsCodecTest {

    private val sessions = listOf(
        UpcomingSession(SessionKind.FP1, 1_719_849_600_000L),
        UpcomingSession(SessionKind.QUALIFYING, 1_719_936_000_000L),
        UpcomingSession(SessionKind.RACE, 1_720_022_400_000L),
    )

    @Test
    fun `encode then decode round-trips the session list`() {
        assertEquals(sessions, SessionsCodec.decode(SessionsCodec.encode(sessions)))
    }

    @Test
    fun `empty string decodes to empty list`() {
        assertEquals(emptyList<UpcomingSession>(), SessionsCodec.decode(""))
    }

    @Test
    fun `malformed rows are skipped`() {
        val raw = "FP1,1000;garbage;QUALIFYING,notanumber;RACE,2000"
        assertEquals(
            listOf(
                UpcomingSession(SessionKind.FP1, 1000L),
                UpcomingSession(SessionKind.RACE, 2000L),
            ),
            SessionsCodec.decode(raw)
        )
    }
}
