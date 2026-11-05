package com.pricewatch.electronics.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchSpacing

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(PriceWatchSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = PriceWatchColors.White)
        Spacer(modifier = Modifier.height(PriceWatchSpacing.sm))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = PriceWatchColors.SecondaryText)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(PriceWatchSpacing.lg))
            PriceWatchGoldButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(PriceWatchSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge, color = PriceWatchColors.WarningRed)
        Spacer(modifier = Modifier.height(PriceWatchSpacing.sm))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = PriceWatchColors.SecondaryText)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(PriceWatchSpacing.lg))
            PriceWatchOutlineButton(text = "Retry", onClick = onRetry)
        }
    }
}
