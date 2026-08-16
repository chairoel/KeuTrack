package com.mascill.keutrack.feature.transaction

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.transaction.presentation.history.TransactionHistoryViewModel
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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

    private val userRepository = mockk<UserRepository>()
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
            assertThat(state.scope).isEqualTo(HistoryScope.All)
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

    @Test
    fun `family only loads transactions for current family`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(
                transactions =
                    listOf(
                        Transaction(
                            id = "tx-fam",
                            walletId = "w-fam",
                            userId = "u",
                            familyId = "fam-1",
                            type = TransactionType.EXPENSE,
                            amount = 12_000L,
                            categoryId = "c",
                            note = "Belanja keluarga",
                            date = Instant.parse("2026-08-01T00:00:00Z"),
                            addedByName = "Irul",
                        ),
                    ),
                familyId = "fam-1",
                familyOnly = true,
            )
            val vm = createViewModel(familyOnly = true)

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.scope).isEqualTo(HistoryScope.Family)
                assertThat(state.items).hasSize(1)
                assertThat(state.items.first().title).isEqualTo("Belanja keluarga")
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.familyId == "fam-1" && params.limit == 200
                    },
                )
            }
        }

    @Test
    fun `family only without family emits empty list`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList(), familyId = null, familyOnly = true)
            val vm = createViewModel(familyOnly = true)

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.scope).isEqualTo(HistoryScope.Family)
                assertThat(state.items).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) { getTransactions(any()) }
        }

    @Test
    fun `personal only loads transactions for personal wallet`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(
                transactions =
                    listOf(
                        Transaction(
                            id = "tx-p",
                            walletId = "w-p",
                            userId = "u",
                            type = TransactionType.EXPENSE,
                            amount = 9_000L,
                            categoryId = "c",
                            note = "Kopi personal",
                            date = Instant.parse("2026-08-01T00:00:00Z"),
                            addedByName = "Irul",
                        ),
                    ),
                personalOnly = true,
                personalWalletId = "w-p",
            )
            val vm = createViewModel(personalOnly = true)

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.scope).isEqualTo(HistoryScope.Personal)
                assertThat(state.items).hasSize(1)
                assertThat(state.items.first().title).isEqualTo("Kopi personal")
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.walletId == "w-p" && params.limit == 50
                    },
                )
            }
        }

    @Test
    fun `personal only without wallet emits empty list`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList(), personalOnly = true)
            val vm = createViewModel(personalOnly = true)

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.scope).isEqualTo(HistoryScope.Personal)
                assertThat(state.items).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) { getTransactions(any()) }
        }

    private fun stub(
        transactions: List<Transaction>,
        familyId: String? = null,
        familyOnly: Boolean = false,
        personalOnly: Boolean = false,
        personalWalletId: String? = null,
    ) {
        every { userRepository.getCurrentUser() } returns flowOf(
            User("u", "Irul", "a@b.c", null, familyId = familyId),
        )
        val shouldLoadTransactions =
            when {
                familyOnly -> !familyId.isNullOrBlank()
                personalOnly -> !personalWalletId.isNullOrBlank()
                else -> true
            }
        if (shouldLoadTransactions) {
            every { getTransactions(any()) } returns flowOf(transactions)
        }
        every { getCategories() } returns flowOf(emptyList())
        every { getWalletSummary() } returns flowOf(
            WalletSummary(
                personalWallet = personalWalletId?.let { personalWallet(it) },
                familyWallets = emptyList(),
                totalPersonalBalance = 0L,
                totalFamilyBalance = 0L,
            ),
        )
    }

    private fun createViewModel(
        familyOnly: Boolean = false,
        personalOnly: Boolean = false,
    ) = TransactionHistoryViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "familyOnly" to familyOnly,
                "personalOnly" to personalOnly,
            ),
        ),
        userRepository = userRepository,
        getTransactions = getTransactions,
        getCategories = getCategories,
        getWalletSummary = getWalletSummary,
        retryPendingSync = retryPendingSync,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )

    private fun personalWallet(id: String) = Wallet(
        id = id,
        ownerId = "u",
        name = "Personal",
        type = WalletType.PERSONAL,
        balance = 0L,
    )
}
