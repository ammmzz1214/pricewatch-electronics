package com.pricewatch.electronics.presentation.sites

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.PriceWatchGoldButton
import com.pricewatch.electronics.core.designsystem.components.PriceWatchOutlineButton
import com.pricewatch.electronics.domain.models.IntegrationStatus
import com.pricewatch.electronics.domain.models.SiteGroup
import com.pricewatch.electronics.presentation.components.EmptyState
import com.pricewatch.electronics.presentation.util.formatDateTime
import com.pricewatch.electronics.presentation.util.integrationStatusDisplayName
import com.pricewatch.electronics.presentation.util.integrationTypeDisplayName
import com.pricewatch.electronics.presentation.util.siteGroupDisplayName
import kotlinx.coroutines.launch

private fun siteRole(group: SiteGroup): String = when (group) {
    SiteGroup.MAJOR_RETAILERS -> "Direct retailer — sells electronics to UK customers."
    SiteGroup.SPECIALISTS -> "Specialist retailer or manufacturer store for computers and electronics."
    SiteGroup.MARKETPLACES -> "Marketplace — products are sold by many different sellers."
    SiteGroup.PRICE_COMPARISON -> "Price comparison platform — a directory of prices, not a retailer."
    SiteGroup.OTHER_RETAILERS -> "Electronics retailer — mobile networks, camera and home-electronics shops."
}

/**
 * Site detail. Signature matches the NavHost in app/Navigation.kt.
 */
@Composable
fun SiteDetailScreen(
    siteId: Long,
    onNavigateBack: () -> Unit,
    viewModel: SiteDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val site = state.site
    Box(modifier = Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = PriceWatchColors.Gold)
            }
        } else if (site == null) {
            EmptyState(
                title = "Website not found",
                message = "This website is not in the directory.",
                actionLabel = "Back",
                onAction = onNavigateBack
            )
        } else {
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
                        SiteLogo(site = site)
                        Spacer(modifier = Modifier.width(PriceWatchSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = site.name,
                                style = MaterialTheme.typography.headlineMedium,
                                color = PriceWatchColors.White
                            )
                            Text(
                                text = site.domain,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PriceWatchColors.Teal
                            )
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Group: ${siteGroupDisplayName(site.siteGroup)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Role: ${siteRole(site.siteGroup)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Integration: ${integrationStatusDisplayName(site.integrationStatus)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = integrationStatusColor(site.integrationStatus)
                        )
                        Text(
                            text = "Method: ${integrationTypeDisplayName(site.integrationType)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Price checking: " +
                                if (site.integrationStatus == IntegrationStatus.MANUAL_URL) {
                                    "Manual only — save the product URL and update the price yourself."
                                } else {
                                    "Limited automated support where permitted; otherwise manual updates."
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Status: ${if (site.isEnabled) "Enabled" else "Disabled"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.White
                        )
                        Text(
                            text = "Last successful connection: ${formatDateTime(site.lastSuccessfulConnectionAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.SecondaryText
                        )
                    }
                }
                item {
                    PriceWatchGoldButton(
                        text = "Open Website",
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://${site.domain}"))
                                )
                            } catch (_: ActivityNotFoundException) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("No browser app found to open this link.")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PriceWatchOutlineButton(
                        text = "Copy Website URL",
                        onClick = {
                            clipboardManager.setText(AnnotatedString("https://${site.domain}"))
                            scope.launch {
                                snackbarHostState.showSnackbar("Website URL copied.")
                            }
                        }
                    )
                }
                item {
                    Text(
                        text = "Tracked products from this site (${state.trackedProducts.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = PriceWatchColors.White
                    )
                }
                if (state.trackedProducts.isEmpty()) {
                    item {
                        Text(
                            text = "No products tracked from ${site.name} yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.SecondaryText
                        )
                    }
                } else {
                    items(state.trackedProducts, key = { it.id }) { product ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PriceWatchColors.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = Money.format(product.currentPriceMinorUnits, product.currencyCode),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = PriceWatchColors.Gold
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
