package com.pricewatch.electronics.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricewatch.electronics.core.common.Result
import com.pricewatch.electronics.domain.models.MonitoringSettings
import com.pricewatch.electronics.domain.repositories.PriceHistoryRepository
import com.pricewatch.electronics.domain.repositories.ProductRepository
import com.pricewatch.electronics.domain.repositories.SettingsRepository
import com.pricewatch.electronics.domain.usecases.DeleteProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: MonitoringSettings = MonitoringSettings(),
    val themePreference: AppThemePreference = AppThemePreference.DARK,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: PriceHistoryRepository,
    private val productRepository: ProductRepository,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val messageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        context.observeThemePreference(),
        messageFlow
    ) { settings, theme, message ->
        SettingsUiState(settings = settings, themePreference = theme, message = message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun clearMessage() {
        messageFlow.value = null
    }

    private fun persist(transform: (MonitoringSettings) -> MonitoringSettings) {
        viewModelScope.launch {
            val current = settingsRepository.getSettings()
            when (val result = settingsRepository.updateSettings(transform(current))) {
                is Result.Success -> Unit
                is Result.Error -> messageFlow.value = "Could not save settings: ${result.message}"
                Result.Loading -> Unit
            }
        }
    }

    fun setIntervalMinutes(minutes: Int) {
        if (minutes !in listOf(15, 30, 60, 120)) return
        // Persisted immediately; the background worker reads
        // MonitoringSettings.intervalMinutes when it (re)schedules.
        persist { it.copy(intervalMinutes = minutes) }
    }

    fun setMonitoringEnabled(enabled: Boolean) = persist { it.copy(monitoringEnabled = enabled) }
    fun setNotificationsEnabled(enabled: Boolean) = persist { it.copy(notificationsEnabled = enabled) }
    fun setPriceIncreaseAlerts(enabled: Boolean) = persist { it.copy(priceIncreaseAlerts = enabled) }
    fun setPriceDecreaseAlerts(enabled: Boolean) = persist { it.copy(priceDecreaseAlerts = enabled) }
    fun setTargetPriceAlerts(enabled: Boolean) = persist { it.copy(targetPriceAlerts = enabled) }
    fun setCurrency(code: String) {
        if (code !in listOf("GBP", "EUR", "USD")) return
        persist { it.copy(currencyPreference = code) }
    }

    fun setThemePreference(preference: AppThemePreference) {
        viewModelScope.launch {
            context.setThemePreference(preference)
            messageFlow.value = if (preference == AppThemePreference.SYSTEM) {
                "Theme set to System. The dark luxury theme remains the default look."
            } else {
                "Dark luxury theme enabled."
            }
        }
    }

    /** Deletes all price-history records; products and their prices are kept. */
    fun clearHistory() {
        viewModelScope.launch {
            when (val result = historyRepository.clearHistory(null)) {
                is Result.Success -> messageFlow.value = "Price history cleared."
                is Result.Error -> messageFlow.value = "Could not clear history: ${result.message}"
                Result.Loading -> Unit
            }
        }
    }

    /** Deletes every tracked product (history cascade-deleted with each one). */
    fun deleteAllProducts() {
        viewModelScope.launch {
            val products = productRepository.observeProducts().first()
            var failures = 0
            for (product in products) {
                if (deleteProductUseCase(product.id) is Result.Error) failures++
            }
            messageFlow.value = if (failures == 0) {
                "All ${products.size} products deleted."
            } else {
                "Deleted with $failures failures. Please try again."
            }
        }
    }
}
