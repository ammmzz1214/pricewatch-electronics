package com.pricewatch.electronics.presentation.util

import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.IntegrationStatus
import com.pricewatch.electronics.domain.models.IntegrationType
import com.pricewatch.electronics.domain.models.SiteGroup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Money is stored as integer minor units (pence/cents) — never as Double. */
fun formatPrice(minorUnits: Long, currencyCode: String): String =
    Money.format(minorUnits, currencyCode)

/** Parses user-typed decimal input (e.g. "129.99") to minor units, or null if invalid. */
fun parsePriceToMinorUnits(input: String): Long? = Money.fromDecimalString(input)

/** "+£12.34" / "-£12.34" / "£0.00" — sign is always shown for a difference. */
fun formatPriceDifference(minorUnits: Long, currencyCode: String): String {
    val sign = when {
        minorUnits > 0 -> "+"
        minorUnits < 0 -> "-"
        else -> ""
    }
    return sign + Money.format(abs(minorUnits), currencyCode)
}

fun formatPercent(value: Double?): String {
    if (value == null || value.isNaN() || value.isInfinite()) return "—"
    return String.format(Locale.UK, "%+.1f%%", value)
}

/** Percentage change of [newPrice] versus [oldPrice]; null when not computable. */
fun percentChange(oldPriceMinorUnits: Long?, newPriceMinorUnits: Long): Double? {
    if (oldPriceMinorUnits == null) return null
    return Money.percentageChange(oldPriceMinorUnits, newPriceMinorUnits)
}

private val dateTimeFormat = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.UK)
private val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.UK)

fun formatDateTime(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0) return "Never"
    return dateTimeFormat.format(Date(epochMillis))
}

fun formatDate(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0) return "—"
    return dateFormat.format(Date(epochMillis))
}

/** Short human relative time, e.g. "5m ago", "3h ago", "2d ago". */
fun formatTimeAgo(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0) return "Never"
    val diff = System.currentTimeMillis() - epochMillis
    if (diff < 0) return "Just now"
    val minutes = diff / 60_000
    if (minutes < 1) return "Just now"
    if (minutes < 60) return "${minutes}m ago"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h ago"
    val days = hours / 24
    if (days < 30) return "${days}d ago"
    return formatDate(epochMillis)
}

fun siteGroupDisplayName(group: SiteGroup): String = when (group) {
    SiteGroup.MAJOR_RETAILERS -> "Major UK Retailers"
    SiteGroup.SPECIALISTS -> "Computer & Electronics Specialists"
    SiteGroup.MARKETPLACES -> "Marketplaces"
    SiteGroup.PRICE_COMPARISON -> "Price Comparison Platforms"
    SiteGroup.OTHER_RETAILERS -> "Other Electronics Retailers"
}

fun integrationStatusDisplayName(status: IntegrationStatus): String = when (status) {
    IntegrationStatus.LIVE_INTEGRATION -> "Live Integration"
    IntegrationStatus.PARTIAL_SUPPORT -> "Partial Support"
    IntegrationStatus.MANUAL_URL -> "Manual URL"
    IntegrationStatus.UNAVAILABLE -> "Unavailable"
}

fun integrationTypeDisplayName(type: IntegrationType): String = when (type) {
    IntegrationType.OFFICIAL_API -> "Official API"
    IntegrationType.PRODUCT_FEED -> "Product feed"
    IntegrationType.THIRD_PARTY -> "Third-party integration"
    IntegrationType.COMPLIANT_EXTRACTION -> "Compliant extraction"
    IntegrationType.MANUAL -> "Manual URL"
}

fun checkStatusDisplayName(status: CheckStatus): String = when (status) {
    CheckStatus.NEVER_CHECKED -> "Not checked yet"
    CheckStatus.CHECK_OK -> "Checked OK"
    CheckStatus.OK -> "Checked OK"
    CheckStatus.CHECKING -> "Checking…"
    CheckStatus.CHECK_FAILED -> "Check failed"
    CheckStatus.PRICE_DECREASED -> "Price decreased"
    CheckStatus.PRICE_INCREASED -> "Price increased"
    CheckStatus.PRICE_UNCHANGED -> "Price unchanged"
}

/** Price direction derived only from verified previous vs current prices. */
enum class PriceDirection { UP, DOWN, UNCHANGED }

fun priceDirection(previousMinorUnits: Long?, currentMinorUnits: Long): PriceDirection {
    if (previousMinorUnits == null) return PriceDirection.UNCHANGED
    return when {
        currentMinorUnits < previousMinorUnits -> PriceDirection.DOWN
        currentMinorUnits > previousMinorUnits -> PriceDirection.UP
        else -> PriceDirection.UNCHANGED
    }
}
