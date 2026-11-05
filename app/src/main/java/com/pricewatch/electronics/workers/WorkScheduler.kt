package com.pricewatch.electronics.workers

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.pricewatch.electronics.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Schedules unique periodic checking. 15 minutes is the WorkManager
     * minimum and execution is inexact — never promise exact timing.
     * Allowed intervals: 15, 30, 60, 120 minutes.
     */
    fun schedulePeriodicChecks(intervalMinutes: Int) {
        val interval = when (intervalMinutes) {
            15, 30, 60, 120 -> intervalMinutes.toLong()
            else -> Constants.DEFAULT_MONITORING_INTERVAL_MINUTES.toLong()
        }
        val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .addTag(Constants.PRICE_CHECK_WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            Constants.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelPeriodicChecks() {
        WorkManager.getInstance(context).cancelUniqueWork(Constants.PERIODIC_WORK_NAME)
    }
}
