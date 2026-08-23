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
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.feature.transaction.presentation.history.TransactionHistoryViewModel
import com.mascill.keutrack.feature.transaction.presentation.model.HistoryPeriodPreset
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
import java.time.LocalDate
import java.time.YearMonth

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
                        params.familyId == "fam-1" &&
                            params.limit == 200 &&
                            params.startDate == null &&
                            params.endDate == null
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
                        params.walletId == "w-p" &&
                            params.limit == 50 &&
                            params.startDate == null &&
                            params.endDate == null
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

    @Test
    fun `last 7 days uses inclusive local date range`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList())
            val vm = createViewModel()
            val range =
                PeriodBounds.ofLocalDates(
                    LocalDate.now().minusDays(6),
                    LocalDate.now(),
                )

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onPeriodPresetSelected(HistoryPeriodPreset.Last7Days)
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.periodPreset).isEqualTo(HistoryPeriodPreset.Last7Days)
                assertThat(state.hasActivePeriodFilter).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.startDate == range.start && params.endDate == range.endInclusive
                    },
                )
            }
        }

    @Test
    fun `all preset uses null date bounds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList())
            val vm = createViewModel()

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onPeriodPresetSelected(HistoryPeriodPreset.Last7Days)
                advanceUntilIdle()
                expectMostRecentItem()
                vm.onPeriodPresetSelected(HistoryPeriodPreset.All)
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.periodPreset).isEqualTo(HistoryPeriodPreset.All)
                assertThat(state.hasActivePeriodFilter).isFalse()
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.startDate == null && params.endDate == null
                    },
                )
            }
        }

    @Test
    fun `custom from after to is rejected`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList())
            val vm = createViewModel()
            val today = LocalDate.now()

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onCustomRangeConfirmed(today, today.minusDays(1))
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.periodPreset).isEqualTo(HistoryPeriodPreset.All)
                assertThat(state.hasActivePeriodFilter).isFalse()
                assertThat(state.periodRangeError).isNotEmpty()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) {
                getTransactions(match { it.startDate != null })
            }
        }

    @Test
    fun `custom range applies inclusive bounds`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList())
            val vm = createViewModel()
            val from = LocalDate.now().minusDays(3)
            val to = LocalDate.now()
            val range = PeriodBounds.ofLocalDates(from, to)

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onCustomRangeConfirmed(from, to)
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.periodPreset).isEqualTo(HistoryPeriodPreset.Custom)
                assertThat(state.customFrom).isEqualTo(from)
                assertThat(state.customTo).isEqualTo(to)
                assertThat(state.hasActivePeriodFilter).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.startDate == range.start && params.endDate == range.endInclusive
                    },
                )
            }
        }

    @Test
    fun `restores custom period from SavedStateHandle`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val from = LocalDate.of(2026, 8, 12)
            val to = LocalDate.of(2026, 8, 20)
            val range = PeriodBounds.ofLocalDates(from, to)
            stub(emptyList())
            val vm =
                createViewModel(
                    extraState =
                        mapOf(
                            "periodPreset" to HistoryPeriodPreset.Custom.name,
                            "customFromEpochDay" to from.toEpochDay(),
                            "customToEpochDay" to to.toEpochDay(),
                        ),
                )

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.periodPreset).isEqualTo(HistoryPeriodPreset.Custom)
                assertThat(state.customFrom).isEqualTo(from)
                assertThat(state.customTo).isEqualTo(to)
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.startDate == range.start && params.endDate == range.endInclusive
                    },
                )
            }
        }

    @Test
    fun `personal current month applies wallet and month range`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stub(emptyList(), personalOnly = true, personalWalletId = "w-p")
            val vm = createViewModel(personalOnly = true)
            val monthRange = PeriodBounds.ofYearMonth(YearMonth.now())

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onPeriodPresetSelected(HistoryPeriodPreset.CurrentMonth)
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match { params ->
                        params.walletId == "w-p" &&
                            params.startDate == monthRange.start &&
                            params.endDate == monthRange.endInclusive &&
                            params.limit == 50
                    },
                )
            }
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
        extraState: Map<String, Any> = emptyMap(),
    ) = TransactionHistoryViewModel(
        savedStateHandle = SavedStateHandle(
            mapOf(
                "familyOnly" to familyOnly,
                "personalOnly" to personalOnly,
            ) + extraState,
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
