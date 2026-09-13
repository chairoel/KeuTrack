package com.mascill.keutrack.feature.transaction.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun NewEntryRouting(
    onBack: () -> Unit,
    viewModel: NewEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.navigateBack) {
        if (uiState.navigateBack) {
            viewModel.onNavigateBackConsumed()
            onBack()
        }
    }

    NewEntryScreen(
        uiState = uiState,
        onBack = onBack,
        onKindChanged = viewModel::onKindChanged,
        onDigit = viewModel::onDigit,
        onTripleZero = viewModel::onTripleZero,
        onBackspace = viewModel::onBackspace,
        onCategorySelected = viewModel::onCategorySelected,
        onWalletSelected = viewModel::onWalletSelected,
        onDateSelected = viewModel::onDateSelected,
        onNoteChanged = viewModel::onNoteChanged,
        onSave = viewModel::onSave,
        onClearError = viewModel::clearError,
    )
}
