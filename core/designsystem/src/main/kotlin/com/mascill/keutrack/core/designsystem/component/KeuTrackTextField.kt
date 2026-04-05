package com.mascill.keutrack.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextFieldColors
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun KeuTrackTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(KeuTrackTheme.shapeTokens.radiusMd)

    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = label?.let { text ->
            { KeuTrackTextFieldLabel(text) }
        },
        placeholder = placeholder?.let { text ->
            { KeuTrackTextFieldPlaceholder(text) }
        },
        isError = isError,
        shape = shape,
        colors = keuTrackTextFieldColors(isError),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
    )
}

/**
 * Sub-komponen untuk Label
 */
@Composable
private fun KeuTrackTextFieldLabel(text: String) {
    Text(
        text = text,
        style = KeuTrackTheme.typography.bodyRegular12
    )
}

/**
 * Sub-komponen untuk Placeholder
 */
@Composable
private fun KeuTrackTextFieldPlaceholder(text: String) {
    Text(
        text = text,
        style = KeuTrackTheme.typography.bodyRegular14
    )
}

/**
 * Fungsi pembantu (Helper) untuk mendefinisikan warna secara terpisah
 */
@Composable
private fun keuTrackTextFieldColors(isError: Boolean): TextFieldColors {
    val semantic = KeuTrackTheme.semanticColors

    return TextFieldDefaults.textFieldColors(
        // Background & Text
        backgroundColor = semantic.surfaceContainerHighest,
        textColor = if (isError) semantic.error else semantic.onSurface,
        cursorColor = semantic.primary,

        // Indicators (Garis bawah)
        focusedIndicatorColor = semantic.primary,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = semantic.error,

        // Label & Placeholder Colors
        focusedLabelColor = semantic.primary,
        unfocusedLabelColor = semantic.onSurfaceVariant,
        placeholderColor = semantic.onSurfaceVariant,
    )
}
