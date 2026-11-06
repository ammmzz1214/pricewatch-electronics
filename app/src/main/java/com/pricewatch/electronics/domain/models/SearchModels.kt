package com.pricewatch.electronics.domain.models

/**
 * Search v2: the user types a product name, picks spec filters and a price
 * range, and the app searches every built-in shop. No product URL is ever
 * typed by the user — URLs come from the shops' own search results.
 */
data class SearchFilters(
    val query: String = "",
    val ramGb: Int? = null,
    val storageGb: Int? = null,
    val screenSizeInches: Double? = null,
    val minPriceMinor: Long? = null,
    val maxPriceMinor: Long? = null
)

data class SearchResultItem(
    val title: String,
    val priceMinor: Long,
    val currencyCode: String = "GBP",
    val siteId: Long,
    val siteName: String,
    val productUrl: String,
    val imageUrl: String? = null
)

/** One shop's outcome for a search: its results, or why it gave none. */
data class SiteSearchOutcome(
    val siteId: Long,
    val siteName: String,
    val results: List<SearchResultItem> = emptyList(),
    val error: String? = null
) {
    val succeeded: Boolean get() = error == null
}
