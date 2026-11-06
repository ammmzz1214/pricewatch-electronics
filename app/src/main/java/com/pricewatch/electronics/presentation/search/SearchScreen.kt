package com.pricewatch.electronics.presentation.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.common.SearchText
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.PriceWatchGoldButton
import com.pricewatch.electronics.data.search.SiteSearchConfigs
import com.pricewatch.electronics.domain.models.SearchResultItem

private val RAM_OPTIONS = listOf<Int?>(null, 8, 16, 32, 64)
private val STORAGE_OPTIONS = listOf<Int?>(null, 256, 512, 1024, 2048)
private val SCREEN_OPTIONS = listOf<Double?>(null, 13.0, 14.0, 15.6, 16.0, 17.3)

private fun formatPriceShort(minorUnits: Long): String =
    if (minorUnits % 100L == 0L) "£${minorUnits / 100}" else Money.format(minorUnits, "GBP")

/**
 * The only screen the user needs: one search box, spec filters, a price
 * range, and every built-in shop searched behind it. No URL is ever asked
 * for — results come from the shops themselves, cheapest first.
 */
@Composable
fun SearchScreen(viewModel: SearchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(PriceWatchSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
        ) {
            item {
                Text(
                    text = "Search all shops",
                    style = MaterialTheme.typography.headlineMedium,
                    color = PriceWatchColors.White
                )
                Text(
                    text = "Type a product, set the specs and your price range. Every built-in shop is searched and the cheapest is shown first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
            }

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Product name") },
                    placeholder = { Text(text = "e.g. MacBook Pro") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = PriceWatchColors.Gold
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    colors = fieldColors
                )
            }

            item {
                FilterGroup(label = "RAM") {
                    RAM_OPTIONS.forEach { ram ->
                        SpecChip(
                            label = ram?.let { "$it GB" } ?: "Any",
                            selected = state.ramGb == ram,
                            onClick = { viewModel.updateRamGb(ram) }
                        )
                    }
                }
            }

            item {
                FilterGroup(label = "Storage (SSD)") {
                    STORAGE_OPTIONS.forEach { storage ->
                        SpecChip(
                            label = when (storage) {
                                null -> "Any"
                                1024 -> "1 TB"
                                2048 -> "2 TB"
                                else -> "$storage GB"
                            },
                            selected = state.storageGb == storage,
                            onClick = { viewModel.updateStorageGb(storage) }
                        )
                    }
                }
            }

            item {
                FilterGroup(label = "Screen size") {
                    SCREEN_OPTIONS.forEach { size ->
                        SpecChip(
                            label = size?.let {
                                if (it == it.toInt().toDouble()) "${it.toInt()}\"" else "$it\""
                            } ?: "Any",
                            selected = state.screenSizeInches == size,
                            onClick = { viewModel.updateScreenSize(size) }
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                    OutlinedTextField(
                        value = state.minPriceText,
                        onValueChange = { viewModel.updateMinPrice(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = "Min price (£)") },
                        placeholder = { Text(text = "200") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = state.maxPriceText,
                        onValueChange = { viewModel.updateMaxPrice(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text(text = "Max price (£)") },
                        placeholder = { Text(text = "250") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = fieldColors
                    )
                }
            }

            item {
                if (state.isSearching) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = PriceWatchColors.Gold,
                            trackColor = PriceWatchColors.Border
                        )
                        Text(
                            text = "Checked ${state.searchedSiteCount} of ${state.totalSiteCount} shops…",
                            style = MaterialTheme.typography.bodySmall,
                            color = PriceWatchColors.SecondaryText
                        )
                        OutlinedButton(
                            onClick = { viewModel.cancelSearch() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(text = "Cancel search") }
                    }
                } else {
                    PriceWatchGoldButton(
                        text = "Search all shops",
                        onClick = { viewModel.search() }
                    )
                }
            }

            if (state.isSearching && state.outcomes.isNotEmpty()) {
                item {
                    Text(
                        text = state.outcomes.joinToString("  •  ") { outcome ->
                            if (outcome.succeeded) "${outcome.siteName}: ${outcome.results.size}"
                            else "${outcome.siteName}: failed"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
            }

            if (state.results.isNotEmpty() || (state.hasSearched && !state.isSearching)) {
                item {
                    Text(
                        text = if (state.results.isEmpty()) {
                            if (state.minPriceText.isBlank() && state.maxPriceText.isBlank()) {
                                "No results found."
                            } else {
                                "No results in this price range."
                            }
                        } else if (state.isSearching) {
                            "${state.results.size} results so far — cheapest first"
                        } else {
                            "${state.results.size} results — cheapest first"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = PriceWatchColors.White
                    )
                }
            }

            itemsIndexed(state.results) { index, item ->
                SearchResultCard(
                    item = item,
                    isCheapest = index == 0,
                    isTracked = state.trackedUrls.contains(item.productUrl),
                    onView = { uriHandler.openUri(item.productUrl) },
                    onTrack = { viewModel.trackResult(item) }
                )
            }

            if (state.hasSearched && !state.isSearching) {
                val failed = state.outcomes.filter { !it.succeeded }
                if (failed.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Shops that could not be searched this time:",
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                            failed.forEach { outcome ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${outcome.siteName}: ${outcome.error ?: "could not load"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = PriceWatchColors.WarningRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val config = SiteSearchConfigs.forSite(outcome.siteId)
                                    if (config != null) {
                                        TextButton(onClick = {
                                            uriHandler.openUri(
                                                SearchText.buildSearchUrl(
                                                    config.searchUrlTemplate,
                                                    state.query
                                                )
                                            )
                                        }) { Text(text = "Open") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!state.hasSearched && !state.isSearching) {
                item {
                    Text(
                        text = "Shops searched: eBay, Amazon, Argos, Currys, John Lewis, Back Market, Laptops Direct, eBuyer, Scan, AO, Very and MusicMagpie, plus Idealo, PriceSpy, PriceRunner and Google Shopping. Prices are read live from each shop's own website on your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FilterGroup(label: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = PriceWatchColors.SecondaryText
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun SpecChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PriceWatchColors.Gold,
            selectedLabelColor = PriceWatchColors.PrimaryBackground,
            containerColor = PriceWatchColors.CardBackground,
            labelColor = PriceWatchColors.White
        )
    )
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    isCheapest: Boolean,
    isTracked: Boolean,
    onView: () -> Unit,
    onTrack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PriceWatchSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatPriceShort(item.priceMinor),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PriceWatchColors.Gold
                )
                if (isCheapest) {
                    Text(
                        text = "★ CHEAPEST",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = PriceWatchColors.Success
                    )
                }
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                color = PriceWatchColors.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.siteName,
                style = MaterialTheme.typography.bodySmall,
                color = PriceWatchColors.SecondaryText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onView, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(text = "View")
                }
                OutlinedButton(
                    onClick = onTrack,
                    enabled = !isTracked,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTracked) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(text = if (isTracked) "Tracked" else "Track price")
                }
            }
        }
    }
}
