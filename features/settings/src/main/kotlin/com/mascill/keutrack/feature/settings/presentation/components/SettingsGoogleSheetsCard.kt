package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackButton
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.model.KeuTrackButtonStyle
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val SETTINGS_SHEETS_ICON_BOX = 44
private const val SETTINGS_SHEETS_ICON_INNER = 26
private const val SETTINGS_SHEETS_CONTENT_WEIGHT = 1f
private const val SETTINGS_SHEETS_CONTENT_PADDING = 16
private const val SETTINGS_SHEETS_CONTENT_DESC_PADDING = 2
private const val SETTINGS_SHEETS_EXPORT_PT = 12
private const val SETTINGS_SHEETS_EXPORT_ICON = 20
private const val SETTINGS_SHEETS_TITLE = "Google Sheets"
private const val SETTINGS_SHEETS_SUBTITLE = "Real-time sync enabled"
private const val SETTINGS_SHEETS_EXPORT_CTA = "Export Now"

@Composable
fun SettingsGoogleSheetsCard(
    syncEnabled: Boolean,
    onSyncChange: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val shapes = KeuTrackTheme.shapeTokens
    val neutral = KeuTrackTheme.neutralColors

    KeuTrackCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(SETTINGS_SHEETS_ICON_BOX.dp)
                            .clip(RoundedCornerShape(shapes.radiusMd))
                            .background(semantic.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TableChart,
                        contentDescription = null,
                        tint = semantic.primary,
                        modifier = Modifier.size(SETTINGS_SHEETS_ICON_INNER.dp),
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .weight(SETTINGS_SHEETS_CONTENT_WEIGHT)
                            .padding(horizontal = SETTINGS_SHEETS_CONTENT_PADDING.dp),
                ) {
                    Text(
                        text = SETTINGS_SHEETS_TITLE,
                        style = typography.bodyBold16,
                        color = textColors.title,
                    )
                    Text(
                        text = SETTINGS_SHEETS_SUBTITLE,
                        style = typography.bodyRegular14,
                        color = textColors.body,
                        modifier = Modifier.padding(top = SETTINGS_SHEETS_CONTENT_DESC_PADDING.dp),
                    )
                }
                Switch(
                    checked = syncEnabled,
                    onCheckedChange = onSyncChange,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = neutral.white,
                            checkedTrackColor = semantic.primary,
                            uncheckedThumbColor = semantic.onSurfaceVariant,
                            uncheckedTrackColor = semantic.surfaceContainerHigh,
                        ),
                )
            }
            Spacer(modifier = Modifier.height(SETTINGS_SHEETS_EXPORT_PT.dp))
            KeuTrackButton(
                text = SETTINGS_SHEETS_EXPORT_CTA,
                onClick = onExportClick,
                style = KeuTrackButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(SETTINGS_SHEETS_EXPORT_ICON.dp),
                    )
                },
            )
        }
    }
}
//
//@Preview
//@Composable
//private fun SettingsGoogleSheetsCardPreview() {
//    KeuTrackTheme {
//        SettingsGoogleSheetsCard(
//            syncEnabled = true,
//            onSyncChange = {},
//            onExportClick = {}
//        )
//    }
//}
