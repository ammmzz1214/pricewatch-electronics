package com.pricewatch.electronics.core.designsystem.Theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.pricewatch.electronics.core.designsystem.PriceWatchColors
import com.pricewatch.electronics.core.designsystem.PriceWatchTypography

private val PriceWatchDarkScheme = darkColorScheme(
    primary = PriceWatchColors.Gold,
    onPrimary = PriceWatchColors.PrimaryBackground,
    secondary = PriceWatchColors.Teal,
    onSecondary = PriceWatchColors.White,
    background = PriceWatchColors.PrimaryBackground,
    onBackground = PriceWatchColors.White,
    surface = PriceWatchColors.SecondaryBackground,
    onSurface = PriceWatchColors.White,
    surfaceVariant = PriceWatchColors.CardBackground,
    onSurfaceVariant = PriceWatchColors.SecondaryText,
    error = PriceWatchColors.WarningRed,
    onError = PriceWatchColors.White,
    outline = PriceWatchColors.Border
)

@Composable
fun PriceWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // PriceWatch dark luxury theme is the default and is used in both modes;
    // system dark mode is respected by keeping the same dark palette.
    MaterialTheme(
        colorScheme = PriceWatchDarkScheme,
        typography = PriceWatchTypography,
        content = content
    )
}
