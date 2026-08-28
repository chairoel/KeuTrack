package com.mascill.keutrack.feature.settings.presentation.components

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.common.utils.PeriodLabels
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val CARD_TITLE = "Siklus bulanan"
private const val CARD_ACTION = "Atur"
private const val CARD_ICON = 28
private const val CARD_TITLE_GAP = 16
private const val CARD_DESC_PT = 2

@Composable
fun SettingsPeriodCycleCard(
    cycleStartDay: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors
    val subtitle = PeriodLabels.cycleSubtitle(cycleStartDay)
    val description = "$CARD_TITLE, tanggal $cycleStartDay. $subtitle"

    KeuTrackCard(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics { contentDescription = description },
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = null,
                tint = semantic.primary,
                modifier = Modifier.size(CARD_ICON.dp),
            )
            Spacer(modifier = Modifier.width(CARD_TITLE_GAP.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = CARD_TITLE,
                    style = typography.bodyBold16,
                    color = textColors.title,
                )
                Text(
                    text = subtitle,
                    style = typography.bodyRegular14,
                    color = textColors.body,
                    modifier = Modifier.padding(top = CARD_DESC_PT.dp),
                )
            }
            Text(
                text = CARD_ACTION,
                style = typography.bodyBold14,
                color = semantic.primary,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = semantic.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Period cycle card — Light")
@Composable
private fun SettingsPeriodCycleCardLightPreview() {
    KeuTrackTheme(darkTheme = false) {
        SettingsPeriodCycleCard(cycleStartDay = 1, onClick = {})
    }
}

@Preview(
    showBackground = true,
    name = "Period cycle card — Dark",
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsPeriodCycleCardDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        SettingsPeriodCycleCard(cycleStartDay = 25, onClick = {})
    }
}
