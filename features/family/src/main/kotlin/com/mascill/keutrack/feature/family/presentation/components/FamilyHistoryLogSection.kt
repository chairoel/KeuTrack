package com.mascill.keutrack.feature.family.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.family.presentation.model.FamilyInsightsMockContent

private const val FAM_HISTORY_ROW_SPACING = 12
private const val FAM_HISTORY_VIEW_ALL = "View All"

@Composable
fun FamilyHistoryLogSection(
    content: FamilyInsightsMockContent,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = KeuTrackTheme.typography
    val textColors = KeuTrackTheme.textColors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = content.historySectionTitle,
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
        Column(verticalArrangement = Arrangement.spacedBy(FAM_HISTORY_ROW_SPACING.dp)) {
            content.historyRows.forEach { row ->
                FamilyHistoryRow(row = row)
            }
        }
    }
}
