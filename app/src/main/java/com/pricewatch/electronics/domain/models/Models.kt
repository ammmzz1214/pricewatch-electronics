package com.pricewatch.electronics.domain.models

data class Product(
    val id: Long = 0,
    val name: String,
    val category: Category = Category.ELECTRONICS,
    val subcategory: Subcategory,
    val siteId: Long,
    /** Denormalised display name resolved from the site directory; not persisted. */
    val siteName: String = "",
    val productUrl: String,
    val currentPriceMinorUnits: Long,
    val previousPriceMinorUnits: Long? = null,
    val currencyCode: String = "GBP",
    val targetPriceMinorUnits: Long? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSuccessfulCheckAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val checkStatus: CheckStatus = CheckStatus.NEVER_CHECKED,
    val monitoringEnabled: Boolean = true
) {
    val targetReached: Boolean
        get() = targetPriceMinorUnits != null && currentPriceMinorUnits <= targetPriceMinorUnits

    /** Aliases kept so callers using epoch-millis naming compile unchanged. */
    val createdAtEpochMillis: Long get() = createdAt
    val lastSuccessfulCheckAtEpochMillis: Long? get() = lastSuccessfulCheckAt
}

/** Progress of a Check Now run: completed/total is the real ratio. */
data class CheckProgress(
    val total: Int,
    val completed: Int,
    val succeeded: Int,
    val failed: Int,
    val currentProductName: String? = null,
    val isComplete: Boolean = false
)

data class PriceHistory(
    val id: Long = 0,
    val productId: Long,
    val oldPriceMinorUnits: Long,
    val newPriceMinorUnits: Long,
    val differenceMinorUnits: Long,
    val percentageChange: Double,
    val currencyCode: String = "GBP",
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class Site(
    val id: Long = 0,
    val name: String,
    val domain: String,
    val siteGroup: SiteGroup,
    val integrationType: IntegrationType,
    val integrationStatus: IntegrationStatus,
    val isEnabled: Boolean = true,
    val lastSuccessfulConnectionAt: Long? = null
)

data class CheckResult(
    val productId: Long,
    val type: CheckResultType,
    val oldPriceMinorUnits: Long?,
    val newPriceMinorUnits: Long?,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val attemptedAt: Long = System.currentTimeMillis(),
    val completedAt: Long = System.currentTimeMillis()
)

data class MonitoringSettings(
    val id: Long = 1,
    val intervalMinutes: Int = 60,
    val monitoringEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val priceIncreaseAlerts: Boolean = true,
    val priceDecreaseAlerts: Boolean = true,
    val targetPriceAlerts: Boolean = true,
    val currencyPreference: String = "GBP",
    val lastScheduledAt: Long? = null
)

data class CheckLog(
    val id: Long = 0,
    val productId: Long,
    val result: CheckResultType,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val attemptedAt: Long,
    val completedAt: Long? = null
)
