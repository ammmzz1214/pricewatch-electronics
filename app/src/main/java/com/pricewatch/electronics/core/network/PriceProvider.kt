package com.pricewatch.electronics.core.network

import com.pricewatch.electronics.core.common.Result

data class ProviderPrice(
    val priceMinorUnits: Long,
    val currencyCode: String,
    val inStock: Boolean?,
    val sourceTimestamp: Long = System.currentTimeMillis()
)

data class ProviderProductInfo(
    val name: String?,
    val priceMinorUnits: Long?,
    val currencyCode: String?,
    val imageUrl: String?,
    val inStock: Boolean?
)

/**
 * Modular price-provider architecture. Priority:
 * 1. Official APIs, 2. Authorised feeds, 3. Permitted third-party
 * integrations, 4. Compliant extraction where permitted,
 * 5. Manual price updates. Never bypass auth, CAPTCHA or access controls.
 */
interface PriceProvider {
    val siteId: Long
    val siteDomain: String
    fun supportsUrl(url: String): Boolean
    fun validateUrl(url: String): Boolean
    suspend fun fetchProductInfo(url: String): Result<ProviderProductInfo>
    suspend fun fetchCurrentPrice(url: String): Result<ProviderPrice>
}

/** Fallback for MANUAL_URL sites: no automated fetch — user updates manually. */
class ManualPriceProvider(
    override val siteId: Long,
    override val siteDomain: String
) : PriceProvider {
    override fun supportsUrl(url: String): Boolean =
        url.contains(siteDomain.substringBefore("/"), ignoreCase = true)

    override fun validateUrl(url: String): Boolean = supportsUrl(url)

    override suspend fun fetchProductInfo(url: String): Result<ProviderProductInfo> =
        Result.Error("Manual-only site ($siteDomain): automated price checking is not available. Update the price manually.")

    override suspend fun fetchCurrentPrice(url: String): Result<ProviderPrice> =
        Result.Error("Manual-only site ($siteDomain): automated price checking is not available. Update the price manually.")
}
