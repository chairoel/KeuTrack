package com.mascill.keutrack.feature.transaction.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * @param transactionId Optional deep-link id. Form is create-only until edit UI exists.
 */
@Composable
fun NewEntryRouting(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") transactionId: String? = null,
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
