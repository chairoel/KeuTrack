package com.mascill.keutrack.feature.transaction

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.AddTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.DeleteTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionByIdUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.UpdateTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.transaction.presentation.NewEntryViewModel
import com.mascill.keutrack.feature.transaction.presentation.model.EntryTransactionKind
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class NewEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val transactionRepo = mockk<TransactionRepository>(relaxed = true)
    private val addTransaction = AddTransactionUseCase(transactionRepo)
    private val getTransactionById = GetTransactionByIdUseCase(transactionRepo)
    private val updateTransaction = UpdateTransactionUseCase(transactionRepo)
    private val deleteTransaction = DeleteTransactionUseCase(transactionRepo)

    @Test
    fun `initial state is loading`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFormData()
        val vm = createViewModel()
        assertThat(vm.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `amount zero save returns validation error`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFormData()
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.onSave()
            advanceUntilIdle()
            assertThat(expectMostRecentItem().errorMessage).isEqualTo("Amount must be greater than 0")
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { transactionRepo.addTransaction(any()) }
    }

    @Test
    fun `save success navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFormData()
        coEvery { transactionRepo.addTransaction(any()) } just runs
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.onDigit(1)
            vm.onDigit(5)
            vm.onDigit(0)
            vm.onDigit(0)
            vm.onDigit(0)
            advanceUntilIdle()
            assertThat(expectMostRecentItem().amount).isEqualTo(15_000L)
            vm.onSave()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.navigateBack).isTrue()
            assertThat(state.isSaving).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify {
            transactionRepo.addTransaction(match { it.amount == 15_000L && it.categoryId == "cat_makanan" })
        }
        coVerify(exactly = 0) { transactionRepo.updateTransaction(any()) }
    }

    @Test
    fun `save without wallet shows wallet error`() = runTest(mainDispatcherRule.testDispatcher) {
        every { userRepo.getCurrentUser() } returns flowOf(user())
        every { getWalletSummary() } returns flowOf(WalletSummary(null, emptyList(), 0L, 0L))
        every { getCategories() } returns flowOf(listOf(foodCategory()))
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.onDigit(1)
            vm.onSave()
            advanceUntilIdle()
            assertThat(expectMostRecentItem().errorMessage)
                .isEqualTo("Buat dompet dulu sebelum menambah transaksi")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `edit id prefills form and uses update on save`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFormData()
            val existing = existingTransaction()
            coEvery { transactionRepo.getTransactionById("tx-1") } returns existing
            coEvery { transactionRepo.updateTransaction(any()) } just runs
            val vm = createViewModel(SavedStateHandle(mapOf("transactionId" to "tx-1")))

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val prefilled = expectMostRecentItem()
                assertThat(prefilled.isLoading).isFalse()
                assertThat(prefilled.isEditMode).isTrue()
                assertThat(prefilled.editingTransactionId).isEqualTo("tx-1")
                assertThat(prefilled.amount).isEqualTo(25_000L)
                assertThat(prefilled.kind).isEqualTo(EntryTransactionKind.Expense)
                assertThat(prefilled.selectedCategoryId).isEqualTo("cat_makanan")
                assertThat(prefilled.selectedWalletId).isEqualTo("w-p")
                assertThat(prefilled.selectedWallet?.name).isEqualTo("Dompet Utama")
                assertThat(prefilled.note).isEqualTo("Lunch")

                vm.onSave()
                advanceUntilIdle()
                val saved = expectMostRecentItem()
                assertThat(saved.navigateBack).isTrue()
                assertThat(saved.isSaving).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
            coVerify {
                transactionRepo.updateTransaction(
                    match {
                        it.id == "tx-1" &&
                            it.amount == 25_000L &&
                            it.createdAt == existing.createdAt &&
                            it.userId == existing.userId &&
                            it.addedByName == existing.addedByName
                    },
                )
            }
            coVerify(exactly = 0) { transactionRepo.addTransaction(any()) }
        }

    @Test
    fun `delete in edit mode navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFormData()
        coEvery { transactionRepo.getTransactionById("tx-1") } returns existingTransaction()
        coEvery { transactionRepo.deleteTransaction("tx-1") } just runs
        val vm = createViewModel(SavedStateHandle(mapOf("transactionId" to "tx-1")))

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.onDelete()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.navigateBack).isTrue()
            assertThat(state.isSaving).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { transactionRepo.deleteTransaction("tx-1") }
        coVerify(exactly = 0) { transactionRepo.addTransaction(any()) }
    }

    @Test
    fun `missing edit id navigates back`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFormData()
        coEvery { transactionRepo.getTransactionById("gone") } returns null
        val vm = createViewModel(SavedStateHandle(mapOf("transactionId" to "gone")))

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.navigateBack).isTrue()
            assertThat(state.errorMessage).isEqualTo("Transaksi tidak ditemukan")
            assertThat(state.isLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { transactionRepo.updateTransaction(any()) }
        coVerify(exactly = 0) { transactionRepo.deleteTransaction(any()) }
    }

    @Test
    fun `edit prefills second personal wallet from summary`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val extraPersonal = personalWallet().copy(id = "w-p2", name = "Dompet Cadangan")
            every { userRepo.getCurrentUser() } returns flowOf(user())
            every { getWalletSummary() } returns flowOf(
                WalletSummary(
                    personalWallet = personalWallet(),
                    familyWallets = emptyList(),
                    totalPersonalBalance = 10_000L,
                    totalFamilyBalance = 0L,
                    personalWallets = listOf(personalWallet(), extraPersonal),
                ),
            )
            every { getCategories() } returns flowOf(listOf(foodCategory()))
            coEvery { transactionRepo.getTransactionById("tx-1") } returns
                existingTransaction().copy(walletId = "w-p2")
            val vm = createViewModel(SavedStateHandle(mapOf("transactionId" to "tx-1")))

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.selectedWalletId).isEqualTo("w-p2")
                assertThat(state.selectedWallet?.name).isEqualTo("Dompet Cadangan")
                assertThat(state.wallets.map { it.id }).containsAtLeast("w-p", "w-p2")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `edit remaps missing wallet id to current personal wallet`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFormData()
            coEvery { transactionRepo.getTransactionById("tx-1") } returns
                existingTransaction().copy(walletId = "wallet-gone")
            val vm = createViewModel(SavedStateHandle(mapOf("transactionId" to "tx-1")))

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.selectedWalletId).isEqualTo("w-p")
                assertThat(state.selectedWallet?.name).isEqualTo("Dompet Utama")
                assertThat(state.wallets).hasSize(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `delete without edit id is ignored`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFormData()
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onDelete()
        advanceUntilIdle()

        assertThat(vm.uiState.value.navigateBack).isFalse()
        assertThat(vm.uiState.value.isEditMode).isFalse()
        coVerify(exactly = 0) { transactionRepo.deleteTransaction(any()) }
    }

    private fun stubFormData() {
        every { userRepo.getCurrentUser() } returns flowOf(user())
        every { getWalletSummary() } returns flowOf(
            WalletSummary(personalWallet(), emptyList(), 10_000L, 0L),
        )
        every { getCategories() } returns flowOf(listOf(foodCategory()))
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = NewEntryViewModel(
        savedStateHandle = savedStateHandle,
        userRepository = userRepo,
        getWalletSummary = getWalletSummary,
        getCategories = getCategories,
        getTransactionById = getTransactionById,
        addTransaction = addTransaction,
        updateTransaction = updateTransaction,
        deleteTransaction = deleteTransaction,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )

    private fun user() = User("user-1", "Irul", "irul@example.com", null)

    private fun personalWallet() = Wallet(
        id = "w-p",
        ownerId = "user-1",
        name = "Dompet Utama",
        type = WalletType.PERSONAL,
        balance = 10_000L,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun foodCategory() = Category(
        id = "cat_makanan",
        name = "Makanan",
        icon = "Restaurant",
        color = "#FF7043",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    private fun existingTransaction() = Transaction(
        id = "tx-1",
        walletId = "w-p",
        userId = "owner-1",
        familyId = null,
        type = TransactionType.EXPENSE,
        amount = 25_000L,
        categoryId = "cat_makanan",
        note = "Lunch",
        date = Instant.parse("2026-08-15T04:00:00Z"),
        addedByName = "Original Name",
        createdAt = Instant.parse("2026-08-10T00:00:00Z"),
    )
}
