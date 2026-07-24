package com.demetrius.f1glyph.glyph

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.demetrius.f1glyph.R
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.work.LiveMatrixWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Quick Settings toggle for the automatic Glyph Matrix takeover during live
 * sessions (event + leader + progress on the matrix). Replaces the in-app
 * button so the app needs no launcher entry at all.
 */
class GlyphToggleTileService : TileService() {

    // Outlives the tile (app-process scope): the release can spend up to 5s
    // rebinding the glyph service, too long for the main-thread onClick.
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        updateTile(readEnabled())
    }

    override fun onClick() {
        val cache = WidgetStateCache(applicationContext)
        val enabled = runBlocking {
            val next = !cache.autoGlyphEnabled()
            cache.setAutoGlyph(next)
            next
        }
        android.util.Log.i("GlyphToggleTile", "toggled auto glyph -> $enabled")
        if (enabled) {
            LiveMatrixWorker.scheduleNext(applicationContext)
        } else {
            LiveMatrixWorker.cancel(applicationContext)
            val appContext = applicationContext
            releaseScope.launch { GlyphAppMatrix.release(appContext, force = true) }
        }
        updateTile(enabled)
    }

    private fun readEnabled(): Boolean =
        runBlocking { WidgetStateCache(applicationContext).autoGlyphEnabled() }

    private fun updateTile(enabled: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = getString(
            if (enabled) R.string.tile_on_subtitle else R.string.tile_off_subtitle
        )
        tile.updateTile()
    }
}
