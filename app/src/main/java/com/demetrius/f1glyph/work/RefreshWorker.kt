package com.demetrius.f1glyph.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.demetrius.f1glyph.data.F1Repository
import com.demetrius.f1glyph.data.WidgetStateCache
import com.demetrius.f1glyph.widget.F1WidgetProvider
import java.util.concurrent.TimeUnit

/**
 * Sole network caller in the app. Fetches the F1 state, persists it to the
 * DataStore cache, then re-renders all widgets. The Glyph Toy picks up the
 * new cache on its own 1/min redraw tick.
 */
class RefreshWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val state = F1Repository().fetchState()
            WidgetStateCache(applicationContext).save(state)
            F1WidgetProvider.renderAll(applicationContext)
            LiveMatrixWorker.scheduleNext(applicationContext)
            CountdownTickWorker.scheduleNext(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val PERIODIC_WORK = "f1_refresh_periodic"
        private const val ONESHOT_WORK = "f1_refresh_now"

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(30, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
        }

        fun refreshNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RefreshWorker>()
                .setConstraints(networkConstraint)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONESHOT_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
