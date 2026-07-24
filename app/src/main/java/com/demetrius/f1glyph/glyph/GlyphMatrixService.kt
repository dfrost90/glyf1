package com.demetrius.f1glyph.glyph

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

/**
 * Base class for Glyph Toy services. Adapted from Nothing's
 * GlyphMatrix-Example-Project (GlyphMatrixService.kt).
 */
abstract class GlyphMatrixService(private val tag: String) : Service() {

    private val eventHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    msg.data?.getString(KEY_DATA)?.let { value ->
                        when (value) {
                            GlyphToy.EVENT_ACTION_DOWN -> onTouchPointPressed()
                            GlyphToy.EVENT_ACTION_UP -> onTouchPointReleased()
                            GlyphToy.EVENT_CHANGE -> onTouchPointLongPress()
                            GlyphToy.EVENT_AOD -> onAodEvent()
                        }
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    private val serviceMessenger = Messenger(eventHandler)

    var glyphMatrixManager: GlyphMatrixManager? = null
        private set

    private val gmmCallback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            glyphMatrixManager?.let { gmm ->
                gmm.register(glyphTargetDevice())
                performOnServiceConnected(applicationContext, gmm)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    final override fun onBind(intent: Intent?): IBinder? {
        GlyphMatrixManager.getInstance(applicationContext)?.let { gmm ->
            glyphMatrixManager = gmm
            gmm.init(gmmCallback)
        }
        return serviceMessenger.binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        glyphMatrixManager?.let {
            performOnServiceDisconnected(applicationContext)
            it.turnOff()
            it.unInit()
        }
        glyphMatrixManager = null
        return false
    }

    open fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {}
    open fun performOnServiceDisconnected(context: Context) {}
    open fun onTouchPointPressed() {}
    open fun onTouchPointLongPress() {}
    open fun onTouchPointReleased() {}
    open fun onAodEvent() {}

    private companion object {
        const val KEY_DATA = "data"
    }
}

/** The example kit hardcodes Phone (3); pick whatever device we run on. */
internal fun glyphTargetDevice(): String = when {
    Common.is25111p() -> Glyph.DEVICE_25111p
    Common.is25111() -> Glyph.DEVICE_25111
    Common.is24111() -> Glyph.DEVICE_24111
    else -> Glyph.DEVICE_23112
}
