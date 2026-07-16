package com.limeday.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Mint,
    onPrimary = SurfaceLight,
    primaryContainer = MintContainer,
    onPrimaryContainer = Ink,
    secondary = Marigold,
    onSecondary = SurfaceLight,
    secondaryContainer = MarigoldContainer,
    onSecondaryContainer = Ink,
    tertiary = Coral,
    onTertiary = SurfaceLight,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = SurfaceContainerLight,
    onSurfaceVariant = MutedInk,
    surfaceContainer = SurfaceContainerLight,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFFD9DEDB)
)

private val DarkColors = darkColorScheme(
    primary = MintDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00382E),
    primaryContainer = MintContainerDark,
    onPrimaryContainer = InkDark,
    secondary = MarigoldDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF452B00),
    secondaryContainer = MarigoldContainerDark,
    onSecondaryContainer = InkDark,
    tertiary = CoralDark,
    onTertiary = androidx.compose.ui.graphics.Color(0xFF5F111B),
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = InkDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceContainerDark,
    onSurfaceVariant = MutedInkDark,
    surfaceContainer = SurfaceContainerDark,
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF3E4743)
)

@Composable
fun LimeDayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
