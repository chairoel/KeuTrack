package com.mascill.keutrack.feature.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.dashboard.presentation.model.DefaultDashboardMockContent
import com.mascill.keutrack.feature.dashboard.presentation.model.TransactionRowUi

private const val DASH_TXN_SECTION_ROW_SPACING = 10

@Composable
fun RecentTransactionsSection(
    transactions: List<TransactionRowUi>,
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
                text = "Recent Transactions",
                style = typography.headingBold20,
                color = textColors.title,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "View All",
                    style = typography.bodyBold14,
                    color = textColors.link,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(DASH_TXN_SECTION_ROW_SPACING.dp)) {
            transactions.forEach { row ->
                TransactionRowCard(row = row)
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
private fun RecentTransactionsSectionPreview() {
    KeuTrackTheme(darkTheme = false) {
        RecentTransactionsSection(
            transactions = DefaultDashboardMockContent.transactions,
            onViewAllClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
private fun RecentTransactionsSectionDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        RecentTransactionsSection(
            transactions = DefaultDashboardMockContent.transactions,
            onViewAllClick = {}
        )
    }
}
