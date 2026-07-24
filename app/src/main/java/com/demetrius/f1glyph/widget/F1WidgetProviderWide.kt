package com.demetrius.f1glyph.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.demetrius.f1glyph.work.RefreshWorker

class F1WidgetProviderWide : F1WidgetProvider() {

    override fun onDisabled(context: Context) {
        val awm = AppWidgetManager.getInstance(context)
        val compactStillPresent = awm
            .getAppWidgetIds(ComponentName(context, F1WidgetProvider::class.java))
            .isNotEmpty()
        if (!compactStillPresent) RefreshWorker.cancelPeriodic(context)
    }
}
