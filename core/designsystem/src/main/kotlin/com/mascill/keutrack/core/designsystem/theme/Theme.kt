package com.mascill.keutrack.core.designsystem.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme

private val primaryDarkColors =
    KeuTrackPrimaryColors(
        primary900 = MidnightPrimaryTint,
        primary700 = MidnightPrimaryContainer,
        primary500 = MidnightPrimary,
        primary300 = MidnightPrimaryTint,
        primary100 = AliceBlue,
    )

private val primaryLightColors =
    KeuTrackPrimaryColors(
        primary900 = FinancialPrimary,
        primary700 = FinancialPrimaryContainer,
        primary500 = FinancialPrimary,
        primary300 = MidnightPrimary,
        primary100 = AliceBlue,
    )

private val neutralDarkColors =
    KeuTrackNeutralColors(
        black = ChineseBlack,
        white = White,
    )

private val neutralLightColors =
    KeuTrackNeutralColors(
        black = ChineseBlack,
        white = White,
    )

private val textDarkColors =
    KeuTrackTextColors(
        title = MidnightOnSurface,
        subtitle = MidnightOnSurface,
        body = MidnightOnSurfaceVariant,
        placeholder = MidnightOnSurfaceVariant,
        disable = CadetBlue,
        link = MidnightPrimaryTint,
    )

private val textLightColors =
    KeuTrackTextColors(
        title = FinancialOnSurface,
        subtitle = FinancialOnSurface,
        body = FinancialOnSurfaceVariant,
        placeholder = FinancialOnSurfaceVariant,
        disable = CadetBlue,
        link = FinancialPrimary,
    )

private val contentDarkColors =
    KeuTrackContentColors(
        pageNeutral = MidnightBackground,
        pageColor = MidnightBackground,
        popupCardNeutral = MidnightSurfaceHigh,
        popupCardColor = MidnightSurfaceHigh,
        section = MidnightSurfaceLow,
        border = MidnightOutlineVariant,
        icon2ndButton = White,
        formInput = MidnightSurfaceLowest,
    )

private val contentLightColors =
    KeuTrackContentColors(
        pageNeutral = FinancialBackground,
        pageColor = FinancialBackground,
        popupCardNeutral = FinancialSurfaceLowest,
        popupCardColor = FinancialSurfaceLowest,
        section = FinancialSurfaceLow,
        border = FinancialOutlineVariant,
        icon2ndButton = FinancialOnSurface,
        formInput = FinancialSurfaceHighest,
    )

private val successDarkColors =
    KeuTrackSuccessColors(
        s900 = ForestGreen,
        s700 = NorthTexasGreen,
        s500 = Green,
        s300 = Emerald,
        s100 = ChineseWhite,
    )

private val successLightColors =
    KeuTrackSuccessColors(
        s900 = ForestGreen,
        s700 = NorthTexasGreen,
        s500 = Green,
        s300 = Emerald,
        s100 = ChineseWhite,
    )

private val warningDarkColors =
    KeuTrackWarningColors(
        w900 = WindsorTan,
        w700 = Fulvous,
        w500 = Beer,
        w300 = YellowOrange,
        w100 = PapayaWhip,
    )

private val warningLightColors =
    KeuTrackWarningColors(
        w900 = WindsorTan,
        w700 = Fulvous,
        w500 = Beer,
        w300 = YellowOrange,
        w100 = PapayaWhip,
    )

private val dangerDarkColors =
    KeuTrackDangerColors(
        d900 = Blood,
        d700 = FireEngineRed,
        d500 = CarminePink,
        d300 = Begonia,
        d100 = MistyRose,
    )

private val dangerLightColors =
    KeuTrackDangerColors(
        d900 = Blood,
        d700 = FireEngineRed,
        d500 = CarminePink,
        d300 = Begonia,
        d100 = MistyRose,
    )

private val semanticDarkColors =
    KeuTrackSemanticColors(
        background = MidnightBackground,
        surface = MidnightBackground,
        surfaceContainerLowest = MidnightSurfaceLowest,
        surfaceContainerLow = MidnightSurfaceLow,
        surfaceContainerHigh = MidnightSurfaceHigh,
        surfaceContainerHighest = MidnightSurfaceHighest,
        onSurface = MidnightOnSurface,
        onSurfaceVariant = MidnightOnSurfaceVariant,
        outlineVariantGhost = MidnightOutlineVariant.copy(alpha = 0.2f),
        primary = MidnightPrimary,
        primaryContainer = MidnightPrimaryContainer,
        primaryGradientStart = MidnightPrimaryTint,
        primaryGradientEnd = MidnightPrimaryContainer,
        secondary = Emerald,
        tertiary = CarminePink,
        success = MidnightSuccess,
        error = MidnightError,
    )

