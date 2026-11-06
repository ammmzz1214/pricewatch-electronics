package com.pricewatch.electronics.presentation.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.core.common.UrlValidator
import com.pricewatch.electronics.domain.models.Category
import com.pricewatch.electronics.domain.models.MonitoringSettings
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.models.Subcategory
import com.pricewatch.electronics.domain.repositories.SettingsRepository
import com.pricewatch.electronics.domain.repositories.SiteRepository
import com.pricewatch.electronics.domain.usecases.AddProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddProductUiState(
    val name: String = "",
    val subcategory: Subcategory = Subcategory.SMARTPHONES,
    val siteId: Long? = null,
    val sites: List<Site> = emptyList(),
    val productUrl: String = "",
    val priceText: String = "",
    val currencyCode: String = "GBP",
    val imageUrl: String = "",
    val targetPriceText: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val createdProductId: Long? = null
)

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val settingsRepository: SettingsRepository,
    private val addProductUseCase: AddProductUseCase
) : ViewModel() {

    private val formFlow = MutableStateFlow(AddProductUiState())
    private val errorFlow = MutableStateFlow<String?>(null)
    private val savingFlow = MutableStateFlow(false)
    private val createdFlow = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AddProductUiState> = combine(
        formFlow,
        siteRepository.observeSites(),
        settingsRepository.observeSettings(),
        errorFlow,
        savingFlow,
        createdFlow
    ) { form: AddProductUiState, sites: List<Site>, settings: MonitoringSettings, error: String?, saving: Boolean, created: Long? ->
        // Default the shop to the first site and the currency to the saved
        // preference when the user has not chosen yet.
        val resolvedSiteId = form.siteId ?: sites.firstOrNull()?.id
        val resolvedCurrency =
            if (form.currencyCode.isBlank()) settings.currencyPreference else form.currencyCode
        form.copy(
            sites = sites,
            siteId = resolvedSiteId,
            currencyCode = resolvedCurrency,
            saveError = error,
            isSaving = saving,
            createdProductId = created
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddProductUiState())

    fun updateName(value: String) = updateForm { it.copy(name = value) }
    fun updateSubcategory(value: Subcategory) = updateForm { it.copy(subcategory = value) }
    fun updateSiteId(value: Long) = updateForm { it.copy(siteId = value) }
    fun updateProductUrl(value: String) = updateForm { it.copy(productUrl = value) }
    fun updatePriceText(value: String) = updateForm { it.copy(priceText = value) }
    fun updateCurrency(value: String) = updateForm { it.copy(currencyCode = value) }
    fun updateImageUrl(value: String) = updateForm { it.copy(imageUrl = value) }
    fun updateTargetPriceText(value: String) = updateForm { it.copy(targetPriceText = value) }

    fun clearSaveError() {
        errorFlow.value = null
    }

    private fun updateForm(transform: (AddProductUiState) -> AddProductUiState) {
        formFlow.value = transform(formFlow.value).copy(fieldErrors = emptyMap())
    }

    /**
     * Validates client-side, then delegates to [AddProductUseCase], which
     * re-validates (category, URL/site match, duplicate URL) and saves the
     * product plus its initial baseline history record in one transaction.
     * Money is stored as integer minor units — never as Double.
     */
    fun saveProduct() {
        val form = formFlow.value
        val errors = mutableMapOf<String, String>()
        if (form.name.isBlank()) errors["name"] = "Product name is required."
        if (form.name.length > 200) errors["name"] = "Product name is too long."
        if (form.siteId == null) errors["site"] = "Select a shop website."
        if (!UrlValidator.isValidUrl(form.productUrl)) {
            errors["url"] = "Enter a valid product URL starting with http:// or https://."
        }
        val priceMinor = Money.fromDecimalString(form.priceText, form.currencyCode)
        if (priceMinor == null) errors["price"] = "Enter a price greater than zero (max 2 decimals)."
        val targetMinor = if (form.targetPriceText.isBlank()) {
            null
        } else {
            Money.fromDecimalString(form.targetPriceText, form.currencyCode)
                ?: run { errors["target"] = "Target price must be greater than zero."; null }
        }
        if (form.imageUrl.isNotBlank() && !UrlValidator.isValidUrl(form.imageUrl)) {
            errors["image"] = "Image URL must be a valid http(s) URL."
        }
        if (errors.isNotEmpty()) {
            formFlow.value = form.copy(fieldErrors = errors)
            return
        }
        val siteId = form.siteId ?: return
        val product = Product(
            name = form.name.trim(),
            category = Category.ELECTRONICS,
            subcategory = form.subcategory,
            siteId = siteId,
            productUrl = UrlValidator.normalize(form.productUrl),
            currentPriceMinorUnits = priceMinor ?: return,
            currencyCode = form.currencyCode.uppercase(),
            targetPriceMinorUnits = targetMinor,
            imageUrl = form.imageUrl.trim().ifBlank { null }
        )
        viewModelScope.launch {
            savingFlow.value = true
            errorFlow.value = null
            when (val result = addProductUseCase(product)) {
                is Result.Success -> createdFlow.value = result.data
                is Result.Error -> errorFlow.value = result.message
                Result.Loading -> Unit
            }
            savingFlow.value = false
        }
    }
}
