package com.mascill.keutrack.core.designsystem.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

@Composable
fun KeuTrackButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: KeuTrackButtonStyle = KeuTrackButtonStyle.Primary,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val shapes = KeuTrackTheme.shapeTokens
    val semantic = KeuTrackTheme.semanticColors
    val textColor =
        when (style) {
            KeuTrackButtonStyle.Primary -> Color.White
            KeuTrackButtonStyle.Secondary -> semantic.onSurface
            KeuTrackButtonStyle.Tertiary -> semantic.primary
        }

    val backgroundModifier =
        if (style == KeuTrackButtonStyle.Primary) {
            Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(semantic.primaryGradientStart, semantic.primaryGradientEnd)
                ),
                shape = RoundedCornerShape(shapes.radiusXl)
            )
        } else {
            Modifier
        }

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(shapes.radiusXl),
        colors = ButtonDefaults.buttonColors(
            backgroundColor =
                when (style) {
                    KeuTrackButtonStyle.Primary -> Color.Transparent
                    KeuTrackButtonStyle.Secondary -> semantic.surfaceContainerHigh
                    KeuTrackButtonStyle.Tertiary -> Color.Transparent
                },
            contentColor = textColor,
            disabledBackgroundColor = semantic.surfaceContainerHigh,
            disabledContentColor = semantic.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        modifier = modifier
            .height(shapes.buttonHeight)
            .clip(RoundedCornerShape(shapes.radiusXl))
            .then(backgroundModifier)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = textColor,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.defaultMinSize(minHeight = 24.dp)
            ) {
                leading?.invoke()
                if (leading != null) {
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(text = text, style = KeuTrackTheme.typography.bodyBold16)
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun KeuTrackButtonPreview() {
    KeuTrackTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KeuTrackButton(
                text = "Primary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
            KeuTrackButton(
                text = "Secondary",
                onClick = {},
                style = KeuTrackButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            KeuTrackButton(
                text = "Tertiary",
                onClick = {},
                style = KeuTrackButtonStyle.Tertiary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
