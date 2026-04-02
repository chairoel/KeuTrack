package com.mascill.keutrack.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class KeuTrackShapeTokens(
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 16.dp,
    val radiusXl: Dp = 24.dp,
    val buttonHeight: Dp = 56.dp,
    val progressThickness: Dp = 8.dp,
)

@Immutable
data class KeuTrackEffectTokens(
    val ghostBorderColor: Color = Color.Unspecified,
    val ghostBorderWidth: Dp = 1.dp,
    val ambientGlowColor: Color = Color.Unspecified,
    val ambientGlowBlur: Dp = 24.dp,
    val ambientGlowSpread: Dp = (-4).dp,
)

val LocalCustomShapeTokens = staticCompositionLocalOf { KeuTrackShapeTokens() }
val LocalCustomEffectTokens = staticCompositionLocalOf { KeuTrackEffectTokens() }
