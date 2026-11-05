package com.pricewatch.electronics.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated gold progress ball. [progress] is 0f..1f and must reflect real
 * completed checks — never set to 1f while checks remain.
 */
@Composable
fun LoadingGoldBall(
    progress: Float,
    currentProduct: String,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    val infiniteTransition = rememberInfiniteTransition(label = "goldBall")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(120.dp)) {
                val radius = size.minDimension / 2f - 8.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PriceWatchColors.GoldLight, PriceWatchColors.Gold, PriceWatchColors.GoldDark)
                    ),
                    radius = radius * 0.55f
                )
                drawArc(
                    color = PriceWatchColors.Gold,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx())
                )
                val rad = Math.toRadians(angle.toDouble())
                val orbitX = center.x + cos(rad).toFloat() * radius
                val orbitY = center.y + sin(rad).toFloat() * radius
                drawCircle(color = PriceWatchColors.Teal, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(orbitX, orbitY))
            }
            Text(
                text = "${(clamped * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                color = PriceWatchColors.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(currentProduct, style = MaterialTheme.typography.bodyMedium, color = PriceWatchColors.SecondaryText)
    }
}
