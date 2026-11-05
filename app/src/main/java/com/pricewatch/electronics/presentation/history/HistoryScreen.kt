package com.pricewatch.electronics.presentation.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.presentation.components.EmptyState
import com.pricewatch.electronics.presentation.components.StatCard
import com.pricewatch.electronics.presentation.util.formatDate
import com.pricewatch.electronics.presentation.util.formatDateTime
import com.pricewatch.electronics.presentation.util.formatPercent
import com.pricewatch.electronics.presentation.util.formatPrice
import com.pricewatch.electronics.presentation.util.formatPriceDifference

/**
 * Price history and analytics. Signature matches the NavHost in
 * app/Navigation.kt. Only verified price updates appear here — failed checks
 * never create history records (enforced in the repository).
 */
@Composable
fun HistoryScreen(
    onNavigateToProduct: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var productMenuExpanded by remember { mutableStateOf(false) }
    var chartMenuExpanded by remember { mutableStateOf(false) }
    val chartProductName = state.productNames[state.chartProductId] ?: "No product"
    val currency = state.products.firstOrNull { it.id == state.chartProductId }?.currencyCode ?: "GBP"

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = PriceWatchColors.Gold)
        }
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PriceWatchSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
    ) {
        item {
            Text(
                text = "History",
                style = MaterialTheme.typography.headlineMedium,
                color = PriceWatchColors.White
            )
            Text(
                text = "Verified price changes only — failed checks are never recorded as changes.",
                style = MaterialTheme.typography.bodySmall,
                color = PriceWatchColors.SecondaryText
            )
        }

        // Time filters.
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (range in HistoryRange.values()) {
                    item {
                        FilterChip(
                            selected = state.range == range,
                            onClick = { viewModel.setRange(range) },
                            label = { Text(text = range.label) },
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

        // Product filter for the list.
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { productMenuExpanded = true }) {
                    Text(
                        text = "Product: ${state.selectedProductId?.let { state.productNames[it] } ?: "All products"}",
                        color = PriceWatchColors.Gold
                    )
                }
                DropdownMenu(
                    expanded = productMenuExpanded,
                    onDismissRequest = { productMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = "All products") },
                        onClick = { viewModel.setProductFilter(null); productMenuExpanded = false }
                    )
                    for (product in state.products) {
                        DropdownMenuItem(
                            text = { Text(text = product.name) },
                            onClick = {
                                viewModel.setProductFilter(product.id)
                                productMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Interactive price chart.
        item {
            Card(
                shape = PriceWatchShapes.large,
                colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground)
            ) {
                Column(modifier = Modifier.padding(PriceWatchSpacing.lg)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Price chart",
                            style = MaterialTheme.typography.titleLarge,
                            color = PriceWatchColors.White
                        )
                        TextButton(onClick = { chartMenuExpanded = true }) {
                            Text(text = chartProductName, color = PriceWatchColors.Gold)
                        }
                        DropdownMenu(
                            expanded = chartMenuExpanded,
                            onDismissRequest = { chartMenuExpanded = false }
                        ) {
                            for (product in state.products) {
                                DropdownMenuItem(
                                    text = { Text(text = product.name) },
                                    onClick = {
                                        viewModel.setChartProduct(product.id)
                                        chartMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = "${state.range.label} • ${state.chartPoints.size} points",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceChart(
                        points = state.chartPoints,
                        currencyCode = currency,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }
        }

        // Historical statistics.
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                StatCard(
                    label = "Price changes",
                    value = state.stats.totalChanges.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Biggest drop",
                    value = state.stats.biggestDropMinorUnits?.let {
                        formatPriceDifference(it, currency)
                    } ?: "—",
                    accent = PriceWatchColors.Success,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                StatCard(
                    label = "Biggest rise",
                    value = state.stats.biggestRiseMinorUnits?.let {
                        formatPriceDifference(it, currency)
                    } ?: "—",
                    accent = PriceWatchColors.WarningRed,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Average change",
                    value = state.stats.averageChangeMinorUnits?.let {
                        formatPriceDifference(it, currency)
                    } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // History list.
        if (state.entries.isEmpty()) {
            item {
                EmptyState(
                    title = "No price history yet",
                    message = "Price changes in the selected period will appear here. " +
                        "Add a product or run Check Now to start recording history."
                )
            }
        } else {
            items(state.entries, key = { it.id }) { entry ->
                val isBaseline = entry.differenceMinorUnits == 0L &&
                    entry.oldPriceMinorUnits == entry.newPriceMinorUnits
                Card(
                    shape = PriceWatchShapes.large,
                    colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground),
                    onClick = { onNavigateToProduct(entry.productId) }
                ) {
                    Column(modifier = Modifier.padding(PriceWatchSpacing.md)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.productNames[entry.productId] ?: "Product",
                                style = MaterialTheme.typography.titleMedium,
                                color = PriceWatchColors.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatDate(entry.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBaseline) {
                                "Baseline: ${formatPrice(entry.newPriceMinorUnits, entry.currencyCode)}"
                            } else {
                                "${formatPrice(entry.oldPriceMinorUnits, entry.currencyCode)} → " +
                                    "${formatPrice(entry.newPriceMinorUnits, entry.currencyCode)}  •  " +
                                    "${formatPriceDifference(entry.differenceMinorUnits, entry.currencyCode)} " +
                                    "(${formatPercent(entry.percentageChange)})"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                isBaseline -> PriceWatchColors.SecondaryText
                                entry.differenceMinorUnits < 0 -> PriceWatchColors.Success
                                entry.differenceMinorUnits > 0 -> PriceWatchColors.WarningRed
                                else -> PriceWatchColors.White
                            }
                        )
                        Text(
                            text = "Source: ${entry.source} • ${formatDateTime(entry.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PriceWatchColors.SecondaryText
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive line chart drawn with Canvas. Tap or drag horizontally to
 * select the nearest recorded price point; the tooltip shows its price
 * and date. Empty data renders a labelled empty chart, never a fake line.
 */
@Composable
private fun PriceChart(
    points: List<ChartPoint>,
    currencyCode: String,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        selectedIndex = nearestIndex(points, offset.x, size.width)
                    }
                }
                .pointerInput(points) {
                    detectHorizontalDragGestures { change, _ ->
                        selectedIndex = nearestIndex(points, change.position.x, size.width)
                    }
                }
        ) {
            val paddingX = 8.dp.toPx()
            val paddingY = 24.dp.toPx()
            val width = size.width - paddingX * 2
            val height = size.height - paddingY * 2
            if (points.isEmpty()) {
                drawLine(
                    color = PriceWatchColors.Border,
                    start = Offset(paddingX, size.height / 2),
                    end = Offset(size.width - paddingX, size.height / 2),
                    strokeWidth = 1.dp.toPx()
                )
                return@Canvas
            }
            val minPrice = points.minOf { it.priceMinorUnits }.toFloat()
            val maxPrice = points.maxOf { it.priceMinorUnits }.toFloat()
            val span = (maxPrice - minPrice).coerceAtLeast(1f)
            fun xAt(index: Int): Float = if (points.size == 1) {
                paddingX + width / 2
            } else {
                paddingX + width * index / (points.size - 1)
            }
            fun yAt(price: Long): Float =
                paddingY + height - (height * (price - minPrice) / span)
            // Grid lines.
            for (i in 0..3) {
                val y = paddingY + height * i / 3
                drawLine(
                    color = PriceWatchColors.Border.copy(alpha = 0.5f),
                    start = Offset(paddingX, y),
                    end = Offset(size.width - paddingX, y),
                    strokeWidth = 1.dp.toPx()
                )
            }
            val linePath = Path()
            val fillPath = Path()
            points.forEachIndexed { index, point ->
                val x = xAt(index)
                val y = yAt(point.priceMinorUnits)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, paddingY + height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(xAt(points.lastIndex), paddingY + height)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PriceWatchColors.Gold.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = linePath,
                color = PriceWatchColors.Gold,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
            // Points.
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = if (selectedIndex == index) PriceWatchColors.Teal else PriceWatchColors.Gold,
                    radius = if (selectedIndex == index) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(xAt(index), yAt(point.priceMinorUnits))
                )
            }
        }
        val selected = selectedIndex?.let { points.getOrNull(it) }
        if (selected != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = Money.format(selected.priceMinorUnits, currencyCode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PriceWatchColors.Gold
                )
                Text(
                    text = formatDateTime(selected.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
            }
        } else if (points.isEmpty()) {
            Text(
                text = "No price points in this period yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = PriceWatchColors.SecondaryText,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

private fun nearestIndex(points: List<ChartPoint>, x: Float, width: Int): Int? {
    if (points.isEmpty() || width <= 0) return null
    val fraction = (x / width).coerceIn(0f, 1f)
    return (fraction * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
}
