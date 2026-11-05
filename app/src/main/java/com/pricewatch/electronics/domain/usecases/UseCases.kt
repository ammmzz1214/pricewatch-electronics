package com.pricewatch.electronics.domain.usecases

import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.core.common.UrlValidator
import com.pricewatch.electronics.domain.models.Category
import com.pricewatch.electronics.domain.models.PriceHistory
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.repositories.PriceHistoryRepository
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class ValidationResult(val isValid: Boolean, val errors: List<String> = emptyList())

class ValidateProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository
) {
    suspend operator fun invoke(product: Product): ValidationResult {
        val errors = mutableListOf<String>()
        if (product.category != Category.ELECTRONICS) errors += "Category must be Electronics."
        if (product.name.isBlank()) errors += "Product name is required."
        if (product.name.length > 200) errors += "Product name is too long."
        if (!UrlValidator.isValidUrl(product.productUrl)) errors += "A valid product URL is required."
        if (!Money.isValidPrice(product.currentPriceMinorUnits)) errors += "Price must be greater than zero."
        if (!Money.isValidCurrency(product.currencyCode)) errors += "Currency is invalid."
        if (product.targetPriceMinorUnits != null && !Money.isValidPrice(product.targetPriceMinorUnits)) {
            errors += "Target price must be greater than zero."
        }
        val site: Site? = siteRepository.getSite(product.siteId)
        if (site == null) {
            errors += "A valid shop website is required."
        } else if (!UrlValidator.matchesSiteDomain(product.productUrl, site.domain)) {
            errors += "Product URL does not match the selected website (${site.domain})."
        }
        if (errors.isEmpty() && productRepository.existsDuplicate(product.productUrl, if (product.id == 0L) null else product.id)) {
            errors += "A product with this URL already exists."
        }
        return ValidationResult(errors.isEmpty(), errors)
    }
}

class AddProductUseCase @Inject constructor(
    private val productRepository: ProductRepository,
    private val validateProduct: ValidateProductUseCase
) {
    suspend operator fun invoke(product: Product): Result<Long> {
        val validation = validateProduct(product)
        if (!validation.isValid) return Result.Error(validation.errors.joinToString("; "))
        return productRepository.addProduct(product)
    }
}

class DeleteProductUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: Long): Result<Unit> =
        productRepository.deleteProduct(productId)
}

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> = productRepository.observeProducts()
    suspend fun getOne(id: Long): Product? = productRepository.getProduct(id)
}

class GetPriceHistoryUseCase @Inject constructor(
    private val historyRepository: PriceHistoryRepository
) {
    operator fun invoke(productId: Long): Flow<List<PriceHistory>> =
        historyRepository.observeHistory(productId)

    suspend fun inRange(productId: Long, sinceMillis: Long?): List<PriceHistory> =
        historyRepository.getHistory(productId, sinceMillis)
}

class CalculatePriceChangeUseCase @Inject constructor() {
    data class Change(val differenceMinorUnits: Long, val percentageChange: Double?)

    operator fun invoke(oldPriceMinorUnits: Long, newPriceMinorUnits: Long): Change =
        Change(
            differenceMinorUnits = Money.differenceMinorUnits(oldPriceMinorUnits, newPriceMinorUnits),
            percentageChange = Money.percentageChange(oldPriceMinorUnits, newPriceMinorUnits)
        )
}
