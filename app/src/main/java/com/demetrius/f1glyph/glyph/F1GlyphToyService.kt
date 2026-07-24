package com.demetrius.f1glyph.glyph

import android.content.Context
import android.util.Log
import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.util.GlyphFace
import com.demetrius.f1glyph.util.SessionWindow
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

/**
 * Glyph Toy: shows context-aware F1 info on the Glyph Matrix.
 * Loop cadence scales with phase: 200 ms live (spinner), 350 ms race week
 * (marquee / countdown), 1 s standings cycle, 30 s post-session, 60 s idle.
 */
class F1GlyphToyService : GlyphMatrixService("F1-Toy") {

    private var scope: CoroutineScope? = null

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()).also { s ->
            s.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val state = WidgetStateCache(applicationContext).load()
                    redraw(state, now)
                    delay(loopDelay(state, now))
                }
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        scope?.cancel()
        scope = null
    }

    override fun onTouchPointReleased() {
        scope?.launch {
            redraw(WidgetStateCache(applicationContext).load(), System.currentTimeMillis())
        }
    }

    override fun onTouchPointLongPress() {
        scope?.launch {
            redraw(WidgetStateCache(applicationContext).load(), System.currentTimeMillis())
        }
    }

    override fun onAodEvent() {
        scope?.launch {
            redraw(WidgetStateCache(applicationContext).load(), System.currentTimeMillis())
        }
    }

    private fun loopDelay(state: F1WidgetState, nowMillis: Long): Long {
        // Post-session result: static display, check every 30 s for midnight rollover
        val result = state.todayResult
        if (result != null) {
            val resultDay = Instant.ofEpochMilli(result.sessionEpochMillis)
                .atZone(ZoneOffset.UTC).toLocalDate()
            val todayUtc = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC).toLocalDate()
            if (resultDay == todayUtc) return 30_000L
        }

        val session = state.weekend?.nextSession
        if (session == null || session.epochMillis - nowMillis > 7 * 24 * 60 * 60 * 1000L) {
            // Standings cycle: tick every 1 s so transitions happen within 1 s of the 3 s boundary
            return if (state.topStandings.isNotEmpty()) 1_000L else 60_000L
        }

        val isLive = nowMillis >= session.epochMillis &&
            nowMillis < session.epochMillis + SessionWindow.liveWindowMillis(session.kind)
        if (isLive) return 200L

        return 60_000L // race week countdown (static, updates once per minute is enough)
    }

    private suspend fun redraw(state: F1WidgetState, now: Long) {
        val gmm = glyphMatrixManager ?: return
        val gridSize = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
        val bitmap = GlyphFace.render(state, now, gridSize)
        runCatching {
            val obj = GlyphMatrixObject.Builder()
                .setImageSource(bitmap)
                .setPosition(0, 0)
                .setScale(100)
                .setBrightness(128)
                .build()
            val frame = GlyphMatrixFrame.Builder()
                .addTop(obj)
                .build(applicationContext)
            gmm.setMatrixFrame(frame.render())
        }.onFailure { Log.w(TAG, "glyph frame update failed", it) }
    }

    private companion object {
        const val TAG = "F1GlyphToyService"
    }
}
