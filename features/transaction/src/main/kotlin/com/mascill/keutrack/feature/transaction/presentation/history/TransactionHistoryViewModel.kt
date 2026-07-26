package com.mascill.keutrack.feature.transaction.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    private val getTransactions: GetTransactionsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    val uiState: StateFlow<HistoryUIState> =
        combine(
            getTransactions(GetTransactionsUseCase.Params(limit = HISTORY_LIMIT)),
            getCategories(),
            getWalletSummary(),
        ) { transactions, categories, walletSummary ->
            val categoriesById = categories.associateBy { it.id }
            val walletsById = TransactionUiMapper.mapWallets(walletSummary)
            HistoryUIState(
                isLoading = false,
                items =
                    TransactionUiMapper.toTransactionRows(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        walletsById = walletsById,
                    ),
                errorMessage = null,
            )
        }.catch { e ->
            emit(
                HistoryUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUIState(),
        )

    fun onScreenRendered() {
        viewModelScope.launch(dispatcher.io) {
            try {
                retryPendingSync()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort; rows already show local sync badges.
            }
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 50
        const val ERR_LOAD_FAILED = "Gagal memuat riwayat transaksi"
    }
}
