package com.pricewatch.electronics.presentation.sites

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SiteDetailUiState(
    val site: Site? = null,
    val trackedProducts: List<Product> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SiteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    siteRepository: SiteRepository,
    productRepository: ProductRepository
) : ViewModel() {

    private val siteId: Long = savedStateHandle["siteId"] ?: 0L

    val uiState: StateFlow<SiteDetailUiState> = combine(
        siteRepository.observeSite(siteId),
        productRepository.observeProducts()
    ) { site, products ->
        SiteDetailUiState(
            site = site,
            trackedProducts = products.filter { it.siteId == siteId }
                .sortedByDescending { it.lastSuccessfulCheckAt ?: 0L },
            isLoading = site == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SiteDetailUiState())
}
