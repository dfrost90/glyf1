package com.demetrius.f1glyph.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.LeaderInfo
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.SessionResult
import com.demetrius.f1glyph.data.StandingEntry
import com.demetrius.f1glyph.data.UpcomingSession
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.widget.F1WidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only. Injects a synthetic cached state so every widget/toy state can be
 * screenshotted on demand, regardless of the real F1 calendar:
 *
 *   adb shell am broadcast -a com.demetrius.f1glyph.DEBUG_INJECT \
 *     --es scenario live-race -n com.demetrius.f1glyph/.debug.DebugStateReceiver
 *
 * scenarios: upcoming | countdown | live-race | live-quali |
 *            finished-race | finished-quali | nodata
 *
 * Overwrites the cache; trigger a normal refresh afterwards to restore live data.
 */
class DebugStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scenario = intent.getStringExtra("scenario") ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val cache = WidgetStateCache(context)
                val standings = cache.load().topStandings.ifEmpty { DEMO_STANDINGS }
                cache.save(buildState(scenario, standings, System.currentTimeMillis()))
                F1WidgetProvider.renderAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun buildState(scenario: String, standings: List<StandingEntry>, now: Long): F1WidgetState {
        val minute = 60_000L
        val hour = 60 * minute
        val day = 24 * hour
        val leader = standings.firstOrNull()?.let { LeaderInfo(it.code) }
        val winner = standings.firstOrNull()?.code ?: "VER"

        fun weekend(next: UpcomingSession?, live: Boolean, all: List<UpcomingSession>) = RaceWeekend(
            round = 11, gpName = "Hungarian Grand Prix", circuitName = "Hungaroring",
            country = "Hungary", nextSession = next, isSessionLiveNow = live, sessions = all
        )

        fun state(weekend: RaceWeekend?, result: SessionResult? = null) = F1WidgetState(
            weekend = weekend, leader = leader, fetchedAtMillis = now,
            topStandings = standings, todayResult = result
        )

        return when (scenario) {
            "countdown" -> {
                val q = UpcomingSession(SessionKind.QUALIFYING, now + 7 * minute)
                state(weekend(q, live = false, listOf(q)))
            }
            "live-race" -> {
                val r = UpcomingSession(SessionKind.RACE, now - 30 * minute)
                state(weekend(r, live = true, listOf(r)))
            }
            "live-quali" -> {
                val q = UpcomingSession(SessionKind.QUALIFYING, now - 20 * minute)
                state(weekend(q, live = true, listOf(q)))
            }
            "finished-race" -> {
                // 4h ago: safely past the 135-min RACE live window.
                val r = UpcomingSession(SessionKind.RACE, now - 4 * hour)
                state(
                    weekend(null, live = false, listOf(r)),
                    SessionResult(winner, "GP", now - 4 * hour, 11, "Hungarian Grand Prix")
                )
            }
            "finished-quali" -> {
                val q = UpcomingSession(SessionKind.QUALIFYING, now - 4 * hour)
                state(
                    weekend(null, live = false, listOf(q)),
                    SessionResult(winner, "Q3", now - 4 * hour, 11, "Hungarian Grand Prix")
                )
            }
            // Genuine pre-first-fetch state: nothing cached at all.
            "nodata" -> F1WidgetState(
                weekend = null, leader = null, fetchedAtMillis = now,
                topStandings = emptyList(), todayResult = null
            )
            else /* upcoming */ -> {
                val fp2 = UpcomingSession(SessionKind.FP2, now + 2 * day)
                state(weekend(fp2, live = false, listOf(fp2)))
            }
        }
    }

    companion object {
        private val DEMO_STANDINGS = listOf(
            StandingEntry("VER", "204", "red_bull"),
            StandingEntry("NOR", "159", "mclaren"),
            StandingEntry("LEC", "154", "ferrari"),
            StandingEntry("PIA", "126", "mclaren"),
            StandingEntry("HAM", "103", "ferrari"),
            StandingEntry("RUS", "92", "mercedes"),
        )
    }
}
