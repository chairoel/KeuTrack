package com.mascill.keutrack.feature.transaction.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryScope
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    userRepository: UserRepository,
    private val getTransactions: GetTransactionsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val scope = readHistoryScope(savedStateHandle)

    private val transactionsFlow =
        when (scope) {
            HistoryScope.Family -> {
                userRepository.getCurrentUser().flatMapLatest { user ->
                    val familyId = user?.familyId
                    if (familyId.isNullOrBlank()) {
                        flowOf(emptyList())
                    } else {
                        getTransactions(
                            GetTransactionsUseCase.Params(
                                familyId = familyId,
                                limit = FAMILY_HISTORY_LIMIT,
                            ),
                        )
                    }
                }
            }

            HistoryScope.Personal -> {
                getWalletSummary().flatMapLatest { summary ->
                    val walletId = summary.personalWallet?.id
                    if (walletId.isNullOrBlank()) {
                        flowOf(emptyList())
                    } else {
                        getTransactions(
                            GetTransactionsUseCase.Params(
                                walletId = walletId,
                                limit = HISTORY_LIMIT,
                            ),
                        )
                    }
                }
            }

            HistoryScope.All -> {
                getTransactions(GetTransactionsUseCase.Params(limit = HISTORY_LIMIT))
            }
        }

    val uiState: StateFlow<HistoryUIState> =
        combine(
            transactionsFlow,
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
                scope = scope,
            )
        }.catch { e ->
            emit(
                HistoryUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                    scope = scope,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUIState(scope = scope),
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
        const val ARG_FAMILY_ONLY = "familyOnly"
        const val ARG_PERSONAL_ONLY = "personalOnly"
        const val HISTORY_LIMIT = 50
        const val FAMILY_HISTORY_LIMIT = 200
        const val ERR_LOAD_FAILED = "Gagal memuat riwayat transaksi"

        fun readHistoryScope(savedStateHandle: SavedStateHandle): HistoryScope =
            when {
                readFlag(savedStateHandle, ARG_FAMILY_ONLY) -> HistoryScope.Family
                readFlag(savedStateHandle, ARG_PERSONAL_ONLY) -> HistoryScope.Personal
                else -> HistoryScope.All
            }

        fun readFlag(savedStateHandle: SavedStateHandle, key: String): Boolean =
            when (val value = savedStateHandle.get<Any>(key)) {
                is Boolean -> value
                is String -> value.toBoolean()
                else -> false
            }
    }
}
