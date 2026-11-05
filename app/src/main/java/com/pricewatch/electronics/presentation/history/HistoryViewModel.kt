package com.pricewatch.electronics.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.domain.models.PriceHistory
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.repositories.PriceHistoryRepository
import com.pricewatch.electronics.domain.repositories.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class HistoryRange(val label: String, val millis: Long?) {
    LAST_24H("24 Hours", 24L * 60 * 60 * 1000),
    DAYS_7("7 Days", 7L * 24 * 60 * 60 * 1000),
    DAYS_30("30 Days", 30L * 24 * 60 * 60 * 1000),
    DAYS_90("90 Days", 90L * 24 * 60 * 60 * 1000),
    MONTHS_6("6 Months", 182L * 24 * 60 * 60 * 1000),
    ALL("All Time", null)
}

data class HistoryStats(
    val totalChanges: Int = 0,
    val biggestDropMinorUnits: Long? = null,
    val biggestRiseMinorUnits: Long? = null,
    val averageChangeMinorUnits: Long? = null
)

data class ChartPoint(val timestamp: Long, val priceMinorUnits: Long)

data class HistoryUiState(
    val entries: List<PriceHistory> = emptyList(),
    val products: List<Product> = emptyList(),
    val productNames: Map<Long, String> = emptyMap(),
    val selectedProductId: Long? = null,
    val chartProductId: Long? = null,
    val chartPoints: List<ChartPoint> = emptyList(),
    val range: HistoryRange = HistoryRange.DAYS_30,
    val stats: HistoryStats = HistoryStats(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    historyRepository: PriceHistoryRepository,
    productRepository: ProductRepository
) : ViewModel() {

    private val rangeFlow = MutableStateFlow(HistoryRange.DAYS_30)
    private val productFilterFlow = MutableStateFlow<Long?>(null)
    private val chartProductFlow = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        historyRepository.observeAllHistory(),
        productRepository.observeProducts(),
        rangeFlow,
        productFilterFlow,
        chartProductFlow
    ) { allHistory, products, range, selectedProductId, chartProductId ->
        val since = range.millis?.let { System.currentTimeMillis() - it }
        var entries = allHistory
        if (since != null) entries = entries.filter { it.timestamp >= since }
        if (selectedProductId != null) {
            entries = entries.filter { it.productId == selectedProductId }
        }
        entries = entries.sortedByDescending { it.timestamp }

        // Chart product: explicit choice, else the first product with history
        // in range, else the first tracked product.
        val resolvedChartId = chartProductId
            ?: entries.firstOrNull()?.productId
            ?: products.firstOrNull()?.id
        val chartProduct = products.firstOrNull { it.id == resolvedChartId }
        val chartEntries = allHistory
            .filter { it.productId == resolvedChartId }
            .filter { since == null || it.timestamp >= since }
            .sortedBy { it.timestamp }
        val points = chartEntries
            .map { ChartPoint(it.timestamp, it.newPriceMinorUnits) }
            .toMutableList()
        if (chartProduct != null && (points.isEmpty() || points.last().priceMinorUnits != chartProduct.currentPriceMinorUnits)) {
            points += ChartPoint(System.currentTimeMillis(), chartProduct.currentPriceMinorUnits)
        }

        val changes = entries.filter { it.differenceMinorUnits != 0L }
        HistoryUiState(
            entries = entries,
            products = products,
            productNames = products.associate { it.id to it.name },
            selectedProductId = selectedProductId,
            chartProductId = resolvedChartId,
            chartPoints = points,
            range = range,
            stats = HistoryStats(
                totalChanges = changes.size,
                biggestDropMinorUnits = changes.minOfOrNull { it.differenceMinorUnits },
                biggestRiseMinorUnits = changes.maxOfOrNull { it.differenceMinorUnits },
                averageChangeMinorUnits = if (changes.isEmpty()) null
                else changes.sumOf { it.differenceMinorUnits } / changes.size
            ),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(range: HistoryRange) {
        rangeFlow.value = range
    }

    fun setProductFilter(productId: Long?) {
        productFilterFlow.value = productId
    }

    fun setChartProduct(productId: Long) {
        chartProductFlow.value = productId
    }
}
