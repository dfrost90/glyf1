package com.demetrius.f1glyph.data

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class F1RepositoryTest {
    private open class EmptyApi : JolpicaApi {
        override suspend fun getNextRace() = JolpicaRoot(MrData(raceTable = RaceTable()))
        override suspend fun getDriverStandings() = JolpicaRoot(MrData())
        override suspend fun getLastRaceResult() = JolpicaRoot(MrData())
        override suspend fun getLastQualifyingResult() = JolpicaRoot(MrData())
    }

    @Test
    fun `schedule failure propagates so workers keep the existing cache and retry`() {
        val failure = IOException("offline")
        val api = object : EmptyApi() {
            override suspend fun getNextRace(): JolpicaRoot = throw failure
        }
        val thrown = assertThrows(IOException::class.java) {
            runBlocking { F1Repository(api).fetchState() }
        }
        assertEquals(failure.message, thrown.message)
    }

    @Test
    fun `successful empty schedule can clear the previous season`() = runBlocking {
        assertNull(F1Repository(EmptyApi()).fetchState().weekend)
    }

    @Test
    fun `optional requests propagate cancellation`() {
        for (endpoint in listOf("standings", "race", "qualifying")) {
            val cancellation = CancellationException(endpoint)
            val api = object : EmptyApi() {
                override suspend fun getDriverStandings(): JolpicaRoot =
                    if (endpoint == "standings") throw cancellation else super.getDriverStandings()
                override suspend fun getLastRaceResult(): JolpicaRoot =
                    if (endpoint == "race") throw cancellation else super.getLastRaceResult()
                override suspend fun getLastQualifyingResult(): JolpicaRoot =
                    if (endpoint == "qualifying") throw cancellation else super.getLastQualifyingResult()
            }
            val thrown = assertThrows(CancellationException::class.java) {
                runBlocking { F1Repository(api).fetchState() }
            }
            assertEquals(cancellation.message, thrown.message)
        }
    }
}
