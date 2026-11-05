package com.pricewatch.electronics.data.repositories

import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.data.local.PriceWatchDatabase
import com.pricewatch.electronics.data.local.toDomain
import com.pricewatch.electronics.data.local.toEntity
import com.pricewatch.electronics.domain.models.CheckLog
import com.pricewatch.electronics.domain.models.MonitoringSettings
import com.pricewatch.electronics.domain.models.PriceHistory
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.repositories.CheckLogRepository
import com.pricewatch.electronics.domain.repositories.PriceHistoryRepository
import com.pricewatch.electronics.domain.repositories.SettingsRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceHistoryRepositoryImpl @Inject constructor(
    private val db: PriceWatchDatabase
) : PriceHistoryRepository {
    override fun observeHistory(productId: Long): Flow<List<PriceHistory>> =
        db.priceHistoryDao().observeByProduct(productId).map { it.map { e -> e.toDomain() } }

    override fun observeAllHistory(): Flow<List<PriceHistory>> =
        db.priceHistoryDao().observeAll().map { it.map { e -> e.toDomain() } }

    override suspend fun getHistory(productId: Long, sinceMillis: Long?): List<PriceHistory> =
        db.priceHistoryDao().getByProductSince(productId, sinceMillis).map { it.toDomain() }

    override suspend fun clearHistory(productId: Long?): Result<Unit> = try {
        if (productId == null) db.priceHistoryDao().deleteAll()
        else db.priceHistoryDao().deleteByProduct(productId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to clear history.", e)
    }
}

@Singleton
class SiteRepositoryImpl @Inject constructor(
    private val db: PriceWatchDatabase
) : SiteRepository {
    override fun observeSites(): Flow<List<Site>> =
        db.siteDao().observeAll().map { it.map { e -> e.toDomain() } }

    override fun observeSite(id: Long): Flow<Site?> =
        db.siteDao().observeById(id).map { it?.toDomain() }

    override suspend fun getSite(id: Long): Site? = db.siteDao().getById(id)?.toDomain()

    override suspend fun getAllSites(): List<Site> = db.siteDao().getAll().map { it.toDomain() }

    override suspend fun searchSites(query: String): List<Site> =
        if (query.isBlank()) getAllSites() else db.siteDao().search(query.trim()).map { it.toDomain() }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val db: PriceWatchDatabase
) : SettingsRepository {
    override fun observeSettings(): Flow<MonitoringSettings> =
        db.settingsDao().observe().map { it?.toDomain() ?: MonitoringSettings() }

    override suspend fun getSettings(): MonitoringSettings =
        db.settingsDao().get()?.toDomain() ?: MonitoringSettings()

    override suspend fun updateSettings(settings: MonitoringSettings): Result<Unit> = try {
        db.settingsDao().upsert(settings.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e.message ?: "Failed to save settings.", e)
    }
}

@Singleton
class CheckLogRepositoryImpl @Inject constructor(
    private val db: PriceWatchDatabase
) : CheckLogRepository {
    override fun observeLogs(productId: Long): Flow<List<CheckLog>> =
        db.checkLogDao().observeByProduct(productId).map { it.map { e -> e.toDomain() } }

    override suspend fun getRecentFailures(productId: Long, limit: Int): List<CheckLog> =
        db.checkLogDao().getRecentFailures(productId, limit).map { it.toDomain() }
}
