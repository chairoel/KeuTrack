package com.mascill.keutrack.feature.dashboard.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.keutrack.core.designsystem.component.KeuTrackFab
import com.mascill.keutrack.core.designsystem.component.snackbar.KeuTrackInlineSnackbar
import com.mascill.keutrack.core.designsystem.format.CurrencyFormat
import com.mascill.keutrack.core.designsystem.theme.KeuTrackTheme
import com.mascill.keutrack.feature.dashboard.presentation.components.DashboardStatCardsRow
import com.mascill.keutrack.feature.dashboard.presentation.components.DashboardTopBar
import com.mascill.keutrack.feature.dashboard.presentation.components.RecentTransactionsSection
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryCard
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryCardKind
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryFamilySharedFooter
import com.mascill.keutrack.feature.dashboard.presentation.components.WalletSummaryPersonalMonthChangeFooter
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUIState
import com.mascill.keutrack.feature.dashboard.presentation.model.DefaultDashboardMockContent
import com.mascill.keutrack.feature.dashboard.presentation.model.toPreviewUiState

private const val DASH_FAB_LIST_CLEARANCE = 72
private const val DASH_TOP_BAR_ELEVATION = 4
private const val DASH_TOP_BAR_PH = 20
private const val DASH_TOP_BAR_PV = 4
private const val DASH_CONTENT_PH = 20
private const val DASH_CONTENT_PT = 8
private const val DASH_CONTENT_PB_EXTRA = 24
private const val DASH_LIST_SECTION_SPACING = 16
private const val DASH_GREETING_TITLE_PT = 4
private const val DASH_BALANCE_LABEL_PERSONAL = "Current Balance"
private const val DASH_BALANCE_LABEL_FAMILY = "Available Shared"
private const val DASH_INCOME_LABEL = "INCOME"
private const val DASH_EXPENSE_LABEL = "EXPENSE"
private const val DASH_ERROR_DISMISS = "Dismiss"

/**
 * Dashboard routing — collects ViewModel state and wires navigation callbacks.
 */
@Composable
fun DashboardRouting(
    onSettingsClick: () -> Unit = {},
    onAddTransaction: () -> Unit = {},
    onViewAllTransactions: () -> Unit = {},
    onViewAllPersonalHistory: () -> Unit = {},
    onViewAllFamilyHistory: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenRendered()
    }

    DashboardScreen(
        uiState = uiState,
        onSettingsClick = onSettingsClick,
        onViewAllTransactions = onViewAllTransactions,
        onViewAllPersonalHistory = onViewAllPersonalHistory,
        onViewAllFamilyHistory = onViewAllFamilyHistory,
        onTogglePersonalBalanceVisibility = viewModel::onTogglePersonalBalanceVisibility,
        onToggleFamilyBalanceVisibility = viewModel::onToggleFamilyBalanceVisibility,
        onFabClick = onAddTransaction,
        onDismissError = viewModel::dismissError,
    )
}

/**
 * Dashboard screen — wallet summary, income/expense overview, and recent transactions.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUIState,
    onSettingsClick: () -> Unit,
    onViewAllTransactions: () -> Unit,
    onViewAllPersonalHistory: () -> Unit = {},
    onViewAllFamilyHistory: () -> Unit = {},
    onTogglePersonalBalanceVisibility: () -> Unit = {},
    onToggleFamilyBalanceVisibility: () -> Unit = {},
    onFabClick: () -> Unit,
    onDismissError: () -> Unit = {},
) {
    val pageBg = KeuTrackTheme.contentColors.pageColor
    val semantic = KeuTrackTheme.semanticColors

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
                                .padding(
                                    horizontal = DASH_TOP_BAR_PH.dp,
                                    vertical = DASH_TOP_BAR_PV.dp,
                                ),
                    ) {
                        DashboardTopBar(
                            avatar = uiState.avatarUrl,
                            onSettingsClick = onSettingsClick,
                        )
                    }
                }
            },
            floatingActionButton = {
                KeuTrackFab(
                    onClick = onFabClick,
                    contentDescription = "Add transaction",
                )
            },
        ) { innerPadding ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = semantic.primary)
                    }
                }

                else -> {
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
                                    text = "Welcome back, ${uiState.userFirstName}",
                                    style = KeuTrackTheme.typography.bodyRegular14,
                                    color = KeuTrackTheme.textColors.body,
                                )
                                Text(
                                    text = uiState.pageTitle,
                                    style = KeuTrackTheme.typography.headingBold24,
                                    color = KeuTrackTheme.textColors.title,
                                    modifier = Modifier.padding(top = DASH_GREETING_TITLE_PT.dp),
                                )
                            }
                        }

                        item {
                            WalletSummaryCard(
                                kind = WalletSummaryCardKind.Personal,
                                balanceLabel = DASH_BALANCE_LABEL_PERSONAL,
                                balanceAmount = CurrencyFormat.formatIdr(uiState.personalBalance),
                                isBalanceVisible = uiState.isPersonalBalanceVisible,
                                onToggleBalanceVisibility = onTogglePersonalBalanceVisibility,
                                onHistoryClick = onViewAllPersonalHistory,
                            ) {
                                uiState.monthChangeLabel?.let { label ->
                                    WalletSummaryPersonalMonthChangeFooter(label)
                                }
                            }
                        }

                        item {
                            WalletSummaryCard(
                                kind = WalletSummaryCardKind.Family,
                                balanceLabel = DASH_BALANCE_LABEL_FAMILY,
                                balanceAmount = CurrencyFormat.formatIdr(uiState.familyBalance),
                                isBalanceVisible = uiState.isFamilyBalanceVisible,
                                onToggleBalanceVisibility = onToggleFamilyBalanceVisibility,
                                onHistoryClick = onViewAllFamilyHistory,
                            ) {
                                WalletSummaryFamilySharedFooter(
                                    sharedSummary = uiState.familySharedSummary,
                                    memberInitials = uiState.familyMemberInitials,
                                )
                            }
                        }

                        item {
                            DashboardStatCardsRow(
                                incomeLabel = DASH_INCOME_LABEL,
                                incomeAmount = CurrencyFormat.formatIdr(uiState.incomeTotal),
                                expenseLabel = DASH_EXPENSE_LABEL,
                                expenseAmount = CurrencyFormat.formatIdr(uiState.expenseTotal),
                            )
                        }

                        item {
                            RecentTransactionsSection(
                                transactions = uiState.recentTransactions,
                                onViewAllClick = onViewAllTransactions,
                            )
                        }
                    }
                }
            }
        }

        uiState.errorMessage?.let { message ->
            KeuTrackInlineSnackbar(
                message = message,
                onDismiss = onDismissError,
                actionLabel = DASH_ERROR_DISMISS,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    KeuTrackTheme(darkTheme = false) {
        DashboardScreen(
            uiState = DefaultDashboardMockContent.toPreviewUiState(),
            onSettingsClick = { },
            onViewAllTransactions = { },
            onFabClick = { },
        )
    }
}

@Preview(
    name = "Dashboard — Dark mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun DashboardScreenDarkPreview() {
    KeuTrackTheme(darkTheme = true) {
        DashboardScreen(
            uiState = DefaultDashboardMockContent.toPreviewUiState(),
            onSettingsClick = { },
            onViewAllTransactions = { },
            onFabClick = { },
        )
    }
}
