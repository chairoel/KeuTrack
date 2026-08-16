package com.mascill.keutrack.feature.transaction

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.transaction.presentation.history.TransactionHistoryViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionHistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getTransactions = mockk<GetTransactionsUseCase>()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    private val retryPendingSync = mockk<RetryPendingSyncUseCase>(relaxed = true)

    @Test
    fun `initial state is loading`() = runTest(mainDispatcherRule.testDispatcher) {
        stub(emptyList())
        val vm = createViewModel()
        assertThat(vm.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `empty list emits empty content`() = runTest(mainDispatcherRule.testDispatcher) {
        stub(emptyList())
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.items).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `populated list maps transactions`() = runTest(mainDispatcherRule.testDispatcher) {
        stub(
            listOf(
                Transaction(
                    id = "tx-1",
                    walletId = "w-1",
                    userId = "u",
                    type = TransactionType.EXPENSE,
                    amount = 8_000L,
                    categoryId = "c",
                    note = "Kopi",
                    date = Instant.parse("2026-08-01T00:00:00Z"),
                    addedByName = "Irul",
                ),
            ),
        )
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.items).hasSize(1)
            assertThat(state.items.first().title).isEqualTo("Kopi")
            assertThat(state.items.first().amountLabel).isEqualTo("Rp 8.000")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stub(transactions: List<Transaction>) {
        every { getTransactions(any()) } returns flowOf(transactions)
        every { getCategories() } returns flowOf(emptyList())
        every { getWalletSummary() } returns flowOf(WalletSummary(null, emptyList(), 0L, 0L))
    }

    private fun createViewModel() = TransactionHistoryViewModel(
        getTransactions = getTransactions,
        getCategories = getCategories,
        getWalletSummary = getWalletSummary,
        retryPendingSync = retryPendingSync,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )
}
