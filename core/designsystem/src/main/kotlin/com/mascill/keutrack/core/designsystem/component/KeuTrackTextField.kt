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
    val semantic = KeuTrackTheme.semanticColors
    val textColor = if (isError) semantic.error else semantic.onSurface
    val shape = RoundedCornerShape(KeuTrackTheme.shapeTokens.radiusMd)

    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        label = label?.let { { Text(text = it, style = KeuTrackTheme.typography.bodyRegular12) } },
        placeholder = placeholder?.let {
            { Text(text = it, style = KeuTrackTheme.typography.bodyRegular14) }
        },
        isError = isError,
        shape = shape,
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = semantic.surfaceContainerHighest,
            textColor = textColor,
            cursorColor = semantic.primary,
            focusedIndicatorColor = semantic.primary,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = semantic.error,
            focusedLabelColor = semantic.primary,
            unfocusedLabelColor = semantic.onSurfaceVariant,
            placeholderColor = semantic.onSurfaceVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape),
    )
}
