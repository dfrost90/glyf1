package com.demetrius.f1glyph.util

import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.SessionKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

object DisplayFormat {

    /** Timer text for a state already resolved against [nowMillis]. */
    fun widgetTimer(state: F1WidgetState, nowMillis: Long): String {
        if (state.weekend?.isSessionLiveNow == true) return "LIVE"
        if (state.todayResult != null) return "FINISHED"
        val session = state.weekend?.nextSession ?: return "--"
        val remaining = session.epochMillis - nowMillis
        return if (remaining in 1..CountdownTickPlanner.WINDOW_MILLIS) {
            countdownCompact(session.epochMillis, nowMillis)
        } else {
            sessionWhen(session.epochMillis, nowMillis)
        }
    }

    /**
     * Event schedule label for the widget timer row: time-of-day if today,
     * day-of-week if within the next 7 days, otherwise day + month.
     */
    fun sessionWhen(
        epochMillis: Long,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val eventDate = dt.toLocalDate()
        return when {
            eventDate == today ->
                "%02d:%02d".format(dt.hour, dt.minute)
            eventDate.isBefore(today.plusDays(7)) -> {
                val day = dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
                "$day %02d:%02d".format(dt.hour, dt.minute)
            }
            else -> {
                val month = dt.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
                "${dt.dayOfMonth} $month"
            }
        }
    }

    /**
     * Short event date/time for the widget, local time: "SUN 13:00" within
     * the coming week, "5 JUL 13:00" further out.
     */
    fun eventDateTime(
        epochMillis: Long,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): String {
        val dt = Instant.ofEpochMilli(epochMillis).atZone(zone)
        val time = "%02d:%02d".format(dt.hour, dt.minute)
        return if (epochMillis - nowMillis < 6L * 24 * 60 * 60 * 1000) {
            val day = dt.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            "$day $time"
        } else {
            val month = dt.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).uppercase()
            "${dt.dayOfMonth} $month $time"
        }
    }

    fun sessionShortLabel(kind: SessionKind): String = when (kind) {
        SessionKind.FP1 -> "FP1"
        SessionKind.FP2 -> "FP2"
        SessionKind.FP3 -> "FP3"
        SessionKind.SPRINT_QUALIFYING -> "SPR-Q"
        SessionKind.SPRINT -> "SPRINT"
        SessionKind.QUALIFYING -> "QUALI"
        SessionKind.RACE -> "RACE"
    }

    /** Ultra-short code for the Glyph Matrix pre-event face (13px wide). */
    fun sessionCode(kind: SessionKind): String = when (kind) {
        SessionKind.FP1 -> "FP1"
        SessionKind.FP2 -> "FP2"
        SessionKind.FP3 -> "FP3"
        SessionKind.SPRINT_QUALIFYING -> "SQ"
        SessionKind.SPRINT -> "S"
        SessionKind.QUALIFYING -> "Q"
        SessionKind.RACE -> "R"
    }

    fun sessionFullLabel(kind: SessionKind): String = when (kind) {
        SessionKind.FP1 -> "PRACTICE 1"
        SessionKind.FP2 -> "PRACTICE 2"
        SessionKind.FP3 -> "PRACTICE 3"
        SessionKind.SPRINT_QUALIFYING -> "SPRINT QUALI"
        SessionKind.SPRINT -> "SPRINT"
        SessionKind.QUALIFYING -> "QUALIFYING"
        SessionKind.RACE -> "RACE"
    }

    /** Two short strings for the dot-matrix panel: e.g. ("QUALI", "VER") or ("RACE", "-2H14M") */
    fun matrixPanelText(state: F1WidgetState): Pair<String, String> {
        val weekend = state.weekend
        val session = weekend?.nextSession

        val top = session?.let { sessionShortLabel(it.kind) } ?: "F1"
        val bottom = state.leader?.label ?: "--"

        return top to bottom
    }

    /** Very short relative time for the Glyph Matrix, e.g. "-2D5H", "-3H12M", "-42M", "LIVE". */
    fun compactCountdown(state: F1WidgetState, nowMillis: Long): String {
        val weekend = state.weekend ?: return "--"
        if (weekend.isSessionLiveNow) return "LIVE"
        val session = weekend.nextSession ?: return "--"
        val delta = session.epochMillis - nowMillis
        if (delta <= 0) return "NOW"
        val totalMinutes = delta / 60_000
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "-${days}D${hours}H"
            hours > 0 -> "-${hours}H${minutes}M"
            else -> "-${minutes}M"
        }
    }

    /**
     * Event label above the timer/date row, e.g. "QUALIFYING" — no prefix,
     * since the row below can be a date, a countdown, or elapsed time.
     * When a result exists (finished state), reflects the session that ended.
     */
    fun countdownCaption(state: F1WidgetState): String {
        state.todayResult?.let { result ->
            return when (result.sessionLabel) {
                "GP"  -> "GRAND PRIX"
                "Q3"  -> "QUALIFYING"
                "S"   -> "SPRINT"
                "SQ3" -> "SPRINT QUALIFYING"
                else  -> "FINISHED"
            }
        }
        val weekend = state.weekend ?: return "NO DATA"
        val session = weekend.nextSession ?: return "SCHEDULE TBC"
        return sessionFullLabel(session.kind)
    }

    /**
     * Countdown for the widget timer row inside the final minutes before a
     * session, e.g. "-9M". Ceiling, so a tick that runs late still shows the
     * minute that is actually left.
     */
    fun countdownCompact(startMillis: Long, nowMillis: Long): String {
        val minutes = ((startMillis - nowMillis + 59_999) / 60_000).coerceAtLeast(0)
        return "-${minutes}M"
    }

    /** Elapsed session time for the live timer row, e.g. "+1H12M" / "+42M". */
    fun elapsedCompact(startMillis: Long, nowMillis: Long): String {
        val totalMinutes = ((nowMillis - startMillis) / 60_000).coerceAtLeast(0)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "+${hours}H${minutes}M" else "+${minutes}M"
    }
}
