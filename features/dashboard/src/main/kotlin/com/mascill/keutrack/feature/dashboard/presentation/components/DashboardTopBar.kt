package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.component.ProfileImage
import com.mascill.keutrack.core.designsystem.model.KeuTrackTopBarTitleAlignment
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val DASH_TOP_BRAND_SPACING = 8
private const val DASH_TOP_AVATAR = 44
private const val DASH_TOP_AVATAR_ICON = 36
private const val DASH_TOP_BAR_TITLE = "KeuTrack"
private const val DASH_TOP_BAR_TRAILING_CD = "Settings"

@Composable
fun DashboardTopBar(
    avatar: String?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    KeuTrackTopBar(
        modifier = modifier,
        titleAlignment = KeuTrackTopBarTitleAlignment.Start,
        leading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DASH_TOP_BRAND_SPACING.dp),
            ) {
                ProfileImage(
                    photoUrl = avatar,
                    avatarSize = DASH_TOP_AVATAR,
                    iconSize = DASH_TOP_AVATAR_ICON,
                )
                Text(
                    text = DASH_TOP_BAR_TITLE,
                    style = typography.headingBold20,
                    color = textColors.title,
                )
            }
        },
        title = {},
        trailing = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = DASH_TOP_BAR_TRAILING_CD,
                    tint = semantic.onSurface,
                )
            }
        },
    )
}

//@Preview(showBackground = true)
//@Composable
//private fun DashboardTopBarPreview() {
//    KeuTrackTheme(darkTheme = false) {
//        DashboardTopBar(
//            onSettingsClick = { }
//        )
//    }
//}

