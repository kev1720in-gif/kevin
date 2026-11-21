package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.frostedGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = SmokedGlassSurface,
    accentBorder: Boolean = false,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                tint.copy(alpha = (tint.alpha + 0.08f).coerceAtMost(0.95f)),
                tint,
                Color(0xA60B0B0F)
            )
        ),
        shape = shape
    )
    .border(
        border = BorderStroke(
            width = borderWidth,
            brush = Brush.linearGradient(
                colors = if (accentBorder) {
                    listOf(
                        CrimsonPrimary.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.20f),
                        CrimsonDeep.copy(alpha = 0.35f)
                    )
                } else {
                    listOf(
                        Color.White.copy(alpha = 0.24f),
                        Color.White.copy(alpha = 0.06f),
                        CrimsonPrimary.copy(alpha = 0.22f)
                    )
                }
            )
        ),
        shape = shape
    )

@Composable
fun GlassSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    accentBorder: Boolean = false,
    tint: Color = SmokedGlassSurface,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.frostedGlass(
                shape = shape,
                tint = tint,
                accentBorder = accentBorder
            ),
            content = content
        )
    }
}

@Composable
fun AtmosphericGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .drawBehind {
                // Top-left crimson refraction glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CrimsonDeep.copy(alpha = 0.26f),
                            CrimsonPrimary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.08f),
                        radius = size.maxDimension * 0.58f
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.08f),
                    radius = size.maxDimension * 0.58f
                )
                // Bottom-right subtle ruby depth glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            CrimsonPrimary.copy(alpha = 0.18f),
                            CrimsonDeep.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.86f),
                        radius = size.maxDimension * 0.52f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.86f),
                    radius = size.maxDimension * 0.52f
                )
                // Diagonal smoked-glass sheen
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent,
                            CrimsonPrimary.copy(alpha = 0.025f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
            },
        content = content
    )
}
