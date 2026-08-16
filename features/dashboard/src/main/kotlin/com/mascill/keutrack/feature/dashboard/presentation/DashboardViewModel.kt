package com.mascill.keutrack.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetMonthlySummaryUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUIState
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class DashboardViewModel @Inject constructor(
    userRepository: UserRepository,
    familyRepository: FamilyRepository,
    getWalletSummary: GetWalletSummaryUseCase,
    getTransactions: GetTransactionsUseCase,
    getMonthlySummary: GetMonthlySummaryUseCase,
    getCategories: GetCategoriesUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val priorMonth = currentMonth.minusMonths(1)
    private val currentMonthKey = currentMonth.toString()
    private val priorMonthKey = priorMonth.toString()

    val uiState: StateFlow<DashboardUIState> =
        combine(
            combine(
                userRepository.getCurrentUser(),
                familyRepository.observeCurrentFamily(),
                ::Pair,
            ),
            getWalletSummary(),
            getTransactions(GetTransactionsUseCase.Params(limit = RECENT_TX_LIMIT)),
            getMonthlySummary(
                currentMonth = currentMonthKey,
                trendMonths = listOf(priorMonthKey),
            ),
            getCategories(),
        ) { userAndFamily, walletSummary, transactions, monthlySummary, categories ->
            val (user, family) = userAndFamily
            val categoriesById = categories.associateBy { it.id }
            val walletTypes = DashboardUiMapper.mapWalletTypes(walletSummary)
            val prior =
                DashboardUiMapper.priorFromTrend(monthlySummary, priorMonthKey)

            DashboardUIState(
                isLoading = false,
                errorMessage = null,
                userFirstName = DashboardUiMapper.greetingFirstName(user),
                avatarUrl = user?.photoUrl,
                personalBalance = walletSummary.totalPersonalBalance,
                familyBalance = walletSummary.totalFamilyBalance,
                familyMemberInitials = DashboardUiMapper.familyMemberInitials(user, family),
                familySharedSummary =
                    DashboardUiMapper.familySharedSummary(walletSummary.familyWallets.size),
                monthChangeLabel =
                    DashboardUiMapper.monthChangeLabel(
                        current = monthlySummary.currentMonth,
                        prior = prior,
                    ),
                incomeTotal = monthlySummary.currentMonth?.totalIncome ?: 0L,
                expenseTotal = monthlySummary.currentMonth?.totalExpense ?: 0L,
                recentTransactions =
                    DashboardUiMapper.toTransactionRows(
                        transactions = transactions,
                        categoriesById = categoriesById,
                        walletsById = walletTypes,
                    ),
            )
        }.catch { e ->
            emit(
                DashboardUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUIState(),
        )

    /**
     * Called when the dashboard becomes visible. Retries sync only if Room still
     * has PENDING/FAILED items; otherwise no WorkManager work is enqueued.
     */
    fun onScreenRendered() {
        viewModelScope.launch(dispatcher.io) {
            try {
                retryPendingSync()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort; UI already shows local sync badges.
            }
        }
    }

    private companion object {
        const val RECENT_TX_LIMIT = 5
        const val ERR_LOAD_FAILED = "Gagal memuat dashboard"
    }
}
