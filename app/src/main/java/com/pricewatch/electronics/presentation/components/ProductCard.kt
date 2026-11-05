package com.pricewatch.electronics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.Product
import com.pricewatch.electronics.presentation.util.formatPriceDifference
import com.pricewatch.electronics.presentation.util.formatTimeAgo
import com.pricewatch.electronics.presentation.util.percentChange
import com.pricewatch.electronics.presentation.util.priceDirection

/**
 * Tracked-product card used on Home and My Products.
 * All values come from verified Room data; a failed check never changes the
 * displayed price — it only changes the status chip. [siteName] is resolved
 * by the ViewModel from SiteRepository (Product only stores [Product.siteId]).
 * Pass [onDelete] to show a delete button (My Products); Home omits it.
 */
@Composable
fun ProductCard(
    product: Product,
    siteName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    val direction = priceDirection(product.previousPriceMinorUnits, product.currentPriceMinorUnits)
    val percent = percentChange(product.previousPriceMinorUnits, product.currentPriceMinorUnits)
    val difference = product.currentPriceMinorUnits -
        (product.previousPriceMinorUnits ?: product.currentPriceMinorUnits)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = PriceWatchShapes.large,
        colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.padding(PriceWatchSpacing.md)) {
            // Product image (Coil). Falls back to a gold monogram tile.
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(PriceWatchShapes.medium)
                    .background(PriceWatchColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = product.name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = PriceWatchColors.Gold
                    )
                }
            }
            Spacer(modifier = Modifier.width(PriceWatchSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = PriceWatchColors.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Electronics • ${product.subcategory.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Money.format(product.currentPriceMinorUnits, product.currencyCode),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = PriceWatchColors.White
                    )
                    if (product.previousPriceMinorUnits != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Money.format(product.previousPriceMinorUnits, product.currencyCode),
                            style = MaterialTheme.typography.bodySmall,
                            color = PriceWatchColors.SecondaryText
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PriceChangeBadge(direction = direction, percent = percent)
                    if (product.previousPriceMinorUnits != null && difference != 0L) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatPriceDifference(difference, product.currencyCode),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (difference < 0) PriceWatchColors.Success else PriceWatchColors.WarningRed
                        )
                    }
                    if (product.targetReached) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TargetReachedBadge()
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = siteName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = PriceWatchColors.Teal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Checked ${formatTimeAgo(product.lastSuccessfulCheckAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = PriceWatchColors.SecondaryText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                CheckStatusChip(status = product.checkStatus)
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete ${product.name}",
                        tint = PriceWatchColors.WarningRed
                    )
                }
            }
        }
    }
}

/** Convenience for screens that only need the failure state. */
fun Product.isCheckFailed(): Boolean = checkStatus == CheckStatus.CHECK_FAILED
