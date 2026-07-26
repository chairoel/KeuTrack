package com.mascill.keutrack.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetBudgetProgressUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetMonthlySummaryUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.MonthlySummaryResult
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.family.presentation.model.FamilyUIState
import com.mascill.keutrack.feature.family.presentation.model.FamilyUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FamilyViewModel @Inject constructor(
    userRepository: UserRepository,
    getWalletSummary: GetWalletSummaryUseCase,
    getTransactions: GetTransactionsUseCase,
    getMonthlySummary: GetMonthlySummaryUseCase,
    getBudgetProgress: GetBudgetProgressUseCase,
    getCategories: GetCategoriesUseCase,
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val priorMonth = currentMonth.minusMonths(1)
    private val currentMonthKey = currentMonth.toString()
    private val priorMonthKey = priorMonth.toString()

    private val walletSummaryFlow = getWalletSummary()

    private val familyTransactionsFlow =
        walletSummaryFlow.flatMapLatest { summary ->
            val familyWalletId = summary.familyWallets.firstOrNull()?.id
            if (familyWalletId == null) {
                flowOf(emptyList())
            } else {
                getTransactions(
                    GetTransactionsUseCase.Params(
                        walletId = familyWalletId,
                        limit = RECENT_TX_LIMIT,
                    ),
                )
            }
        }

    val uiState: StateFlow<FamilyUIState> =
        combine(
            combine(
                userRepository.getCurrentUser(),
                walletSummaryFlow,
                familyTransactionsFlow,
                getMonthlySummary(
                    currentMonth = currentMonthKey,
                    trendMonths = listOf(priorMonthKey),
                ),
                getBudgetProgress(currentMonthKey),
            ) { user, walletSummary, transactions, monthlySummary, budgets ->
                FamilyLoadBundle(
                    user = user,
                    walletSummary = walletSummary,
                    transactions = transactions,
                    monthlySummary = monthlySummary,
                    budgets = budgets,
                )
            },
            getCategories(),
        ) { bundle, categories ->
            FamilyUiMapper.toUiState(
                user = bundle.user,
                walletSummary = bundle.walletSummary,
                transactions = bundle.transactions,
                monthlySummary = bundle.monthlySummary,
                budgets = bundle.budgets,
                categoriesById = categories.associateBy { it.id },
                priorPeriod = priorMonthKey,
            )
        }.catch { e ->
            emit(
                FamilyUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilyUIState(),
        )

    private data class FamilyLoadBundle(
        val user: User?,
        val walletSummary: WalletSummary,
        val transactions: List<Transaction>,
        val monthlySummary: MonthlySummaryResult,
        val budgets: List<Budget>,
    )

    private companion object {
        const val RECENT_TX_LIMIT = 5
        const val ERR_LOAD_FAILED = "Gagal memuat family insights"
    }
}
