package com.demetrius.f1glyph.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.view.View
import android.widget.RemoteViews
import com.demetrius.f1glyph.R
import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.util.DisplayFormat
import com.demetrius.f1glyph.util.resolvedAt
import com.demetrius.f1glyph.util.MatrixRenderer
import com.demetrius.f1glyph.util.StandingsRenderer
import com.demetrius.f1glyph.util.TextPanelRenderer
import com.demetrius.f1glyph.util.WidgetPalette
import com.demetrius.f1glyph.util.WidgetPanelGeometry
import com.demetrius.f1glyph.work.RefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

open class F1WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val state = WidgetStateCache(context).load()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, appWidgetManager, id, state))
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onEnabled(context: Context) {
        RefreshWorker.schedulePeriodic(context)
        RefreshWorker.refreshNow(context)
    }

    override fun onDisabled(context: Context) {
        if (widgetIds(context).isEmpty()) RefreshWorker.cancelPeriodic(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MANUAL_REFRESH) {
            RefreshWorker.refreshNow(context)
        }
    }

    companion object {
        const val ACTION_MANUAL_REFRESH = "com.demetrius.f1glyph.ACTION_MANUAL_REFRESH"
        private const val WIDE_MIN_WIDTH_DP = 180

        /**
         * The launcher caches both images and picks the matching one when it
         * (re)applies RemoteViews after a theme change, even if our process is
         * asleep or gone. A single bitmap freezes its colors until a refresh.
         */
        private inline fun RemoteViews.setThemedImage(
            viewId: Int,
            render: (WidgetPalette) -> Bitmap
        ) {
            setIcon(
                viewId, "setImageIcon",
                Icon.createWithBitmap(render(WidgetPalette.LIGHT)),
                Icon.createWithBitmap(render(WidgetPalette.DARK))
            )
        }

        private fun widgetIds(context: Context): IntArray {
            val manager = AppWidgetManager.getInstance(context)
            return manager.getAppWidgetIds(ComponentName(context, F1WidgetProvider::class.java)) +
                manager.getAppWidgetIds(ComponentName(context, F1WidgetProviderWide::class.java))
        }

        /** Called by RefreshWorker after the cache is updated. */
        suspend fun renderAll(context: Context) {
            val awm = AppWidgetManager.getInstance(context)
            val ids = widgetIds(context)
            if (ids.isEmpty()) return
            val state = WidgetStateCache(context).load()
            ids.forEach { id -> awm.updateAppWidget(id, buildViews(context, awm, id, state)) }
        }

        private fun buildViews(
            context: Context,
            awm: AppWidgetManager,
            appWidgetId: Int,
            rawState: F1WidgetState
        ): RemoteViews {
            val options = awm.getAppWidgetOptions(appWidgetId)
            return buildViews(
                context,
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                rawState
            )
        }

        internal fun buildViews(
            context: Context,
            minWidthDp: Int,
            maxHeightDp: Int,
            rawState: F1WidgetState,
            now: Long = System.currentTimeMillis()
        ): RemoteViews {
            // Resolve which session is live/next for the current clock, so a
            // just-finished session doesn't linger as "upcoming" until the next
            // network fetch (see SessionSelection.resolvedAt).
            val state = rawState.resolvedAt(now)
            val wide = minWidthDp >= WIDE_MIN_WIDTH_DP
            val views = RemoteViews(
                context.packageName,
                if (wide) R.layout.widget_wide else R.layout.widget_compact
            )

            val res = context.resources
            val ndot = res.getFont(R.font.ndot57)
            val ntype = res.getFont(R.font.ntype82)

            val weekend = state.weekend
            val session = weekend?.nextSession
            // nextSession / isSessionLiveNow were resolved for `now` above.
            val liveNow = weekend?.isSessionLiveNow == true

            val entries = state.topStandings
            val todayResult = state.todayResult

            // --- Left pane text (bitmaps: Nothing fonts don't load in
            // RemoteViews TextViews, see TextPanelRenderer) ---
            val contentWDp = (minWidthDp - 40).coerceAtLeast(80)
            // Wide left column: weight 1.1 of 2.1 (~52%) minus its 16dp paddingEnd
            val leftColWDp = if (wide) ((contentWDp * 0.52f).toInt() - 16).coerceAtLeast(60) else contentWDp

            // When showing a finished result, display that race's round/name — not
            // the next event's, which getNextRace() already returns post-race.
            val displayRound = if (todayResult != null && todayResult.round > 0) todayResult.round
                               else weekend?.round
            val displayGpName = if (todayResult != null && todayResult.gpName.isNotEmpty()) todayResult.gpName
                                 else weekend?.gpName ?: "F1 — no data yet"

            views.setThemedImage(R.id.header_panel) { palette ->
                if (wide) {
                    TextPanelRenderer.wideHeader(
                        displayRound?.let { "ROUND $it" }.orEmpty(),
                        displayGpName,
                        if (todayResult != null) "" else weekend?.circuitName.orEmpty(),
                        ndot, ntype, leftColWDp, palette
                    )
                } else {
                    // Split "British Grand Prix" → "British" / "Grand Prix" so the
                    // title always occupies two lines in the compact widget.
                    val gpIdx = displayGpName.indexOf(" Grand Prix")
                    if (gpIdx > 0) {
                        TextPanelRenderer.twoLine(
                            displayGpName.substring(0, gpIdx), "Grand Prix",
                            16f, palette.primary, ntype, leftColWDp
                        )
                    } else {
                        TextPanelRenderer.line(displayGpName, 16f, palette.primary, ntype, leftColWDp)
                    }
                }
            }
            views.setContentDescription(R.id.header_panel,
                listOfNotNull(displayRound?.let { "Round $it" }, displayGpName).joinToString(", "))
            val caption = DisplayFormat.countdownCaption(state)
            views.setContentDescription(R.id.caption_panel, caption)
            views.setThemedImage(R.id.caption_panel) { palette ->
                TextPanelRenderer.line(caption, 12f, palette.secondary, ndot, leftColWDp)
            }

            // --- Timer row, Ndot bitmap: "LIVE" during sessions; time-of-day
            // if the event is today; day-of-week if within the next 7 days;
            // day+month otherwise. ---
            val timerSizeDp = if (wide) 25f else 20f
            val timerText = DisplayFormat.widgetTimer(state, now)
            views.setContentDescription(R.id.event_time_panel, timerText)
            views.setThemedImage(R.id.event_time_panel) { palette ->
                val timerColor = if (liveNow || !wide) palette.accent else palette.primary
                TextPanelRenderer.line(timerText, timerSizeDp, timerColor, ndot, leftColWDp)
            }

            // Winner name row: "VER WINS" in Ndot, above the matrix (wide finished only).
            // winner_name_panel only exists in widget_wide.xml; compact ignores these calls.
            val rightColWDp = (contentWDp * 0.45f).toInt().coerceAtLeast(60)
            if (wide) {
                val showWinner = todayResult != null
                views.setViewVisibility(R.id.winner_name_panel,
                    if (showWinner) View.VISIBLE else View.GONE)
                if (todayResult != null) {
                    val winnerText = "${todayResult.driverCode} ${todayResult.action}"
                    views.setContentDescription(R.id.winner_name_panel, winnerText)
                    views.setThemedImage(R.id.winner_name_panel) { palette ->
                        TextPanelRenderer.centeredLine(
                            winnerText,
                            20f, palette.primary, ndot, rightColWDp
                        )
                    }
                }
            }

            // --- Right/lower panel: F1 car dot art + progress row when live
            // (wide only); championship standings otherwise; dot matrix until
            // the first fetch lands. ---
            views.setThemedImage(R.id.info_panel) { palette ->
                when {
                    wide && liveNow -> {
                        val aspect = WidgetPanelGeometry.widePanelAspect(minWidthDp, maxHeightDp)
                        val liveLabel = when (session?.kind) {
                            SessionKind.RACE              -> "RACE"
                            SessionKind.QUALIFYING        -> "QUAL"
                            SessionKind.SPRINT_QUALIFYING -> "SQUAL"
                            SessionKind.SPRINT            -> "SPR"
                            SessionKind.FP1               -> "FP1"
                            SessionKind.FP2               -> "FP2"
                            SessionKind.FP3               -> "FP3"
                            else                          -> "LIVE"
                        }
                        StandingsRenderer.renderLiveArt(
                            (300 * aspect).toInt(), 300,
                            label = liveLabel,
                            palette = palette
                        )
                    }
                    wide && todayResult != null -> {
                        val aspect = WidgetPanelGeometry.widePanelAspect(minWidthDp, maxHeightDp)
                        StandingsRenderer.renderResult(
                            (300 * aspect).toInt(), 300,
                            driverCode = todayResult.driverCode,
                            action = todayResult.action,
                            typeface = ndot,
                            palette = palette
                        )
                    }
                    !wide && todayResult != null -> {
                        TextPanelRenderer.line(
                            "${todayResult.driverCode} ${todayResult.action}",
                            20f, palette.primary, ndot, contentWDp
                        )
                    }
                    !wide && liveNow -> {
                        val aspect = WidgetPanelGeometry.standingsPanelAspect(minWidthDp, maxHeightDp)
                        StandingsRenderer.renderLiveArtCompact(
                            (300 * aspect).toInt(), 300,
                            palette = palette
                        )
                    }
                    entries.isNotEmpty() -> if (wide) {
                        val aspect = WidgetPanelGeometry.widePanelAspect(minWidthDp, maxHeightDp)
                        StandingsRenderer.render(
                            (300 * aspect).toInt(), 300, entries,
                            maxRows = 6,
                            dotRadiusCells = 0.15f,
                            dotStepCells = 0.85f,
                            typeface = ndot,
                            frameDots = true,
                            frameDotRadiusCells = 0.20f,
                            frameBottom = false,
                            progressFraction = null,
                            progressLabel = null,
                            title = "STANDINGS",
                            palette = palette
                        )
                    } else {
                        val aspect = WidgetPanelGeometry.standingsPanelAspect(minWidthDp, maxHeightDp)
                        StandingsRenderer.render(
                            (300 * aspect).toInt(), 300, entries,
                            maxRows = 3,
                            typeface = ndot,
                            palette = palette
                        )
                    }
                    else -> {
                        val (top, bottom) = DisplayFormat.matrixPanelText(state)
                        MatrixRenderer.renderWidgetPanel(300, 300, top, bottom, palette = palette)
                    }
                }
            }
            views.setContentDescription(R.id.info_panel, when {
                liveNow -> "${DisplayFormat.countdownCaption(state)} live"
                todayResult != null -> "${todayResult.driverCode} ${todayResult.action}"
                entries.isNotEmpty() -> "Championship standings: " + entries.mapIndexed { index, entry ->
                    "${index + 1}. ${entry.code}, ${entry.points} points"
                }.take(if (wide) 6 else 3).joinToString("; ")
                else -> "No standings available"
            })

            val refreshIntent = Intent(context, F1WidgetProvider::class.java)
                .setAction(ACTION_MANUAL_REFRESH)
            val pi = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            return views
        }
    }
}
