package com.pricewatch.electronics.presentation.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.PriceWatchGoldButton
import com.pricewatch.electronics.domain.models.Subcategory

private val CURRENCIES = listOf("GBP", "EUR", "USD")

/**
 * Add Product. Signature matches the NavHost in app/Navigation.kt.
 * Category is locked to Electronics — there is no category picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onProductCreated: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var subcategoryExpanded by remember { mutableStateOf(false) }
    var siteExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.createdProductId) {
        state.createdProductId?.let { onProductCreated(it) }
    }
    LaunchedEffect(state.saveError) {
        state.saveError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PriceWatchColors.Gold,
        unfocusedBorderColor = PriceWatchColors.Border,
        focusedLabelColor = PriceWatchColors.Gold,
        unfocusedLabelColor = PriceWatchColors.SecondaryText,
        focusedTextColor = PriceWatchColors.White,
        unfocusedTextColor = PriceWatchColors.White,
        cursorColor = PriceWatchColors.Gold
    )
    val selectedSite = state.sites.firstOrNull { it.id == state.siteId }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(PriceWatchSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
        ) {
            item {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PriceWatchColors.White
                        )
                    }
                    Text(
                        text = "Add Product",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PriceWatchColors.White
                    )
                }
                Text(
                    text = "Track an electronics product from any of the 51 supported UK shops.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
            }

            // Product name.
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Product name *") },
                    singleLine = true,
                    isError = state.fieldErrors.containsKey("name"),
                    supportingText = state.fieldErrors["name"]?.let { { Text(text = it) } },
                    colors = fieldColors
                )
            }

            // Category — locked to Electronics, displayed, not editable.
            item {
                OutlinedTextField(
                    value = "Electronics (locked)",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Category") },
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    colors = fieldColors
                )
            }

            // Subcategory — all 18 Electronics subcategories.
            item {
                ExposedDropdownMenuBox(
                    expanded = subcategoryExpanded,
                    onExpandedChange = { subcategoryExpanded = !subcategoryExpanded }
                ) {
                    OutlinedTextField(
                        value = state.subcategory.displayName,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text(text = "Subcategory *") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcategoryExpanded) },
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = subcategoryExpanded,
                        onDismissRequest = { subcategoryExpanded = false }
                    ) {
                        for (subcategory in Subcategory.entries) {
                            DropdownMenuItem(
                                text = { Text(text = subcategory.displayName) },
                                onClick = {
                                    viewModel.updateSubcategory(subcategory)
                                    subcategoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Shop website — exactly the 51 seeded sites.
            item {
                ExposedDropdownMenuBox(
                    expanded = siteExpanded,
                    onExpandedChange = { siteExpanded = !siteExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSite?.let { "${it.name} (${it.domain})" } ?: "Select a shop",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text(text = "Shop website *") },
                        readOnly = true,
                        isError = state.fieldErrors.containsKey("site"),
                        supportingText = state.fieldErrors["site"]?.let { { Text(text = it) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = siteExpanded) },
                        colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = siteExpanded,
                        onDismissRequest = { siteExpanded = false }
                    ) {
                        for (site in state.sites) {
                            DropdownMenuItem(
                                text = { Text(text = "${site.name} (${site.domain})") },
                                onClick = {
                                    viewModel.updateSiteId(site.id)
                                    siteExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Product URL.
            item {
                OutlinedTextField(
                    value = state.productUrl,
                    onValueChange = { viewModel.updateProductUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Product URL *") },
                    placeholder = { Text(text = "https://www.example.co.uk/product") },
                    singleLine = true,
                    isError = state.fieldErrors.containsKey("url"),
                    supportingText = state.fieldErrors["url"]?.let { { Text(text = it) } },
                    colors = fieldColors
                )
            }

            // Price + currency.
            item {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
                ) {
                    OutlinedTextField(
                        value = state.priceText,
                        onValueChange = { viewModel.updatePriceText(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = "Current price *") },
                        placeholder = { Text(text = "129.99") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = state.fieldErrors.containsKey("price"),
                        supportingText = state.fieldErrors["price"]?.let { { Text(text = it) } },
                        colors = fieldColors
                    )
                    ExposedDropdownMenuBox(
                        expanded = currencyExpanded,
                        onExpandedChange = { currencyExpanded = !currencyExpanded },
                        modifier = Modifier.weight(0.45f)
                    ) {
                        OutlinedTextField(
                            value = state.currencyCode,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(text = "Currency") },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                            colors = fieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = currencyExpanded,
                            onDismissRequest = { currencyExpanded = false }
                        ) {
                            for (currency in CURRENCIES) {
                                DropdownMenuItem(
                                    text = { Text(text = currency) },
                                    onClick = {
                                        viewModel.updateCurrency(currency)
                                        currencyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Optional image URL.
            item {
                OutlinedTextField(
                    value = state.imageUrl,
                    onValueChange = { viewModel.updateImageUrl(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Product image URL (optional)") },
                    singleLine = true,
                    isError = state.fieldErrors.containsKey("image"),
                    supportingText = state.fieldErrors["image"]?.let { { Text(text = it) } },
                    colors = fieldColors
                )
            }

            // Optional target price.
            item {
                OutlinedTextField(
                    value = state.targetPriceText,
                    onValueChange = { viewModel.updateTargetPriceText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Target price (optional)") },
                    placeholder = { Text(text = "99.99") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = state.fieldErrors.containsKey("target"),
                    supportingText = state.fieldErrors["target"]?.let { { Text(text = it) } },
                    colors = fieldColors
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You will be alerted when the price falls to or below the target price.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
            }

            item {
                if (state.isSaving) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PriceWatchColors.Gold)
                    }
                } else {
                    PriceWatchGoldButton(text = "Save Product", onClick = { viewModel.saveProduct() })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Most shops are manual-URL: the price is checked automatically only where a verified integration exists. Otherwise update the price from the product page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
