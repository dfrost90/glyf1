package com.demetrius.f1glyph.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------- Jolpica (Ergast-compatible) schedule/standings ----------

@JsonClass(generateAdapter = true)
data class JolpicaRoot(@Json(name = "MRData") val mrData: MrData)

@JsonClass(generateAdapter = true)
data class MrData(
    @Json(name = "RaceTable") val raceTable: RaceTable? = null,
    @Json(name = "StandingsTable") val standingsTable: StandingsTable? = null
)

@JsonClass(generateAdapter = true)
data class RaceTable(@Json(name = "Races") val races: List<RaceDto> = emptyList())

@JsonClass(generateAdapter = true)
data class RaceDto(
    val season: String,
    val round: String,
    val raceName: String,
    val date: String,
    val time: String? = null,
    val Circuit: CircuitDto,
    val FirstPractice: SessionTimeDto? = null,
    val SecondPractice: SessionTimeDto? = null,
    val ThirdPractice: SessionTimeDto? = null,
    val Qualifying: SessionTimeDto? = null,
    val Sprint: SessionTimeDto? = null,
    val SprintQualifying: SessionTimeDto? = null,
    @Json(name = "SprintShootout") val sprintShootoutAlt: SessionTimeDto? = null,
    @Json(name = "Results") val results: List<RaceResultDto>? = null,
    @Json(name = "QualifyingResults") val qualifyingResults: List<QualifyingResultDto>? = null
)

@JsonClass(generateAdapter = true)
data class CircuitDto(
    val circuitId: String,
    val circuitName: String,
    val Location: LocationDto
)

@JsonClass(generateAdapter = true)
data class LocationDto(val locality: String, val country: String)

@JsonClass(generateAdapter = true)
data class SessionTimeDto(val date: String, val time: String)

@JsonClass(generateAdapter = true)
data class RaceResultDto(
    val position: String,
    @Json(name = "Driver") val driver: DriverDto
)

@JsonClass(generateAdapter = true)
data class QualifyingResultDto(
    val position: String,
    @Json(name = "Driver") val driver: DriverDto
)

@JsonClass(generateAdapter = true)
data class StandingsTable(@Json(name = "StandingsLists") val lists: List<StandingsListDto> = emptyList())

@JsonClass(generateAdapter = true)
data class StandingsListDto(@Json(name = "DriverStandings") val driverStandings: List<DriverStandingDto> = emptyList())

@JsonClass(generateAdapter = true)
data class DriverStandingDto(
    val position: String,
    val points: String,
    val Driver: DriverDto,
    val Constructors: List<ConstructorDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DriverDto(
    val driverId: String,
    val code: String? = null,
    val familyName: String,
    val givenName: String
)

@JsonClass(generateAdapter = true)
data class ConstructorDto(val constructorId: String, val name: String)

// ---------- Domain model consumed by the widget + Glyph toy ----------

enum class SessionKind { FP1, FP2, FP3, SPRINT_QUALIFYING, SPRINT, QUALIFYING, RACE }

data class UpcomingSession(val kind: SessionKind, val epochMillis: Long)

data class RaceWeekend(
    val round: Int,
    val gpName: String,
    val circuitName: String,
    val country: String,
    val nextSession: UpcomingSession?,
    val isSessionLiveNow: Boolean
)

data class LeaderInfo(val label: String)

/** One row of the championship table shown on the compact widget. */
data class StandingEntry(
    val code: String,          // driver abbreviation, e.g. "VER"
    val points: String,        // kept as string, API returns e.g. "255" or "255.5"
    val constructorId: String  // Ergast constructor id, e.g. "red_bull"
)

data class SessionResult(
    val driverCode: String,       // "VER"
    val sessionLabel: String,     // "GP", "Q3", "SQ3", "S"
    val sessionEpochMillis: Long, // epoch of qualifying/race start, for same-day check
    val round: Int = 0,
    val gpName: String = ""
)

data class F1WidgetState(
    val weekend: RaceWeekend?,
    val leader: LeaderInfo?,
    val fetchedAtMillis: Long,
    val topStandings: List<StandingEntry> = emptyList(),
    val todayResult: SessionResult? = null
)
