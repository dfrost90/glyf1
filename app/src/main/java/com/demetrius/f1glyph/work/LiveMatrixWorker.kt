package com.demetrius.f1glyph.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.demetrius.f1glyph.data.F1Repository
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.glyph.GlyphAppMatrix
import com.demetrius.f1glyph.util.GlyphFace
import com.demetrius.f1glyph.util.LiveMatrixPlanner
import com.demetrius.f1glyph.widget.F1WidgetProvider
import com.nothing.ketchum.Common
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

/**
 * Drives the Glyph Matrix takeover. While the QS toggle is on it holds the
 * matrix continuously — pre-event face (session code + countdown bar), then
 * the live face, then a release once the window ends. Chains itself as
 * unique one-shot work; scheduled by RefreshWorker after every cache update
 * and by the QS tile toggle.
 */
class LiveMatrixWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val cache = WidgetStateCache(applicationContext)
        if (!cache.autoGlyphEnabled()) {
            GlyphAppMatrix.release(applicationContext)
            return Result.success()
        }
        val state = cache.load()
        val now = System.currentTimeMillis()
        when (val decision = LiveMatrixPlanner.decide(state, now)) {
            is LiveMatrixPlanner.Decision.PushCountdown -> {
                pushFace(cache)
                schedule(applicationContext, decision.recheckDelayMillis, ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
            is LiveMatrixPlanner.Decision.PushLive -> {
                // Live window: fetch inline so positions/lap stay fresh.
                // (Not via RefreshWorker — its doWork re-schedules this
                // worker, which would tight-loop the two queues.)
                try {
                    cache.save(F1Repository().fetchState())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("LiveMatrixWorker", "Refresh failed; using cached state", e)
                }
                pushFace(cache)
                F1WidgetProvider.renderAll(applicationContext)
                schedule(applicationContext, decision.recheckDelayMillis, ExistingWorkPolicy.APPEND_OR_REPLACE)
            }
            LiveMatrixPlanner.Decision.Release -> {
                GlyphAppMatrix.release(applicationContext)
                F1WidgetProvider.renderAll(applicationContext)
            }
            LiveMatrixPlanner.Decision.Idle -> Unit
        }
        return Result.success()
    }

    private suspend fun pushFace(cache: WidgetStateCache) {
        val gridSize = Common.getDeviceMatrixLength().takeIf { it > 0 } ?: 13
        GlyphAppMatrix.push(
            applicationContext,
            GlyphFace.render(cache.load(), System.currentTimeMillis(), gridSize)
        )
    }

    companion object {
        private const val UNIQUE_WORK = "f1_live_matrix"

        /** Run the planner now; it re-schedules itself as needed. */
        fun scheduleNext(context: Context) = schedule(context, 0L)

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }

        private fun schedule(
            context: Context,
            delayMillis: Long,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE
        ) {
            val request = OneTimeWorkRequestBuilder<LiveMatrixWorker>()
                .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK, policy, request)
        }
    }
}
