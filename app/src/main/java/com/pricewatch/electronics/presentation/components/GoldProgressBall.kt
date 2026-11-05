package com.pricewatch.electronics.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pricewatch.electronics.core.designsystem.components.LoadingGoldBall

/**
 * Animated gold progress ball for Check Now.
 * [progress] is real completed/total work (0f..1f) supplied by the ViewModel —
 * never simulated. Delegates to the design-system LoadingGoldBall, which
 * renders the gold arc, orbiting teal dot and percentage.
 */
@Composable
fun GoldProgressBall(
    progress: Float,
    currentProduct: String = "",
    modifier: Modifier = Modifier
) {
    LoadingGoldBall(
        progress = progress.coerceIn(0f, 1f),
        currentProduct = currentProduct,
        modifier = modifier
    )
}
