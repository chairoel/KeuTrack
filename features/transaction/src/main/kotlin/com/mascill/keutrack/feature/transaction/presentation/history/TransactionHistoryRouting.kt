package com.mascill.keutrack.feature.transaction.presentation.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TransactionHistoryRouting(
    onBack: () -> Unit,
    onAddTransaction: () -> Unit,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenRendered()
    }

    TransactionHistoryScreen(
        uiState = uiState,
        onBack = onBack,
        onAddTransaction = onAddTransaction,
        onPeriodPresetSelected = viewModel::onPeriodPresetSelected,
        onCustomRangeConfirmed = viewModel::onCustomRangeConfirmed,
        onClearPeriodFilter = viewModel::onClearPeriodFilter,
    )
}
