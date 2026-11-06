package com.pricewatch.electronics.data.search

/**
 * Built-in shop search configs. The shops live in the backend: the user
 * never types a site or a URL — a search runs every enabled shop here.
 *
 * Each config is a search-page URL template ({q} = the encoded query) plus
 * optional CSS selectors for the results. When selectors are absent the
 * engine falls back to a generic extractor (links with a nearby £ price),
 * so a shop is never silently skipped just because its layout changed.
 * A shop that cannot be loaded is reported as failed, never faked.
 */
data class SiteSearchConfig(
    val siteId: Long,
    val searchUrlTemplate: String,
    val itemSelector: String? = null,
    val titleSelector: String? = null,
    val priceSelector: String? = null,
    val linkSelector: String? = null,
    /** CSS selector for the price on a single product page (Check Now). */
    val productPriceSelector: String? = null
)

object SiteSearchConfigs {

    fun all(): List<SiteSearchConfig> = listOf(
        SiteSearchConfig(
            siteId = 28,
            searchUrlTemplate = "https://www.ebay.co.uk/sch/i.html?_nkw={q}&LH_BIN=1&_sop=15",
            itemSelector = ".s-item",
            titleSelector = ".s-item__title",
            priceSelector = ".s-item__price",
            linkSelector = ".s-item__link",
            productPriceSelector = ".x-price-primary span"
        ),
        SiteSearchConfig(
            siteId = 1,
            searchUrlTemplate = "https://www.amazon.co.uk/s?k={q}&s=price-asc-rank",
            itemSelector = "[data-component-type=s-search-result]",
            titleSelector = "h2 a span",
            priceSelector = ".a-price .a-offscreen",
            linkSelector = "h2 a",
            productPriceSelector = "#corePriceDisplay_desktop_feature_div .a-offscreen"
        ),
        SiteSearchConfig(
            siteId = 2,
            searchUrlTemplate = "https://www.argos.co.uk/search/{q}/",
            productPriceSelector = "[data-test=product-price]"
        ),
        SiteSearchConfig(
            siteId = 3,
            searchUrlTemplate = "https://www.currys.co.uk/search?searchTerm={q}"
        ),
        SiteSearchConfig(
            siteId = 5,
            searchUrlTemplate = "https://www.johnlewis.com/search?search-term={q}"
        ),
        SiteSearchConfig(
            siteId = 30,
            searchUrlTemplate = "https://www.backmarket.co.uk/en-gb/search?q={q}"
        ),
        SiteSearchConfig(
            siteId = 13,
            searchUrlTemplate = "https://www.laptopsdirect.co.uk/search?query={q}"
        ),
        SiteSearchConfig(
            siteId = 11,
            searchUrlTemplate = "https://www.box.co.uk/search?query={q}"
        ),
        SiteSearchConfig(
            siteId = 8,
            searchUrlTemplate = "https://www.ebuyer.com/search?q={q}"
        ),
        SiteSearchConfig(
            siteId = 9,
            searchUrlTemplate = "https://www.scan.co.uk/search?q={q}"
        ),
        SiteSearchConfig(
            siteId = 6,
            searchUrlTemplate = "https://ao.com/l/search/{q}/"
        ),
        SiteSearchConfig(
            siteId = 4,
            searchUrlTemplate = "https://www.very.co.uk/search/{q}.end"
        ),
        SiteSearchConfig(
            siteId = 32,
            searchUrlTemplate = "https://www.musicmagpie.co.uk/store/search/?q={q}"
        )
    )

    fun forSite(siteId: Long): SiteSearchConfig? = all().firstOrNull { it.siteId == siteId }
}
