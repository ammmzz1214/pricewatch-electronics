package com.pricewatch.electronics.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.domain.models.CheckStatus
import com.pricewatch.electronics.presentation.util.PriceDirection
import com.pricewatch.electronics.presentation.util.checkStatusDisplayName
import com.pricewatch.electronics.presentation.util.formatPercent

/**
 * UP / DOWN / UNCHANGED badge. Direction is computed from verified prices
 * only — a failed check never produces a badge change (see CheckStatus chip).
 */
@Composable
fun PriceChangeBadge(
    direction: PriceDirection,
    percent: Double?,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (direction) {
        PriceDirection.DOWN -> "▼ DOWN ${formatPercent(percent)}" to PriceWatchColors.Success
        PriceDirection.UP -> "▲ UP ${formatPercent(percent)}" to PriceWatchColors.WarningRed
        PriceDirection.UNCHANGED -> "— UNCHANGED" to PriceWatchColors.SecondaryText
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun CheckStatusChip(
    status: CheckStatus,
    modifier: Modifier = Modifier
) {
    val color = when (status) {
        CheckStatus.CHECK_OK,
        CheckStatus.OK,
        CheckStatus.PRICE_DECREASED -> PriceWatchColors.Success
        CheckStatus.PRICE_INCREASED -> PriceWatchColors.WarningRed
        CheckStatus.CHECK_FAILED -> PriceWatchColors.WarningRed
        CheckStatus.NEVER_CHECKED -> PriceWatchColors.SecondaryText
        CheckStatus.CHECKING -> PriceWatchColors.Teal
        CheckStatus.PRICE_UNCHANGED -> PriceWatchColors.Teal
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = checkStatusDisplayName(status),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun TargetReachedBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(PriceWatchColors.Gold.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "★ TARGET REACHED",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = PriceWatchColors.Gold
        )
    }
}
