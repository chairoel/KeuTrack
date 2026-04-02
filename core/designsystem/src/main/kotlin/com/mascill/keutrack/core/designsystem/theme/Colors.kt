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

// Atelier palette - dark (Midnight)
val MidnightPrimary = Color(0xFF7986CB)
val MidnightPrimaryTint = Color(0xFFB9C3FF)
val MidnightPrimaryContainer = Color(0xFF808DD2)
val MidnightBackground = Color(0xFF0B1326)
val MidnightSurfaceLowest = Color(0xFF060E20)
val MidnightSurfaceLow = Color(0xFF131B2E)
val MidnightSurfaceHigh = Color(0xFF222A3D)
val MidnightSurfaceHighest = Color(0xFF2D3449)
val MidnightOnSurface = Color(0xFFE6EBF3)
val MidnightOnSurfaceVariant = Color(0xFFC6C5D2)
val MidnightOutlineVariant = Color(0xFF454650)
val MidnightSuccess = Color(0xFF4ADE80)
val MidnightError = Color(0xFFFB7185)

// Atelier palette - light (Financial)
val FinancialPrimary = Color(0xFF0B1A7D)
val FinancialPrimaryContainer = Color(0xFF283593)
val FinancialSecondary = Color(0xFF27EFD3)
val FinancialTertiary = Color(0xFFC62828)
val FinancialBackground = Color(0xFFF9F9FB)
val FinancialSurfaceLowest = Color(0xFFFFFFFF)
val FinancialSurfaceLow = Color(0xFFF3F3F5)
val FinancialSurfaceHigh = Color(0xFFECECF0)
val FinancialSurfaceHighest = Color(0xFFE2E2E4)
val FinancialOnSurface = Color(0xFF1A1C1D)
val FinancialOnSurfaceVariant = Color(0xFF4E5565)
val FinancialOutlineVariant = Color(0xFF7A8090)

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

@Immutable
data class KeuTrackSemanticColors(
    val background: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
    val surfaceContainerLowest: Color = Color.Unspecified,
    val surfaceContainerLow: Color = Color.Unspecified,
    val surfaceContainerHigh: Color = Color.Unspecified,
    val surfaceContainerHighest: Color = Color.Unspecified,
    val onSurface: Color = Color.Unspecified,
    val onSurfaceVariant: Color = Color.Unspecified,
    val outlineVariantGhost: Color = Color.Unspecified,
    val primary: Color = Color.Unspecified,
    val primaryContainer: Color = Color.Unspecified,
    val primaryGradientStart: Color = Color.Unspecified,
    val primaryGradientEnd: Color = Color.Unspecified,
    val secondary: Color = Color.Unspecified,
    val tertiary: Color = Color.Unspecified,
    val success: Color = Color.Unspecified,
    val error: Color = Color.Unspecified,
)

val LocalCustomPrimaryColors = staticCompositionLocalOf { KeuTrackPrimaryColors() }
val LocalCustomNeutralColors = staticCompositionLocalOf { KeuTrackNeutralColors() }
val LocalCustomTextColors = staticCompositionLocalOf { KeuTrackTextColors() }
val LocalCustomContentColors = staticCompositionLocalOf { KeuTrackContentColors() }
val LocalCustomSuccessColors = staticCompositionLocalOf { KeuTrackSuccessColors() }
val LocalCustomWarningColors = staticCompositionLocalOf { KeuTrackWarningColors() }
val LocalCustomDangerColors = staticCompositionLocalOf { KeuTrackDangerColors() }
val LocalCustomSemanticColors = staticCompositionLocalOf { KeuTrackSemanticColors() }
