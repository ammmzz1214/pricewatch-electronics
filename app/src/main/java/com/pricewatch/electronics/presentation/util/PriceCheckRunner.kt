package com.pricewatch.electronics.presentation.util

import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.data.providers.ProviderRegistry
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import javax.inject.Inject

/** Real progress of a check run: completed/total is never simulated. */
data class CheckProgress(
    val total: Int,
    val completed: Int,
    val succeeded: Int,
    val failed: Int,
    val currentProductName: String?,
    val isComplete: Boolean
)

sealed interface CheckOutcome {
    data class Success(val newPriceMinorUnits: Long) : CheckOutcome
    data class Failed(val message: String) : CheckOutcome
}

/**
 * Runs real price checks through the provider architecture:
 * site -> ProviderRegistry -> PriceProvider.fetchCurrentPrice, then
 * ProductRepository.recordSuccessfulCheck / recordFailedCheck.
 *
 * Today every seeded site resolves to ManualPriceProvider (eBay is partial
 * support and also declines automated fetching), so a check run honestly
 * records CHECK_FAILED with the provider's message and NEVER overwrites the
 * last known price — the failed-check rule is enforced in the repository.
 * When a verified live provider lands, the same code path records real
 * price changes and history.
 */
class PriceCheckRunner @Inject constructor(
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository,
    private val providerRegistry: ProviderRegistry
) {
    suspend fun checkProduct(product: Product): CheckOutcome {
        val site = siteRepository.getSite(product.siteId)
            ?: return CheckOutcome.Failed("Shop website not found.")
        return when (val result = providerRegistry.providerFor(site).fetchCurrentPrice(product.productUrl)) {
            is Result.Success -> {
                val price = result.data
                if (!price.currencyCode.equals(product.currencyCode, ignoreCase = true)) {
                    productRepository.recordFailedCheck(
                        product.id,
                        "CURRENCY_MISMATCH",
                        "Provider returned ${price.currencyCode}, product is ${product.currencyCode}."
                    )
                    return CheckOutcome.Failed(
                        "Provider returned a different currency (${price.currencyCode}). Price unchanged."
                    )
                }
                when (val recorded = productRepository.recordSuccessfulCheck(
                    product.id, price.priceMinorUnits, site.domain
                )) {
                    is Result.Success -> CheckOutcome.Success(price.priceMinorUnits)
                    is Result.Error -> CheckOutcome.Failed(recorded.message)
                    Result.Loading -> CheckOutcome.Failed("Unexpected check state.")
                }
            }
            is Result.Error -> {
                productRepository.recordFailedCheck(product.id, "PROVIDER_ERROR", result.message)
                CheckOutcome.Failed(result.message)
            }
            Result.Loading -> CheckOutcome.Failed("Unexpected check state.")
        }
    }

    suspend fun checkAll(products: List<Product>, onProgress: (CheckProgress) -> Unit) {
        if (products.isEmpty()) {
            onProgress(CheckProgress(0, 0, 0, 0, null, true))
            return
        }
        var succeeded = 0
        var failed = 0
        products.forEachIndexed { index, product ->
            onProgress(CheckProgress(products.size, index, succeeded, failed, product.name, false))
            when (checkProduct(product)) {
                is CheckOutcome.Success -> succeeded++
                is CheckOutcome.Failed -> failed++
            }
            onProgress(
                CheckProgress(
                    total = products.size,
                    completed = index + 1,
                    succeeded = succeeded,
                    failed = failed,
                    currentProductName = product.name,
                    isComplete = index + 1 == products.size
                )
            )
        }
    }
}
