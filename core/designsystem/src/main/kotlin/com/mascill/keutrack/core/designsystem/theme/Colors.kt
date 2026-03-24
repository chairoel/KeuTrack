package com.mascill.keutrack.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color name based on hex color from [https://www.color-name.com/hex/a1afc2] submenu ntc and
 * grouped by base basic color from [https://colordesigner.io/color-name-finder].
 *
 * Note:
 * - not using basic color website to get color name, because of some hex have same name
 */
// Black
val DarkJungleGreen = Color(0xFF0F172A)
val ChineseBlack = Color(0xFF121212)
val YankeesBlue = Color(0xFF202938)

// White
val AliceBlue = Color(0xFFF2F7FC)
val AzureWhite = Color(0xFFD3DDEB)
val BrightGray = Color(0xFFE6EBF3)
val GhostWhite = Color(0xFFF3F6FF)
val LightPeriwinkle = Color(0xFFC2CEE1)
val White = Color(0xFFFFFFFF)

// Gray
val CadetBlue = Color(0xFFA1AFC2)
val DarkElectricBlue = Color(0xFF5B6C83)
val JapaneseIndigo = Color(0xFF2B3A4E)
val PewterBlue = Color(0xFF8A9DB9)
val PickledBlueWood = Color(0xFF334155)

// Blue
val AzureRadiance = Color(0xFF147AFC)
val Denim = Color(0xFF0A60CD)
val CobaltBlue = Color(0xFF0846A1)
val MidnightBlue = Color(0xFF08448F)
val JordyBlue = Color(0xFF89BDFF)

// Green
val Emerald = Color(0xFF5DC985)
val NorthTexasGreen = Color(0xFF008932)
val ChineseWhite = Color(0xFFD8F3E2)
val Green = Color(0xFF00B844)
val ForestGreen = Color(0xFF00501D)

// Brown
val WindsorTan = Color(0xFFA95903)

// Orange
val Beer = Color(0xFFFA8B15)
val Fulvous = Color(0xFFE07400)
val PapayaWhip = Color(0xFFFFEEDB)
val YellowOrange = Color(0xFFFFA544)

// Red
val Begonia = Color(0xFFF97676)
val Blood = Color(0xFF8E0A0A)
val CarminePink = Color(0xFFEF4444)
val FireEngineRed = Color(0xFFCF2424)
val MistyRose = Color(0xFFFFDFDF)

/**
 * -----------------------------------------------------------------------------------------------
 */

/**
 * Primary Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackPrimaryColors(
    val primary900: Color = Color.Unspecified,
    val primary700: Color = Color.Unspecified,
    val primary500: Color = Color.Unspecified,
    val primary300: Color = Color.Unspecified,
    val primary100: Color = Color.Unspecified,
)

/**
 * Neutral Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackNeutralColors(
    val black: Color = Color.Unspecified,
    val white: Color = Color.Unspecified,
)

/**
 * Text Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackTextColors(
    val title: Color = Color.Unspecified,
    val subtitle: Color = Color.Unspecified,
    val body: Color = Color.Unspecified,
    val placeholder: Color = Color.Unspecified,
    val disable: Color = Color.Unspecified,
    val link: Color = Color.Unspecified,
)

/**
 * Content Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackContentColors(
    val pageNeutral: Color = Color.Unspecified,
    val pageColor: Color = Color.Unspecified,
    val popupCardNeutral: Color = Color.Unspecified,
    val popupCardColor: Color = Color.Unspecified,
    val section: Color = Color.Unspecified,
    val border: Color = Color.Unspecified,
    val icon2ndButton: Color = Color.Unspecified,
    val formInput: Color = Color.Unspecified,
)

/**
 * Success Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackSuccessColors(
    val s900: Color = Color.Unspecified,
    val s700: Color = Color.Unspecified,
    val s500: Color = Color.Unspecified,
    val s300: Color = Color.Unspecified,
    val s100: Color = Color.Unspecified,
)

/**
 * Warning Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackWarningColors(
    val w900: Color = Color.Unspecified,
    val w700: Color = Color.Unspecified,
    val w500: Color = Color.Unspecified,
    val w300: Color = Color.Unspecified,
    val w100: Color = Color.Unspecified,
)

/**
 * Danger Colors based on KeuTrack DesignSystem
 *
 * @see
 * [https://www.figma.com/design/omc8qRxtrvUnEASl8J3Kvm/KeuTrack---Design-System?node-id=2001-13&p=f&m=dev]
 */
@Immutable
data class KeuTrackDangerColors(
    val d900: Color = Color.Unspecified,
    val d700: Color = Color.Unspecified,
    val d500: Color = Color.Unspecified,
    val d300: Color = Color.Unspecified,
    val d100: Color = Color.Unspecified,
)

val LocalCustomPrimaryColors = staticCompositionLocalOf { KeuTrackPrimaryColors() }
val LocalCustomNeutralColors = staticCompositionLocalOf { KeuTrackNeutralColors() }
val LocalCustomTextColors = staticCompositionLocalOf { KeuTrackTextColors() }
val LocalCustomContentColors = staticCompositionLocalOf { KeuTrackContentColors() }
val LocalCustomSuccessColors = staticCompositionLocalOf { KeuTrackSuccessColors() }
val LocalCustomWarningColors = staticCompositionLocalOf { KeuTrackWarningColors() }
val LocalCustomDangerColors = staticCompositionLocalOf { KeuTrackDangerColors() }
