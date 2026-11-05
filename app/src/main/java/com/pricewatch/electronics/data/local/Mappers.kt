package com.pricewatch.electronics.data.local

import com.pricewatch.electronics.domain.models.Category
import com.pricewatch.electronics.domain.models.CheckLog
import com.pricewatch.electronics.domain.models.CheckResultType
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.IntegrationStatus
import com.pricewatch.electronics.domain.models.IntegrationType
import com.pricewatch.electronics.domain.models.MonitoringSettings
import com.pricewatch.electronics.domain.models.PriceHistory
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.models.SiteGroup
import com.pricewatch.electronics.domain.models.Subcategory

fun ProductEntity.toDomain() = Product(
    id = id,
    name = name,
    category = runCatching { Category.valueOf(category) }.getOrDefault(Category.ELECTRONICS),
    subcategory = runCatching { Subcategory.valueOf(subcategory) }.getOrDefault(Subcategory.SMARTPHONES),
    siteId = siteId,
    productUrl = productUrl,
    currentPriceMinorUnits = currentPriceMinorUnits,
    previousPriceMinorUnits = previousPriceMinorUnits,
    currencyCode = currencyCode,
    targetPriceMinorUnits = targetPriceMinorUnits,
    imageUrl = imageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSuccessfulCheckAt = lastSuccessfulCheckAt,
    lastAttemptAt = lastAttemptAt,
    checkStatus = runCatching { CheckStatus.valueOf(checkStatus) }.getOrDefault(CheckStatus.NEVER_CHECKED),
    monitoringEnabled = monitoringEnabled
)

fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    category = category.name,
    subcategory = subcategory.name,
    siteId = siteId,
    productUrl = productUrl,
    currentPriceMinorUnits = currentPriceMinorUnits,
    previousPriceMinorUnits = previousPriceMinorUnits,
    currencyCode = currencyCode,
    targetPriceMinorUnits = targetPriceMinorUnits,
    imageUrl = imageUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSuccessfulCheckAt = lastSuccessfulCheckAt,
    lastAttemptAt = lastAttemptAt,
    checkStatus = checkStatus.name,
    monitoringEnabled = monitoringEnabled
)

fun PriceHistoryEntity.toDomain() = PriceHistory(
    id = id,
    productId = productId,
    oldPriceMinorUnits = oldPriceMinorUnits,
    newPriceMinorUnits = newPriceMinorUnits,
    differenceMinorUnits = differenceMinorUnits,
    percentageChange = percentageChange,
    currencyCode = currencyCode,
    source = source,
    timestamp = timestamp
)

fun PriceHistory.toEntity() = PriceHistoryEntity(
    id = id,
    productId = productId,
    oldPriceMinorUnits = oldPriceMinorUnits,
    newPriceMinorUnits = newPriceMinorUnits,
    differenceMinorUnits = differenceMinorUnits,
    percentageChange = percentageChange,
    currencyCode = currencyCode,
    source = source,
    timestamp = timestamp
)

fun SiteEntity.toDomain() = Site(
    id = id,
    name = name,
    domain = domain,
    siteGroup = runCatching { SiteGroup.valueOf(siteGroup) }.getOrDefault(SiteGroup.MAJOR_RETAILERS),
    integrationType = runCatching { IntegrationType.valueOf(integrationType) }.getOrDefault(IntegrationType.MANUAL),
    integrationStatus = runCatching { IntegrationStatus.valueOf(integrationStatus) }.getOrDefault(IntegrationStatus.MANUAL_URL),
    isEnabled = isEnabled,
    lastSuccessfulConnectionAt = lastSuccessfulConnectionAt
)

fun MonitoringSettingsEntity.toDomain() = MonitoringSettings(
    id = id,
    intervalMinutes = intervalMinutes,
    monitoringEnabled = monitoringEnabled,
    notificationsEnabled = notificationsEnabled,
    priceIncreaseAlerts = priceIncreaseAlerts,
    priceDecreaseAlerts = priceDecreaseAlerts,
    targetPriceAlerts = targetPriceAlerts,
    currencyPreference = currencyPreference,
    lastScheduledAt = lastScheduledAt
)

fun MonitoringSettings.toEntity() = MonitoringSettingsEntity(
    id = id,
    intervalMinutes = intervalMinutes,
    monitoringEnabled = monitoringEnabled,
    notificationsEnabled = notificationsEnabled,
    priceIncreaseAlerts = priceIncreaseAlerts,
    priceDecreaseAlerts = priceDecreaseAlerts,
    targetPriceAlerts = targetPriceAlerts,
    currencyPreference = currencyPreference,
    lastScheduledAt = lastScheduledAt
)

fun CheckLogEntity.toDomain() = CheckLog(
    id = id,
    productId = productId,
    result = runCatching { CheckResultType.valueOf(result) }.getOrDefault(CheckResultType.FAILED),
    errorCode = errorCode,
    errorMessage = errorMessage,
    attemptedAt = attemptedAt,
    completedAt = completedAt
)
