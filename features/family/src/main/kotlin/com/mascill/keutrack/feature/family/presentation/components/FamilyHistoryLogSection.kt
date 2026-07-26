package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilyHistoryRowUi

private const val FAM_HISTORY_ROW_SPACING = 12
private const val FAM_HISTORY_VIEW_ALL = "View All"
private const val FAM_HISTORY_SECTION_TITLE = "Family History Log"
private const val FAM_HISTORY_EMPTY =
    "Belum ada transaksi di dompet keluarga. Gunakan FAB untuk menambah transaksi bersama."

@Composable
fun FamilyHistoryLogSection(
    historyRows: List<FamilyHistoryRowUi>,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors
    val semantic = KeuTrackTheme.semanticColors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = FAM_HISTORY_SECTION_TITLE,
                style = typography.headingBold20,
                color = textColors.title,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = FAM_HISTORY_VIEW_ALL,
                    style = typography.bodyBold14,
                    color = textColors.link,
                )
            }
        }
        if (historyRows.isEmpty()) {
            Text(
                text = FAM_HISTORY_EMPTY,
                style = typography.bodyRegular14,
                color = semantic.onSurfaceVariant,
                modifier = Modifier.padding(top = FAM_HISTORY_ROW_SPACING.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(FAM_HISTORY_ROW_SPACING.dp)) {
                historyRows.forEach { row ->
                    FamilyHistoryRow(row = row)
                }
            }
        }
    }
}
