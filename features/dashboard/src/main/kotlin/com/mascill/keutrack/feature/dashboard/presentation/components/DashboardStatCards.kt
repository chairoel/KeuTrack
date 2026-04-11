package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val DASH_STAT_ROW_SPACING = 12
private const val DASH_STAT_BORDER_ALPHA = 0.45f
private const val DASH_STAT_ICON_BG_ALPHA = 0.28f
private const val DASH_STAT_CARD_PH = 20
private const val DASH_STAT_CARD_PV = 16
private const val DASH_STAT_BORDER_WIDTH = 1
private const val DASH_STAT_ICON_BOX = 40
private const val DASH_STAT_ICON_SIZE = 22
private const val DASH_STAT_TEXT_LEADING = 12
private const val DASH_STAT_VALUE_PT = 4

@Composable
fun DashboardStatCardsRow(
    incomeLabel: String,
    incomeAmount: String,
    expenseLabel: String,
    expenseAmount: String,
    modifier: Modifier = Modifier,
) {
    val success = KeuTrackTheme.successColors
    val danger = KeuTrackTheme.dangerColors
    val typography = KeuTrackTheme.typography

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DASH_STAT_ROW_SPACING.dp),
    ) {
        StatMiniCard(
            modifier = Modifier.weight(1f),
            containerColor = success.s100,
            borderColor = success.s300.copy(alpha = DASH_STAT_BORDER_ALPHA),
            iconContainerColor = success.s300.copy(alpha = DASH_STAT_ICON_BG_ALPHA),
            iconTint = success.s700,
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            label = incomeLabel,
            value = incomeAmount,
            labelColor = success.s700,
            valueColor = success.s900,
            labelStyle = typography.bodyBold10,
            valueStyle = typography.headingBold20,
        )
        StatMiniCard(
            modifier = Modifier.weight(1f),
            containerColor = danger.d100,
            borderColor = danger.d300.copy(alpha = DASH_STAT_BORDER_ALPHA),
            iconContainerColor = danger.d300.copy(alpha = DASH_STAT_ICON_BG_ALPHA),
            iconTint = danger.d700,
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            label = expenseLabel,
            value = expenseAmount,
            labelColor = danger.d700,
            valueColor = danger.d900,
            labelStyle = typography.bodyBold10,
            valueStyle = typography.headingBold20,
        )
    }
}

@Composable
private fun StatMiniCard(
    containerColor: Color,
    borderColor: Color,
    iconContainerColor: Color,
    iconTint: Color,
    icon: ImageVector,
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    labelStyle: TextStyle,
    valueStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val shapes = KeuTrackTheme.shapeTokens
    val cardShape = RoundedCornerShape(shapes.radiusLg)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(containerColor)
                .border(width = DASH_STAT_BORDER_WIDTH.dp, color = borderColor, shape = cardShape)
                .padding(horizontal = DASH_STAT_CARD_PH.dp, vertical = DASH_STAT_CARD_PV.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(DASH_STAT_ICON_BOX.dp)
                    .clip(RoundedCornerShape(shapes.radiusMd))
                    .background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(DASH_STAT_ICON_SIZE.dp),
            )
        }

        Column(
            modifier =
                Modifier
                    .padding(start = DASH_STAT_TEXT_LEADING.dp)
                    .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                style = labelStyle,
                color = labelColor,
            )
            Text(
                text = value,
                style = valueStyle,
                color = valueColor,
                modifier = Modifier.padding(top = DASH_STAT_VALUE_PT.dp),
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//private fun DashboardStatCardsRowPreview() {
//    KeuTrackTheme(darkTheme = false) {
//        DashboardStatCardsRow(
//            incomeLabel = "INCOME",
//            incomeAmount = "IDR 8.2M",
//            expenseLabel = "EXPENSE",
//            expenseAmount = "IDR 3.5M",
//        )
//    }
//}

