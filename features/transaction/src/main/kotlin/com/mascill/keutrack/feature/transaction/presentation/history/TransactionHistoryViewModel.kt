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

    private val familyOnly = readFamilyOnly(savedStateHandle)

    private val transactionsFlow =
        if (familyOnly) {
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
        } else {
            getTransactions(GetTransactionsUseCase.Params(limit = HISTORY_LIMIT))
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
                isFamilyOnly = familyOnly,
            )
        }.catch { e ->
            emit(
                HistoryUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                    isFamilyOnly = familyOnly,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUIState(isFamilyOnly = familyOnly),
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
        const val HISTORY_LIMIT = 50
        const val FAMILY_HISTORY_LIMIT = 200
        const val ERR_LOAD_FAILED = "Gagal memuat riwayat transaksi"

        fun readFamilyOnly(savedStateHandle: SavedStateHandle): Boolean =
            when (val value = savedStateHandle.get<Any>(ARG_FAMILY_ONLY)) {
                is Boolean -> value
                is String -> value.toBoolean()
                else -> false
            }
    }
}
