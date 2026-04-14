package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val SETTINGS_FAMILY_TILE_ICON = 24
private const val SETTINGS_FAMILY_TILE_INNER_PAD = 12
private const val SETTINGS_FAMILY_TILE_LABEL_PT = 8

@Composable
fun SettingsFamilyActionTile(
    imageVector: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    KeuTrackCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(SETTINGS_FAMILY_TILE_INNER_PAD.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = semantic.primary,
                modifier = Modifier.size(SETTINGS_FAMILY_TILE_ICON.dp),
            )
            Text(
                text = label,
                style = typography.bodyBold12,
                color = textColors.title,
                modifier = Modifier.padding(top = SETTINGS_FAMILY_TILE_LABEL_PT.dp),
            )
        }
    }
}

//@Preview
//@Composable
//private fun SettingsFamilyActionTilePreview() {
//    KeuTrackTheme {
//        SettingsFamilyActionTile(
//            imageVector = Icons.Outlined.PersonAdd,
//            label = "Invite Member",
//            onClick = {},
//            modifier = Modifier.fillMaxWidth(),
//        )
//    }
//}
