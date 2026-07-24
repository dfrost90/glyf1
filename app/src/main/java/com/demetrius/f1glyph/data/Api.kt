package com.demetrius.f1glyph.data

import retrofit2.http.GET

/**
 * Jolpica-F1: Ergast-compatible successor API. No auth, ~200 req/hr unauthenticated.
 * Base URL: https://api.jolpi.ca/ergast/f1/
 */
interface JolpicaApi {
    @GET("current/next.json")
    suspend fun getNextRace(): JolpicaRoot

    @GET("current/driverStandings.json")
    suspend fun getDriverStandings(): JolpicaRoot

    @GET("current/last/results.json")
    suspend fun getLastRaceResult(): JolpicaRoot

    @GET("current/last/qualifying.json")
    suspend fun getLastQualifyingResult(): JolpicaRoot
}
