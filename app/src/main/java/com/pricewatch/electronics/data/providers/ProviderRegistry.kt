package com.pricewatch.electronics.data.providers

import com.pricewatch.electronics.core.network.ManualPriceProvider
import com.pricewatch.electronics.core.network.PriceProvider
import com.pricewatch.electronics.core.network.WebViewPriceProvider
import com.pricewatch.electronics.data.search.SiteSearchConfigs
import com.pricewatch.electronics.data.search.WebViewSearchEngine
import com.pricewatch.electronics.domain.models.Site
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider registry. Shops with a built-in search config resolve to the
 * WebView price provider, which reads the live price from the shop's own
 * product page on the phone. Shops without a config stay manual: the
 * price is only updated when the user changes it.
 */
@Singleton
class ProviderRegistry @Inject constructor(
    private val engine: WebViewSearchEngine
) {
    fun providerFor(site: Site): PriceProvider =
        if (SiteSearchConfigs.forSite(site.id) != null) {
            WebViewPriceProvider(site.id, site.domain, engine, site)
        } else {
            ManualPriceProvider(site.id, site.domain)
        }
}
