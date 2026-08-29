package com.mascill.keutrack.feature.transaction.presentation.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme

private const val TOTALS_ROW_SPACING = 12
private const val TOTALS_BORDER_ALPHA = 0.45f
private const val TOTALS_ICON_BG_ALPHA = 0.28f
private const val TOTALS_CARD_PH = 12
private const val TOTALS_CARD_PV = 12
private const val TOTALS_BORDER_WIDTH = 1
private const val TOTALS_ICON_BOX = 28
private const val TOTALS_ICON_SIZE = 16
private const val TOTALS_HEADER_GAP = 8
private const val TOTALS_VALUE_PT = 8
private const val TOTALS_CARD_WEIGHT = 1f
private const val TOTALS_CAPTION_PT = 8
private const val LABEL_INCOME = "PEMASUKAN"
private const val LABEL_EXPENSE = "PENGELUARAN"

@Composable
fun HistoryPeriodTotalsRow(
    incomeTotal: Long,
    expenseTotal: Long,
    caption: String?,
    modifier: Modifier = Modifier,
) {
    val success = KeuTrackTheme.successColors
    val danger = KeuTrackTheme.dangerColors
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(TOTALS_ROW_SPACING.dp),
        ) {
            HistoryStatMiniCard(
                modifier = Modifier
                    .weight(TOTALS_CARD_WEIGHT)
                    .fillMaxHeight(),
                containerColor = success.s100,
                borderColor = success.s300.copy(alpha = TOTALS_BORDER_ALPHA),
                iconContainerColor = success.s300.copy(alpha = TOTALS_ICON_BG_ALPHA),
                iconTint = success.s700,
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = LABEL_INCOME,
                value = CurrencyFormat.formatIdr(incomeTotal),
                labelColor = success.s700,
                valueColor = success.s900,
                labelStyle = typography.bodyBold10,
                valueStyle = typography.bodyBold16,
            )
            HistoryStatMiniCard(
                modifier = Modifier
                    .weight(TOTALS_CARD_WEIGHT)
                    .fillMaxHeight(),
                containerColor = danger.d100,
                borderColor = danger.d300.copy(alpha = TOTALS_BORDER_ALPHA),
                iconContainerColor = danger.d300.copy(alpha = TOTALS_ICON_BG_ALPHA),
                iconTint = danger.d700,
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                label = LABEL_EXPENSE,
                value = CurrencyFormat.formatIdr(expenseTotal),
                labelColor = danger.d700,
                valueColor = danger.d900,
                labelStyle = typography.bodyBold10,
                valueStyle = typography.bodyBold16,
            )
        }
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = typography.bodyRegular12,
                color = textColors.body,
                modifier = Modifier.padding(top = TOTALS_CAPTION_PT.dp),
            )
        }
    }
}

@Composable
private fun HistoryStatMiniCard(
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(containerColor)
            .border(width = TOTALS_BORDER_WIDTH.dp, color = borderColor, shape = cardShape)
            .padding(horizontal = TOTALS_CARD_PH.dp, vertical = TOTALS_CARD_PV.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TOTALS_HEADER_GAP.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(TOTALS_ICON_BOX.dp)
                    .clip(RoundedCornerShape(shapes.radiusMd))
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(TOTALS_ICON_SIZE.dp),
                )
            }
            Text(
                text = label,
                style = labelStyle,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(TOTALS_CARD_WEIGHT, fill = false),
            )
        }
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.padding(top = TOTALS_VALUE_PT.dp),
        )
    }
}

@Preview(showBackground = true, name = "History totals — Light")
@Composable
private fun HistoryPeriodTotalsRowPreview() {
    KeuTrackTheme(darkTheme = false) {
        HistoryPeriodTotalsRow(
            incomeTotal = 8_200_000L,
            expenseTotal = 3_500_000L,
            caption = "Semua",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(
    name = "History totals — Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HistoryPeriodTotalsRowDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        HistoryPeriodTotalsRow(
            incomeTotal = 8_200_000L,
            expenseTotal = 3_500_000L,
            caption = "7 hari",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(showBackground = true, name = "History totals — Empty zeros")
@Composable
private fun HistoryPeriodTotalsRowEmptyPreview() {
    KeuTrackTheme {
        HistoryPeriodTotalsRow(
            incomeTotal = 0L,
            expenseTotal = 0L,
            caption = "Semua",
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Preview(
    showBackground = true,
    name = "History totals — Narrow large amount",
    widthDp = 320,
)
@Composable
private fun HistoryPeriodTotalsRowNarrowPreview() {
    KeuTrackTheme {
        HistoryPeriodTotalsRow(
            incomeTotal = 13_000_000L,
            expenseTotal = 520_000L,
            caption = "Semua",
            modifier = Modifier.padding(20.dp),
        )
    }
}
