package com.demetrius.f1glyph.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import com.demetrius.f1glyph.R
import com.demetrius.f1glyph.data.WidgetStateCache
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Opened only by the launcher's widget configuration action; no app icon. */
class WidgetConfigurationActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(this)
        val provider = manager.getAppWidgetInfo(id)?.provider
        if (provider?.packageName != packageName) {
            finish()
            return
        }
        setContentView(R.layout.widget_configuration)
        val choices = findViewById<RadioGroup>(R.id.widget_appearance_choices)
        // Android restores the radio selection after onCreate on rotation.
        if (savedInstanceState == null) {
            choices.check(if (WidgetAppearance.isTranslucent(this, id))
                R.id.appearance_translucent else R.id.appearance_solid)
        }
        findViewById<Button>(R.id.appearance_cancel).setOnClickListener { finish() }
        findViewById<Button>(R.id.appearance_save).setOnClickListener { save ->
            save.isEnabled = false
            val translucent = choices.checkedRadioButtonId == R.id.appearance_translucent
            scope.launch {
                val state = WidgetStateCache(this@WidgetConfigurationActivity).load()
                WidgetAppearance.setTranslucent(this@WidgetConfigurationActivity, id, translucent)
                manager.updateAppWidget(id, F1WidgetProvider.buildViews(
                    this@WidgetConfigurationActivity, manager, id, state
                ))
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
                finish()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
