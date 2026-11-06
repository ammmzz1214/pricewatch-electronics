package com.pricewatch.electronics.domain.usecases

import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.core.common.SearchText
import com.pricewatch.electronics.data.search.SiteSearchConfigs
import com.pricewatch.electronics.data.search.WebViewSearchEngine
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.SearchFilters
import com.pricewatch.electronics.domain.models.SearchResultItem
import com.pricewatch.electronics.domain.models.SiteSearchOutcome
import com.pricewatch.electronics.domain.repositories.SiteRepository
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Searches every built-in shop for the query and reports each shop's
 * outcome as it finishes so the UI can show live progress. Results are
 * streamed to [onResults] as each shop finishes, cheapest first, so the
 * user sees prices appearing instead of waiting for every shop.
 *
 * Comparison sites (Idealo, PriceSpy, PriceRunner, Google Shopping)
 * answer one search with offers from many retailers, and they block
 * automated access far less often than the retailers do — so they are
 * searched first. Up to [MAX_CONCURRENT_SEARCHES] shops load at once:
 * searching one shop at a time made a full search take minutes when
 * several shops were slow or blocked.
 *
 * Returns the merged results with the user's spec filters and price
 * range applied, cheapest first.
 */
class SearchAcrossSitesUseCase @Inject constructor(
    private val siteRepository: SiteRepository,
    private val engine: WebViewSearchEngine
) {
    suspend operator fun invoke(
        filters: SearchFilters,
        onSiteFinished: (SiteSearchOutcome) -> Unit,
        onResults: (List<SearchResultItem>) -> Unit = {}
    ): List<SearchResultItem> {
        val query = filters.query.trim()
        if (query.isBlank()) return emptyList()
        val sites = siteRepository.getAllSites()
            .filter { it.isEnabled && SiteSearchConfigs.forSite(it.id) != null }
            .sortedWith(compareBy({ it.id !in COMPARISON_SITE_IDS }, { it.id }))
        val merged = mutableListOf<SearchResultItem>()
        val lock = Any()
        coroutineScope {
            sites.chunked(MAX_CONCURRENT_SEARCHES).forEach { batch ->
                batch.map { site ->
                    async {
                        val config = SiteSearchConfigs.forSite(site.id) ?: return@async
                        val outcome = when (val result = engine.searchSite(site, config, query)) {
                            is Result.Success -> {
                                val items = result.data.mapNotNull { raw ->
                                    val minor = SearchText.parsePriceToMinor(raw.priceText) ?: return@mapNotNull null
                                    SearchResultItem(
                                        title = raw.title,
                                        priceMinor = minor,
                                        siteId = site.id,
                                        siteName = raw.sellerName?.let { "$it (via ${site.name})" } ?: site.name,
                                        productUrl = raw.url,
                                        imageUrl = raw.imageUrl
                                    )
                                }
                                SiteSearchOutcome(site.id, site.name, items)
                            }
                            is Result.Error -> SiteSearchOutcome(site.id, site.name, emptyList(), result.message)
                            Result.Loading -> SiteSearchOutcome(site.id, site.name, emptyList(), "Search failed.")
                        }
                        val snapshot: List<SearchResultItem> = synchronized(lock) {
                            merged += outcome.results
                            SearchText.applyFilters(merged.toList(), filters)
                        }
                        onSiteFinished(outcome)
                        onResults(snapshot)
                    }
                }.awaitAll()
            }
        }
        return SearchText.applyFilters(merged.toList(), filters)
    }

    companion object {
        private val COMPARISON_SITE_IDS = listOf(33L, 34L, 35L, 38L)
        private const val MAX_CONCURRENT_SEARCHES = 4
    }
}

/** Saves a search result as a tracked product. The URL comes from the shop, never typed. */
class TrackSearchResultUseCase @Inject constructor(
    private val addProductUseCase: AddProductUseCase,
    private val siteRepository: SiteRepository
) {
    suspend operator fun invoke(item: SearchResultItem, query: String): Result<Long> {
        val product = Product(
            name = item.title.take(200),
            subcategory = SearchText.inferSubcategory(query),
            // The siteId is the shop the URL actually belongs to (for
            // comparison-page results, the retailer behind the offer).
            siteId = resolveRetailerSiteId(item),
            productUrl = item.productUrl,
            currentPriceMinorUnits = item.priceMinor,
            currencyCode = item.currencyCode,
            imageUrl = item.imageUrl
        )
        return addProductUseCase(product)
    }

    /**
     * A result from a comparison page points at the retailer's own URL, so
     * the tracked product belongs to that retailer, not the comparison site.
     * Falls back to the result's own site when the URL matches no known shop.
     */
    private suspend fun resolveRetailerSiteId(item: SearchResultItem): Long {
        val host = try {
            java.net.URI(item.productUrl).host?.lowercase() ?: return item.siteId
        } catch (e: Exception) {
            return item.siteId
        }
        val sites = try {
            siteRepository.getAllSites()
        } catch (e: Exception) {
            return item.siteId
        }
        return sites.firstOrNull { site ->
            val domain = site.domain.substringBefore("/").lowercase()
            host == domain || host.endsWith(".$domain")
        }?.id ?: item.siteId
    }
}
