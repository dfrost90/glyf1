package com.demetrius.f1glyph.widget

import android.graphics.drawable.BitmapDrawable
import android.os.Parcel
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import com.demetrius.f1glyph.R
import com.demetrius.f1glyph.data.F1WidgetState
import com.demetrius.f1glyph.data.RaceWeekend
import com.demetrius.f1glyph.data.SessionKind
import com.demetrius.f1glyph.data.SessionResult
import com.demetrius.f1glyph.data.StandingEntry
import com.demetrius.f1glyph.data.UpcomingSession
import com.demetrius.f1glyph.util.TeamColors
import com.demetrius.f1glyph.util.WidgetPalette
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "notnight")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetThemeTest {
    private val now = Instant.parse("2026-09-13T18:00:00Z").toEpochMilli()
    private val empty = F1WidgetState(null, null, now)
    private val standings = listOf(StandingEntry("NOR", "200", "mclaren"))

    @Test fun compactUpcoming() = checkCachedThemes(wide = false, state = upcoming())
    @Test fun wideUpcoming() = checkCachedThemes(wide = true, state = upcoming())
    @Test fun compactLive() = checkCachedThemes(wide = false, state = live())
    @Test fun wideLive() = checkCachedThemes(wide = true, state = live())
    @Test fun compactFinished() = checkCachedThemes(wide = false, state = finished())
    @Test fun wideFinished() = checkCachedThemes(wide = true, state = finished())
    @Test fun compactNoData() = checkCachedThemes(wide = false, state = empty)
    @Test fun wideNoData() = checkCachedThemes(wide = true, state = empty)

    private fun checkCachedThemes(wide: Boolean, state: F1WidgetState) {
        val context = RuntimeEnvironment.getApplication()
        // Build and parcel just once, as the widget service does before caching
        // RemoteViews. Theme changes below never call the provider or renderer.
        val cached = parcelCopy(F1WidgetProvider.buildViews(
            context, if (wide) 320 else 160, 180, state, now
        ))
        val root = cached.apply(context, FrameLayout(context))
        val palettes = listOf(WidgetPalette.LIGHT, WidgetPalette.DARK, WidgetPalette.LIGHT)
        for (palette in palettes) {
            RuntimeEnvironment.setQualifiers(
                if (palette == WidgetPalette.DARK) "+night" else "+notnight"
            )
            // Cover both launcher paths: reuse an existing view, or reinflate it.
            cached.reapply(context, root)
            checkPanels(root, palette, wide, state)
            checkPanels(cached.apply(context, FrameLayout(context)), palette, wide, state)
        }
    }

    private fun checkPanels(root: View, palette: WidgetPalette, wide: Boolean, state: F1WidgetState) {
        assertContainsColor(root, R.id.header_panel, palette.primary)
        assertContainsColor(root, R.id.caption_panel, palette.secondary)
        val live = state.weekend?.isSessionLiveNow == true
        assertContainsColor(root, R.id.event_time_panel,
            if (live || !wide) palette.accent else palette.primary)
        val result = state.todayResult != null
        assertContainsColor(root, R.id.info_panel,
            if (wide && result) palette.yellow else palette.primary)
        if (wide) {
            val winner = root.findViewById<ImageView>(R.id.winner_name_panel)
            assertEquals(if (result) View.VISIBLE else View.GONE, winner.visibility)
            if (result) assertContainsColor(root, R.id.winner_name_panel, palette.primary)
        }
        if (!live && !result && state.topStandings.isNotEmpty()) {
            assertContainsColor(root, R.id.info_panel, TeamColors.of("mclaren"))
        }
        assertTrue(root.findViewById<ImageView>(R.id.header_panel).contentDescription.isNotBlank())
    }

    private fun assertContainsColor(root: View, id: Int, color: Int) {
        val panel = root.findViewById<ImageView>(id)
        val bitmap = (panel.drawable as BitmapDrawable).bitmap
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        assertTrue("${root.resources.getResourceEntryName(id)} missing ${Integer.toHexString(color)}",
            pixels.any { it == color })
    }

    private fun parcelCopy(views: RemoteViews): RemoteViews {
        val parcel = Parcel.obtain()
        return try {
            views.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            RemoteViews.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    private fun upcoming(): F1WidgetState = withSession(now + 86_400_000L, live = false)
    private fun live(): F1WidgetState = withSession(now - 1_800_000L, live = true)
    private fun finished(): F1WidgetState = upcoming().copy(
        todayResult = SessionResult("NOR", "GP", now - 14_400_000L, 16, "Italian Grand Prix")
    )

    private fun withSession(start: Long, live: Boolean): F1WidgetState {
        val session = UpcomingSession(SessionKind.RACE, start)
        return empty.copy(
            weekend = RaceWeekend(16, "Italian Grand Prix", "Monza", "Italy",
                session, live, listOf(session)),
            topStandings = standings
        )
    }
}
