package com.demetrius.f1glyph.data

import android.util.Log
import com.demetrius.f1glyph.util.SessionSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val TAG = "F1Repository"

class F1Repository(
    private val jolpica: JolpicaApi = NetworkModule.jolpicaApi
) {

    suspend fun fetchState(): F1WidgetState = withContext(Dispatchers.IO) {
        // A failed request must retry without replacing the cached schedule.
        // A successful empty response still clears it at the end of the season.
        val weekend = fetchWeekend()

        val standings = optionalFetch("standings") { fetchTopStandings() }.orEmpty()

        val leader = standings.firstOrNull()?.let { LeaderInfo(it.code) }

        val now = System.currentTimeMillis()
        val todayResult = if (weekend?.isSessionLiveNow != true) {
            fetchTodayResult(now)
        } else null

        F1WidgetState(
            weekend = weekend,
            leader = leader,
            fetchedAtMillis = now,
            topStandings = standings,
            todayResult = todayResult
        )
    }

    private suspend fun fetchWeekend(): RaceWeekend? {
        val race = jolpica.getNextRace().mrData.raceTable?.races?.firstOrNull() ?: return null

        val sessions = buildList {
            race.FirstPractice?.let { add(UpcomingSession(SessionKind.FP1, it.toEpochMillis())) }
            race.SecondPractice?.let { add(UpcomingSession(SessionKind.FP2, it.toEpochMillis())) }
            race.ThirdPractice?.let { add(UpcomingSession(SessionKind.FP3, it.toEpochMillis())) }
            (race.SprintQualifying ?: race.sprintShootoutAlt)?.let {
                add(UpcomingSession(SessionKind.SPRINT_QUALIFYING, it.toEpochMillis()))
            }
            race.Sprint?.let { add(UpcomingSession(SessionKind.SPRINT, it.toEpochMillis())) }
            race.Qualifying?.let { add(UpcomingSession(SessionKind.QUALIFYING, it.toEpochMillis())) }
            if (race.time != null) {
                add(UpcomingSession(SessionKind.RACE, parseSessionInstant(race.date, race.time)))
            }
        }.sortedBy { it.epochMillis }

        val active = SessionSelection.select(sessions, System.currentTimeMillis())

        return RaceWeekend(
            round = race.round.toIntOrNull() ?: 0,
            gpName = race.raceName,
            circuitName = race.Circuit.circuitName,
            country = race.Circuit.Location.country,
            nextSession = active.session,
            isSessionLiveNow = active.isLive,
            sessions = sessions
        )
    }

    private suspend fun fetchTopStandings(): List<StandingEntry> =
        jolpica.getDriverStandings()
            .mrData.standingsTable?.lists?.firstOrNull()?.driverStandings
            .orEmpty()
            .sortedBy { it.position.toIntOrNull() ?: Int.MAX_VALUE }
            .take(6)
            .map {
                StandingEntry(
                    code = it.Driver.code ?: it.Driver.familyName.take(3).uppercase(),
                    points = it.points,
                    constructorId = it.Constructors.firstOrNull()?.constructorId.orEmpty()
                )
            }

    private suspend fun fetchTodayResult(nowMillis: Long): SessionResult? {
        val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate()

        // Race result (Sunday)
        val lastRaceDto = optionalFetch("race result") {
            jolpica.getLastRaceResult().mrData.raceTable?.races?.firstOrNull()
        }
        if (lastRaceDto != null) {
            val raceDate = runCatching { LocalDate.parse(lastRaceDto.date) }.getOrNull()
            if (raceDate == today) {
                val winner = lastRaceDto.results?.firstOrNull { it.position == "1" }?.driver?.code
                if (winner != null) return SessionResult(
                    driverCode = winner,
                    sessionLabel = "GP",
                    sessionEpochMillis = if (lastRaceDto.time != null)
                        parseSessionInstant(lastRaceDto.date, lastRaceDto.time)
                    else raceDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    round = lastRaceDto.round.toIntOrNull() ?: 0,
                    gpName = lastRaceDto.raceName
                )
            }
        }

        // Qualifying result (Saturday)
        val lastQualDto = optionalFetch("qualifying result") {
            jolpica.getLastQualifyingResult().mrData.raceTable?.races?.firstOrNull()
        }
        if (lastQualDto != null) {
            val qualDate = runCatching {
                lastQualDto.Qualifying?.date?.let { LocalDate.parse(it) }
            }.getOrNull()
            if (qualDate == today) {
                val pole = lastQualDto.qualifyingResults?.firstOrNull { it.position == "1" }?.driver?.code
                if (pole != null) return SessionResult(
                    driverCode = pole,
                    sessionLabel = "Q3",
                    sessionEpochMillis = lastQualDto.Qualifying?.let {
                        parseSessionInstant(it.date, it.time)
                    } ?: qualDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
                    round = lastQualDto.round.toIntOrNull() ?: 0,
                    gpName = lastQualDto.raceName
                )
            }
        }

        return null
    }

    private fun SessionTimeDto.toEpochMillis(): Long = parseSessionInstant(date, time)

    private suspend fun <T> optionalFetch(label: String, fetch: suspend () -> T): T? = try {
        fetch()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Failed to fetch $label", e)
        null
    }
}

internal fun parseSessionInstant(date: String, time: String): Long {
    val cleanTime = if (time.endsWith("Z") || '+' in time || '-' in time) time else "${time}Z"
    val odt = OffsetDateTime.parse(
        "${date}T$cleanTime",
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
    )
    return odt.toInstant().toEpochMilli()
}
