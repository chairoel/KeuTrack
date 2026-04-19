package com.mascill.keutrack.feature.dashboard.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mascill.keutrack.core.designsystem.component.KeuTrackModalBottomSheet
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.dashboard.presentation.components.DashboardStatCardsRow
import com.mascill.keutrack.feature.dashboard.presentation.components.DashboardTopBar
import com.mascill.keutrack.feature.dashboard.presentation.components.NewEntryBottomSheetContent
import com.mascill.keutrack.feature.dashboard.presentation.components.RecentTransactionsSection
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryCard
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryCardKind
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryFamilySharedFooter
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryPersonalMonthChangeFooter
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardMockContent
import com.mascill.keutrack.feature.dashboard.presentation.model.DefaultDashboardMockContent

private const val DASH_FAB_LIST_CLEARANCE = 72
private const val DASH_TOP_BAR_ELEVATION = 4
private const val DASH_TOP_BAR_PH = 20
private const val DASH_TOP_BAR_PV = 4
private const val DASH_CONTENT_PH = 20
private const val DASH_CONTENT_PT = 8
private const val DASH_CONTENT_PB_EXTRA = 24
private const val DASH_LIST_SECTION_SPACING = 16
private const val DASH_GREETING_TITLE_PT = 4

/**
 * Dashboard routing to handle screen that will be showing and to handle view model flow /
 * live data collection
 */
@Composable
fun DashboardRouting(
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        // Reserved for binding DashboardUIState from viewModel.
    }
    var showNewEntrySheet by remember { mutableStateOf(false) }
    DashboardScreen(
        content = DefaultDashboardMockContent,
        onSettingsClick = { },
        onViewAllTransactions = { },
        onFabClick = { showNewEntrySheet = true },
        showNewEntrySheet = showNewEntrySheet,
        onNewEntrySheetDismiss = { showNewEntrySheet = false },
    )
}

/**
 * Dashboard screen — wallet summary, income/expense overview, and recent transactions.
 */
@Composable
private fun DashboardScreen(
    content: DashboardMockContent,
    onSettingsClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onFabClick: () -> Unit,
    showNewEntrySheet: Boolean,
    onNewEntrySheetDismiss: () -> Unit,
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors
    val neutral = KeuTrackTheme.neutralColors

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = pageBg,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = pageBg,
                elevation = DASH_TOP_BAR_ELEVATION.dp,
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = DASH_TOP_BAR_PH.dp, vertical = DASH_TOP_BAR_PV.dp),
                ) {
                    DashboardTopBar(onSettingsClick = onSettingsClick)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onFabClick,
                shape = RoundedCornerShape(KeuTrackTheme.shapeTokens.radiusLg),
                backgroundColor = semantic.primary,
                contentColor = neutral.white,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add transaction",
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = DASH_CONTENT_PH.dp,
                    end = DASH_CONTENT_PH.dp,
                    top = DASH_CONTENT_PT.dp,
                    bottom = DASH_CONTENT_PB_EXTRA.dp + DASH_FAB_LIST_CLEARANCE.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(DASH_LIST_SECTION_SPACING.dp),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Welcome back, ${content.userFirstName}",
                        style = KeuTrackTheme.typography.bodyRegular14,
                        color = KeuTrackTheme.textColors.body,
                    )
                    Text(
                        text = content.pageTitle,
                        style = KeuTrackTheme.typography.headingBold24,
                        color = KeuTrackTheme.textColors.title,
                        modifier = Modifier.padding(top = DASH_GREETING_TITLE_PT.dp),
                    )
                }
            }

            item {
                WalletSummaryCard(
                    kind = WalletSummaryCardKind.Personal,
                    balanceLabel = content.personalBalanceLabel,
                    balanceAmount = content.personalBalanceAmount,
                ) {
                    WalletSummaryPersonalMonthChangeFooter(content.personalMonthChangeLabel)
                }
            }

            item {
                WalletSummaryCard(
                    kind = WalletSummaryCardKind.Family,
                    balanceLabel = content.familyBalanceLabel,
                    balanceAmount = content.familyBalanceAmount,
                ) {
                    WalletSummaryFamilySharedFooter(content.familySharedSummary)
                }
            }

            item {
                DashboardStatCardsRow(
                    incomeLabel = content.incomeLabel,
                    incomeAmount = content.incomeAmount,
                    expenseLabel = content.expenseLabel,
                    expenseAmount = content.expenseAmount,
                )
            }

            item {
                RecentTransactionsSection(
                    transactions = content.transactions,
                    onViewAllClick = onViewAllTransactions,
                )
            }
        }
    }

        if (showNewEntrySheet) {
            KeuTrackModalBottomSheet(onDismissRequest = onNewEntrySheetDismiss) {
                NewEntryBottomSheetContent(onDismiss = onNewEntrySheetDismiss)
            }
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//private fun DashboardScreenPreview() {
//    KeuTrackTheme(darkTheme = false) {
//        DashboardScreen(
//            content = DefaultDashboardMockContent,
//            onSettingsClick = { },
//            onViewAllTransactions = { },
//            onFabClick = { },
//        )
//    }
//}
//
//@Preview(
//    name = "Dashboard — Dark mode",
//    showBackground = true,
//    uiMode = Configuration.UI_MODE_NIGHT_YES,
//)
//@Composable
//private fun DashboardScreenDarkPreview() {
//    KeuTrackTheme(darkTheme = true) {
//        DashboardScreen(
//            content = DefaultDashboardMockContent,
//            onSettingsClick = { },
//            onViewAllTransactions = { },
//            onFabClick = { },
//        )
//    }
//}