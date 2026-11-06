package com.pricewatch.electronics.data.repositories

import androidx.room.withTransaction
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.data.local.CheckLogEntity
import com.pricewatch.electronics.data.local.PriceHistoryEntity
import com.pricewatch.electronics.data.local.PriceWatchDatabase
import com.pricewatch.electronics.data.local.toDomain
import com.pricewatch.electronics.data.local.toEntity
import com.pricewatch.electronics.data.providers.ProviderRegistry
import com.pricewatch.electronics.domain.models.CheckProgress
import com.pricewatch.electronics.domain.models.CheckResultType
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.repositories.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val db: PriceWatchDatabase,
    private val providerRegistry: ProviderRegistry
) : ProductRepository {

    override fun observeProducts(): Flow<List<Product>> =
        kotlinx.coroutines.flow.combine(
            db.productDao().observeAll(),
            db.siteDao().observeAll()
        ) { products, sites ->
            val names = sites.associate { it.id to it.name }
            products.map { it.toDomain().copy(siteName = names[it.siteId] ?: "") }
        }

    override fun observeProduct(id: Long): Flow<Product?> =
        kotlinx.coroutines.flow.combine(
            db.productDao().observeById(id),
            db.siteDao().observeAll()
        ) { product, sites ->
            product?.toDomain()?.copy(
                siteName = sites.find { it.id == product.siteId }?.name ?: ""
            )
        }

    override suspend fun getProduct(id: Long): Product? =
        db.productDao().getById(id)?.toDomain()

    override suspend fun addProduct(product: Product): Result<Long> { return try {
        if (db.productDao().countDuplicateUrl(product.productUrl, null) > 0) {
            return Result.Error("A product with this URL already exists.")
        }
        val newId = addProductTransactional(product)
        Result.Success(newId)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to add product.", e)
    }
    }

    private suspend fun addProductTransactional(product: Product): Long {
        var newId = 0L
        db.withTransaction {
            newId = db.productDao().insert(product.copy(id = 0).toEntity())
            // Initial price history: old == new, difference 0 — this is the
            // baseline record, not a price change.
            db.priceHistoryDao().insert(
                PriceHistoryEntity(
                    productId = newId,
                    oldPriceMinorUnits = product.currentPriceMinorUnits,
                    newPriceMinorUnits = product.currentPriceMinorUnits,
                    differenceMinorUnits = 0,
                    percentageChange = 0.0,
                    currencyCode = product.currencyCode,
                    source = "manual_entry",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return newId
    }

    override suspend fun updateProduct(product: Product): Result<Unit> { return try {
        if (db.productDao().countDuplicateUrl(product.productUrl, product.id) > 0) {
            return Result.Error("Another product already uses this URL.")
        }
        db.productDao().update(product.copy(updatedAt = System.currentTimeMillis()).toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to update product.", e)
    }
    }

    override suspend fun deleteProduct(id: Long): Result<Unit> { return try {
        db.withTransaction {
            db.productDao().deleteById(id)
            // Price history and check logs are deleted by CASCADE foreign keys.
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to delete product.", e)
    }
    }

    /**
     * CRITICAL FAILED-CHECK RULE (enforced here):
     * On failure, NEVER overwrite the last known price, NEVER insert a
     * price-history record, and NEVER report an increase/decrease.
     * Only lastAttemptAt, checkStatus=CHECK_FAILED and a CheckLog row change.
     */
    override suspend fun recordFailedCheck(
        productId: Long,
        errorCode: String?,
        errorMessage: String?
    ): Result<Unit> { return try {
        val now = System.currentTimeMillis()
        val existing = db.productDao().getById(productId)
            ?: return Result.Error("Product not found.")
        db.withTransaction {
            db.productDao().update(
                existing.copy(lastAttemptAt = now, checkStatus = CheckStatus.CHECK_FAILED.name)
            )
            db.checkLogDao().insert(
                CheckLogEntity(
                    productId = productId,
                    result = CheckResultType.FAILED.name,
                    errorCode = errorCode,
                    errorMessage = errorMessage,
                    attemptedAt = now,
                    completedAt = now
                )
            )
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to record failed check.", e)
    }
    }

    override suspend fun recordSuccessfulCheck(
        productId: Long,
        newPriceMinorUnits: Long,
        source: String
    ): Result<Unit> { return try {
        val now = System.currentTimeMillis()
        val existing = db.productDao().getById(productId)
            ?: return Result.Error("Product not found.")
        if (!Money.isValidPrice(newPriceMinorUnits)) {
            return recordFailedCheck(productId, "INVALID_PRICE", "Provider returned an invalid price.")
        }
        val oldPrice = existing.currentPriceMinorUnits
        val status = when {
            newPriceMinorUnits < oldPrice -> CheckStatus.PRICE_DECREASED
            newPriceMinorUnits > oldPrice -> CheckStatus.PRICE_INCREASED
            else -> CheckStatus.PRICE_UNCHANGED
        }
        db.withTransaction {
            if (newPriceMinorUnits != oldPrice) {
                val pct = Money.percentageChange(oldPrice, newPriceMinorUnits) ?: 0.0
                db.priceHistoryDao().insert(
                    PriceHistoryEntity(
                        productId = productId,
                        oldPriceMinorUnits = oldPrice,
                        newPriceMinorUnits = newPriceMinorUnits,
                        differenceMinorUnits = newPriceMinorUnits - oldPrice,
                        percentageChange = pct,
                        currencyCode = existing.currencyCode,
                        source = source,
                        timestamp = now
                    )
                )
            }
            db.productDao().update(
                existing.copy(
                    previousPriceMinorUnits = oldPrice,
                    currentPriceMinorUnits = newPriceMinorUnits,
                    updatedAt = now,
                    lastSuccessfulCheckAt = now,
                    lastAttemptAt = now,
                    checkStatus = status.name
                )
            )
            db.checkLogDao().insert(
                CheckLogEntity(
                    productId = productId,
                    result = when (status) {
                        CheckStatus.PRICE_DECREASED -> CheckResultType.PRICE_DECREASED.name
                        CheckStatus.PRICE_INCREASED -> CheckResultType.PRICE_INCREASED.name
                        else -> CheckResultType.PRICE_UNCHANGED.name
                    },
                    attemptedAt = now,
                    completedAt = now
                )
            )
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to record successful check.", e)
    }
    }

    override suspend fun existsDuplicate(productUrl: String, excludeId: Long?): Boolean =
        db.productDao().countDuplicateUrl(productUrl.trim(), excludeId) > 0

    override fun checkAllNow(): Flow<CheckProgress> = flow {
        val products = db.productDao().getMonitored()
        val total = products.size
        var completed = 0
        var succeeded = 0
        var failed = 0
        emit(CheckProgress(total = total, completed = 0, succeeded = 0, failed = 0, isComplete = total == 0))
        for (product in products) {
            emit(
                CheckProgress(
                    total = total,
                    completed = completed,
                    succeeded = succeeded,
                    failed = failed,
                    currentProductName = product.name
                )
            )
            val site = db.siteDao().getById(product.siteId)?.toDomain()
            if (site == null) {
                recordFailedCheck(product.id, "SITE_NOT_FOUND", "Site not found.")
                failed++
            } else {
                val result = providerRegistry.providerFor(site).fetchCurrentPrice(product.productUrl)
                if (result is Result.Success) {
                    recordSuccessfulCheck(product.id, result.data.priceMinorUnits, site.domain)
                    succeeded++
                } else {
                    val err = (result as? Result.Error)
                    recordFailedCheck(product.id, "PROVIDER_UNAVAILABLE", err?.message)
                    failed++
                }
            }
            completed++
        }
        emit(
            CheckProgress(
                total = total,
                completed = completed,
                succeeded = succeeded,
                failed = failed,
                isComplete = true
            )
        )
    }.flowOn(Dispatchers.IO)
}
