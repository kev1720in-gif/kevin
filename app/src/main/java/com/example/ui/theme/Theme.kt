package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KImagenDarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonGlassTint,
    onPrimaryContainer = CrimsonSoftText,
    secondary = CrimsonDeep,
    onSecondary = Color.White,
    secondaryContainer = SmokedGlassElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = CrimsonSoftText,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = SmokedCharcoal,
    onSurface = TextPrimary,
    surfaceVariant = SmokedGlassSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = SmokedCharcoal,
    surfaceContainer = SmokedGlassSurface,
    surfaceContainerHigh = SmokedGlassElevated,
    surfaceContainerHighest = SmokedGlassHigh,
    outline = GlassBorderLight,
    outlineVariant = GlassBorderSubtle,
    error = CrimsonPrimary,
    onError = Color.White
)

private val KImagenShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun KImagenTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KImagenDarkColorScheme,
        typography = KImagenTypography,
        shapes = KImagenShapes,
        content = content
    )
}
