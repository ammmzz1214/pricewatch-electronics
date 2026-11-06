package com.pricewatch.electronics.presentation.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.presentation.components.ConfirmDeleteDialog
import com.pricewatch.electronics.presentation.components.EmptyState
import com.pricewatch.electronics.presentation.components.ProductCard
import com.pricewatch.electronics.presentation.util.ProductFilter
import com.pricewatch.electronics.presentation.util.ProductSort
import com.pricewatch.electronics.presentation.util.filterLabel
import com.pricewatch.electronics.presentation.util.sortLabel

/**
 * My Products. Signature matches the NavHost in app/Navigation.kt.
 * The Add FAB is provided by the NavHost scaffold — this screen has none.
 */
@Composable
fun MyProductsScreen(
    onNavigateToProduct: (Long) -> Unit,
    onNavigateToAddProduct: () -> Unit,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    // Undo snackbar after a deletion.
    LaunchedEffect(state.showUndo, state.deletedProduct) {
        val deleted = state.deletedProduct
        if (state.showUndo && deleted != null) {
            val result = snackbarHostState.showSnackbar(
                message = "${deleted.name} deleted",
                actionLabel = "Undo"
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
            else viewModel.dismissUndo()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = PriceWatchColors.Gold)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(PriceWatchSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
            ) {
                item {
                    Text(
                        text = "My Products",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PriceWatchColors.White
                    )
                    Text(
                        text = "${state.products.size} tracked • Electronics only",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = "Search products or shops") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PriceWatchColors.Gold,
                            unfocusedBorderColor = PriceWatchColors.Border,
                            focusedLabelColor = PriceWatchColors.Gold,
                            unfocusedLabelColor = PriceWatchColors.SecondaryText,
                            focusedTextColor = PriceWatchColors.White,
                            unfocusedTextColor = PriceWatchColors.White,
                            cursorColor = PriceWatchColors.Gold
                        )
                    )
                }
                item {
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text(text = "Sort: ${sortLabel(state.sort)}", color = PriceWatchColors.Gold)
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        for (sort in ProductSort.values()) {
                            DropdownMenuItem(
                                text = { Text(text = sortLabel(sort)) },
                                onClick = { viewModel.setSort(sort); sortMenuExpanded = false }
                            )
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (filter in ProductFilter.values()) {
                            item {
                                FilterChip(
                                    selected = state.filter == filter,
                                    onClick = { viewModel.setFilter(filter) },
                                    label = { Text(text = filterLabel(filter)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PriceWatchColors.Gold,
                                        selectedLabelColor = PriceWatchColors.PrimaryBackground,
                                        containerColor = PriceWatchColors.CardBackground,
                                        labelColor = PriceWatchColors.White
                                    )
                                )
                            }
                        }
                    }
                }
                if (state.products.isEmpty()) {
                    item {
                        EmptyState(
                            title = if (state.searchQuery.isBlank()) "No products yet" else "No matches",
                            message = if (state.searchQuery.isBlank()) {
                                "Add your first electronics product to start tracking its price."
                            } else {
                                "No products match “${state.searchQuery}”. Try a different search."
                            },
                            actionLabel = "Add product",
                            onAction = onNavigateToAddProduct
                        )
                    }
                } else {
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            siteName = state.siteNames[product.siteId] ?: "Unknown site",
                            onClick = { onNavigateToProduct(product.id) },
                            onDelete = { productToDelete = product }
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    productToDelete?.let { product ->
        ConfirmDeleteDialog(
            productName = product.name,
            onConfirm = { viewModel.deleteProduct(product); productToDelete = null },
            onDismiss = { productToDelete = null }
        )
    }
}
