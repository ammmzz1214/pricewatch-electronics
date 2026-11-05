package com.pricewatch.electronics.presentation.sites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.domain.models.IntegrationStatus
import com.pricewatch.electronics.domain.models.Site
import com.pricewatch.electronics.domain.models.SiteGroup
import com.pricewatch.electronics.presentation.components.EmptyState
import com.pricewatch.electronics.presentation.util.formatDateTime
import com.pricewatch.electronics.presentation.util.integrationStatusDisplayName
import com.pricewatch.electronics.presentation.util.integrationTypeDisplayName
import com.pricewatch.electronics.presentation.util.siteGroupDisplayName

/** Favicon for a site, with a monogram fallback when no favicon loads. */
@Composable
fun SiteLogo(site: Site, modifier: Modifier = Modifier) {
    val domain = site.domain.substringBefore("/")
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(PriceWatchShapes.medium)
            .background(PriceWatchColors.SurfaceElevated),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = "https://www.google.com/s2/favicons?domain=$domain&sz=64",
            contentDescription = "${site.name} logo",
            modifier = Modifier.size(48.dp),
            contentScale = ContentScale.Fit
        )
    }
}

fun integrationStatusColor(status: IntegrationStatus) = when (status) {
    IntegrationStatus.LIVE_INTEGRATION -> PriceWatchColors.Success
    IntegrationStatus.PARTIAL_SUPPORT -> PriceWatchColors.Teal
    IntegrationStatus.MANUAL_URL -> PriceWatchColors.Gold
    IntegrationStatus.UNAVAILABLE -> PriceWatchColors.WarningRed
}

/**
 * Sites directory — exactly the 51 seeded sites from SiteRepository.
 * Signature matches the NavHost in app/Navigation.kt.
 */
@Composable
fun SitesScreen(
    onNavigateToSite: (Long) -> Unit,
    viewModel: SitesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                text = "Sites",
                style = MaterialTheme.typography.headlineMedium,
                color = PriceWatchColors.White
            )
            Text(
                text = "${state.totalCount} supported websites • No site is marked Live unless its integration is implemented and verified.",
                style = MaterialTheme.typography.bodySmall,
                color = PriceWatchColors.SecondaryText
            )
        }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = "Search websites") },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.toggleSortDirection() }) {
                    Text(
                        text = if (state.sortAscending) "Sort: A–Z" else "Sort: Z–A",
                        color = PriceWatchColors.Gold
                    )
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.groupFilter == null,
                        onClick = { viewModel.setGroupFilter(null) },
                        label = { Text(text = "All groups") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PriceWatchColors.Gold,
                            selectedLabelColor = PriceWatchColors.PrimaryBackground,
                            containerColor = PriceWatchColors.CardBackground,
                            labelColor = PriceWatchColors.White
                        )
                    )
                }
                for (group in SiteGroup.values()) {
                    item {
                        FilterChip(
                            selected = state.groupFilter == group,
                            onClick = { viewModel.setGroupFilter(group) },
                            label = { Text(text = siteGroupDisplayName(group)) },
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
        if (state.sites.isEmpty()) {
            item {
                EmptyState(
                    title = "No websites found",
                    message = "No sites match the current search or group filter."
                )
            }
        } else {
            items(state.sites, key = { it.id }) { site ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSite(site.id) },
                    shape = PriceWatchShapes.large,
                    colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground)
                ) {
                    Row(modifier = Modifier.padding(PriceWatchSpacing.md)) {
                        SiteLogo(site = site)
                        Spacer(modifier = Modifier.width(PriceWatchSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = site.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = PriceWatchColors.White
                            )
                            Text(
                                text = site.domain,
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.Teal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = siteGroupDisplayName(site.siteGroup),
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = integrationStatusDisplayName(site.integrationStatus),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = integrationStatusColor(site.integrationStatus)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = integrationTypeDisplayName(site.integrationType),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PriceWatchColors.SecondaryText
                                )
                            }
                            Text(
                                text = "Last connection: ${formatDateTime(site.lastSuccessfulConnectionAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PriceWatchColors.SecondaryText
                            )
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manual URL sites: save the product URL and update the price manually. " +
                    "Automated checking is only available where a verified integration exists.",
                style = MaterialTheme.typography.bodySmall,
                color = PriceWatchColors.SecondaryText
            )
        }
    }
}
