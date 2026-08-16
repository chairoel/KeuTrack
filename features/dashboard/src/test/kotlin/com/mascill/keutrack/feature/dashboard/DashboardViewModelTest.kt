package com.mascill.keutrack.feature.dashboard

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetMonthlySummaryUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.MonthlySummaryResult
import com.mascill.keutrack.core.domain.usecase.ObserveWalletUiPreferencesUseCase
import com.mascill.keutrack.core.domain.usecase.RetryPendingSyncUseCase
import com.mascill.keutrack.core.domain.usecase.SetWalletBalanceVisibilityUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.dashboard.presentation.DashboardViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val familyRepo = mockk<FamilyRepository>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    private val getTransactions = mockk<GetTransactionsUseCase>()
    private val getMonthlySummary = mockk<GetMonthlySummaryUseCase>()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val observeWalletUiPreferences = mockk<ObserveWalletUiPreferencesUseCase>()
    private val setWalletBalanceVisibility = mockk<SetWalletBalanceVisibilityUseCase>()
    private val retryPendingSync = mockk<RetryPendingSyncUseCase>()

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
    fun `onScreenRendered retries pending sync`() = runTest(mainDispatcherRule.testDispatcher) {
        stubHappyPath()
        coEvery { retryPendingSync() } just runs
        val vm = createViewModel()

        vm.onScreenRendered()
        advanceUntilIdle()

        coVerify(exactly = 1) { retryPendingSync() }
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
        every { getMonthlySummary(any(), any()) } returns flowOf(
            MonthlySummaryResult(currentMonth = null, trend = emptyList()),
        )
        every { getCategories() } returns flowOf(emptyList())
        every { observeWalletUiPreferences() } returns flowOf(WalletUiPreferences())
    }

    private fun createViewModel() = DashboardViewModel(
        userRepository = userRepo,
        familyRepository = familyRepo,
        getWalletSummary = getWalletSummary,
        getTransactions = getTransactions,
        getMonthlySummary = getMonthlySummary,
        getCategories = getCategories,
        observeWalletUiPreferences = observeWalletUiPreferences,
        setWalletBalanceVisibility = setWalletBalanceVisibility,
        retryPendingSync = retryPendingSync,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )
}
