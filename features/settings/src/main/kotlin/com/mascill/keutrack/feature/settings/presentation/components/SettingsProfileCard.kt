package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.settings.presentation.model.SettingsProfileUi

private const val SETTINGS_PROFILE_AVATAR = 56
private const val SETTINGS_PROFILE_AVATAR_ICON = 44
private const val SETTINGS_PROFILE_EDIT_BADGE = 22
private const val SETTINGS_PROFILE_EDIT_ICON = 14
private const val SETTINGS_PROFILE_EDIT_ICON_OFFSET = 4
private const val SETTINGS_PROFILE_NAME_PT = 4
private const val SETTINGS_PROFILE_ROW_SPACING = 16

@Composable
fun SettingsProfileCard(
    profile: SettingsProfileUi,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    KeuTrackCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Box(
                    modifier =
                        Modifier
                            .size(SETTINGS_PROFILE_AVATAR.dp)
                            .clip(CircleShape)
                            .background(semantic.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = semantic.onSurfaceVariant,
                        modifier = Modifier.size(SETTINGS_PROFILE_AVATAR_ICON.dp),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(
                                x = SETTINGS_PROFILE_EDIT_ICON_OFFSET.dp,
                                y = SETTINGS_PROFILE_EDIT_ICON_OFFSET.dp
                            )
                            .size(SETTINGS_PROFILE_EDIT_BADGE.dp)
                            .clip(CircleShape)
                            .background(semantic.success),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit profile",
                        tint = KeuTrackTheme.neutralColors.white,
                        modifier = Modifier.size(SETTINGS_PROFILE_EDIT_ICON.dp),
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .padding(start = SETTINGS_PROFILE_ROW_SPACING.dp)
                        .weight(1f),
            ) {
                Text(
                    text = profile.displayName,
                    style = typography.headingBold20,
                    color = textColors.title,
                )
                Spacer(modifier = Modifier.height(SETTINGS_PROFILE_NAME_PT.dp))
                Text(
                    text = profile.email,
                    style = typography.bodyRegular14,
                    color = textColors.body,
                )
            }
        }
    }
}

//@Preview
//@Composable
//private fun SettingsProfileCardPreview() {
//    KeuTrackTheme {
//        SettingsProfileCard(
//            profile = SettingsProfileUi(
//                displayName = "Adhi",
//                email = "adhi.k@family.com"
//            )
//        )
//    }
//}
