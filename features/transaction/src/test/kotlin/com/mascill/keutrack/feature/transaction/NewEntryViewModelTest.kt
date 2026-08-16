package com.mascill.keutrack.feature.transaction

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.AddTransactionUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.transaction.presentation.NewEntryViewModel
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

    private fun stubFormData() {
        every { userRepo.getCurrentUser() } returns flowOf(user())
        every { getWalletSummary() } returns flowOf(
            WalletSummary(personalWallet(), emptyList(), 10_000L, 0L),
        )
        every { getCategories() } returns flowOf(listOf(foodCategory()))
    }

    private fun createViewModel() = NewEntryViewModel(
        userRepository = userRepo,
        getWalletSummary = getWalletSummary,
        getCategories = getCategories,
        addTransaction = addTransaction,
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
}
