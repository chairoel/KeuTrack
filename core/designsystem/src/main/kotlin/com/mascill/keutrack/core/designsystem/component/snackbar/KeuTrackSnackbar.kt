package com.mascill.keutrack.core.designsystem.component.snackbar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mascill.keutrack.core.designsystem.component.snackbar.model.KeuTrackSnackbarDuration
import com.mascill.keutrack.core.designsystem.component.snackbar.util.toMillis
import com.mascill.keutrack.core.designsystem.model.KeuTrackSnackbarData
import com.mascill.keutrack.core.designsystem.model.KeuTrackSnackbarTone
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import kotlinx.coroutines.delay

private const val SB_MARGIN = 16
private const val SB_MARGIN_TOP = 64
private const val SB_Z_INDEX = 100f
private const val SB_PADDING = 16
private const val SB_CONTENT_GAP = 12
private const val SB_IC_SIZE = 24
private const val SB_ACTION_PAD = 8
private const val SB_MAX_WIDTH = 560

private val DefaultSnackbarMargin
    get() = PaddingValues(
        start = SB_MARGIN.dp,
        top = SB_MARGIN_TOP.dp,
        end = SB_MARGIN.dp,
        bottom = SB_MARGIN.dp,
    )

/**
 * Custom snackbar with leading icon, message, and optional trailing action.
 */
@Composable
fun BoxScope.KeuTrackSnackbar(
    icon: ImageVector,
    text: String,
    textColor: Color,
    backgroundColor: Color,
    actionText: String? = null,
    position: Alignment = Alignment.TopCenter,
    margin: PaddingValues = DefaultSnackbarMargin,
    onClick: () -> Unit = {},
) {
    val isShowAction = !actionText.isNullOrEmpty()

    Row(
        modifier = Modifier
            .padding(margin)
            .align(position)
            .zIndex(SB_Z_INDEX)
            .widthIn(max = SB_MAX_WIDTH.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(KeuTrackTheme.shapeTokens.radiusMd))
            .background(backgroundColor)
            .padding(
                start = SB_PADDING.dp,
                top = SB_PADDING.dp,
                bottom = SB_PADDING.dp,
                end = if (isShowAction) SB_ACTION_PAD.dp else SB_PADDING.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(SB_IC_SIZE.dp),
        )
        Text(
            text = text,
            textAlign = TextAlign.Start,
            style = KeuTrackTheme.typography.bodyRegular14,
            color = textColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = SB_CONTENT_GAP.dp),
        )
        if (isShowAction) {
            Text(
                text = actionText,
                style = KeuTrackTheme.typography.bodyBold14,
                color = textColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(KeuTrackTheme.shapeTokens.radiusMd))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onClick,
                    )
                    .padding(SB_ACTION_PAD.dp),
            )
        }
    }
}

/**
 * Renders [data] with tone-based defaults for icon and colors.
 */
@Composable
fun BoxScope.KeuTrackSnackbar(
    data: KeuTrackSnackbarData,
    position: Alignment = Alignment.TopCenter,
    onAction: () -> Unit = {},
) {
    val visuals = data.tone.visuals()
    KeuTrackSnackbar(
        icon = data.icon ?: visuals.icon,
        text = data.message,
        textColor = data.textColor ?: visuals.textColor,
        backgroundColor = data.backgroundColor ?: visuals.backgroundColor,
        actionText = data.actionLabel,
        position = position,
        margin = data.margin ?: DefaultSnackbarMargin,
        onClick = onAction,
    )
}

/**
 * Inline overlay snackbar for screens that keep the message in UI state.
 */
@Composable
fun BoxScope.KeuTrackInlineSnackbar(
    message: String,
    onDismiss: () -> Unit,
    actionLabel: String,
    tone: KeuTrackSnackbarTone = KeuTrackSnackbarTone.Danger,
    position: Alignment = Alignment.TopCenter,
) {
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(message) {
        val duration = KeuTrackSnackbarDuration.Short.toMillis(
            hasAction = actionLabel.isNotEmpty(),
            accessibilityManager = accessibilityManager,
        )
        delay(duration)
        onDismiss()
    }
    KeuTrackSnackbar(
        data = KeuTrackSnackbarData(
            message = message,
            actionLabel = actionLabel,
            tone = tone,
        ),
        position = position,
        onAction = onDismiss,
    )
}

@Composable
internal fun KeuTrackSnackbarTone.visuals(): SnackbarToneVisuals {
    val icon = when (this) {
        KeuTrackSnackbarTone.Danger -> Icons.Filled.Error
        KeuTrackSnackbarTone.Success -> Icons.Filled.CheckCircle
        KeuTrackSnackbarTone.Warning -> Icons.Filled.Warning
        KeuTrackSnackbarTone.Info -> Icons.Filled.Info
    }
    val background = when (this) {
        KeuTrackSnackbarTone.Danger -> KeuTrackTheme.dangerColors.d500
        KeuTrackSnackbarTone.Success -> KeuTrackTheme.successColors.s500
        KeuTrackSnackbarTone.Warning -> KeuTrackTheme.warningColors.w500
        KeuTrackSnackbarTone.Info -> KeuTrackTheme.primaryColors.primary500
    }
    return SnackbarToneVisuals(
        icon = icon,
        backgroundColor = background,
        textColor = KeuTrackTheme.neutralColors.white,
    )
}

internal data class SnackbarToneVisuals(
    val icon: ImageVector,
    val backgroundColor: Color,
    val textColor: Color,
)

@Preview(name = "Light", showBackground = true, widthDp = 360, heightDp = 160)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true, widthDp = 360, heightDp = 160)
@Composable
private fun KeuTrackSnackbarPreview() {
    KeuTrackTheme {
        Box {
            KeuTrackSnackbar(
                data = KeuTrackSnackbarData(
                    message = "Gagal menyimpan perubahan, coba lagi.",
                    actionLabel = "X",
                    tone = KeuTrackSnackbarTone.Danger,
                ),
            )
        }
    }
}

@Preview(name = "Success", showBackground = true, widthDp = 360, heightDp = 160)
@Composable
private fun KeuTrackSnackbarSuccessPreview() {
    KeuTrackTheme {
        Box {
            KeuTrackSnackbar(
                data = KeuTrackSnackbarData(
                    message = "Kode keluarga berhasil disalin.",
                    actionLabel = "Tutup",
                    tone = KeuTrackSnackbarTone.Success,
                ),
            )
        }
    }
}
