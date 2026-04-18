package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackTopBar
import com.mascill.keutrack.core.designsystem.model.KeuTrackTopBarTitleAlignment
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val FAM_TOP_BRAND_SPACING = 8
private const val FAM_TOP_AVATAR_BOX = 44
private const val FAM_TOP_AVATAR_ICON = 28
private const val FAM_TOP_BAR_TITLE = "Family Insights"
private const val FAM_TOP_BAR_SETTINGS_CD = "Settings"

@Composable
fun FamilyTopBar(
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
                horizontalArrangement = Arrangement.spacedBy(FAM_TOP_BRAND_SPACING.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(FAM_TOP_AVATAR_BOX.dp)
                            .clip(CircleShape)
                            .background(semantic.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = semantic.onSurfaceVariant,
                        modifier = Modifier.size(FAM_TOP_AVATAR_ICON.dp),
                    )
                }

                Text(
                    text = FAM_TOP_BAR_TITLE,
                    style = typography.headingBold20,
                    color = textColors.title,
                )
            }
        },
        title = {},
        trailing = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = FAM_TOP_BAR_SETTINGS_CD,
                    tint = semantic.onSurface,
                )
            }
        },
    )
}
