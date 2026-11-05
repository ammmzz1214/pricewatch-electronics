package com.pricewatch.electronics.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pricewatch.electronics.domain.repositories.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.last

@HiltWorker
class PriceCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val productRepository: ProductRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val progress = productRepository.checkAllNow().last()
            // A run with failures still counts as work done: failed checks
            // are recorded per product (price preserved) and retried next run.
            // Only an unexpected exception triggers WorkManager retry.
            if (progress.isComplete) Result.success() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
