package com.pricewatch.electronics.core.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchShapes

@Composable
fun PriceWatchGoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = PriceWatchShapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = PriceWatchColors.Gold,
            contentColor = PriceWatchColors.PrimaryBackground,
            disabledContainerColor = PriceWatchColors.Border,
            disabledContentColor = PriceWatchColors.SecondaryText
        )
    ) { Text(text) }
}

@Composable
fun PriceWatchOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = PriceWatchShapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PriceWatchColors.Gold)
    ) { Text(text) }
}
