package com.demetrius.f1glyph.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioGroup
import com.demetrius.f1glyph.R
import com.demetrius.f1glyph.data.F1WidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "notnight")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetAppearanceTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun `appearance survives content refresh independently per widget and resets on deletion`() {
        val manager = AppWidgetManager.getInstance(context)
        WidgetAppearance.setTranslucent(context, 10, true)
        val empty = F1WidgetState(null, null, 0L)
        fun background(id: Int): Int {
            val views = F1WidgetProvider.buildViews(context, manager, id, empty)
            return views.apply(context, FrameLayout(context)).backgroundTintList!!.defaultColor
        }
        assertEquals(context.getColor(R.color.widget_bg_translucent), background(10))
        assertEquals(context.getColor(R.color.widget_bg), background(11))
        // A later rebuild still uses the stored appearance.
        assertEquals(context.getColor(R.color.widget_bg_translucent), background(10))
        F1WidgetProviderWide().onDeleted(context, intArrayOf(10))
        assertEquals(context.getColor(R.color.widget_bg), background(10))
    }

    @Test fun `configuration restores saved choice and cancel discards edits`() {
        val id = 10
        val info = AppWidgetProviderInfo().apply {
            provider = ComponentName(context, F1WidgetProvider::class.java)
        }
        shadowOf(AppWidgetManager.getInstance(context)).addBoundWidget(id, info)
        WidgetAppearance.setTranslucent(context, id, true)
        val intent = Intent(context, WidgetConfigurationActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        val controller = Robolectric.buildActivity(WidgetConfigurationActivity::class.java, intent).setup()
        val activity = controller.get()
        val choices = activity.findViewById<RadioGroup>(R.id.widget_appearance_choices)
        assertEquals(R.id.appearance_translucent, choices.checkedRadioButtonId)
        choices.check(R.id.appearance_solid)
        activity.findViewById<Button>(R.id.appearance_cancel).performClick()
        assertTrue(activity.isFinishing)
        assertTrue(WidgetAppearance.isTranslucent(context, id))
        controller.pause().stop().destroy()
    }

    @Test fun `configuration rejects an invalid widget id`() {
        val controller = Robolectric.buildActivity(WidgetConfigurationActivity::class.java).setup()
        assertTrue(controller.get().isFinishing)
        assertFalse(WidgetAppearance.isTranslucent(context, AppWidgetManager.INVALID_APPWIDGET_ID))
        controller.pause().stop().destroy()
    }
}
