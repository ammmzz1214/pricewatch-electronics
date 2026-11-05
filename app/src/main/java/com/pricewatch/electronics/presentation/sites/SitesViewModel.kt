package com.pricewatch.electronics.presentation.sites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.models.SiteGroup
import com.pricewatch.electronics.domain.repositories.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SitesUiState(
    val sites: List<Site> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val groupFilter: SiteGroup? = null,
    val sortAscending: Boolean = true,
    val isLoading: Boolean = true
)

@HiltViewModel
class SitesViewModel @Inject constructor(
    siteRepository: SiteRepository
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    private val groupFlow = MutableStateFlow<SiteGroup?>(null)
    private val sortFlow = MutableStateFlow(true)

    val uiState: StateFlow<SitesUiState> = combine(
        siteRepository.observeSites(),
        queryFlow,
        groupFlow,
        sortFlow
    ) { sites, query, group, ascending ->
        var filtered = sites
        if (group != null) filtered = filtered.filter { it.siteGroup == group }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.domain.contains(query, ignoreCase = true)
            }
        }
        filtered = if (ascending) {
            filtered.sortedBy { it.name.lowercase() }
        } else {
            filtered.sortedByDescending { it.name.lowercase() }
        }
        SitesUiState(
            sites = filtered,
            totalCount = sites.size,
            searchQuery = query,
            groupFilter = group,
            sortAscending = ascending,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SitesUiState())

    fun setSearchQuery(query: String) {
        queryFlow.value = query
    }

    fun setGroupFilter(group: SiteGroup?) {
        groupFlow.value = group
    }

    fun toggleSortDirection() {
        sortFlow.value = !sortFlow.value
    }
}
