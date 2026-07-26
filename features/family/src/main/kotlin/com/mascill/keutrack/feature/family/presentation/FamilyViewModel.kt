package com.mascill.keutrack.feature.family.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.CreateFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.GetBudgetProgressUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.JoinFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.family.presentation.model.FamilyUIState
import com.mascill.keutrack.feature.family.presentation.model.FamilyUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FamilyViewModel @Inject constructor(
    userRepository: UserRepository,
    familyRepository: FamilyRepository,
    getWalletSummary: GetWalletSummaryUseCase,
    getTransactions: GetTransactionsUseCase,
    getBudgetProgress: GetBudgetProgressUseCase,
    getCategories: GetCategoriesUseCase,
    private val createFamilyGroup: CreateFamilyGroupUseCase,
    private val joinFamilyGroup: JoinFamilyGroupUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val currentMonth = YearMonth.now()
    private val priorMonth = currentMonth.minusMonths(1)
    private val currentMonthKey = currentMonth.toString()

    private val _membershipLoading = MutableStateFlow(false)
    private val _membershipMessage = MutableStateFlow<String?>(null)

    private val walletSummaryFlow = getWalletSummary()
    private val userFlow = userRepository.getCurrentUser()
    private val familyGroupFlow = familyRepository.observeCurrentFamily()

    private val familyTransactionsFlow =
        combine(userFlow, walletSummaryFlow) { user, summary ->
            FamilyUiMapper.resolveFamilyWallet(user, summary)?.id
        }.flatMapLatest { familyWalletId ->
            if (familyWalletId == null) {
                flowOf(emptyList())
            } else {
                getTransactions(
                    GetTransactionsUseCase.Params(
                        walletId = familyWalletId,
                        limit = FAMILY_TX_LIMIT,
                    ),
                )
            }
        }

    private val insightsFlow =
        combine(
            combine(
                userFlow,
                familyGroupFlow,
                walletSummaryFlow,
                familyTransactionsFlow,
                getBudgetProgress(currentMonthKey),
            ) { user, familyGroup, walletSummary, transactions, budgets ->
                FamilyLoadBundle(
                    user = user,
                    familyGroup = familyGroup,
                    walletSummary = walletSummary,
                    transactions = transactions,
                    budgets = budgets,
                )
            },
            getCategories(),
        ) { bundle, categories ->
            FamilyUiMapper.toUiState(
                user = bundle.user,
                familyGroup = bundle.familyGroup,
                walletSummary = bundle.walletSummary,
                familyTransactions = bundle.transactions,
                budgets = bundle.budgets,
                categoriesById = categories.associateBy { it.id },
                currentMonth = currentMonth,
                priorMonth = priorMonth,
            )
        }.catch { e ->
            emit(
                FamilyUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                ),
            )
        }

    val uiState: StateFlow<FamilyUIState> =
        combine(
            insightsFlow,
            _membershipLoading,
            _membershipMessage,
        ) { insights, membershipLoading, membershipMessage ->
            insights.copy(
                isMembershipLoading = membershipLoading,
                membershipMessage = membershipMessage,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilyUIState(),
        )

    fun createFamily(name: String) {
        viewModelScope.launch(dispatcher.io) {
            _membershipLoading.value = true
            _membershipMessage.value = null
            try {
                createFamilyGroup(name)
                    .onSuccess {
                        _membershipMessage.value =
                            "Keluarga dibuat. Kode: ${it.inviteCode}"
                    }
                    .onFailure { e ->
                        _membershipMessage.value =
                            e.message ?: "Gagal membuat keluarga"
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _membershipMessage.value = e.message ?: "Gagal membuat keluarga"
            } finally {
                _membershipLoading.value = false
            }
        }
    }

    fun joinFamily(inviteCode: String) {
        viewModelScope.launch(dispatcher.io) {
            _membershipLoading.value = true
            _membershipMessage.value = null
            try {
                joinFamilyGroup(inviteCode)
                    .onSuccess {
                        _membershipMessage.value =
                            "Berhasil bergabung ke ${it.name}"
                    }
                    .onFailure { e ->
                        _membershipMessage.value =
                            e.message ?: "Gagal bergabung ke keluarga"
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _membershipMessage.value = e.message ?: "Gagal bergabung ke keluarga"
            } finally {
                _membershipLoading.value = false
            }
        }
    }

    fun dismissMembershipMessage() {
        _membershipMessage.update { null }
    }

    private data class FamilyLoadBundle(
        val user: User?,
        val familyGroup: FamilyGroup?,
        val walletSummary: WalletSummary,
        val transactions: List<Transaction>,
        val budgets: List<Budget>,
    )

    private companion object {
        const val FAMILY_TX_LIMIT = 200
        const val ERR_LOAD_FAILED = "Gagal memuat family insights"
    }
}
