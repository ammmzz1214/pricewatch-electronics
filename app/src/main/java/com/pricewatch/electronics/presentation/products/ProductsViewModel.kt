package com.pricewatch.electronics.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import com.pricewatch.electronics.domain.usecases.DeleteProductUseCase
import com.pricewatch.electronics.presentation.home.ProductFilter
import com.pricewatch.electronics.presentation.home.ProductSort
import com.pricewatch.electronics.presentation.util.applySortAndFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductsUiState(
    val products: List<Product> = emptyList(),
    val siteNames: Map<Long, String> = emptyMap(),
    val searchQuery: String = "",
    val sort: ProductSort = ProductSort.RECENTLY_ADDED,
    val filter: ProductFilter = ProductFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val deletedProduct: Product? = null,
    val showUndo: Boolean = false
)

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val siteRepository: SiteRepository,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val sortFlow = MutableStateFlow(ProductSort.RECENTLY_ADDED)
    private val filterFlow = MutableStateFlow(ProductFilter.ALL)
    private val errorFlow = MutableStateFlow<String?>(null)
    private val deletedFlow = MutableStateFlow<Product?>(null)

    val uiState: StateFlow<ProductsUiState> = combine(
        productRepository.observeProducts(),
        siteRepository.observeSites(),
        queryFlow,
        sortFlow,
        filterFlow,
        errorFlow,
        deletedFlow
    ) { args ->
        val products = args[0] as List<Product>
        val sites = args[1] as List<Site>
        val query = args[2] as String
        val sort = args[3] as ProductSort
        val filter = args[4] as ProductFilter
        val error = args[5] as String?
        val deleted = args[6] as Product?
        val siteNames = sites.associate { it.id to it.name }
        val searched = if (query.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(query, ignoreCase = true) ||
                    (siteNames[it.siteId] ?: "").contains(query, ignoreCase = true)
            }
        }
        ProductsUiState(
            products = applySortAndFilter(searched, sort, filter),
            siteNames = siteNames,
            searchQuery = query,
            sort = sort,
            filter = filter,
            isLoading = false,
            errorMessage = error,
            deletedProduct = deleted,
            showUndo = deleted != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductsUiState())

    fun setSearchQuery(query: String) {
        queryFlow.value = query
    }

    fun setSort(sort: ProductSort) {
        sortFlow.value = sort
    }

    fun setFilter(filter: ProductFilter) {
        filterFlow.value = filter
    }

    fun clearError() {
        errorFlow.value = null
    }

    /** Deletes one product only; history is removed by CASCADE in the DB. */
    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            when (val result = deleteProductUseCase(product.id)) {
                is Result.Success -> deletedFlow.value = product
                is Result.Error -> errorFlow.value =
                    "Could not delete ${product.name}: ${result.message}"
                Result.Loading -> Unit
            }
        }
    }

    /**
     * Restores the most recently deleted product as a new row (a fresh id and
     * a fresh baseline history record — the deleted history itself was
     * cascade-deleted and cannot be recovered).
     */
    fun undoDelete() {
        val product = deletedFlow.value ?: return
        deletedFlow.value = null
        viewModelScope.launch {
            when (val result = productRepository.addProduct(product.copy(id = 0))) {
                is Result.Success -> Unit
                is Result.Error -> errorFlow.value =
                    "Could not restore ${product.name}: ${result.message}"
                Result.Loading -> Unit
            }
        }
    }

    fun dismissUndo() {
        deletedFlow.value = null
    }
}
