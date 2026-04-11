package com.mascill.keutrack.feature.dashboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mascill.keutrack.core.designsystem.component.KeuTrackCard
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import android.content.res.Configuration.UI_MODE_NIGHT_YES

/**
 * Home routing to handle screen that will be showing and to handle view model flow /
 * live data collection
 */
@Composable
fun DashboardRouting(
    viewModel: DashboardViewModel = hiltViewModel(),
) {

    DashboardScreen()
}

/**
 * Dashboard screen — the main tab that shows wallet summary,
 * income/expense overview, and recent transactions.
 */
@Composable
private fun DashboardScreen() {

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KeuTrackTheme.contentColors.pageColor)
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                KeuTrackCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Track your finances easily",
                        style = KeuTrackTheme.typography.bodyRegular16,
                        color = KeuTrackTheme.textColors.body
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    KeuTrackTheme {
        DashboardScreen()
    }
}

@Preview(
    name = "Dashboard — Dark mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun DashboardScreenDarkPreview() {
    KeuTrackTheme {
        DashboardScreen()
    }
}