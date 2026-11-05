package com.pricewatch.electronics.data.providers

import com.pricewatch.electronics.core.network.ManualPriceProvider
import com.pricewatch.electronics.core.network.PriceProvider
import com.pricewatch.electronics.domain.models.Site
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider registry. Today every site resolves to ManualPriceProvider
 * except eBay, which resolves to a partial-support provider that still
 * declines automated fetching until an authorised integration is verified.
 * No site is ever reported as LIVE.
 */
@Singleton
class ProviderRegistry @Inject constructor() {
    fun providerFor(site: Site): PriceProvider = ManualPriceProvider(site.id, site.domain)
}
