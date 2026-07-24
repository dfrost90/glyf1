package com.demetrius.f1glyph.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.util.CountdownTickPlanner
import com.demetrius.f1glyph.util.SessionSelection
import com.demetrius.f1glyph.util.SessionWindow
import com.demetrius.f1glyph.widget.F1WidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Re-renders the widgets once a minute during the last 10 minutes before a
 * session so the Ndot bitmap countdown counts down. Cache-only, no network.
 * Independent of the auto-glyph toggle — this drives the widget, not the
 * matrix. Scheduled by RefreshWorker after every cache update.
 */
class CountdownTickWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        F1WidgetProvider.renderAll(applicationContext)
        scheduleNext(applicationContext)
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK = "f1_countdown_tick"

        /** Schedule the next tick from the cached schedule (or stand down). */
        suspend fun scheduleNext(context: Context) {
            val now = System.currentTimeMillis()
            val sessions = WidgetStateCache(context).load().weekend?.sessions ?: emptyList()
            val active = SessionSelection.select(sessions, now)
            val delay = buildList {
                // Advance the widget the instant a live session's window closes,
                // so the finished session doesn't linger as "upcoming".
                if (active.isLive) active.session?.let { s ->
                    add(s.epochMillis + SessionWindow.liveWindowMillis(s.kind) - now + 1_000L)
                }
                // Per-minute countdown ticks before the next upcoming session.
                val upcoming = if (active.isLive)
                    sessions.filter { it.epochMillis > now }.minByOrNull { it.epochMillis }
                else active.session
                CountdownTickPlanner.nextDelayMillis(upcoming?.epochMillis, now)?.let { add(it) }
            }.filter { it > 0 }.minOrNull()
            val wm = WorkManager.getInstance(context)
            if (delay == null) {
                // Nothing pending: drop any stale queued tick.
                wm.cancelUniqueWork(UNIQUE_WORK)
                return
            }
            val request = OneTimeWorkRequestBuilder<CountdownTickWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
