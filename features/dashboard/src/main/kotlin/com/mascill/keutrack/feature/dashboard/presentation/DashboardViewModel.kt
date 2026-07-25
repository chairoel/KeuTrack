package com.mascill.keutrack.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.AddTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetMonthlySummaryUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.MonthlySummaryResult
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUIState
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUiMapper
import com.mascill.keutrack.feature.dashboard.presentation.model.EntryTransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val getTransactions: GetTransactionsUseCase,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val retryPendingSync: RetryPendingSyncUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val selectedEntryKind =
        MutableStateFlow(EntryTransactionKind.Expense)

    private val saveOverlay =
        MutableStateFlow(SaveOverlay())

    private val currentMonth = YearMonth.now()
    private val priorMonth = currentMonth.minusMonths(1)
    private val currentMonthKey = currentMonth.toString()
    private val priorMonthKey = priorMonth.toString()

    private val dashboardData =
        combine(
            userRepository.getCurrentUser(),
            getWalletSummary(),
            getTransactions(GetTransactionsUseCase.Params(limit = RECENT_TX_LIMIT)),
            getMonthlySummary(
                currentMonth = currentMonthKey,
                trendMonths = listOf(priorMonthKey),
            ),
            getCategories(),
        ) { user, walletSummary, transactions, monthlySummary, categories ->
            DashboardSnapshot(
                user = user,
                walletSummary = walletSummary,
                transactions = transactions,
                monthlySummary = monthlySummary,
                categories = categories,
            )
        }

    val uiState: StateFlow<DashboardUIState> =
        combine(
            dashboardData,
            selectedEntryKind,
            saveOverlay,
        ) { snapshot, kind, save ->
            val user = snapshot.user
            val wallet = snapshot.walletSummary
            val categoriesForKind =
                DashboardUiMapper.filterCategoriesForKind(snapshot.categories, kind)
            val categoriesById = snapshot.categories.associateBy { it.id }
            val walletTypes = DashboardUiMapper.mapWalletTypes(wallet)
            val prior =
                DashboardUiMapper.priorFromTrend(snapshot.monthlySummary, priorMonthKey)

            DashboardUIState(
                isLoading = false,
                errorMessage = null,
                userFirstName = DashboardUiMapper.greetingFirstName(user),
                avatarUrl = user?.photoUrl,
                personalBalance = wallet.totalPersonalBalance,
                familyBalance = wallet.totalFamilyBalance,
                familySharedSummary =
                    DashboardUiMapper.familySharedSummary(wallet.familyWallets.size),
                monthChangeLabel =
                    DashboardUiMapper.monthChangeLabel(
                        current = snapshot.monthlySummary.currentMonth,
                        prior = prior,
                    ),
                incomeTotal = snapshot.monthlySummary.currentMonth?.totalIncome ?: 0L,
                expenseTotal = snapshot.monthlySummary.currentMonth?.totalExpense ?: 0L,
                recentTransactions =
                    DashboardUiMapper.toTransactionRows(
                        transactions = snapshot.transactions,
                        categoriesById = categoriesById,
                        walletsById = walletTypes,
                    ),
                categories = DashboardUiMapper.toNewEntryCategories(categoriesForKind),
                selectedEntryKind = kind,
                personalWalletId = wallet.personalWallet?.id,
                currentUserId = user?.uid,
                currentUserDisplayName = user?.displayName.orEmpty(),
                currentUserEmail = user?.email.orEmpty(),
                isSavingTransaction = save.isSaving,
                saveError = save.error,
                dismissNewEntrySheet = save.dismissSheet,
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

    fun onEntryKindChanged(kind: EntryTransactionKind) {
        selectedEntryKind.value = kind
        saveOverlay.update { it.copy(error = null) }
    }

    fun onSaveTransaction(
        amount: Long,
        categoryId: String,
        kind: EntryTransactionKind,
    ) {
        viewModelScope.launch(dispatcher.io) {
            if (saveOverlay.value.isSaving) return@launch

            val state = uiState.value
            val walletId = state.personalWalletId
            val userId = state.currentUserId

            when {
                walletId.isNullOrBlank() -> {
                    saveOverlay.value =
                        SaveOverlay(error = ERR_NO_WALLET)
                    return@launch
                }
                userId.isNullOrBlank() -> {
                    saveOverlay.value =
                        SaveOverlay(error = ERR_NO_USER)
                    return@launch
                }
                amount <= 0L -> {
                    saveOverlay.value =
                        SaveOverlay(error = ERR_AMOUNT)
                    return@launch
                }
                categoryId.isBlank() -> {
                    saveOverlay.value =
                        SaveOverlay(error = ERR_CATEGORY)
                    return@launch
                }
            }

            saveOverlay.value = SaveOverlay(isSaving = true)

            val addedByName =
                state.currentUserDisplayName.ifBlank { state.currentUserEmail }
            val transaction =
                Transaction(
                    id = UUID.randomUUID().toString(),
                    walletId = walletId!!,
                    userId = userId!!,
                    familyId = null,
                    type =
                        when (kind) {
                            EntryTransactionKind.Expense -> TransactionType.EXPENSE
                            EntryTransactionKind.Income -> TransactionType.INCOME
                        },
                    amount = amount,
                    categoryId = categoryId,
                    note = null,
                    date = Instant.now(),
                    addedByName = addedByName,
                    syncStatus = SyncStatus.PENDING,
                )

            try {
                val result = addTransaction(transaction)
                result.fold(
                    onSuccess = {
                        saveOverlay.value = SaveOverlay(dismissSheet = true)
                    },
                    onFailure = { error ->
                        saveOverlay.value =
                            SaveOverlay(
                                error = error.message ?: ERR_SAVE_FAILED,
                            )
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saveOverlay.value =
                    SaveOverlay(error = e.message ?: ERR_SAVE_FAILED)
            }
        }
    }

    fun onNewEntrySheetDismissed() {
        saveOverlay.value = SaveOverlay()
        selectedEntryKind.value = EntryTransactionKind.Expense
    }

    fun clearSaveError() {
        saveOverlay.update { it.copy(error = null) }
    }

    private data class DashboardSnapshot(
        val user: User?,
        val walletSummary: WalletSummary,
        val transactions: List<Transaction>,
        val monthlySummary: MonthlySummaryResult,
        val categories: List<Category>,
    )

    private data class SaveOverlay(
        val isSaving: Boolean = false,
        val error: String? = null,
        val dismissSheet: Boolean = false,
    )

    private companion object {
        const val RECENT_TX_LIMIT = 5
        const val ERR_NO_WALLET = "Buat dompet dulu sebelum menambah transaksi"
        const val ERR_NO_USER = "Sesi tidak valid. Silakan login ulang"
        const val ERR_AMOUNT = "Amount must be greater than 0"
        const val ERR_CATEGORY = "Category must be selected"
        const val ERR_SAVE_FAILED = "Gagal menyimpan transaksi"
        const val ERR_LOAD_FAILED = "Gagal memuat dashboard"
    }
}
