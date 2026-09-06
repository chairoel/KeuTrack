package com.mascill.keutrack.feature.transaction.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.designsystem.format.MAX_AMOUNT_RUPIAH
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.AddTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.DeleteTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionByIdUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.UpdateTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.feature.transaction.presentation.model.EntryTransactionKind
import com.mascill.keutrack.feature.transaction.presentation.model.NewEntryUIState
import com.mascill.keutrack.feature.transaction.presentation.model.TransactionUiMapper
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
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class NewEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getTransactionById: GetTransactionByIdUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val formState = MutableStateFlow(
        readEditingTransactionId(savedStateHandle).let { editingId ->
            FormDraft(
                editingTransactionId = editingId,
                isLoadingEdit = editingId != null,
            )
        },
    )

    private val data =
        combine(
            userRepository.getCurrentUser(),
            getWalletSummary(),
            getCategories(),
        ) { user, walletSummary, categories ->
            EntrySnapshot(user = user, walletSummary = walletSummary, categories = categories)
        }

    val uiState: StateFlow<NewEntryUIState> =
        combine(data, formState) { snapshot, draft ->
            val wallets = TransactionUiMapper.toWalletOptions(snapshot.walletSummary)
            val selectedWalletId =
                TransactionUiMapper.resolveSelectedWalletId(
                    summary = snapshot.walletSummary,
                    selectedWalletId = draft.selectedWalletId,
                    selectedFamilyId = draft.preservedFamilyId,
                )
            val categoriesForKind =
                TransactionUiMapper.filterCategoriesForKind(snapshot.categories, draft.kind)
            val categoryUi = TransactionUiMapper.toNewEntryCategories(categoriesForKind)
            val selectedCategoryId =
                draft.selectedCategoryId
                    ?.takeIf { id -> categoryUi.any { it.id == id } }
                    ?: categoryUi.firstOrNull()?.id
            val user = snapshot.user
            val addedByName =
                user?.displayName?.ifBlank { null }
                    ?: user?.email.orEmpty()

            NewEntryUIState(
                isLoading = draft.isLoadingEdit,
                isSaving = draft.isSaving,
                errorMessage = draft.errorMessage,
                navigateBack = draft.navigateBack,
                editingTransactionId = draft.editingTransactionId,
                kind = draft.kind,
                amount = draft.amount,
                categories = categoryUi,
                selectedCategoryId = selectedCategoryId,
                wallets = wallets,
                selectedWalletId = selectedWalletId,
                selectedDate = draft.selectedDate,
                note = draft.note,
                userId = user?.uid,
                addedByName = addedByName,
            )
        }.catch { e ->
            emit(
                NewEntryUIState(
                    isLoading = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                ),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NewEntryUIState(),
        )

    init {
        val editingId = formState.value.editingTransactionId
        if (editingId != null) {
            viewModelScope.launch(dispatcher.io) {
                loadExistingTransaction(editingId)
            }
        }
    }

    fun onKindChanged(kind: EntryTransactionKind) {
        formState.update {
            it.copy(
                kind = kind,
                selectedCategoryId = null,
                errorMessage = null,
            )
        }
    }

    fun onDigit(digit: Long) {
        formState.update { draft ->
            val next = draft.amount * 10L + digit
            if (next <= MAX_AMOUNT_RUPIAH) {
                draft.copy(amount = next, errorMessage = null)
            } else {
                draft
            }
        }
    }

    fun onTripleZero() {
        formState.update { draft ->
            if (draft.amount <= MAX_AMOUNT_RUPIAH / 1000L) {
                draft.copy(amount = draft.amount * 1000L, errorMessage = null)
            } else {
                draft
            }
        }
    }

    fun onBackspace() {
        formState.update { it.copy(amount = it.amount / 10L, errorMessage = null) }
    }

    fun onCategorySelected(categoryId: String) {
        formState.update {
            it.copy(selectedCategoryId = categoryId, errorMessage = null)
        }
    }

    fun onWalletSelected(walletId: String) {
        formState.update {
            it.copy(selectedWalletId = walletId, errorMessage = null)
        }
    }

    fun onDateSelected(date: LocalDate) {
        formState.update {
            it.copy(
                selectedDate = TransactionUiMapper.localDateToInstant(date),
                errorMessage = null,
            )
        }
    }

    fun onNoteChanged(note: String) {
        formState.update {
            it.copy(note = note.take(NOTE_MAX_LENGTH), errorMessage = null)
        }
    }

    fun clearError() {
        formState.update { it.copy(errorMessage = null) }
    }

    fun onNavigateBackConsumed() {
        formState.update { it.copy(navigateBack = false) }
    }

    fun onSave() {
        viewModelScope.launch(dispatcher.io) {
            if (formState.value.isSaving) return@launch

            val state = uiState.value
            val walletId = state.selectedWalletId
            val userId = state.userId
            val categoryId = state.selectedCategoryId
            val wallet = state.selectedWallet

            when {
                walletId.isNullOrBlank() || wallet == null -> {
                    formState.update { it.copy(errorMessage = ERR_NO_WALLET) }
                    return@launch
                }
                userId.isNullOrBlank() -> {
                    formState.update { it.copy(errorMessage = ERR_NO_USER) }
                    return@launch
                }
                state.amount <= 0L -> {
                    formState.update { it.copy(errorMessage = ERR_AMOUNT) }
                    return@launch
                }
                categoryId.isNullOrBlank() -> {
                    formState.update { it.copy(errorMessage = ERR_CATEGORY) }
                    return@launch
                }
            }

            formState.update { it.copy(isSaving = true, errorMessage = null) }
            val draft = formState.value
            val editingId = draft.editingTransactionId

            val transaction =
                Transaction(
                    id = editingId ?: UUID.randomUUID().toString(),
                    walletId = walletId!!,
                    userId = draft.preservedUserId ?: userId!!,
                    familyId = wallet!!.familyId,
                    type =
                        when (state.kind) {
                            EntryTransactionKind.Expense -> TransactionType.EXPENSE
                            EntryTransactionKind.Income -> TransactionType.INCOME
                        },
                    amount = state.amount,
                    categoryId = categoryId!!,
                    note = state.note.takeIf { it.isNotBlank() },
                    date = state.selectedDate,
                    addedByName = draft.preservedAddedByName ?: state.addedByName,
                    syncStatus = SyncStatus.PENDING,
                    createdAt = draft.preservedCreatedAt ?: Instant.now(),
                )

            try {
                val result =
                    if (editingId != null) {
                        updateTransaction(transaction)
                    } else {
                        addTransaction(transaction)
                    }
                applyWriteResult(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: ERR_SAVE_FAILED,
                    )
                }
            }
        }
    }

    fun onDelete() {
        viewModelScope.launch(dispatcher.io) {
            val id = formState.value.editingTransactionId
            if (id.isNullOrBlank() || formState.value.isSaving) return@launch

            formState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                applyWriteResult(deleteTransaction(id), deleteAction = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: ERR_DELETE_FAILED,
                    )
                }
            }
        }
    }

    private suspend fun loadExistingTransaction(id: String) {
        try {
            val existing = getTransactionById(id)
            if (existing == null) {
                formState.update {
                    it.copy(
                        isLoadingEdit = false,
                        errorMessage = ERR_NOT_FOUND,
                        navigateBack = true,
                    )
                }
                return
            }
            formState.update {
                it.copy(
                    isLoadingEdit = false,
                    editingTransactionId = existing.id,
                    kind =
                        when (existing.type) {
                            TransactionType.EXPENSE -> EntryTransactionKind.Expense
                            TransactionType.INCOME -> EntryTransactionKind.Income
                        },
                    amount = existing.amount,
                    selectedCategoryId = existing.categoryId,
                    selectedWalletId = existing.walletId,
                    selectedDate = existing.date,
                    note = existing.note.orEmpty(),
                    preservedCreatedAt = existing.createdAt,
                    preservedUserId = existing.userId,
                    preservedAddedByName = existing.addedByName,
                    preservedFamilyId = existing.familyId,
                    errorMessage = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            formState.update {
                it.copy(
                    isLoadingEdit = false,
                    errorMessage = e.message ?: ERR_LOAD_FAILED,
                    navigateBack = true,
                )
            }
        }
    }

    private fun applyWriteResult(
        result: TransactionWriteResult,
        deleteAction: Boolean = false,
    ) {
        val fallback = if (deleteAction) ERR_DELETE_FAILED else ERR_SAVE_FAILED
        when (result) {
            TransactionWriteResult.Success ->
                formState.update { it.copy(isSaving = false, navigateBack = true) }

            TransactionWriteResult.Error.InvalidAmount ->
                formState.update { it.copy(isSaving = false, errorMessage = ERR_AMOUNT) }

            TransactionWriteResult.Error.MissingWallet ->
                formState.update { it.copy(isSaving = false, errorMessage = ERR_NO_WALLET) }

            TransactionWriteResult.Error.MissingCategory ->
                formState.update { it.copy(isSaving = false, errorMessage = ERR_CATEGORY) }

            TransactionWriteResult.Error.MissingId,
            TransactionWriteResult.Error.NotFound ->
                formState.update {
                    val pop = it.editingTransactionId != null
                    it.copy(
                        isSaving = false,
                        errorMessage = if (pop) ERR_NOT_FOUND else fallback,
                        navigateBack = pop,
                    )
                }

            is TransactionWriteResult.Error.Unknown ->
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.cause.message ?: fallback,
                    )
                }
        }
    }

    private data class EntrySnapshot(
        val user: User?,
        val walletSummary: WalletSummary,
        val categories: List<Category>,
    )

    private data class FormDraft(
        val kind: EntryTransactionKind = EntryTransactionKind.Expense,
        val amount: Long = 0L,
        val selectedCategoryId: String? = null,
        val selectedWalletId: String? = null,
        val selectedDate: Instant = Instant.now(),
        val note: String = "",
        val editingTransactionId: String? = null,
        val preservedCreatedAt: Instant? = null,
        val preservedUserId: String? = null,
        val preservedAddedByName: String? = null,
        val preservedFamilyId: String? = null,
        val isLoadingEdit: Boolean = false,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val navigateBack: Boolean = false,
    )

    private companion object {
        const val ARG_TRANSACTION_ID = "transactionId"
        const val NOTE_MAX_LENGTH = 120
        const val ERR_NO_WALLET = "Buat dompet dulu sebelum menambah transaksi"
        const val ERR_NO_USER = "Sesi tidak valid. Silakan login ulang"
        const val ERR_AMOUNT = "Amount must be greater than 0"
        const val ERR_CATEGORY = "Category must be selected"
        const val ERR_SAVE_FAILED = "Gagal menyimpan transaksi"
        const val ERR_DELETE_FAILED = "Gagal menghapus"
        const val ERR_NOT_FOUND = "Transaksi tidak ditemukan"
        const val ERR_LOAD_FAILED = "Gagal memuat form transaksi"

        fun readEditingTransactionId(handle: SavedStateHandle): String? =
            when (val value = handle.get<Any>(ARG_TRANSACTION_ID)) {
                is String -> value.takeIf { it.isNotBlank() }
                else -> null
            }
    }
}
