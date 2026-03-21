package com.mascill.keutrack.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mascill.keutrack.core.designsystem.R

/** KeuTrack global fonts */
val PTSansFamily =
    FontFamily(
        Font(R.font.ptsans_regular, FontWeight.Normal),
        Font(R.font.ptsans_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.ptsans_bolditalic, FontWeight.Bold, FontStyle.Italic),
        Font(R.font.ptsans_bold, FontWeight.Bold),
    )

/**
 * Typography of KeuTrack
 *
 * Typography is the art and technique of arranging type to make written language legible, readable
 * and appealing when displayed.
 */
@Immutable
data class KeuTrackTypography(
    val headingBold48: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 60.sp,
            letterSpacing = 0.96.sp
        ),
    val headingBold36: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 45.sp,
            letterSpacing = 0.72.sp
        ),
    val headingBold30: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 37.5.sp,
            letterSpacing = 0.6.sp
        ),
    val headingBold24: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp,
            letterSpacing = 0.48.sp
        ),
    val headingBold20: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
            letterSpacing = 0.4.sp
        ),
    val headingRegular48: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 48.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 60.sp,
            letterSpacing = 0.96.sp
        ),
    val headingRegular36: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 36.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 45.sp,
            letterSpacing = 0.72.sp
        ),
    val headingRegular30: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 30.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 37.5.sp,
            letterSpacing = 0.6.sp
        ),
    val headingRegular24: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 36.sp,
            letterSpacing = 0.48.sp
        ),
    val headingRegular20: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 30.sp,
            letterSpacing = 0.4.sp
        ),
    val bodyBold18: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 27.sp,
            letterSpacing = 0.36.sp,
        ),
    val bodyBold16: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
            letterSpacing = 0.32.sp,
        ),
    val bodyBold14: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 21.sp,
            letterSpacing = 0.28.sp,
        ),
    val bodyBold12: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            letterSpacing = 0.24.sp,
        ),
    val bodyBold10: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 14.sp,
            letterSpacing = 0.2.sp,
        ),
    val bodyRegular18: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 27.sp,
            letterSpacing = 0.36.sp,
        ),
    val bodyRegular16: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 24.sp,
            letterSpacing = 0.32.sp,
        ),
    val bodyRegular14: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 21.sp,
            letterSpacing = 0.28.sp,
        ),
    val bodyRegular12: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp,
            letterSpacing = 0.24.sp,
        ),
    val bodyRegular10: TextStyle =
        TextStyle(
            fontFamily = PTSansFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 14.sp,
            letterSpacing = 0.2.sp,
        ),
)

val LocalCustomTypography = staticCompositionLocalOf { KeuTrackTypography() }
