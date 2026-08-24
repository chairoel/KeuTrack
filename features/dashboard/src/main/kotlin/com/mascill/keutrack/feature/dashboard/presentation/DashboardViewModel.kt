package com.mascill.keutrack.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetPeriodTotalsUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.ObservePeriodPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.ObserveWalletUiPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.SetWalletBalanceVisibilityUseCase
import com.mascill.keutrack.core.domain.usecase.SyncFamilyDataUseCase
import com.mascill.keutrack.core.domain.usecase.SyncPersonalDataUseCase
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUIState
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    userRepository: UserRepository,
    familyRepository: FamilyRepository,
    getWalletSummary: GetWalletSummaryUseCase,
    getTransactions: GetTransactionsUseCase,
    getPeriodTotals: GetPeriodTotalsUseCase,
    getCategories: GetCategoriesUseCase,
    observeWalletUiPreferences: ObserveWalletUiPreferencesUseCase,
    observePeriodPreferences: ObservePeriodPreferencesUseCase,
    private val setWalletBalanceVisibility: SetWalletBalanceVisibilityUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val syncPersonalData: SyncPersonalDataUseCase,
    private val syncFamilyData: SyncFamilyDataUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val _syncError = MutableStateFlow<String?>(null)
    private val _isPersonalWalletSyncing = MutableStateFlow(false)
    private val _isFamilyWalletSyncing = MutableStateFlow(false)

    private val periodTotalsFlow =
        observePeriodPreferences().flatMapLatest { prefs ->
            val current = PeriodBounds.containing(LocalDate.now(), prefs.cycleStartDay)
            val prior = current.minusPeriods(1)
            val currentRange = current.toInstantRange()
            val priorRange = prior.toInstantRange()
            combine(
                getPeriodTotals(currentRange.start, currentRange.endInclusive),
                getPeriodTotals(priorRange.start, priorRange.endInclusive),
            ) { currentTotals, priorTotals ->
                PeriodDashboardBundle(
                    cycleStartDay = prefs.cycleStartDay,
                    current = currentTotals,
                    prior = priorTotals,
                )
            }
        }

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
            periodTotalsFlow,
            getCategories(),
        ) { session, walletSummary, transactions, periodTotals, categories ->
            val categoriesById = categories.associateBy { it.id }
            val walletTypes = DashboardUiMapper.mapWalletTypes(walletSummary)

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
                        currentNet = periodTotals.current.netBalance,
                        priorNet = periodTotals.prior.netBalance,
                        cycleStartDay = periodTotals.cycleStartDay,
                    ),
                incomeTotal = periodTotals.current.incomeTotal,
                expenseTotal = periodTotals.current.expenseTotal,
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

    private data class PeriodDashboardBundle(
        val cycleStartDay: Int,
        val current: PeriodTotals,
        val prior: PeriodTotals,
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
