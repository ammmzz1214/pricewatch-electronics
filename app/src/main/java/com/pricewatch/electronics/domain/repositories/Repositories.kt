package com.pricewatch.electronics.domain.repositories

import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.domain.models.CheckLog
import com.pricewatch.electronics.domain.models.CheckProgress
import com.pricewatch.electronics.domain.models.MonitoringSettings
import com.pricewatch.electronics.domain.models.PriceHistory
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>
    fun observeProduct(id: Long): Flow<Product?>
    suspend fun getProduct(id: Long): Product?
    suspend fun addProduct(product: Product): Result<Long>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun deleteProduct(id: Long): Result<Unit>
    suspend fun recordSuccessfulCheck(productId: Long, newPriceMinorUnits: Long, source: String): Result<Unit>
    suspend fun recordFailedCheck(productId: Long, errorCode: String?, errorMessage: String?): Result<Unit>
    /** Checks every monitored product once, emitting real progress per product. */
    fun checkAllNow(): Flow<CheckProgress>
    suspend fun existsDuplicate(productUrl: String, excludeId: Long? = null): Boolean
}

interface PriceHistoryRepository {
    fun observeHistory(productId: Long): Flow<List<PriceHistory>>
    fun observeAllHistory(): Flow<List<PriceHistory>>
    suspend fun getHistory(productId: Long, sinceMillis: Long? = null): List<PriceHistory>
    suspend fun clearHistory(productId: Long? = null): Result<Unit>
}

interface SiteRepository {
    fun observeSites(): Flow<List<Site>>
    fun observeSite(id: Long): Flow<Site?>
    suspend fun getSite(id: Long): Site?
    suspend fun getAllSites(): List<Site>
    suspend fun searchSites(query: String): List<Site>
}

interface SettingsRepository {
    fun observeSettings(): Flow<MonitoringSettings>
    suspend fun getSettings(): MonitoringSettings
    suspend fun updateSettings(settings: MonitoringSettings): Result<Unit>
}

interface CheckLogRepository {
    fun observeLogs(productId: Long): Flow<List<CheckLog>>
    suspend fun getRecentFailures(productId: Long, limit: Int = 5): List<CheckLog>
}
