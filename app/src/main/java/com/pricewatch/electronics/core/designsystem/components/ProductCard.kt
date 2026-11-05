package com.pricewatch.electronics.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pricewatch.electronics.core.common.Money
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.domain.models.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProductCard(
    product: Product,
    siteName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = product.previousPriceMinorUnits?.let {
        Money.percentageChange(it, product.currentPriceMinorUnits)
    }
    val badge = when {
        product.checkStatus == CheckStatus.CHECK_FAILED -> "CHECK FAILED"
        pct == null -> "UNCHANGED"
        pct < 0 -> "DOWN"
        pct > 0 -> "UP"
        else -> "UNCHANGED"
    }
    val badgeColor = when (badge) {
        "DOWN" -> PriceWatchColors.Success
        "UP" -> PriceWatchColors.WarningRed
        "CHECK FAILED" -> PriceWatchColors.WarningRed
        else -> PriceWatchColors.SecondaryText
    }
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = PriceWatchShapes.medium,
        colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground)
    ) {
        Row(modifier = Modifier.padding(PriceWatchSpacing.lg)) {
            if (!product.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.size(64.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(PriceWatchSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, color = PriceWatchColors.White, maxLines = 2)
                Text(
                    "Electronics · ${product.subcategory.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PriceWatchColors.SecondaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        Money.format(product.currentPriceMinorUnits, product.currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        color = PriceWatchColors.Gold
                    )
                    product.previousPriceMinorUnits?.let { prev ->
                        Text(
                            Money.format(prev, product.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PriceWatchColors.SecondaryText
                        )
                    }
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = badgeColor)
                }
                if (pct != null) {
                    Text(
                        String.format(Locale.UK, "%+.2f%%", pct),
                        style = MaterialTheme.typography.bodySmall,
                        color = badgeColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(siteName, style = MaterialTheme.typography.bodySmall, color = PriceWatchColors.Teal)
                product.lastSuccessfulCheckAt?.let {
                    val fmt = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.UK)
                    Text("Last check: ${fmt.format(Date(it))}", style = MaterialTheme.typography.bodySmall, color = PriceWatchColors.SecondaryText)
                }
            }
        }
    }
}
