package com.limeday.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.limeday.app.settings.ThemeMode

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

private val DoodleShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LimeDayTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = DoodleShapes,
        content = content
    )
}
