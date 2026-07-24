package com.demetrius.f1glyph.util

import android.graphics.Bitmap
import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.SessionKind
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Picks what the Glyph Matrix shows based on session timing:
 *
 *  Post-session (until midnight UTC)  → driver code (top) + label (bottom): "VER" / "GP"
 *  Live session                       → spinning 5-spoke wheel
 *  No event / >7 days                 → WDC top-3 standings cycling every 3 s: "VER" / "150"
 *  Race week (any day, not yet live)  → session code (top) + countdown (bottom): "GP" / "-1D"
 */
object GlyphFace {

    private val SEVEN_DAYS = 7 * 24 * 60 * 60 * 1000L

    fun render(rawState: F1WidgetState, nowMillis: Long, gridSize: Int): Bitmap {
        // Resolve live/next session for the current clock (see resolvedAt).
        val state = rawState.resolvedAt(nowMillis)
        val session = state.weekend?.nextSession

        // ── Post-session result (until midnight UTC) ───────────────────────
        val result = state.todayResult
        if (result != null) {
            val resultDay = Instant.ofEpochMilli(result.sessionEpochMillis)
                .atZone(ZoneOffset.UTC).toLocalDate()
            val todayUtc = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate()
            if (resultDay == todayUtc) {
                return MatrixRenderer.renderScheduleFace(
                    result.driverCode, 0, result.sessionLabel, gridSize
                )
            }
        }

        // ── Live session → spinning wheel ──────────────────────────────────
        if (session != null &&
            nowMillis >= session.epochMillis &&
            nowMillis < session.epochMillis + SessionWindow.liveWindowMillis(session.kind)
        ) {
            val frame = ((nowMillis / 200) % 8).toInt()
            return MatrixRenderer.renderWheelBitmap(spinning = true, frameIndex = frame, gridSize = gridSize)
        }

        // ── No event or far away → WDC standings cycle ─────────────────────
        if (session == null || session.epochMillis - nowMillis > SEVEN_DAYS) {
            val standings = state.topStandings.take(3)
            if (standings.isEmpty()) {
                return MatrixRenderer.renderWheelBitmap(spinning = false, gridSize = gridSize)
            }
            val idx = ((nowMillis / 3000) % standings.size).toInt()
            val entry = standings[idx]
            return MatrixRenderer.renderScheduleFace(
                entry.code, 0, entry.points.substringBefore('.'), gridSize
            )
        }

        // ── Race week: code (top) + countdown (bottom) ─────────────────────
        // Always ≤3 chars so text stays inside the circular matrix boundary.
        // No leading dash — session code on top already implies "time until".
        val code = when (session.kind) {
            SessionKind.RACE -> "GP"
            SessionKind.QUALIFYING -> "Q"
            SessionKind.SPRINT_QUALIFYING -> "SQ"
            SessionKind.SPRINT -> "S"
            else -> DisplayFormat.sessionCode(session.kind)
        }
        val toSession = session.epochMillis - nowMillis
        val countdown = when {
            toSession <= 0           -> "NOW"               // 3 chars
            toSession >= 86_400_000L -> "${toSession / 86_400_000}D"  // "1D"–"7D"
            toSession >= 3_600_000L  -> "${toSession / 3_600_000}H"   // "1H"–"23H"
            else                     -> "${toSession / 60_000}M"      // "1M"–"59M"
        }
        return MatrixRenderer.renderScheduleFace(code, 0, countdown, gridSize)
    }
}
