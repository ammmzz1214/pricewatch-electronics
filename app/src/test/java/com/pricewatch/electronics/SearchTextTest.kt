package com.pricewatch.electronics

import com.pricewatch.electronics.core.common.SearchText
import com.pricewatch.electronics.domain.models.SearchFilters
import com.pricewatch.electronics.domain.models.SearchResultItem
import com.pricewatch.electronics.domain.models.Subcategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTextTest {

    @Test
    fun parsePriceToMinor_readsFirstPoundPrice() {
        assertEquals(41900L, SearchText.parsePriceToMinor("£419.00"))
        assertEquals(123456L, SearchText.parsePriceToMinor("Now £1,234.56 was £1,499"))
        assertEquals(23470L, SearchText.parsePriceToMinor("£234.70 + £10.00 delivery"))
        assertNull(SearchText.parsePriceToMinor("Free delivery"))
        assertNull(SearchText.parsePriceToMinor(""))
    }

    @Test
    fun extractRamGb_firstGbNumberIsRam() {
        assertEquals(16, SearchText.extractRamGb("Apple MacBook Pro 16GB 512GB SSD"))
        assertEquals(12, SearchText.extractRamGb("Nothing Phone (4a) Pro 12GB RAM 256GB"))
        assertEquals(32, SearchText.extractRamGb("Lenovo Legion 32GB DDR5 1TB"))
        assertNull(SearchText.extractRamGb("Samsung 512GB SSD"))
    }

    @Test
    fun extractStorageGb_lastGbNumberOrTb() {
        assertEquals(512, SearchText.extractStorageGb("Apple MacBook Pro 16GB 512GB SSD"))
        assertEquals(1024, SearchText.extractStorageGb("Dell XPS 16GB 1TB SSD"))
        assertEquals(2048, SearchText.extractStorageGb("HP Omen 32GB 2TB NVMe"))
        assertEquals(256, SearchText.extractStorageGb("iPhone 17 256GB"))
    }

    @Test
    fun extractScreenSizeInches_readsInches() {
        assertEquals(16.0, SearchText.extractScreenSizeInches("MacBook Pro 16-inch")!!, 0.01)
        assertEquals(15.6, SearchText.extractScreenSizeInches("ASUS VivoBook 15.6\" Laptop")!!, 0.01)
        assertNull(SearchText.extractScreenSizeInches("iPhone 17 256GB"))
    }

    @Test
    fun specFilters_onlyExcludeOnConfidentMismatch() {
        // Title with no parseable RAM passes a RAM filter.
        assertTrue(SearchText.ramMatches("Apple MacBook Pro", 16))
        assertFalse(SearchText.ramMatches("Apple MacBook Pro 8GB 512GB", 16))
        assertTrue(SearchText.storageMatches("Apple MacBook Pro 16GB 512GB", 512))
        assertFalse(SearchText.storageMatches("Apple MacBook Pro 16GB 512GB", 256))
        assertTrue(SearchText.screenSizeMatches("MacBook Pro 16-inch", 16.0))
        assertFalse(SearchText.screenSizeMatches("MacBook Pro 14-inch", 16.0))
    }

    @Test
    fun applyFilters_filtersAndSortsCheapestFirst() {
        val items = listOf(
            SearchResultItem("MacBook Pro 16GB 512GB", 57900L, "GBP", 1, "Amazon", "https://a"),
            SearchResultItem("MacBook Pro 16GB 512GB", 41900L, "GBP", 28, "eBay", "https://b"),
            SearchResultItem("MacBook Pro 8GB 256GB", 39900L, "GBP", 28, "eBay", "https://c"),
            SearchResultItem("MacBook Pro 16GB 1TB", 89900L, "GBP", 3, "Currys", "https://d")
        )
        val filters = SearchFilters(
            query = "MacBook Pro",
            ramGb = 16,
            storageGb = 512,
            minPriceMinor = 40000L,
            maxPriceMinor = 60000L
        )
        val result = SearchText.applyFilters(items, filters)
        assertEquals(2, result.size)
        assertEquals(41900L, result[0].priceMinor)
        assertEquals(57900L, result[1].priceMinor)
    }

    @Test
    fun buildSearchUrl_encodesQuery() {
        val url = SearchText.buildSearchUrl(
            "https://www.ebay.co.uk/sch/i.html?_nkw={q}&LH_BIN=1",
            "macbook pro 16gb"
        )
        assertEquals(
            "https://www.ebay.co.uk/sch/i.html?_nkw=macbook%20pro%2016gb&LH_BIN=1",
            url
        )
    }

    @Test
    fun inferSubcategory_fromQueryWords() {
        assertEquals(Subcategory.LAPTOPS, SearchText.inferSubcategory("MacBook Pro"))
        assertEquals(Subcategory.SMARTPHONES, SearchText.inferSubcategory("iPhone 17"))
        assertEquals(Subcategory.SMARTPHONES, SearchText.inferSubcategory("Nothing Phone 4a Pro"))
        assertEquals(Subcategory.TABLETS, SearchText.inferSubcategory("iPad Air"))
        assertEquals(Subcategory.TELEVISIONS, SearchText.inferSubcategory("Samsung 55 inch TV"))
    }
}
