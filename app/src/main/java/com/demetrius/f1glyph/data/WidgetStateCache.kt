package com.demetrius.f1glyph.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "f1_widget_cache")

/**
 * The widget's WorkManager job is the only thing that talks to the network.
 * Both the widget RemoteViews render and the Glyph Toy read from this cache,
 * so the Glyph Toy (which can be redrawn frequently while active) stays cheap.
 */
class WidgetStateCache(private val context: Context) {

    private object Keys {
        val GP_NAME = stringPreferencesKey("gp_name")
        val CIRCUIT = stringPreferencesKey("circuit")
        val COUNTRY = stringPreferencesKey("country")
        val ROUND = stringPreferencesKey("round")
        val SESSION_KIND = stringPreferencesKey("session_kind")
        val SESSION_EPOCH = longPreferencesKey("session_epoch")
        val SESSIONS = stringPreferencesKey("sessions")
        val IS_LIVE = stringPreferencesKey("is_live")
        val LEADER_LABEL = stringPreferencesKey("leader_label")
        val FETCHED_AT = longPreferencesKey("fetched_at")
        val AUTO_GLYPH = booleanPreferencesKey("auto_glyph")
        val TOP_STANDINGS = stringPreferencesKey("top_standings")
        val GLYPH_PUSHED = booleanPreferencesKey("glyph_pushed")
        val SESSION_RESULT_CODE = stringPreferencesKey("result_code")
        val SESSION_RESULT_LABEL = stringPreferencesKey("result_label")
        val SESSION_RESULT_EPOCH = longPreferencesKey("result_epoch")
        val SESSION_RESULT_ROUND = stringPreferencesKey("result_round")
        val SESSION_RESULT_GP_NAME = stringPreferencesKey("result_gp_name")
    }

    /** Automatic live-session matrix takeover, default ON. */
    suspend fun autoGlyphEnabled(): Boolean =
        context.dataStore.data.first()[Keys.AUTO_GLYPH] ?: true

    suspend fun setAutoGlyph(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_GLYPH] = enabled }
    }

    /**
     * Whether an app-level frame is (or may be) latched on the Glyph Matrix.
     * Persisted because Nothing OS kills the process between worker runs, so
     * an in-memory flag would forget that a frame still needs releasing.
     */
    suspend fun glyphPushed(): Boolean =
        context.dataStore.data.first()[Keys.GLYPH_PUSHED] ?: false

    suspend fun setGlyphPushed(pushed: Boolean) {
        context.dataStore.edit { it[Keys.GLYPH_PUSHED] = pushed }
    }

    suspend fun save(state: F1WidgetState) {
        context.dataStore.edit { p ->
            state.weekend?.let { w ->
                p[Keys.GP_NAME] = w.gpName
                p[Keys.CIRCUIT] = w.circuitName
                p[Keys.COUNTRY] = w.country
                p[Keys.ROUND] = w.round.toString()
                p[Keys.IS_LIVE] = w.isSessionLiveNow.toString()
                w.nextSession?.let { s ->
                    p[Keys.SESSION_KIND] = s.kind.name
                    p[Keys.SESSION_EPOCH] = s.epochMillis
                }
                if (w.sessions.isNotEmpty()) {
                    p[Keys.SESSIONS] = SessionsCodec.encode(w.sessions)
                }
            }
            state.leader?.let { p[Keys.LEADER_LABEL] = it.label }
            if (state.topStandings.isNotEmpty()) {
                p[Keys.TOP_STANDINGS] = StandingsCodec.encode(state.topStandings)
            }
            p[Keys.FETCHED_AT] = state.fetchedAtMillis
            state.todayResult?.let { r ->
                p[Keys.SESSION_RESULT_CODE] = r.driverCode
                p[Keys.SESSION_RESULT_LABEL] = r.sessionLabel
                p[Keys.SESSION_RESULT_EPOCH] = r.sessionEpochMillis
                p[Keys.SESSION_RESULT_ROUND] = r.round.toString()
                p[Keys.SESSION_RESULT_GP_NAME] = r.gpName
            } ?: run {
                p.remove(Keys.SESSION_RESULT_CODE)
                p.remove(Keys.SESSION_RESULT_LABEL)
                p.remove(Keys.SESSION_RESULT_EPOCH)
                p.remove(Keys.SESSION_RESULT_ROUND)
                p.remove(Keys.SESSION_RESULT_GP_NAME)
            }
        }
    }

    suspend fun load(): F1WidgetState {
        val p = context.dataStore.data.first()
        val kind = p[Keys.SESSION_KIND]?.let { runCatching { SessionKind.valueOf(it) }.getOrNull() }
        val epoch = p[Keys.SESSION_EPOCH]
        val nextSession = if (kind != null && epoch != null) UpcomingSession(kind, epoch) else null
        // Migration: caches written before SESSIONS existed only have the single
        // nextSession — fall back to it so those renders still show something.
        val sessions = p[Keys.SESSIONS]?.let { SessionsCodec.decode(it) }
            ?: listOfNotNull(nextSession)
        val weekend = p[Keys.GP_NAME]?.let { name ->
            RaceWeekend(
                round = p[Keys.ROUND]?.toIntOrNull() ?: 0,
                gpName = name,
                circuitName = p[Keys.CIRCUIT].orEmpty(),
                country = p[Keys.COUNTRY].orEmpty(),
                nextSession = nextSession,
                isSessionLiveNow = p[Keys.IS_LIVE]?.toBoolean() ?: false,
                sessions = sessions
            )
        }
        val leader = p[Keys.LEADER_LABEL]?.let { LeaderInfo(it) }
        val topStandings = p[Keys.TOP_STANDINGS]?.let { StandingsCodec.decode(it) }.orEmpty()
        val todayResult = p[Keys.SESSION_RESULT_CODE]?.let { code ->
            val label = p[Keys.SESSION_RESULT_LABEL] ?: return@let null
            val resultEpoch = p[Keys.SESSION_RESULT_EPOCH] ?: return@let null
            SessionResult(
                driverCode = code,
                sessionLabel = label,
                sessionEpochMillis = resultEpoch,
                round = p[Keys.SESSION_RESULT_ROUND]?.toIntOrNull() ?: 0,
                gpName = p[Keys.SESSION_RESULT_GP_NAME].orEmpty()
            )
        }
        return F1WidgetState(
            weekend, leader, p[Keys.FETCHED_AT] ?: 0L, topStandings,
            todayResult = todayResult
        )
    }
}
