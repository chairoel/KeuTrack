package com.mascill.keutrack.feature.transaction.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionCategoryIcon
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionRowUi
import com.mascill.keutrack.feature.transaction.presentation.model.toImageVector

private const val TXN_ROW_PH = 14
private const val TXN_ROW_PV = 12
private const val TXN_ICON_BOX = 44
private const val TXN_ICON_SIZE = 22
private const val TXN_MIDDLE_PH = 12
private const val TXN_SUBTITLE_PT = 2
private const val TXN_WALLET_CHIP_PT = 4
private const val TXN_CHIP_PH = 8
private const val TXN_CHIP_PV = 2
private const val TXN_CHIP_BG_ALPHA = 0.7f
private const val TXN_EXPENSE_PREFIX = "- "
private const val TXN_INCOME_PREFIX = "+ "
private const val TXN_SUBTITLE_SEPARATOR = " • "
private const val TXN_SYNC_BADGE = 16
private const val TXN_SYNC_ICON = 11
private const val TXN_LOCAL_CD = "Belum tersinkron ke cloud"
private const val TXN_SYNC_FAILED_CD = "Gagal sinkron ke cloud"

@Composable
fun TransactionHistoryRow(
    row: TransactionRowUi,
    modifier: Modifier = Modifier,
) {
    val semantic = KeuTrackTheme.semanticColors
    val shapes = KeuTrackTheme.shapeTokens
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val success = KeuTrackTheme.successColors
    val danger = KeuTrackTheme.dangerColors
    val warning = KeuTrackTheme.warningColors

    val amountColor = if (row.isExpense) danger.d500 else success.s500
    val amountPrefix = if (row.isExpense) TXN_EXPENSE_PREFIX else TXN_INCOME_PREFIX
    val syncVisual =
        row.syncStatus.toSyncVisual(
            pendingColor = warning.w500,
            failedColor = danger.d500,
        )

    KeuTrackCard(
        modifier = modifier,
        contentPadding =
            PaddingValues(
                horizontal = TXN_ROW_PH.dp,
                vertical = TXN_ROW_PV.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TXN_ICON_BOX.dp)
                        .clip(RoundedCornerShape(shapes.radiusMd))
                        .background(semantic.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = row.categoryIcon.toImageVector(),
                    contentDescription = null,
                    tint = semantic.primary,
                    modifier = Modifier.size(TXN_ICON_SIZE.dp),
                )
                if (syncVisual != null) {
                    TransactionSyncBadge(
                        visual = syncVisual,
                        surfaceColor = semantic.surface,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = TXN_MIDDLE_PH.dp),
            ) {
                Text(
                    text = row.title,
                    style = typography.bodyBold16,
                    color = textColors.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = row.categoryLabel + TXN_SUBTITLE_SEPARATOR + row.timeLabel,
                    style = typography.bodyRegular12,
                    color = textColors.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = TXN_SUBTITLE_PT.dp),
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
                            .padding(top = TXN_WALLET_CHIP_PT.dp)
                            .clip(RoundedCornerShape(shapes.radiusMd))
                            .background(
                                semantic.surfaceContainerHighest.copy(
                                    alpha = TXN_CHIP_BG_ALPHA,
                                ),
                            )
                            .padding(
                                horizontal = TXN_CHIP_PH.dp,
                                vertical = TXN_CHIP_PV.dp,
                            ),
                )
            }
        }
    }
}

private data class TransactionSyncVisual(
    val icon: ImageVector,
    val contentDescription: String,
    val iconTint: Color,
)

private fun SyncStatus.toSyncVisual(
    pendingColor: Color,
    failedColor: Color,
): TransactionSyncVisual? =
    when (this) {
        SyncStatus.PENDING ->
            TransactionSyncVisual(
                icon = Icons.Filled.CloudOff,
                contentDescription = TXN_LOCAL_CD,
                iconTint = pendingColor,
            )
        SyncStatus.FAILED ->
            TransactionSyncVisual(
                icon = Icons.Filled.SyncProblem,
                contentDescription = TXN_SYNC_FAILED_CD,
                iconTint = failedColor,
            )
        SyncStatus.SYNCED -> null
    }

@Composable
private fun TransactionSyncBadge(
    visual: TransactionSyncVisual,
    surfaceColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(TXN_SYNC_BADGE.dp)
                .clip(CircleShape)
                .background(surfaceColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = visual.contentDescription,
            tint = visual.iconTint,
            modifier = Modifier.size(TXN_SYNC_ICON.dp),
        )
    }
}

@Preview(name = "History row")
@Composable
private fun TransactionHistoryRowPreview() {
    KeuTrackTheme(darkTheme = false) {
        TransactionHistoryRow(
            row =
                TransactionRowUi(
                    id = "1",
                    title = "Bakmi GM Restaurant",
                    categoryLabel = "Food & Drinks",
                    timeLabel = "12:45 PM",
                    amountLabel = "IDR 125.000",
                    isExpense = true,
                    walletLabel = "Personal",
                    categoryIcon = TransactionCategoryIcon.Restaurant,
                    syncStatus = SyncStatus.PENDING,
                ),
        )
    }
}
