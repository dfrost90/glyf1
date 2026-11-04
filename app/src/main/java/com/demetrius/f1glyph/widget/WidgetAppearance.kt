package com.demetrius.f1glyph.widget

import android.content.Context

/** Appearance is per widget; content refreshes never overwrite the selection. */
internal object WidgetAppearance {
    private fun preferences(context: Context) =
        context.getSharedPreferences("widget_appearance", Context.MODE_PRIVATE)

    fun isTranslucent(context: Context, id: Int): Boolean =
        preferences(context).getBoolean("translucent_$id", false)

    fun setTranslucent(context: Context, id: Int, translucent: Boolean) {
        preferences(context).edit().putBoolean("translucent_$id", translucent).apply()
    }

    fun delete(context: Context, ids: IntArray) {
        preferences(context).edit().apply {
            ids.forEach { remove("translucent_$it") }
        }.apply()
    }
}
