package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.dashboard.presentation.model.DefaultDashboardMockContent
import com.mascill.keutrack.feature.dashboard.presentation.model.TransactionRowUi
import com.mascill.keutrack.feature.dashboard.presentation.model.toImageVector

private const val DASH_TXN_ROW_PH = 14
private const val DASH_TXN_ROW_PV = 12
private const val DASH_TXN_ICON_BOX = 44
private const val DASH_TXN_ICON_SIZE = 22
private const val DASH_TXN_MIDDLE_PH = 12
private const val DASH_TXN_SUBTITLE_PT = 2
private const val DASH_TXN_WALLET_CHIP_PT = 4
private const val DASH_TXN_CHIP_PH = 8
private const val DASH_TXN_CHIP_PV = 2
private const val DASH_TXN_CHIP_BG_ALPHA = 0.7f

@Composable
fun TransactionRowCard(
    row: TransactionRowUi,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val success = KeuTrackTheme.successColors
    val danger = KeuTrackTheme.dangerColors

    val amountColor = if (row.isExpense) danger.d500 else success.s500
    val amountPrefix = if (row.isExpense) "- " else "+ "

    KeuTrackCard(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                horizontal = DASH_TXN_ROW_PH.dp,
                vertical = DASH_TXN_ROW_PV.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(DASH_TXN_ICON_BOX.dp)
                        .clip(RoundedCornerShape(shapes.radiusMd))
                        .background(semantic.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = row.categoryIcon.toImageVector(),
                    contentDescription = null,
                    tint = semantic.primary,
                    modifier = Modifier.size(DASH_TXN_ICON_SIZE.dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = DASH_TXN_MIDDLE_PH.dp),
            ) {
                Text(
                    text = row.title,
                    style = typography.bodyBold16,
                    color = textColors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${row.categoryLabel} • ${row.timeLabel}",
                    style = typography.bodyRegular12,
                    color = textColors.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = DASH_TXN_SUBTITLE_PT.dp),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${row.amountLabel}",
                    style = typography.bodyBold14,
                    color = amountColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
                Text(
                    text = row.walletLabel,
                    style = typography.bodyRegular10,
                    color = semantic.onSurfaceVariant,
                    modifier =
                        Modifier
                            .padding(top = DASH_TXN_WALLET_CHIP_PT.dp)
                            .clip(RoundedCornerShape(shapes.radiusMd))
                            .background(
                                semantic.surfaceContainerHighest.copy(
                                    alpha = DASH_TXN_CHIP_BG_ALPHA,
                                ),
                            )
                            .padding(
                                horizontal = DASH_TXN_CHIP_PH.dp,
                                vertical = DASH_TXN_CHIP_PV.dp,
                            ),
                )
            }
        }
    }
}

//@Preview(name = "Transaction row")
//@Composable
//private fun TransactionRowCardPreview() {
//    KeuTrackTheme(darkTheme = false) {
//        TransactionRowCard(row = DefaultDashboardMockContent.transactions.first())
//    }
//}
