package com.mascill.keutrack.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetMonthlySummaryUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.ObserveWalletUiPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.SetWalletBalanceVisibilityUseCase
import com.mascill.keutrack.core.domain.usecase.SyncFamilyDataUseCase
import com.mascill.keutrack.core.domain.usecase.SyncPersonalDataUseCase
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUIState
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
    observeWalletUiPreferences: ObserveWalletUiPreferencesUseCase,
    private val setWalletBalanceVisibility: SetWalletBalanceVisibilityUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val syncPersonalData: SyncPersonalDataUseCase,
    private val syncFamilyData: SyncFamilyDataUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val priorMonth = currentMonth.minusMonths(1)
    private val currentMonthKey = currentMonth.toString()
    private val priorMonthKey = priorMonth.toString()

    private val _syncError = MutableStateFlow<String?>(null)
    private val _isPersonalWalletSyncing = MutableStateFlow(false)
    private val _isFamilyWalletSyncing = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUIState> =
        combine(
            combine(
                userRepository.getCurrentUser(),
                familyRepository.observeCurrentFamily(),
                observeWalletUiPreferences(),
                _syncError,
                combine(
                    _isPersonalWalletSyncing,
                    _isFamilyWalletSyncing,
                ) { personal, family ->
                    WalletSyncFlags(personal, family)
                },
            ) { user, family, walletUiPreferences, syncError, walletSyncFlags ->
                DashboardSession(
                    user = user,
                    family = family,
                    walletUiPreferences = walletUiPreferences,
                    syncError = syncError,
                    isPersonalWalletSyncing = walletSyncFlags.isPersonalWalletSyncing,
                    isFamilyWalletSyncing = walletSyncFlags.isFamilyWalletSyncing,
                )
            },
            getWalletSummary(),
            getTransactions(GetTransactionsUseCase.Params(limit = RECENT_TX_LIMIT)),
            getMonthlySummary(
                currentMonth = currentMonthKey,
                trendMonths = listOf(priorMonthKey),
            ),
            getCategories(),
        ) { session, walletSummary, transactions, monthlySummary, categories ->
            val categoriesById = categories.associateBy { it.id }
            val walletTypes = DashboardUiMapper.mapWalletTypes(walletSummary)
            val prior =
                DashboardUiMapper.priorFromTrend(monthlySummary, priorMonthKey)

            DashboardUIState(
                isLoading = false,
                errorMessage = session.syncError,
                userFirstName = DashboardUiMapper.greetingFirstName(session.user),
                avatarUrl = session.user?.photoUrl,
                personalBalance = walletSummary.totalPersonalBalance,
                familyBalance = walletSummary.totalFamilyBalance,
                familyMemberInitials =
                    DashboardUiMapper.familyMemberInitials(session.user, session.family),
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
                isPersonalBalanceVisible = session.walletUiPreferences.isPersonalBalanceVisible,
                isFamilyBalanceVisible = session.walletUiPreferences.isFamilyBalanceVisible,
                isPersonalWalletSyncing = session.isPersonalWalletSyncing,
                isFamilyWalletSyncing = session.isFamilyWalletSyncing,
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
     * Called when the dashboard becomes visible. Pulls personal and family
     * slices in parallel, then retries push only if Room still has PENDING/FAILED items.
     */
    fun onScreenRendered() {
        viewModelScope.launch(dispatcher.io) {
            try {
                coroutineScope {
                    launch { pullPersonalWallet() }
                    launch { pullFamilyWallet() }
                }
                retryPendingSync()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _syncError.value = ERR_SYNC_PERSONAL
            }
        }
    }

    private suspend fun pullPersonalWallet() {
        _isPersonalWalletSyncing.value = true
        try {
            syncPersonalData()
                .onFailure {
                    _syncError.value = ERR_SYNC_PERSONAL
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _syncError.value = ERR_SYNC_PERSONAL
        } finally {
            _isPersonalWalletSyncing.value = false
        }
    }

    private suspend fun pullFamilyWallet() {
        _isFamilyWalletSyncing.value = true
        try {
            syncFamilyData()
                .onFailure {
                    _syncError.value = ERR_SYNC_FAMILY
                }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _syncError.value = ERR_SYNC_FAMILY
        } finally {
            _isFamilyWalletSyncing.value = false
        }
    }

    fun dismissError() {
        _syncError.update { null }
    }

    private data class DashboardSession(
        val user: User?,
        val family: FamilyGroup?,
        val walletUiPreferences: WalletUiPreferences,
        val syncError: String?,
        val isPersonalWalletSyncing: Boolean,
        val isFamilyWalletSyncing: Boolean,
    )

    private data class WalletSyncFlags(
        val isPersonalWalletSyncing: Boolean,
        val isFamilyWalletSyncing: Boolean,
    )

    fun onTogglePersonalBalanceVisibility() {
        persistBalanceVisibility(
            walletType = WalletType.PERSONAL,
            visible = !uiState.value.isPersonalBalanceVisible,
        )
    }

    fun onToggleFamilyBalanceVisibility() {
        persistBalanceVisibility(
            walletType = WalletType.FAMILY,
            visible = !uiState.value.isFamilyBalanceVisible,
        )
    }

    private fun persistBalanceVisibility(
        walletType: WalletType,
        visible: Boolean,
    ) {
        viewModelScope.launch(dispatcher.io) {
            try {
                setWalletBalanceVisibility(walletType = walletType, visible = visible)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best-effort local preference; next emit keeps last stored value.
            }
        }
    }

    private companion object {
        const val RECENT_TX_LIMIT = 5
        const val ERR_LOAD_FAILED = "Gagal memuat dashboard"
        const val ERR_SYNC_PERSONAL =
            "Gagal memuat dompet. Coba buka ulang Dashboard."
        const val ERR_SYNC_FAMILY =
            "Gagal memuat dompet keluarga. Coba buka ulang Dashboard."
    }
}
