package com.mascill.keutrack.feature.dashboard

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetPeriodTotalsUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.ObservePeriodPreferencesUseCase
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.ObserveWalletUiPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.SetWalletBalanceVisibilityUseCase
import com.mascill.keutrack.core.domain.usecase.SyncFamilyDataUseCase
import com.mascill.keutrack.core.domain.usecase.SyncPersonalDataUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.dashboard.presentation.DashboardViewModel
import com.mascill.keutrack.feature.dashboard.presentation.model.DashboardUIState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val familyRepo = mockk<FamilyRepository>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    private val getTransactions = mockk<GetTransactionsUseCase>()
    private val getPeriodTotals = mockk<GetPeriodTotalsUseCase>()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val observeWalletUiPreferences = mockk<ObserveWalletUiPreferencesUseCase>()
    private val observePeriodPreferences = mockk<ObservePeriodPreferencesUseCase>()
    private val setWalletBalanceVisibility = mockk<SetWalletBalanceVisibilityUseCase>()
    private val retryPendingSync = mockk<RetryPendingSyncUseCase>()
    private val syncPersonalData = mockk<SyncPersonalDataUseCase>()
    private val syncFamilyData = mockk<SyncFamilyDataUseCase>()

    @Test
    fun `initial state is loading`() = runTest(mainDispatcherRule.testDispatcher) {
        stubHappyPath()
        val vm = createViewModel()
        assertThat(vm.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `successful data load emits content state`() = runTest(mainDispatcherRule.testDispatcher) {
        stubHappyPath()
        val vm = createViewModel()

        vm.uiState.test {
            assertThat(awaitItem().isLoading).isTrue()
            advanceUntilIdle()
            val content = awaitItem()
            assertThat(content.isLoading).isFalse()
            assertThat(content.userFirstName).isEqualTo("Irul")
            assertThat(content.personalBalance).isEqualTo(50_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggle personal balance visibility persists hidden state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            coEvery { setWalletBalanceVisibility(any(), any()) } just runs
            val vm = createViewModel()
            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                advanceUntilIdle()
                assertThat(awaitItem().isPersonalBalanceVisible).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            vm.onTogglePersonalBalanceVisibility()
            advanceUntilIdle()

            coVerify {
                setWalletBalanceVisibility(walletType = WalletType.PERSONAL, visible = false)
            }
        }

    @Test
    fun `onScreenRendered pulls personal and family data then retries pending sync`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            val vm = createViewModel()

            vm.onScreenRendered()
            advanceUntilIdle()

            coVerify(exactly = 1) { syncPersonalData() }
            coVerify(exactly = 1) { syncFamilyData() }
            coVerify(exactly = 1) { retryPendingSync() }
        }

    @Test
    fun `onScreenRendered shows personal wallet syncing then clears it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            val pullGate = CompletableDeferred<Unit>()
            coEvery { syncPersonalData() } coAnswers {
                pullGate.await()
                Result.success(Unit)
            }
            val vm = createViewModel()

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                advanceUntilIdle()
                awaitUntil { !it.isLoading }

                vm.onScreenRendered()
                advanceUntilIdle()
                assertThat(awaitUntil { it.isPersonalWalletSyncing }.isPersonalWalletSyncing)
                    .isTrue()

                pullGate.complete(Unit)
                advanceUntilIdle()
                assertThat(awaitUntil { !it.isPersonalWalletSyncing }.isPersonalWalletSyncing)
                    .isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onScreenRendered shows family wallet syncing then clears it`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            val pullGate = CompletableDeferred<Unit>()
            coEvery { syncFamilyData() } coAnswers {
                pullGate.await()
                Result.success(Unit)
            }
            val vm = createViewModel()

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                advanceUntilIdle()
                awaitUntil { !it.isLoading }

                vm.onScreenRendered()
                advanceUntilIdle()
                assertThat(awaitUntil { it.isFamilyWalletSyncing }.isFamilyWalletSyncing).isTrue()

                pullGate.complete(Unit)
                advanceUntilIdle()
                assertThat(awaitUntil { !it.isFamilyWalletSyncing }.isFamilyWalletSyncing).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onScreenRendered still retries push when personal pull fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            coEvery { syncPersonalData() } returns Result.failure(IllegalStateException("offline"))
            val vm = createViewModel()

            vm.onScreenRendered()
            advanceUntilIdle()

            coVerify(exactly = 1) { syncPersonalData() }
            coVerify(exactly = 1) { syncFamilyData() }
            coVerify(exactly = 1) { retryPendingSync() }
        }

    @Test
    fun `onScreenRendered still retries push when family pull fails`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            coEvery { syncFamilyData() } returns Result.failure(IllegalStateException("offline"))
            val vm = createViewModel()

            vm.onScreenRendered()
            advanceUntilIdle()

            coVerify(exactly = 1) { syncPersonalData() }
            coVerify(exactly = 1) { syncFamilyData() }
            coVerify(exactly = 1) { retryPendingSync() }
        }

    @Test
    fun `monthChangeLabel emits from unscoped current versus prior totals`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            every { getPeriodTotals(any()) } returns flowOf(
                PeriodTotals(incomeTotal = 120L, expenseTotal = 0L),
            )
            val vm = createViewModel()

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                advanceUntilIdle()
                val content = awaitItem()
                assertThat(content.monthChangeLabel).isEqualTo("+0,0% this month")
                assertThat(content.incomeTotal).isEqualTo(120L)
                assertThat(content.expenseTotal).isEqualTo(0L)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `period totals use unscoped Params matching current and prior cycle`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            val captured = mutableListOf<GetPeriodTotalsUseCase.Params>()
            every { getPeriodTotals(any()) } answers {
                captured.add(firstArg())
                flowOf(PeriodTotals(incomeTotal = 100L, expenseTotal = 0L))
            }
            val vm = createViewModel()

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                advanceUntilIdle()
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val current = PeriodBounds.containing(LocalDate.now(), 1)
            val prior = current.minusPeriods(1)
            val currentRange = current.toInstantRange()
            val priorRange = prior.toInstantRange()

            assertThat(captured).hasSize(2)
            captured.forEach {
                assertThat(it.walletId).isNull()
                assertThat(it.familyId).isNull()
            }
            assertThat(captured[0].startDate).isEqualTo(currentRange.start)
            assertThat(captured[0].endDate).isEqualTo(currentRange.endInclusive)
            assertThat(captured[1].startDate).isEqualTo(priorRange.start)
            assertThat(captured[1].endDate).isEqualTo(priorRange.endInclusive)
        }

    @Test
    fun `period totals window follows cycle start day 25`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubHappyPath()
            every { observePeriodPreferences() } returns flowOf(
                PeriodPreferences(cycleStartDay = 25),
            )
            val captured = mutableListOf<GetPeriodTotalsUseCase.Params>()
            every { getPeriodTotals(any()) } answers {
                captured.add(firstArg())
                flowOf(PeriodTotals(incomeTotal = 80L, expenseTotal = 0L))
            }
            val vm = createViewModel()

            vm.uiState.test {
                assertThat(awaitItem().isLoading).isTrue()
                advanceUntilIdle()
                val content = awaitItem()
                assertThat(content.monthChangeLabel).isEqualTo("+0,0% periode ini")
                cancelAndIgnoreRemainingEvents()
            }

            val current = PeriodBounds.containing(LocalDate.now(), 25)
            val prior = current.minusPeriods(1)
            assertThat(captured).hasSize(2)
            assertThat(captured[0].startDate).isEqualTo(current.toInstantRange().start)
            assertThat(captured[0].endDate).isEqualTo(current.toInstantRange().endInclusive)
            assertThat(captured[1].startDate).isEqualTo(prior.toInstantRange().start)
            assertThat(captured[1].endDate).isEqualTo(prior.toInstantRange().endInclusive)
        }

    private fun stubHappyPath() {
        every { userRepo.getCurrentUser() } returns flowOf(
            User("user-1", "Irul Amri", "irul@example.com", null),
        )
        every { familyRepo.observeCurrentFamily() } returns flowOf(null)
        every { getWalletSummary() } returns flowOf(
            WalletSummary(
                personalWallet = null,
                familyWallets = emptyList(),
                totalPersonalBalance = 50_000L,
                totalFamilyBalance = 0L,
            ),
        )
        every { getTransactions(any()) } returns flowOf(emptyList())
        every { getPeriodTotals(any()) } returns flowOf(PeriodTotals())
        every { getCategories() } returns flowOf(emptyList())
        every { observeWalletUiPreferences() } returns flowOf(WalletUiPreferences())
        every { observePeriodPreferences() } returns flowOf(PeriodPreferences())
        coEvery { syncPersonalData() } returns Result.success(Unit)
        coEvery { syncFamilyData() } returns Result.success(Unit)
        coEvery { retryPendingSync() } just runs
    }

    private fun createViewModel() = DashboardViewModel(
        userRepository = userRepo,
        familyRepository = familyRepo,
        getWalletSummary = getWalletSummary,
        getTransactions = getTransactions,
        getPeriodTotals = getPeriodTotals,
        getCategories = getCategories,
        observeWalletUiPreferences = observeWalletUiPreferences,
        observePeriodPreferences = observePeriodPreferences,
        setWalletBalanceVisibility = setWalletBalanceVisibility,
        retryPendingSync = retryPendingSync,
        syncPersonalData = syncPersonalData,
        syncFamilyData = syncFamilyData,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )

    private suspend fun ReceiveTurbine<DashboardUIState>.awaitUntil(
        predicate: (DashboardUIState) -> Boolean,
    ): DashboardUIState {
        var item = awaitItem()
        while (!predicate(item)) {
            item = awaitItem()
        }
        return item
    }
}