private val semanticLightColors =
    KeuTrackSemanticColors(
        background = FinancialBackground,
        surface = FinancialBackground,
        surfaceContainerLowest = FinancialSurfaceLowest,
        surfaceContainerLow = FinancialSurfaceLow,
        surfaceContainerHigh = FinancialSurfaceHigh,
        surfaceContainerHighest = FinancialSurfaceHighest,
        onSurface = FinancialOnSurface,
        onSurfaceVariant = FinancialOnSurfaceVariant,
        outlineVariantGhost = FinancialOutlineVariant.copy(alpha = 0.15f),
        primary = FinancialPrimary,
        primaryContainer = FinancialPrimaryContainer,
        primaryGradientStart = FinancialPrimary,
        primaryGradientEnd = FinancialPrimaryContainer,
        secondary = FinancialSecondary,
        tertiary = FinancialTertiary,
        success = Green,
        error = CarminePink,
    )

private val shapeTokens =
    KeuTrackShapeTokens()

private val darkEffectTokens =
    KeuTrackEffectTokens(
        ghostBorderColor = MidnightOutlineVariant.copy(alpha = 0.2f),
        ambientGlowColor = MidnightPrimary.copy(alpha = 0.12f),
    )

private val lightEffectTokens =
    KeuTrackEffectTokens(
        ghostBorderColor = FinancialOutlineVariant.copy(alpha = 0.15f),
        ambientGlowColor = FinancialPrimary.copy(alpha = 0.08f),
    )

private val darkColorScheme =
    darkColors(
        primary = MidnightPrimary,
        primaryVariant = MidnightPrimaryContainer,
        background = MidnightBackground,
        surface = MidnightSurfaceLow,
        onPrimary = White,
        onSurface = MidnightOnSurface,
        error = MidnightError,
        onError = White,
    )

private val lightColorScheme =
    lightColors(
        primary = FinancialPrimary,
        primaryVariant = FinancialPrimaryContainer,
        background = FinancialBackground,
        surface = FinancialSurfaceLowest,
        onPrimary = White,
        onSurface = FinancialOnSurface,
        error = CarminePink,
        onError = White,
    )

/** My theme */
@Composable
fun KeuTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val typography = remember { KeuTrackTypography() }
    CompositionLocalProvider(
        LocalCustomPrimaryColors provides if (darkTheme) primaryDarkColors else primaryLightColors,
        LocalCustomNeutralColors provides if (darkTheme) neutralDarkColors else neutralLightColors,
        LocalCustomTextColors provides if (darkTheme) textDarkColors else textLightColors,
        LocalCustomContentColors provides if (darkTheme) contentDarkColors else contentLightColors,
        LocalCustomSuccessColors provides if (darkTheme) successDarkColors else successLightColors,
        LocalCustomWarningColors provides if (darkTheme) warningDarkColors else warningLightColors,
        LocalCustomDangerColors provides if (darkTheme) dangerDarkColors else dangerLightColors,
        LocalCustomSemanticColors provides if (darkTheme) semanticDarkColors else semanticLightColors,
        LocalCustomShapeTokens provides shapeTokens,
        LocalCustomEffectTokens provides if (darkTheme) darkEffectTokens else lightEffectTokens,
        LocalCustomTypography provides typography,
    ) {
        MaterialTheme(
            colors = if (darkTheme) darkColorScheme else lightColorScheme,
            content = content
        )
    }
}

/**
 * Return the value provided by the nearest CompositionLocalProvider component that invokes,
 * directly or indirectly, the composable function that uses this property.
 */
object KeuTrackTheme {
    val typography
        @Composable @ReadOnlyComposable get() = LocalCustomTypography.current

    val primaryColors
        @Composable @ReadOnlyComposable get() = LocalCustomPrimaryColors.current

    val neutralColors
        @Composable @ReadOnlyComposable get() = LocalCustomNeutralColors.current

    val textColors
        @Composable @ReadOnlyComposable get() = LocalCustomTextColors.current

    val contentColors
        @Composable @ReadOnlyComposable get() = LocalCustomContentColors.current

    val successColors
        @Composable @ReadOnlyComposable get() = LocalCustomSuccessColors.current

    val warningColors
        @Composable @ReadOnlyComposable get() = LocalCustomWarningColors.current

    val dangerColors
        @Composable @ReadOnlyComposable get() = LocalCustomDangerColors.current

    val semanticColors
        @Composable @ReadOnlyComposable get() = LocalCustomSemanticColors.current

    val shapeTokens
        @Composable @ReadOnlyComposable get() = LocalCustomShapeTokens.current

    val effectTokens
        @Composable @ReadOnlyComposable get() = LocalCustomEffectTokens.current
}
