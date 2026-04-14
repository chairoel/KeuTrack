package com.mascill.keutrack.feature.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val SETTINGS_FAMILY_HERO_PH = 20
private const val SETTINGS_FAMILY_HERO_PV = 20
private const val SETTINGS_FAMILY_ID_LABEL_PT = 8
private const val SETTINGS_FAMILY_COPY_PT = 16
private const val SETTINGS_FAMILY_COPY_PV = 4
private const val SETTINGS_FAMILY_COPY_ICON_PS = 6

@Composable
fun SettingsFamilyIdHeroCard(
    familyIdCode: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val typography = KeuTrackTheme.typography
    val shapes = KeuTrackTheme.shapeTokens
    val neutral = KeuTrackTheme.neutralColors

    val gradient =
        Brush.linearGradient(
            colors =
                listOf(
                    semantic.primaryGradientStart,
                    semantic.primaryGradientEnd,
                ),
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(shapes.radiusLg))
                .background(brush = gradient)
                .padding(
                    horizontal = SETTINGS_FAMILY_HERO_PH.dp,
                    vertical = SETTINGS_FAMILY_HERO_PV.dp
                ),
    ) {
        Text(
            text = "Family ID",
            style = typography.bodyBold10,
            color = neutral.white
        )
        Spacer(modifier = Modifier.height(SETTINGS_FAMILY_ID_LABEL_PT.dp))
        Text(
            text = familyIdCode,
            style = typography.headingBold24,
            color = neutral.white,
        )
        Spacer(modifier = Modifier.height(SETTINGS_FAMILY_COPY_PT.dp))
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(shapes.radiusMd))
                    .clickable(onClick = onCopyClick)
                    .padding(vertical = SETTINGS_FAMILY_COPY_PV.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = neutral.white
            )
            Spacer(modifier = Modifier.width(SETTINGS_FAMILY_COPY_ICON_PS.dp))
            Text(
                text = "Copy",
                style = typography.bodyBold14,
                color = neutral.white,
            )
        }
    }
}

//@Preview
//@Composable
//private fun SettingsFamilyIdHeroCardPreview() {
//    KeuTrackTheme {
//        SettingsFamilyIdHeroCard(
//            familyIdCode = "KEU-992-KRT",
//            onCopyClick = {}
//        )
//    }
//}
