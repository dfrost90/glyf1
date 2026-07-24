package com.demetrius.f1glyph.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StandingsCodecTest {

    private val top3 = listOf(
        StandingEntry(code = "VER", points = "255", constructorId = "red_bull"),
        StandingEntry(code = "NOR", points = "241", constructorId = "mclaren"),
        StandingEntry(code = "LEC", points = "199", constructorId = "ferrari")
    )

    @Test
    fun `round trips a standings list`() {
        assertEquals(top3, StandingsCodec.decode(StandingsCodec.encode(top3)))
    }

    @Test
    fun `empty list round trips`() {
        assertEquals(emptyList<StandingEntry>(), StandingsCodec.decode(StandingsCodec.encode(emptyList())))
    }

    @Test
    fun `malformed rows are skipped`() {
        assertEquals(
            listOf(StandingEntry("VER", "255", "red_bull")),
            StandingsCodec.decode("VER,255,red_bull;garbage;also,bad")
        )
    }
}
