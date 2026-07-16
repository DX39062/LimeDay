package com.limeday.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Mint,
    onPrimary = FreshSurface,
    primaryContainer = MintContainer,
    onPrimaryContainer = Ink,
    secondary = Sunshine,
    background = FreshBackground,
    onBackground = Ink,
    surface = FreshSurface,
    onSurface = Ink,
    surfaceVariant = ColorTokens.LightSurfaceVariant,
    onSurfaceVariant = ColorTokens.LightOnSurfaceVariant
)

private val DarkColors = darkColorScheme(
    primary = MintDark,
    onPrimary = ColorTokens.DarkOnPrimary,
    primaryContainer = MintContainerDark,
    onPrimaryContainer = InkDark,
    secondary = Sunshine,
    background = FreshBackgroundDark,
    onBackground = InkDark,
    surface = FreshSurfaceDark,
    onSurface = InkDark,
    surfaceVariant = ColorTokens.DarkSurfaceVariant,
    onSurfaceVariant = ColorTokens.DarkOnSurfaceVariant
)

private object ColorTokens {
    val LightSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8F0ED)
    val LightOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF52635E)
    val DarkOnPrimary = androidx.compose.ui.graphics.Color(0xFF08372E)
    val DarkSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF263530)
    val DarkOnSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB8C9C3)
}

@Composable
fun LimeDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
