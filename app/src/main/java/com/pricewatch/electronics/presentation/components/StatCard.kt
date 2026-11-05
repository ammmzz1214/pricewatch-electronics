package com.pricewatch.electronics.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing
import com.pricewatch.electronics.core.designsystem.components.StatCard as DesignSystemStatCard

/**
 * Dashboard statistic card. Delegates to the design-system StatCard; the
 * [subtext] variant adds a small line underneath for context.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    subtext: String? = null,
    accent: Color = PriceWatchColors.Gold,
    modifier: Modifier = Modifier
) {
    if (subtext == null) {
        DesignSystemStatCard(label = label, value = value, modifier = modifier, valueColor = accent)
        return
    }
    Card(
        modifier = modifier,
        shape = PriceWatchShapes.medium,
        colors = CardDefaults.cardColors(containerColor = PriceWatchColors.CardBackground)
    ) {
        Column(modifier = Modifier.padding(PriceWatchSpacing.lg)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = PriceWatchColors.SecondaryText)
            Spacer(modifier = Modifier.height(PriceWatchSpacing.xs))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
            Spacer(modifier = Modifier.height(PriceWatchSpacing.xs))
            Text(subtext, style = MaterialTheme.typography.bodySmall, color = PriceWatchColors.SecondaryText)
        }
    }
}
