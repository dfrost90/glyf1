package com.demetrius.f1glyph.glyph

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.demetrius.f1glyph.data.WidgetStateCache
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Process-lifetime owner of a GlyphMatrixManager used for *app-level* matrix
 * pushes (setAppMatrixFrame) — showing F1 info during live sessions without
 * being the user-selected toy. Kept bound between chained worker runs; if the
 * process dies in between, the next push simply reconnects.
 */
object GlyphAppMatrix {

    private const val TAG = "GlyphAppMatrix"
    private const val CONNECT_TIMEOUT_MILLIS = 5_000L

    private val mutex = Mutex()
    private var manager: GlyphMatrixManager? = null

    suspend fun push(context: Context, bitmap: Bitmap) {
        mutex.withLock {
            val gmm = manager
                ?: withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { connect(context) }?.also { manager = it }
                ?: run {
                    Log.w(TAG, "could not connect to glyph service")
                    return
                }
            runCatching {
                val obj = GlyphMatrixObject.Builder()
                    .setImageSource(bitmap)
                    .setPosition(0, 0)
                    .setScale(100)
                    .setBrightness(128)
                    .build()
                val frame = GlyphMatrixFrame.Builder()
                    .addTop(obj)
                    .build(context.applicationContext)
                gmm.setAppMatrixFrame(frame.render())
            }.onSuccess {
                WidgetStateCache(context.applicationContext).setGlyphPushed(true)
                Log.i(TAG, "app matrix frame pushed")
            }.onFailure { Log.w(TAG, "app matrix push failed", it) }
        }
    }

    /**
     * Clears any app-level frame off the matrix. The process routinely dies
     * between the worker's pushes, losing [manager]; the persisted pushed
     * flag tells us a frame may still be latched, so we reconnect just to
     * close it. [force] reconnects even without the flag — used by the QS
     * tile so an explicit "off" always clears the matrix.
     */
    suspend fun release(context: Context, force: Boolean = false) {
        mutex.withLock {
            val cache = WidgetStateCache(context.applicationContext)
            var gmm = manager
            manager = null
            if (gmm == null && (force || cache.glyphPushed())) {
                gmm = withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { connect(context) }
            }
            if (gmm == null) {
                // Flag stays set on reconnect failure so the next release
                // (worker or tile) retries instead of leaving a stuck frame.
                if (force || cache.glyphPushed()) Log.w(TAG, "release: glyph service unreachable")
                return
            }
            val closed = runCatching { gmm.closeAppMatrix() }
                .onSuccess { Log.i(TAG, "app matrix released") }
                .onFailure { Log.w(TAG, "closeAppMatrix failed", it) }
                .isSuccess
            runCatching { gmm.unInit() }
            if (closed) cache.setGlyphPushed(false)
        }
    }

    private suspend fun connect(context: Context): GlyphMatrixManager? =
        suspendCancellableCoroutine { cont ->
            val gmm = GlyphMatrixManager.getInstance(context.applicationContext)
            if (gmm == null) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }
            gmm.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(name: ComponentName?) {
                    runCatching { gmm.register(glyphTargetDevice()) }
                    if (cont.isActive) cont.resume(gmm)
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            })
        }
}
