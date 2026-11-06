package com.pricewatch.electronics.presentation.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.domain.models.PriceHistory
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.repositories.PriceHistoryRepository
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import com.pricewatch.electronics.domain.usecases.DeleteProductUseCase
import com.pricewatch.electronics.presentation.util.CheckOutcome
import com.pricewatch.electronics.presentation.util.PriceCheckRunner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailStats(
    val lowestMinorUnits: Long? = null,
    val highestMinorUnits: Long? = null,
    val averageMinorUnits: Long? = null,
    val totalChanges: Int = 0,
    val percentChange: Double? = null
)

data class ProductDetailUiState(
    val product: Product? = null,
    val history: List<PriceHistory> = emptyList(),
    val stats: ProductDetailStats = ProductDetailStats(),
    val isLoading: Boolean = true,
    val isChecking: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val deleted: Boolean = false
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val historyRepository: PriceHistoryRepository,
    private val siteRepository: SiteRepository,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val priceCheckRunner: PriceCheckRunner
) : ViewModel() {

    private val productId: Long = savedStateHandle["productId"] ?: 0L

    private val errorFlow = MutableStateFlow<String?>(null)
    private val infoFlow = MutableStateFlow<String?>(null)
    private val checkingFlow = MutableStateFlow(false)
    private val deletedFlow = MutableStateFlow(false)

    val uiState: StateFlow<ProductDetailUiState> = combine(
        productRepository.observeProduct(productId),
        historyRepository.observeHistory(productId),
        errorFlow,
        infoFlow,
        checkingFlow,
        deletedFlow
    ) { product: Product?, history: List<PriceHistory>, error: String?, info: String?, checking: Boolean, deleted: Boolean ->
        ProductDetailUiState(
            product = product,
            history = history,
            stats = computeStats(product, history),
            isLoading = product == null && !deleted,
            isChecking = checking,
            errorMessage = error,
            infoMessage = info,
            deleted = deleted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    /** Site is resolved from the product's siteId once the product loads. */
    val siteState: StateFlow<Site?> = combine(
        productRepository.observeProduct(productId),
        siteRepository.observeSites()
    ) { product, sites ->
        sites.firstOrNull { it.id == product?.siteId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun clearError() {
        errorFlow.value = null
    }

    fun clearInfo() {
        infoFlow.value = null
    }

    /** Runs one real check for this product through the provider architecture. */
    fun checkNow() {
        viewModelScope.launch {
            val product = productRepository.getProduct(productId) ?: run {
                errorFlow.value = "Product not found."
                return@launch
            }
            if (checkingFlow.value) return@launch
            checkingFlow.value = true
            when (val outcome = priceCheckRunner.checkProduct(product)) {
                is CheckOutcome.Success -> infoFlow.value =
                    "Checked: ${Money.format(outcome.newPriceMinorUnits, product.currencyCode)}"
                is CheckOutcome.Failed -> errorFlow.value =
                    "Check failed: ${outcome.message} Price unchanged."
            }
            checkingFlow.value = false
        }
    }

    /** Deletes this product (history cascade-deleted by the database). */
    fun deleteProduct() {
        viewModelScope.launch {
            when (val result = deleteProductUseCase(productId)) {
                is Result.Success -> deletedFlow.value = true
                is Result.Error -> errorFlow.value = "Could not delete: ${result.message}"
                Result.Loading -> Unit
            }
        }
    }

    /** Saves edits from the Edit dialog (name, price, target, monitoring). */
    fun updateProduct(
        name: String,
        priceMinorUnits: Long,
        targetPriceMinorUnits: Long?,
        monitoringEnabled: Boolean
    ) {
        viewModelScope.launch {
            val current = productRepository.getProduct(productId) ?: run {
                errorFlow.value = "Product not found."
                return@launch
            }
            if (name.isBlank()) {
                errorFlow.value = "Product name is required."
                return@launch
            }
            if (!Money.isValidPrice(priceMinorUnits)) {
                errorFlow.value = "Price must be greater than zero."
                return@launch
            }
            // A manual price edit is a verified user update: record it as a
            // successful check so history and badges stay truthful.
            val updated = current.copy(
                name = name.trim(),
                currentPriceMinorUnits = priceMinorUnits,
                targetPriceMinorUnits = targetPriceMinorUnits,
                monitoringEnabled = monitoringEnabled
            )
            when (val result = productRepository.updateProduct(updated)) {
                is Result.Success -> {
                    if (priceMinorUnits != current.currentPriceMinorUnits) {
                        productRepository.recordSuccessfulCheck(
                            productId, priceMinorUnits, "manual_edit"
                        )
                    }
                    infoFlow.value = "Product updated."
                }
                is Result.Error -> errorFlow.value = "Could not save: ${result.message}"
                Result.Loading -> Unit
            }
        }
    }

    private fun computeStats(product: Product?, history: List<PriceHistory>): ProductDetailStats {
        if (product == null) return ProductDetailStats()
        val prices = (history.map { it.newPriceMinorUnits } + product.currentPriceMinorUnits)
        val changes = history.count { it.differenceMinorUnits != 0L }
        return ProductDetailStats(
            lowestMinorUnits = prices.minOrNull(),
            highestMinorUnits = prices.maxOrNull(),
            averageMinorUnits = if (prices.isEmpty()) null else prices.sum() / prices.size,
            totalChanges = changes,
            percentChange = product.previousPriceMinorUnits?.let {
                Money.percentageChange(it, product.currentPriceMinorUnits)
            }
        )
    }
}
