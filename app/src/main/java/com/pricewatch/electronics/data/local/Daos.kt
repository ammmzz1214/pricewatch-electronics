package com.pricewatch.electronics.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE monitoringEnabled = 1")
    suspend fun getMonitored(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products WHERE productUrl = :url AND (:excludeId IS NULL OR id != :excludeId)")
    suspend fun countDuplicateUrl(url: String, excludeId: Long?): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE productId = :productId ORDER BY timestamp DESC")
    fun observeByProduct(productId: Long): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history WHERE productId = :productId AND (:since IS NULL OR timestamp >= :since) ORDER BY timestamp DESC")
    suspend fun getByProductSince(productId: Long, since: Long?): List<PriceHistoryEntity>

    @Insert
    suspend fun insert(entry: PriceHistoryEntity): Long

    @Query("DELETE FROM price_history WHERE productId = :productId")
    suspend fun deleteByProduct(productId: Long)

    @Query("DELETE FROM price_history")
    suspend fun deleteAll()
}

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY name ASC")
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    fun observeById(id: Long): Flow<SiteEntity?>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getById(id: Long): SiteEntity?

    @Query("SELECT * FROM sites ORDER BY name ASC")
    suspend fun getAll(): List<SiteEntity>

    @Query("SELECT * FROM sites WHERE name LIKE '%' || :query || '%' OR domain LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<SiteEntity>

    @Query("SELECT COUNT(*) FROM sites")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(site: SiteEntity)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM monitoring_settings WHERE id = 1")
    fun observe(): Flow<MonitoringSettingsEntity?>

    @Query("SELECT * FROM monitoring_settings WHERE id = 1")
    suspend fun get(): MonitoringSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: MonitoringSettingsEntity)
}

@Dao
interface CheckLogDao {
    @Query("SELECT * FROM check_logs WHERE productId = :productId ORDER BY attemptedAt DESC")
    fun observeByProduct(productId: Long): Flow<List<CheckLogEntity>>

    @Query("SELECT * FROM check_logs WHERE productId = :productId AND result = 'FAILED' ORDER BY attemptedAt DESC LIMIT :limit")
    suspend fun getRecentFailures(productId: Long, limit: Int): List<CheckLogEntity>

    @Insert
    suspend fun insert(log: CheckLogEntity): Long

    @Query("DELETE FROM check_logs WHERE attemptedAt < :beforeMillis")
    suspend fun deleteOlderThan(beforeMillis: Long)
}
