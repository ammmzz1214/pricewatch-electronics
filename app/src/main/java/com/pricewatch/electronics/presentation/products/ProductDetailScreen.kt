package com.pricewatch.electronics.presentation.products

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.PriceWatchGoldButton
import com.pricewatch.electronics.core.designsystem.components.PriceWatchOutlineButton
import com.pricewatch.electronics.presentation.components.CheckStatusChip
import com.pricewatch.electronics.presentation.components.ConfirmDeleteDialog
import com.pricewatch.electronics.presentation.components.EmptyState
import com.pricewatch.electronics.presentation.components.PriceChangeBadge
import com.pricewatch.electronics.presentation.components.StatCard
import com.pricewatch.electronics.presentation.components.TargetReachedBadge
import com.pricewatch.electronics.presentation.util.formatDateTime
import kotlinx.coroutines.launch
import com.pricewatch.electronics.presentation.util.formatPercent
import com.pricewatch.electronics.presentation.util.formatPrice
import com.pricewatch.electronics.presentation.util.formatPriceDifference
import com.pricewatch.electronics.presentation.util.percentChange
import com.pricewatch.electronics.presentation.util.priceDirection

/**
 * Product detail. Signature matches the NavHost in app/Navigation.kt.
 * Edit is an in-place dialog (there is no separate edit route in the NavHost).
 */
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val site by viewModel.siteState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onNavigateBack()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearInfo()
        }
    }

    val product = state.product
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = PriceWatchColors.Gold)
            }
        } else if (product == null) {
            EmptyState(
                title = "Product not found",
                message = "This product may have been deleted.",
                actionLabel = "Back",
                onAction = onNavigateBack
            )
        } else {
            val direction = priceDirection(product.previousPriceMinorUnits, product.currentPriceMinorUnits)
            val percent = percentChange(product.previousPriceMinorUnits, product.currentPriceMinorUnits)
            val difference = product.currentPriceMinorUnits -
                (product.previousPriceMinorUnits ?: product.currentPriceMinorUnits)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(PriceWatchSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PriceWatchColors.White
                            )
                        }
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineMedium,
                            color = PriceWatchColors.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit product",
                                tint = PriceWatchColors.Gold
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete product",
                                tint = PriceWatchColors.WarningRed
                            )
                        }
                    }
                }
                item {
                    if (!product.imageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = "Electronics • ${product.subcategory.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PriceWatchColors.SecondaryText
                    )
                    Text(
                        text = "Shop: ${site?.name ?: "Unknown site"}${site?.let { " (${it.domain})" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PriceWatchColors.Teal
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                        StatCard(
                            label = "Current price",
                            value = Money.format(product.currentPriceMinorUnits, product.currencyCode),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Previous price",
                            value = product.previousPriceMinorUnits?.let {
                                Money.format(it, product.currencyCode)
                            } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PriceChangeBadge(direction = direction, percent = percent)
                        if (product.previousPriceMinorUnits != null && difference != 0L) {
                            Text(
                                text = formatPriceDifference(difference, product.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (difference < 0) PriceWatchColors.Success else PriceWatchColors.WarningRed
                            )
                        }
                        if (product.targetReached) TargetReachedBadge()
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CheckStatusChip(status = product.checkStatus)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                        StatCard(
                            label = "Lowest recorded",
                            value = state.stats.lowestMinorUnits?.let {
                                Money.format(it, product.currencyCode)
                            } ?: "—",
                            accent = PriceWatchColors.Success,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Highest recorded",
                            value = state.stats.highestMinorUnits?.let {
                                Money.format(it, product.currencyCode)
                            } ?: "—",
                            accent = PriceWatchColors.WarningRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PriceWatchSpacing.md)) {
                        StatCard(
                            label = "Average recorded",
                            value = state.stats.averageMinorUnits?.let {
                                Money.format(it, product.currencyCode)
                            } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Price changes",
                            value = state.stats.totalChanges.toString(),
                            subtext = "Change: ${formatPercent(state.stats.percentChange)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Target price: ${product.targetPriceMinorUnits?.let { formatPrice(it, product.currencyCode) } ?: "Not set"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Last successful check: ${formatDateTime(product.lastSuccessfulCheckAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Last attempt: ${formatDateTime(product.lastAttemptAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.SecondaryText
                        )
                        Text(
                            text = "Monitoring: ${if (product.monitoringEnabled) "On" else "Off"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Added: ${formatDateTime(product.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PriceWatchColors.SecondaryText
                        )
                        Text(
                            text = product.productUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = PriceWatchColors.Teal
                        )
                    }
                }
                item {
                    PriceWatchGoldButton(
                        text = if (state.isChecking) "Checking…" else "Check Now",
                        onClick = { viewModel.checkNow() },
                        enabled = !state.isChecking
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceWatchOutlineButton(
                        text = "Open Website",
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(product.productUrl))
                                )
                            } catch (_: ActivityNotFoundException) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No browser app found to open this link.")
                                }
                            }
                        }
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showDeleteDialog && product != null) {
        ConfirmDeleteDialog(
            productName = product.name,
            onConfirm = { showDeleteDialog = false; viewModel.deleteProduct() },
            onDismiss = { showDeleteDialog = false }
        )
    }
    if (showEditDialog && product != null) {
        EditProductDialog(
            productName = product.name,
            currentPriceMinorUnits = product.currentPriceMinorUnits,
            currencyCode = product.currencyCode,
            targetPriceMinorUnits = product.targetPriceMinorUnits,
            monitoringEnabled = product.monitoringEnabled,
            onDismiss = { showEditDialog = false },
            onSave = { name, price, target, monitoring ->
                viewModel.updateProduct(name, price, target, monitoring)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun EditProductDialog(
    productName: String,
    currentPriceMinorUnits: Long,
    currencyCode: String,
    targetPriceMinorUnits: Long?,
    monitoringEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Long, Long?, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(productName) }
    var priceText by remember { mutableStateOf(Money.toDecimalString(currentPriceMinorUnits)) }
    var targetText by remember {
        mutableStateOf(targetPriceMinorUnits?.let { Money.toDecimalString(it) } ?: "")
    }
    var monitoring by remember { mutableStateOf(monitoringEnabled) }
    var error by remember { mutableStateOf<String?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PriceWatchColors.Gold,
        unfocusedBorderColor = PriceWatchColors.Border,
        focusedLabelColor = PriceWatchColors.Gold,
        unfocusedLabelColor = PriceWatchColors.SecondaryText,
        focusedTextColor = PriceWatchColors.White,
        unfocusedTextColor = PriceWatchColors.White,
        cursorColor = PriceWatchColors.Gold
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PriceWatchColors.CardBackground,
        title = { Text(text = "Edit product", color = PriceWatchColors.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "Product name") },
                    singleLine = true,
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(text = "Current price ($currencyCode)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text(text = "Target price (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Monitoring enabled", color = PriceWatchColors.White)
                    Switch(checked = monitoring, onCheckedChange = { monitoring = it })
                }
                error?.let {
                    Text(text = it, color = PriceWatchColors.WarningRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = Money.fromDecimalString(priceText, currencyCode)
                    if (name.isBlank()) {
                        error = "Product name is required."
                        return@Button
                    }
                    if (price == null) {
                        error = "Enter a price greater than zero."
                        return@Button
                    }
                    val target = if (targetText.isBlank()) {
                        null
                    } else {
                        Money.fromDecimalString(targetText, currencyCode)
                            ?: run { error = "Target price must be greater than zero."; return@Button }
                    }
                    onSave(name, price, target, monitoring)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PriceWatchColors.Gold,
                    contentColor = PriceWatchColors.PrimaryBackground
                )
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = PriceWatchColors.Gold)
            }
        }
    )
}
