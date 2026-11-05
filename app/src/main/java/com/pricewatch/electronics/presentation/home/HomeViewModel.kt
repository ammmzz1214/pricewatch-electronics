package com.pricewatch.electronics.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import com.pricewatch.electronics.presentation.util.CheckProgress
import com.pricewatch.electronics.presentation.util.PriceCheckRunner
import com.pricewatch.electronics.presentation.util.applySortAndFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProductSort {
    LOWEST_PRICE, HIGHEST_PRICE, LARGEST_DROP, LARGEST_RISE, RECENTLY_ADDED, RECENTLY_CHECKED
}

enum class ProductFilter {
    ALL, DECREASED, INCREASED, UNCHANGED, CHECK_FAILED, TARGET_REACHED
}

data class HomeStats(
    val totalTracked: Int = 0,
    val falling: Int = 0,
    val rising: Int = 0,
    val potentialSavingsMinorUnits: Long = 0L,
    val successfulChecks: Int = 0,
    val failedChecks: Int = 0,
    val lastSuccessfulSyncAtEpochMillis: Long? = null
)

data class CheckNowUiState(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val currentProductName: String? = null,
    val checkedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val remainingCount: Int = 0,
    val totalCount: Int = 0,
    val isComplete: Boolean = false,
    val summary: String? = null
)

data class HomeUiState(
    val products: List<Product> = emptyList(),
    val siteNames: Map<Long, String> = emptyMap(),
    val stats: HomeStats = HomeStats(),
    val sort: ProductSort = ProductSort.RECENTLY_CHECKED,
    val filter: ProductFilter = ProductFilter.ALL,
    val checkNow: CheckNowUiState = CheckNowUiState(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository,
    private val priceCheckRunner: PriceCheckRunner
) : ViewModel() {

    private val sortFlow = MutableStateFlow(ProductSort.RECENTLY_CHECKED)
    private val filterFlow = MutableStateFlow(ProductFilter.ALL)
    private val checkNowFlow = MutableStateFlow(CheckNowUiState())
    private val errorFlow = MutableStateFlow<String?>(null)
    private var checkJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        productRepository.observeProducts(),
        siteRepository.observeSites(),
        sortFlow,
        filterFlow,
        checkNowFlow,
        errorFlow
    ) { products, sites, sort, filter, checkNow, error ->
        HomeUiState(
            products = applySortAndFilter(products, sort, filter),
            siteNames = sites.associate { it.id to it.name },
            stats = computeStats(products),
            sort = sort,
            filter = filter,
            checkNow = checkNow,
            isLoading = false,
            errorMessage = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setSort(sort: ProductSort) {
        sortFlow.value = sort
    }

    fun setFilter(filter: ProductFilter) {
        filterFlow.value = filter
    }

    fun clearError() {
        errorFlow.value = null
    }

    /**
     * Starts a real check of every tracked product via [PriceCheckRunner].
     * Duplicate requests are ignored while a run is in progress. Progress is
     * the real completed/total ratio; a failed check never overwrites the
     * last known price (enforced in the repository).
     */
    fun checkNow() {
        if (checkNowFlow.value.isRunning) return
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val products = try {
                productRepository.observeProducts().first()
            } catch (e: Exception) {
                errorFlow.value = "Could not load products: ${e.message ?: "Unknown error"}"
                return@launch
            }
            if (products.isEmpty()) return@launch
            checkNowFlow.value = CheckNowUiState(isRunning = true, totalCount = products.size)
            try {
                priceCheckRunner.checkAll(products) { progress: CheckProgress ->
                    val total = progress.total.coerceAtLeast(1)
                    checkNowFlow.value = CheckNowUiState(
                        isRunning = !progress.isComplete,
                        progress = progress.completed.toFloat() / total,
                        currentProductName = progress.currentProductName,
                        checkedCount = progress.completed,
                        successCount = progress.succeeded,
                        failedCount = progress.failed,
                        remainingCount = (progress.total - progress.completed).coerceAtLeast(0),
                        totalCount = progress.total,
                        isComplete = progress.isComplete,
                        summary = if (progress.isComplete) {
                            "Checked ${progress.total} products: " +
                                "${progress.succeeded} succeeded, ${progress.failed} failed."
                        } else {
                            null
                        }
                    )
                }
            } catch (e: Exception) {
                errorFlow.value = "Check failed: ${e.message ?: "Unknown error"}"
                checkNowFlow.value = CheckNowUiState()
            }
        }
    }

    fun cancelCheckNow() {
        checkJob?.cancel()
        checkJob = null
        checkNowFlow.value = CheckNowUiState()
    }

    /** Dismisses the completion summary and returns the dashboard to idle. */
    fun dismissCheckSummary() {
        checkNowFlow.value = CheckNowUiState()
    }

    private fun computeStats(products: List<Product>): HomeStats {
        var falling = 0
        var rising = 0
        var savings = 0L
        var succeeded = 0
        var failed = 0
        var lastSync: Long? = null
        for (product in products) {
            val previous = product.previousPriceMinorUnits
            if (previous != null) {
                when {
                    product.currentPriceMinorUnits < previous -> {
                        falling++
                        savings += previous - product.currentPriceMinorUnits
                    }
                    product.currentPriceMinorUnits > previous -> rising++
                }
            }
            when (product.checkStatus) {
                CheckStatus.CHECK_OK,
                CheckStatus.OK,
                CheckStatus.PRICE_DECREASED,
                CheckStatus.PRICE_INCREASED,
                CheckStatus.PRICE_UNCHANGED -> succeeded++
                CheckStatus.CHECK_FAILED -> failed++
                CheckStatus.NEVER_CHECKED,
                CheckStatus.CHECKING -> Unit
            }
            val at = product.lastSuccessfulCheckAt
            if (at != null && (lastSync == null || at > lastSync)) lastSync = at
        }
        return HomeStats(
            totalTracked = products.size,
            falling = falling,
            rising = rising,
            potentialSavingsMinorUnits = savings.coerceAtLeast(0L),
            successfulChecks = succeeded,
            failedChecks = failed,
            lastSuccessfulSyncAtEpochMillis = lastSync
        )
    }
}
