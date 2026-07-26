package com.mascill.keutrack.feature.transaction.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.keutrack.core.common.utils.CommonDispatcher
import com.mascill.keutrack.core.designsystem.format.MAX_AMOUNT_RUPIAH
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.AddTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
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
    private val userRepository: UserRepository,
    private val getWalletSummary: GetWalletSummaryUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val dispatcher: CommonDispatcher,
) : ViewModel() {

    private val formState = MutableStateFlow(FormDraft())

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
                draft.selectedWalletId
                    ?: TransactionUiMapper.defaultWalletId(snapshot.walletSummary)
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
                isLoading = false,
                isSaving = draft.isSaving,
                errorMessage = draft.errorMessage,
                navigateBack = draft.navigateBack,
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

            val transaction =
                Transaction(
                    id = UUID.randomUUID().toString(),
                    walletId = walletId!!,
                    userId = userId!!,
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
                    addedByName = state.addedByName,
                    syncStatus = SyncStatus.PENDING,
                )

            try {
                val result = addTransaction(transaction)
                result.fold(
                    onSuccess = {
                        formState.update {
                            it.copy(isSaving = false, navigateBack = true)
                        }
                    },
                    onFailure = { error ->
                        formState.update {
                            it.copy(
                                isSaving = false,
                                errorMessage = error.message ?: ERR_SAVE_FAILED,
                            )
                        }
                    },
                )
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
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val navigateBack: Boolean = false,
    )

    private companion object {
        const val NOTE_MAX_LENGTH = 120
        const val ERR_NO_WALLET = "Buat dompet dulu sebelum menambah transaksi"
        const val ERR_NO_USER = "Sesi tidak valid. Silakan login ulang"
        const val ERR_AMOUNT = "Amount must be greater than 0"
        const val ERR_CATEGORY = "Category must be selected"
        const val ERR_SAVE_FAILED = "Gagal menyimpan transaksi"
        const val ERR_LOAD_FAILED = "Gagal memuat form transaksi"
    }
}
