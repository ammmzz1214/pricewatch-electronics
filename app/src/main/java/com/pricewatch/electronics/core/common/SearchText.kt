package com.pricewatch.electronics.core.common

import com.pricewatch.electronics.domain.models.SearchFilters
import com.pricewatch.electronics.domain.models.SearchResultItem
import com.pricewatch.electronics.domain.models.Subcategory
import java.net.URLEncoder

/**
 * Pure search helpers — no Android dependencies, fully unit-testable.
 *
 * Spec matching is deliberately lenient: a filter only excludes a result
 * when the spec is confidently present in the title and does not match.
 * A title with no parseable spec always passes that filter, so results
 * are never hidden just because the listing text is terse.
 */
object SearchText {

    private val PRICE_REGEX = Regex("""£\s?([\d,]+(?:\.\d{1,2})?)""")
    private val GB_REGEX = Regex("""(\d+)\s*GB\b""", RegexOption.IGNORE_CASE)
    private val TB_REGEX = Regex("""(\d+(?:\.\d+)?)\s*TB\b""", RegexOption.IGNORE_CASE)
    private val RAM_WORD_REGEX = Regex("""(\d+)\s*GB\s*(RAM|DDR|LPDDR|MEMORY)\b""", RegexOption.IGNORE_CASE)
    private val SCREEN_REGEX = Regex("""(\d+(?:\.\d+)?)\s*-?\s*(?:inch|inches|in\b|")""", RegexOption.IGNORE_CASE)

    /** "£1,234.56" -> 123456 (pence). First £ price in the text wins. */
    fun parsePriceToMinor(text: String): Long? {
        val match = PRICE_REGEX.find(text) ?: return null
        val cleaned = match.groupValues[1].replace(",", "")
        return Money.fromDecimalString(cleaned)
    }

    /** RAM in GB from a listing title, e.g. "MacBook Pro 16GB 512GB" -> 16. */
    fun extractRamGb(title: String): Int? {
        RAM_WORD_REGEX.find(title)?.let { return it.groupValues[1].toIntOrNull() }
        val allGb = GB_REGEX.findAll(title).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (allGb.isEmpty()) return null
        val hasStorageMarker = title.contains("SSD", ignoreCase = true) ||
            title.contains("TB", ignoreCase = true) ||
            title.contains("storage", ignoreCase = true) ||
            title.contains("NVMe", ignoreCase = true) ||
            title.contains("UFS", ignoreCase = true)
        // With two GB numbers (e.g. "16GB 512GB"), the first is RAM.
        // With one GB number and a storage marker, it is storage, not RAM.
        return when {
            allGb.size >= 2 -> allGb.first()
            hasStorageMarker -> null
            else -> allGb.first()
        }
    }

    /** Storage in GB from a listing title, e.g. "512GB SSD" -> 512, "1TB" -> 1024. */
    fun extractStorageGb(title: String): Int? {
        TB_REGEX.find(title)?.let { tb ->
            val value = tb.groupValues[1].toDoubleOrNull() ?: return null
            return (value * 1024).toInt()
        }
        val allGb = GB_REGEX.findAll(title).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (allGb.isEmpty()) return null
        // With two GB numbers (e.g. "16GB 512GB"), the last is storage.
        return if (allGb.size >= 2) allGb.last() else allGb.first()
    }

    /** Screen size in inches from a listing title, e.g. '16" MacBook' -> 16.0. */
    fun extractScreenSizeInches(title: String): Double? =
        SCREEN_REGEX.find(title)?.groupValues?.get(1)?.toDoubleOrNull()

    fun ramMatches(title: String, wantedGb: Int?): Boolean {
        if (wantedGb == null) return true
        val ram = extractRamGb(title) ?: return true
        return ram == wantedGb
    }

    fun storageMatches(title: String, wantedGb: Int?): Boolean {
        if (wantedGb == null) return true
        val storage = extractStorageGb(title) ?: return true
        return storage == wantedGb
    }

    fun screenSizeMatches(title: String, wantedInches: Double?): Boolean {
        if (wantedInches == null) return true
        val size = extractScreenSizeInches(title) ?: return true
        return kotlin.math.abs(size - wantedInches) <= 0.6
    }

    fun priceInRange(priceMinor: Long, filters: SearchFilters): Boolean {
        if (filters.minPriceMinor != null && priceMinor < filters.minPriceMinor) return false
        if (filters.maxPriceMinor != null && priceMinor > filters.maxPriceMinor) return false
        return true
    }

    /** All filters applied; results are cheapest-first after this. */
    fun applyFilters(items: List<SearchResultItem>, filters: SearchFilters): List<SearchResultItem> =
        items.filter { item ->
            ramMatches(item.title, filters.ramGb) &&
                storageMatches(item.title, filters.storageGb) &&
                screenSizeMatches(item.title, filters.screenSizeInches) &&
                priceInRange(item.priceMinor, filters)
        }.sortedBy { it.priceMinor }

    fun buildSearchUrl(template: String, query: String): String {
        val encoded = URLEncoder.encode(query.trim(), Charsets.UTF_8.name()).replace("+", "%20")
        return template.replace("{q}", encoded)
    }

    /** Best-guess subcategory from the search words, for tracked products. */
    fun inferSubcategory(query: String): Subcategory {
        val q = query.lowercase()
        return when {
            q.contains("macbook") || q.contains("laptop") || q.contains("notebook") ||
                q.contains("chromebook") -> Subcategory.LAPTOPS
            q.contains("iphone") || q.contains("phone") || q.contains("galaxy") ||
                q.contains("pixel") || q.contains("oneplus") || q.contains("xiaomi") ||
                q.contains("redmi") || q.contains("nothing phone") || q.contains("motorola") ||
                q.contains("honor") || q.contains("oppo") || q.contains("realme") ||
                q.contains("poco") -> Subcategory.SMARTPHONES
            q.contains("ipad") || q.contains("tablet") -> Subcategory.TABLETS
            q.contains(" tv") || q.contains("television") || q.contains("oled tv") ||
                q.contains("qled") -> Subcategory.TELEVISIONS
            q.contains("headphone") || q.contains("earbud") || q.contains("airpod") ->
                Subcategory.HEADPHONES
            q.contains("monitor") -> Subcategory.MONITORS
            q.contains("camera") -> Subcategory.CAMERAS
            q.contains("console") || q.contains("playstation") || q.contains("xbox") ||
                q.contains("nintendo") -> Subcategory.GAMING_CONSOLES
            q.contains("ssd") -> Subcategory.SSDS
            q.contains("hdd") -> Subcategory.HDDS
            q.contains("graphics") || q.contains("gpu") || q.contains("rtx") ||
                q.contains("radeon") -> Subcategory.GRAPHICS_CARDS
            q.contains("watch") -> Subcategory.SMART_HOME_DEVICES
            else -> Subcategory.LAPTOPS
        }
    }
}
