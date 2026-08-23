package com.mascill.keutrack.feature.family.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.designsystem.format.MAX_AMOUNT_RUPIAH
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.CreateFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.DeleteFamilyBudgetUseCase
import com.mascill.keutrack.core.domain.usecase.GetBudgetProgressUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.JoinFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.SyncFamilyDataUseCase
import com.mascill.keutrack.core.domain.usecase.UpsertFamilyBudgetUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.family.presentation.model.FamilyBudgetSheetState
import com.mascill.keutrack.feature.family.presentation.model.FamilyUIState
import com.mascill.keutrack.feature.family.presentation.model.FamilyUiMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    userRepository: UserRepository,
    familyRepository: FamilyRepository,
    getWalletSummary: GetWalletSummaryUseCase,
    private val getTransactions: GetTransactionsUseCase,
    getBudgetProgress: GetBudgetProgressUseCase,
    getCategories: GetCategoriesUseCase,
    private val createFamilyGroup: CreateFamilyGroupUseCase,
    private val joinFamilyGroup: JoinFamilyGroupUseCase,
    private val syncFamilyData: SyncFamilyDataUseCase,
    private val upsertFamilyBudget: UpsertFamilyBudgetUseCase,
    private val deleteFamilyBudget: DeleteFamilyBudgetUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(readSelectedMonth(savedStateHandle))

    private val _membershipLoading = MutableStateFlow(false)
    private val _membershipMessage = MutableStateFlow<String?>(null)
    private val _budgetEditor = MutableStateFlow(BudgetEditorState())

    @Volatile
    private var latestFamilyBudgets: List<Budget> = emptyList()

    private val walletSummaryFlow = getWalletSummary()
    private val userFlow = userRepository.getCurrentUser()
    private val familyGroupFlow = familyRepository.observeCurrentFamily()

    private val selectedMonthTxsFlow =
        combine(userFlow, selectedMonth) { user, month -> user to month }
            .flatMapLatest { (user, month) ->
                observeFamilyTransactions(user?.familyId, month)
            }

    private val priorMonthTxsFlow =
        combine(userFlow, selectedMonth) { user, month ->
            user to month.minusMonths(1)
        }.flatMapLatest { (user, prior) ->
            observeFamilyTransactions(user?.familyId, prior)
        }

    private val budgetsFlow =
        selectedMonth.flatMapLatest { month ->
            getBudgetProgress(month.toString())
        }

    private val insightsFlow =
        combine(
            combine(
                userFlow,
                familyGroupFlow,
                walletSummaryFlow,
                selectedMonthTxsFlow,
                priorMonthTxsFlow,
            ) { user, familyGroup, walletSummary, selectedTxs, priorTxs ->
                FamilyPartyBundle(
                    user = user,
                    familyGroup = familyGroup,
                    walletSummary = walletSummary,
                    selectedMonthTxs = selectedTxs,
                    priorMonthTxs = priorTxs,
                )
            },
            combine(budgetsFlow, getCategories(), selectedMonth) { budgets, categories, month ->
                Triple(budgets, categories, month)
            },
        ) { party, extra ->
            val budgets = extra.first
            val categories = extra.second
            val month = extra.third
            latestFamilyBudgets =
                FamilyUiMapper.filterSharedBudgets(
                    budgets = budgets,
                    familyId = party.user?.familyId,
                )
            FamilyUiMapper.toUiState(
                user = party.user,
                familyGroup = party.familyGroup,
                walletSummary = party.walletSummary,
                selectedMonthTxs = party.selectedMonthTxs,
                priorMonthTxs = party.priorMonthTxs,
                budgets = budgets,
                categoriesById = categories.associateBy { it.id },
                selectedMonth = month,
            )
        }.catch { e ->
            val month = selectedMonth.value
            val now = YearMonth.now()
            emit(
                FamilyUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                    selectedMonthLabel = FamilyUiMapper.formatBudgetMonth(month),
                    canSelectNextMonth = month < now,
                    canSelectPreviousMonth =
                        month > now.minusMonths(FamilyUiMapper.MONTH_LOOK_BACK_LIMIT),
                ),
            )
        }

    val uiState: StateFlow<FamilyUIState> =
        combine(
            insightsFlow,
            _membershipLoading,
            _membershipMessage,
            _budgetEditor,
        ) { insights, membershipLoading, membershipMessage, budgetEditor ->
            insights.copy(
                isMembershipLoading = membershipLoading,
                membershipMessage = membershipMessage,
                budgetSheet = budgetEditor.sheet,
                isBudgetSaving = budgetEditor.isSaving,
                budgetMessage = budgetEditor.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilyUIState(),
        )

    /** Pull shared family wallet/tx into Room when the Family tab opens. */
    fun onScreenRendered() {
        viewModelScope.launch(dispatcher.io) {
            pullFamilyData(showError = true)
        }
    }

    fun onPreviousMonth() {
        persistSelectedMonth(selectedMonth.value.minusMonths(1))
    }

    fun onNextMonth() {
        persistSelectedMonth(selectedMonth.value.plusMonths(1))
    }

    fun onAdjustTargetsClick() {
        if (!uiState.value.canEditBudgets) return
        _budgetEditor.update { editor ->
            editor.copy(
                sheet =
                    FamilyBudgetSheetState(
                        categoryId = null,
                        categoryLocked = false,
                        limitInput = "",
                        existingBudgetId = null,
                        errorMessage = null,
                    ),
                message = null,
            )
        }
    }

    fun onBudgetRowClick(categoryId: String) {
        if (!uiState.value.canEditBudgets || categoryId.isBlank()) return
        val existing = existingBudget(categoryId)
        _budgetEditor.update { editor ->
            editor.copy(
                sheet =
                    FamilyBudgetSheetState(
                        categoryId = categoryId,
                        categoryLocked = true,
                        limitInput = existing?.limit?.toString().orEmpty(),
                        existingBudgetId = existing?.id,
                        errorMessage = null,
                    ),
                message = null,
            )
        }
    }

    fun onSheetCategorySelected(categoryId: String) {
        if (categoryId.isBlank()) return
        val existing = existingBudget(categoryId)
        _budgetEditor.update { editor ->
            val sheet = editor.sheet ?: return@update editor
            if (sheet.categoryLocked) return@update editor
            editor.copy(
                sheet =
                    sheet.copy(
                        categoryId = categoryId,
                        limitInput = existing?.limit?.toString().orEmpty(),
                        existingBudgetId = existing?.id,
                        errorMessage = null,
                    ),
            )
        }
    }

    fun onLimitChanged(raw: String) {
        val digits = raw.filter { it.isDigit() }
        val limitInput =
            if (digits.isEmpty()) {
                ""
            } else {
                val amount = digits.take(MAX_AMOUNT_DIGITS).toLongOrNull() ?: return
                min(amount, MAX_AMOUNT_RUPIAH).toString()
            }
        _budgetEditor.update { editor ->
            val sheet = editor.sheet ?: return@update editor
            editor.copy(sheet = sheet.copy(limitInput = limitInput, errorMessage = null))
        }
    }

    fun onSaveBudget() {
        if (!isCurrentCalendarMonth()) return
        val sheet = _budgetEditor.value.sheet ?: return
        if (_budgetEditor.value.isSaving) return
        val categoryId = sheet.categoryId
        val limit = sheet.limitInput.toLongOrNull() ?: 0L
        if (categoryId.isNullOrBlank()) {
            setSheetError("Kategori wajib dipilih")
            return
        }
        if (limit <= 0L) {
            setSheetError("Limit harus lebih dari 0")
            return
        }
        viewModelScope.launch(dispatcher.io) {
            _budgetEditor.update { it.copy(isSaving = true, message = null) }
            try {
                upsertFamilyBudget(
                    UpsertFamilyBudgetUseCase.Params(
                        categoryId = categoryId,
                        limit = limit,
                        month = YearMonth.now().toString(),
                    ),
                ).onSuccess {
                    _budgetEditor.update { BudgetEditorState() }
                }.onFailure { e ->
                    applyBudgetFailure(e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                applyBudgetFailure(e)
            } finally {
                _budgetEditor.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onDeleteBudget() {
        if (!isCurrentCalendarMonth()) return
        val budgetId = _budgetEditor.value.sheet?.existingBudgetId ?: return
        if (_budgetEditor.value.isSaving) return
        viewModelScope.launch(dispatcher.io) {
            _budgetEditor.update { it.copy(isSaving = true, message = null) }
            try {
                deleteFamilyBudget(budgetId)
                    .onSuccess {
                        _budgetEditor.update { BudgetEditorState() }
                    }
                    .onFailure { e ->
                        applyBudgetFailure(e)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                applyBudgetFailure(e)
            } finally {
                _budgetEditor.update { it.copy(isSaving = false) }
            }
        }
    }

    fun onDismissBudgetSheet() {
        if (_budgetEditor.value.isSaving) return
        _budgetEditor.update { it.copy(sheet = null) }
    }

    fun dismissBudgetMessage() {
        _budgetEditor.update { it.copy(message = null) }
    }

    fun createFamily(name: String) {
        viewModelScope.launch(dispatcher.io) {
            _membershipLoading.value = true
            _membershipMessage.value = null
            try {
                createFamilyGroup(name)
                    .onSuccess {
                        _membershipMessage.value =
                            "Keluarga dibuat. Kode: ${it.inviteCode}"
                        pullFamilyData(showError = false)
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
                        pullFamilyData(showError = true)
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

    private fun observeFamilyTransactions(
        familyId: String?,
        month: YearMonth,
    ): kotlinx.coroutines.flow.Flow<List<Transaction>> {
        if (familyId.isNullOrBlank()) return flowOf(emptyList())
        val range = PeriodBounds.ofYearMonth(month)
        return getTransactions(
            GetTransactionsUseCase.Params(
                familyId = familyId,
                startDate = range.start,
                endDate = range.endInclusive,
                limit = FAMILY_TX_LIMIT,
            ),
        )
    }

    private fun persistSelectedMonth(month: YearMonth) {
        val now = YearMonth.now()
        val min = now.minusMonths(FamilyUiMapper.MONTH_LOOK_BACK_LIMIT)
        val clamped = month.coerceIn(min, now)
        if (clamped == selectedMonth.value) return
        selectedMonth.value = clamped
        savedStateHandle[KEY_SELECTED_MONTH] = clamped.toString()
        if (clamped != now) {
            _budgetEditor.update { it.copy(sheet = null) }
        }
    }

    private fun isCurrentCalendarMonth(): Boolean =
        selectedMonth.value == YearMonth.now()

    private fun readSelectedMonth(handle: SavedStateHandle): YearMonth {
        val raw = handle.get<String>(KEY_SELECTED_MONTH) ?: return YearMonth.now()
        return try {
            val parsed = YearMonth.parse(raw)
            val now = YearMonth.now()
            parsed.coerceIn(now.minusMonths(FamilyUiMapper.MONTH_LOOK_BACK_LIMIT), now)
        } catch (_: DateTimeParseException) {
            YearMonth.now()
        }
    }

    private fun existingBudget(categoryId: String): Budget? =
        latestFamilyBudgets
            .filter { it.categoryId == categoryId }
            .minByOrNull { it.createdAt }

    private fun setSheetError(message: String) {
        _budgetEditor.update { editor ->
            val sheet = editor.sheet ?: return@update editor
            editor.copy(sheet = sheet.copy(errorMessage = message))
        }
    }

    private fun applyBudgetFailure(error: Throwable) {
        val message = error.message ?: ERR_BUDGET_SAVE
        if (error is IllegalArgumentException) {
            setSheetError(message)
        } else {
            _budgetEditor.update { it.copy(message = message) }
        }
    }

    private suspend fun pullFamilyData(showError: Boolean) {
        try {
            syncFamilyData()
                .onFailure { e ->
                    if (showError) {
                        _membershipMessage.value =
                            e.message ?: ERR_SYNC_FAMILY
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (showError) {
                _membershipMessage.value = e.message ?: ERR_SYNC_FAMILY
            }
        }
    }

    private data class FamilyPartyBundle(
        val user: User?,
        val familyGroup: FamilyGroup?,
        val walletSummary: WalletSummary,
        val selectedMonthTxs: List<Transaction>,
        val priorMonthTxs: List<Transaction>,
    )

    private data class BudgetEditorState(
        val sheet: FamilyBudgetSheetState? = null,
        val isSaving: Boolean = false,
        val message: String? = null,
    )

    private companion object {
        const val KEY_SELECTED_MONTH = "selectedMonth"
        const val FAMILY_TX_LIMIT = 200
        const val MAX_AMOUNT_DIGITS = 15
        const val ERR_LOAD_FAILED = "Gagal memuat family insights"
        const val ERR_SYNC_FAMILY =
            "Gagal sinkron data keluarga. Coba buka ulang tab Family."
        const val ERR_BUDGET_SAVE = "Gagal menyimpan target"
    }
}
