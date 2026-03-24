package com.mascill.keutrack.core.common.utils

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import kotlin.getOrDefault
import kotlin.runCatching
import kotlin.text.matches

private const val HEX_COLOR_REGEX = "^#[a-fA-F0-9]{6}$"
private const val OPT_TAG_HEX_COLOR_REGEX = "^#?[a-fA-F0-9]{6}$"

/**
 * Check string is valid color hexadecimal or not
 *
 * @param optionalHashTag flag to determined hash tag is required or not to check the string
 */
fun String.isValidColorHex(
    optionalHashTag: Boolean = false
): Boolean {
    val regexPattern = if (optionalHashTag) OPT_TAG_HEX_COLOR_REGEX else HEX_COLOR_REGEX
    return matches(Regex(regexPattern))
}

/**
 * Convert string to color hexadecimal
 *
 * @return color hexadecimal if it's valid color string or just return the same string back if it's
 * not.
 */
fun String.toHexColor(): String =
    if (isValidColorHex(optionalHashTag = true) && this[0] != '#') {
        "#$this"
    } else {
        this
    }

/**
 * Convert hexadecimal color string to compose color with safe try catch in case [toColorInt]
 * throw Exception
 *
 * @return transformed hexadecimal color
 * @return default color RGBA(0, 0, 0, 0) / [Color.Unspecified]
 */
fun String.toComposeColor(): Color = runCatching {
    Color(this@toComposeColor.toColorInt())
}.getOrDefault(Color.Unspecified)