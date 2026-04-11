package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val DASH_TOP_BRAND_SPACING = 8
private const val DASH_TOP_AVATAR_BOX = 44
private const val DASH_TOP_AVATAR_ICON = 28

@Composable
fun DashboardTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DASH_TOP_BRAND_SPACING.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(DASH_TOP_AVATAR_BOX.dp)
                        .clip(CircleShape)
                        .background(semantic.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    tint = semantic.onSurfaceVariant,
                    modifier = Modifier.size(DASH_TOP_AVATAR_ICON.dp),
                )
            }

            Text(
                text = "KeuTrack",
                style = typography.headingBold20,
                color = textColors.title,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = "Settings",
                tint = semantic.onSurface,
            )
        }
    }
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

