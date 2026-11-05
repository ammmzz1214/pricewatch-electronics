package com.pricewatch.electronics.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products", indices = [Index("siteId"), Index("productUrl", unique = true)])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "ELECTRONICS",
    val subcategory: String,
    val siteId: Long,
    val productUrl: String,
    val currentPriceMinorUnits: Long,
    val previousPriceMinorUnits: Long? = null,
    val currencyCode: String = "GBP",
    val targetPriceMinorUnits: Long? = null,
    val imageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val lastSuccessfulCheckAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val checkStatus: String = "NEVER_CHECKED",
    val monitoringEnabled: Boolean = true
)

@Entity(
    tableName = "price_history",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId"), Index("timestamp")]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val oldPriceMinorUnits: Long,
    val newPriceMinorUnits: Long,
    val differenceMinorUnits: Long,
    val percentageChange: Double,
    val currencyCode: String = "GBP",
    val source: String,
    val timestamp: Long
)

@Entity(tableName = "sites", indices = [Index("domain", unique = true)])
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val domain: String,
    val siteGroup: String,
    val integrationType: String,
    val integrationStatus: String,
    val isEnabled: Boolean = true,
    val lastSuccessfulConnectionAt: Long? = null
)

@Entity(tableName = "monitoring_settings")
data class MonitoringSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val intervalMinutes: Int = 60,
    val monitoringEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val priceIncreaseAlerts: Boolean = true,
    val priceDecreaseAlerts: Boolean = true,
    val targetPriceAlerts: Boolean = true,
    val currencyPreference: String = "GBP",
    val lastScheduledAt: Long? = null
)

@Entity(
    tableName = "check_logs",
    foreignKeys = [ForeignKey(
        entity = ProductEntity::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("productId"), Index("attemptedAt")]
)
data class CheckLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val result: String,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val attemptedAt: Long,
    val completedAt: Long? = null
)
