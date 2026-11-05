package com.pricewatch.electronics.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.BuildConfig
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.PriceWatchGoldButton
import com.pricewatch.electronics.presentation.components.EmptyState
import com.pricewatch.electronics.presentation.components.GoldProgressBall
import com.pricewatch.electronics.presentation.components.ProductCard
import com.pricewatch.electronics.presentation.components.StatCard
import com.pricewatch.electronics.presentation.util.filterLabel
import com.pricewatch.electronics.presentation.util.formatTimeAgo
import com.pricewatch.electronics.presentation.util.sortLabel

/**
 * Home dashboard. Signature matches the NavHost in app/Navigation.kt.
 * Version comes dynamically from BuildConfig.VERSION_NAME ("1.0.0").
 */
@Composable
fun HomeScreen(
    onNavigateToProduct: (Long) -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAlerts by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.checkNow.isComplete, state.checkNow.summary) {
        state.checkNow.summary?.let { snackbarHostState.showSnackbar(it) }
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
                // Header: branding + dynamic version + notification + settings.
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRICEWATCH",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                                color = PriceWatchColors.Gold
                            )
                            Text(
                                text = "ELECTRONICS  •  v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PriceWatchColors.Teal
                            )
                            Text(
                                text = "Track smarter. Buy at the right price.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                        }
                        IconButton(onClick = { showAlerts = true }) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Price alerts",
                                tint = PriceWatchColors.Gold
                            )
                        }
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = PriceWatchColors.White
                            )
                        }
                    }
                }

                // Dashboard statistics.
                item {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        color = PriceWatchColors.White
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                        StatCard(
                            label = "Tracked products",
                            value = state.stats.totalTracked.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Prices falling",
                            value = state.stats.falling.toString(),
                            accent = PriceWatchColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                        StatCard(
                            label = "Prices rising",
                            value = state.stats.rising.toString(),
                            accent = PriceWatchColors.WarningRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Potential savings",
                            value = Money.format(state.stats.potentialSavingsMinorUnits, "GBP"),
                            accent = PriceWatchColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                        StatCard(
                            label = "Successful checks",
                            value = state.stats.successfulChecks.toString(),
                            accent = PriceWatchColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Failed checks",
                            value = state.stats.failedChecks.toString(),
                            accent = PriceWatchColors.WarningRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Text(
                        text = "Last successful sync: ${formatTimeAgo(state.stats.lastSuccessfulSyncAtEpochMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }

                // Check Now — prominent gold button with real progress.
                item {
                    val check = state.checkNow
                    if (check.isRunning || check.isComplete) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GoldProgressBall(
                                progress = check.progress,
                                currentProduct = check.currentProductName ?: ""
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = when {
                                    check.isComplete -> check.summary ?: "Check complete."
                                    check.currentProductName != null -> "Checking ${check.currentProductName}…"
                                    else -> "Starting checks…"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = PriceWatchColors.White
                            )
                            Text(
                                text = "Checked ${check.checkedCount} of ${check.totalCount} • " +
                                    "${check.successCount} succeeded • ${check.failedCount} failed • " +
                                    "${check.remainingCount} remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (check.isRunning) {
                                TextButton(onClick = { viewModel.cancelCheckNow() }) {
                                    Text(text = "Cancel", color = PriceWatchColors.WarningRed)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.dismissCheckSummary() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PriceWatchColors.Gold,
                                        contentColor = PriceWatchColors.PrimaryBackground
                                    )
                                ) {
                                    Text(text = "Back to dashboard")
                                }
                            }
                        }
                    } else {
                        PriceWatchGoldButton(
                            text = "CHECK NOW",
                            onClick = { viewModel.checkNow() },
                            enabled = state.stats.totalTracked > 0
                        )
                        if (state.stats.totalTracked == 0) {
                            Text(
                                text = "Add a product to start checking prices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                        }
                    }
                }

                // Sort + filter.
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                // Product list.
                if (state.products.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No products yet",
                            message = "Add your first electronics product and PriceWatch will track its price across UK shops.",
                            actionLabel = "Add product",
                            onAction = onNavigateToAddProduct
                        )
                    }
                } else {
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            siteName = state.siteNames[product.siteId] ?: "Unknown site",
                            onClick = { onNavigateToProduct(product.id) }
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

    // Notification icon: recent price-change alerts from verified data.
    if (showAlerts) {
        val alerts = state.products.filter {
            it.previousPriceMinorUnits != null &&
                it.currentPriceMinorUnits != it.previousPriceMinorUnits
        }.sortedByDescending { it.lastSuccessfulCheckAt ?: 0L }.take(20)
        AlertDialog(
            onDismissRequest = { showAlerts = false },
            containerColor = PriceWatchColors.CardBackground,
            title = { Text(text = "Recent price alerts", color = PriceWatchColors.White) },
            text = {
                if (alerts.isEmpty()) {
                    Text(
                        text = "No price changes recorded yet. Run Check Now or wait for the next scheduled check.",
                        color = PriceWatchColors.SecondaryText
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (product in alerts) {
                            val previous = product.previousPriceMinorUnits ?: 0L
                            val direction =
                                if (product.currentPriceMinorUnits < previous) "down" else "up"
                            Text(
                                text = "${product.name}: ${Money.format(previous, product.currencyCode)} → " +
                                    "${Money.format(product.currentPriceMinorUnits, product.currencyCode)} " +
                                    "($direction) • ${state.siteNames[product.siteId] ?: "Unknown site"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = PriceWatchColors.White
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlerts = false }) {
                    Text(text = "Close", color = PriceWatchColors.Gold)
                }
            }
        )
    }
}
