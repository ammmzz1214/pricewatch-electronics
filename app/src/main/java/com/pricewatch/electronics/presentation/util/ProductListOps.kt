package com.pricewatch.electronics.presentation.util

import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.presentation.home.ProductFilter
import com.pricewatch.electronics.presentation.home.ProductSort

/** Shared sort/filter logic for the Home dashboard and My Products. */
fun applySortAndFilter(
    products: List<Product>,
    sort: ProductSort,
    filter: ProductFilter
): List<Product> {
    val filtered = when (filter) {
        ProductFilter.ALL -> products
        ProductFilter.DECREASED -> products.filter {
            priceDirection(it.previousPriceMinorUnits, it.currentPriceMinorUnits) == PriceDirection.DOWN
        }
        ProductFilter.INCREASED -> products.filter {
            priceDirection(it.previousPriceMinorUnits, it.currentPriceMinorUnits) == PriceDirection.UP
        }
        ProductFilter.UNCHANGED -> products.filter {
            it.previousPriceMinorUnits != null &&
                priceDirection(it.previousPriceMinorUnits, it.currentPriceMinorUnits) == PriceDirection.UNCHANGED
        }
        ProductFilter.CHECK_FAILED -> products.filter { it.checkStatus == CheckStatus.CHECK_FAILED }
        ProductFilter.TARGET_REACHED -> products.filter { it.targetReached }
    }
    return when (sort) {
        ProductSort.LOWEST_PRICE -> filtered.sortedBy { it.currentPriceMinorUnits }
        ProductSort.HIGHEST_PRICE -> filtered.sortedByDescending { it.currentPriceMinorUnits }
        ProductSort.LARGEST_DROP -> filtered.sortedByDescending {
            (it.previousPriceMinorUnits ?: it.currentPriceMinorUnits) - it.currentPriceMinorUnits
        }
        ProductSort.LARGEST_RISE -> filtered.sortedByDescending {
            it.currentPriceMinorUnits - (it.previousPriceMinorUnits ?: it.currentPriceMinorUnits)
        }
        ProductSort.RECENTLY_ADDED -> filtered.sortedByDescending { it.createdAt }
        ProductSort.RECENTLY_CHECKED -> filtered.sortedByDescending { it.lastSuccessfulCheckAt ?: 0L }
    }
}

fun sortLabel(sort: ProductSort): String = when (sort) {
    ProductSort.LOWEST_PRICE -> "Lowest price"
    ProductSort.HIGHEST_PRICE -> "Highest price"
    ProductSort.LARGEST_DROP -> "Largest price drop"
    ProductSort.LARGEST_RISE -> "Largest price increase"
    ProductSort.RECENTLY_ADDED -> "Recently added"
    ProductSort.RECENTLY_CHECKED -> "Recently checked"
}

fun filterLabel(filter: ProductFilter): String = when (filter) {
    ProductFilter.ALL -> "All"
    ProductFilter.DECREASED -> "Decreased"
    ProductFilter.INCREASED -> "Increased"
    ProductFilter.UNCHANGED -> "Unchanged"
    ProductFilter.CHECK_FAILED -> "Check failed"
    ProductFilter.TARGET_REACHED -> "Target reached"
}
