package com.mascill.keutrack.core.designsystem.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val primaryDarkColors =
    KeuTrackPrimaryColors(
        primary900 = White,
        primary700 = White,
        primary500 = AzureRadiance,
        primary300 = JordyBlue,
        primary100 = AliceBlue,
    )

private val neutralDarkColors =
    KeuTrackNeutralColors(
        black = ChineseBlack,
        white = White,
    )

private val textDarkColors =
    KeuTrackTextColors(
        title = White,
        subtitle = White,
        body = BrightGray,
        placeholder = PewterBlue,
        disable = CadetBlue,
        link = JordyBlue,
    )

private val contentDarkColors =
    KeuTrackContentColors(
        pageNeutral = DarkJungleGreen,
        pageColor = DarkJungleGreen,
        popupCardNeutral = YankeesBlue,
        popupCardColor = YankeesBlue,
        section = PickledBlueWood,
        border = DarkElectricBlue,
        icon2ndButton = White,
        formInput = DarkJungleGreen,
    )

private val successDarkColors =
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

private val dangerDarkColors =
    KeuTrackDangerColors(
        d900 = Blood,
        d700 = FireEngineRed,
        d500 = CarminePink,
        d300 = Begonia,
        d100 = MistyRose,
    )

private val darkColorScheme = darkColors()

/** My theme */
@Composable
fun KeuTrackTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCustomPrimaryColors provides primaryDarkColors,
        LocalCustomNeutralColors provides neutralDarkColors,
        LocalCustomTextColors provides textDarkColors,
        LocalCustomContentColors provides contentDarkColors,
        LocalCustomSuccessColors provides successDarkColors,
        LocalCustomWarningColors provides warningDarkColors,
        LocalCustomDangerColors provides dangerDarkColors,
        LocalCustomTypography provides KeuTrackTypography(),
    ) { MaterialTheme(colors = darkColorScheme, content = content) }
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
}
