package com.pricewatch.electronics.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.data.search.SiteSearchConfigs
import com.pricewatch.electronics.domain.models.SearchFilters
import com.pricewatch.electronics.domain.models.SearchResultItem
import com.pricewatch.electronics.domain.models.SiteSearchOutcome
import com.pricewatch.electronics.domain.usecases.SearchAcrossSitesUseCase
import com.pricewatch.electronics.domain.usecases.TrackSearchResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val ramGb: Int? = null,
    val storageGb: Int? = null,
    val screenSizeInches: Double? = null,
    val minPriceText: String = "",
    val maxPriceText: String = "",
    val isSearching: Boolean = false,
    val searchedSiteCount: Int = 0,
    val totalSiteCount: Int = 0,
    val currentSiteName: String? = null,
    val outcomes: List<SiteSearchOutcome> = emptyList(),
    val results: List<SearchResultItem> = emptyList(),
    val hasSearched: Boolean = false,
    val trackedUrls: Set<String> = emptySet(),
    val snackbarMessage: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAcrossSites: SearchAcrossSitesUseCase,
    private val trackSearchResult: TrackSearchResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun updateQuery(query: String) = _uiState.update { it.copy(query = query) }

    fun updateRamGb(ramGb: Int?) = _uiState.update { it.copy(ramGb = ramGb) }

    fun updateStorageGb(storageGb: Int?) = _uiState.update { it.copy(storageGb = storageGb) }

    fun updateScreenSize(size: Double?) = _uiState.update { it.copy(screenSizeInches = size) }

    fun updateMinPrice(text: String) = _uiState.update { it.copy(minPriceText = text) }

    fun updateMaxPrice(text: String) = _uiState.update { it.copy(maxPriceText = text) }

    fun clearSnackbar() = _uiState.update { it.copy(snackbarMessage = null) }

    fun cancelSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(isSearching = false, currentSiteName = null) }
    }

    fun search() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Type a product name first.") }
            return
        }
        val minMinor = state.minPriceText.trim().takeIf { it.isNotEmpty() }?.let {
            Money.fromDecimalString(it)
        }
        val maxMinor = state.maxPriceText.trim().takeIf { it.isNotEmpty() }?.let {
            Money.fromDecimalString(it)
        }
        if (state.minPriceText.isNotBlank() && minMinor == null) {
            _uiState.update { it.copy(snackbarMessage = "Min price is not a valid amount.") }
            return
        }
        if (state.maxPriceText.isNotBlank() && maxMinor == null) {
            _uiState.update { it.copy(snackbarMessage = "Max price is not a valid amount.") }
            return
        }
        if (minMinor != null && maxMinor != null && minMinor > maxMinor) {
            _uiState.update { it.copy(snackbarMessage = "Min price must be below max price.") }
            return
        }
        val filters = SearchFilters(
            query = query,
            ramGb = state.ramGb,
            storageGb = state.storageGb,
            screenSizeInches = state.screenSizeInches,
            minPriceMinor = minMinor,
            maxPriceMinor = maxMinor
        )
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearching = true,
                    searchedSiteCount = 0,
                    totalSiteCount = SiteSearchConfigs.all().size,
                    currentSiteName = null,
                    outcomes = emptyList(),
                    results = emptyList(),
                    hasSearched = false,
                    snackbarMessage = null
                )
            }
            val results = searchAcrossSites.invoke(filters) { outcome ->
                _uiState.update { current ->
                    current.copy(
                        outcomes = current.outcomes + outcome,
                        searchedSiteCount = current.searchedSiteCount + 1,
                        currentSiteName = outcome.siteName
                    )
                }
            }
            _uiState.update {
                it.copy(
                    isSearching = false,
                    currentSiteName = null,
                    results = results,
                    hasSearched = true,
                    snackbarMessage = if (results.isEmpty()) {
                        "No matching results in your price range. Try widening the range or clearing a filter."
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun trackResult(item: SearchResultItem) {
        viewModelScope.launch {
            when (val result = trackSearchResult(item, _uiState.value.query)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        trackedUrls = it.trackedUrls + item.productUrl,
                        snackbarMessage = "Saved to My Products — price alerts are on."
                    )
                }
                is Result.Error -> {
                    val alreadyTracked = result.message.contains("already exists", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            trackedUrls = if (alreadyTracked) it.trackedUrls + item.productUrl else it.trackedUrls,
                            snackbarMessage = if (alreadyTracked) {
                                "Already in My Products."
                            } else {
                                "Could not save: ${result.message}"
                            }
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }
}
