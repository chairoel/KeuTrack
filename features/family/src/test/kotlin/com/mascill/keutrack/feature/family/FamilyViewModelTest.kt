package com.mascill.keutrack.feature.family

import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
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
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.family.presentation.FamilyViewModel
import com.mascill.keutrack.feature.family.presentation.model.FamilyUiMapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val familyRepo = mockk<FamilyRepository>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    private val getTransactions = mockk<GetTransactionsUseCase>()
    private val getBudgetProgress = mockk<GetBudgetProgressUseCase>()
    private val getCategories = mockk<GetCategoriesUseCase>()
    private val createFamilyGroup = mockk<CreateFamilyGroupUseCase>()
    private val joinFamilyGroup = mockk<JoinFamilyGroupUseCase>()
    private val syncFamilyData = mockk<SyncFamilyDataUseCase>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val upsertFamilyBudget =
        UpsertFamilyBudgetUseCase(
            userRepository = userRepo,
            budgetRepository = budgetRepository,
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
        )
    private val deleteFamilyBudget =
        DeleteFamilyBudgetUseCase(
            userRepository = userRepo,
            budgetRepository = budgetRepository,
        )

    @Test
    fun `family and user combine into content state`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFamilyMember()
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.showJoinBanner).isFalse()
            assertThat(state.familyName).isEqualTo("Keluarga Irul")
            assertThat(state.inviteCode).isEqualTo("KEU-ABC-DEF")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `user without family shows join banner`() = runTest(mainDispatcherRule.testDispatcher) {
        every { userRepo.getCurrentUser() } returns flowOf(
            User("user-1", "Irul", "a@b.c", null),
        )
        every { familyRepo.observeCurrentFamily() } returns flowOf(null)
        every { getWalletSummary() } returns flowOf(WalletSummary(null, emptyList(), 0L, 0L))
        every { getBudgetProgress(any()) } returns flowOf(emptyList())
        every { getCategories() } returns flowOf(emptyList())
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.showJoinBanner).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `create family failure surfaces membership message`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFamilyMember()
        coEvery { createFamilyGroup("Baru") } throws IllegalStateException("Nama keluarga minimal 2 karakter")
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.createFamily("Baru")
            advanceUntilIdle()
            assertThat(expectMostRecentItem().membershipMessage)
                .isEqualTo("Nama keluarga minimal 2 karakter")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `owner save invokes upsert family budget and closes sheet`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFamilyOwner()
            stubFamilyBudgetWrites()
            val vm = createViewModel()

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                assertThat(awaitItem().canEditBudgets).isTrue()

                vm.onAdjustTargetsClick()
                vm.onSheetCategorySelected("cat_food")
                vm.onLimitChanged("1000000")
                advanceUntilIdle()
                assertThat(expectMostRecentItem().budgetSheet?.limitInput).isEqualTo("1000000")

                vm.onSaveBudget()
                advanceUntilIdle()
                val saved = expectMostRecentItem()
                assertThat(saved.budgetMessage).isNull()
                assertThat(saved.budgetSheet).isNull()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify {
                budgetRepository.createBudget(
                    match {
                        it.categoryId == "cat_food" &&
                            it.limit == 1_000_000L &&
                            it.month == YearMonth.now().toString() &&
                            it.familyId == "fam-1"
                    },
                )
            }
        }

    @Test
    fun `member click does not open budget sheet`() = runTest(mainDispatcherRule.testDispatcher) {
        stubFamilyUser(role = "member", wallets = listOf(familyWallet()))
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.canEditBudgets).isFalse()
            vm.onAdjustTargetsClick()
            vm.onBudgetRowClick("cat_food")
            expectNoEvents()
            assertThat(state.budgetSheet).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `owner row click prefills existing family budget`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFamilyOwner(
                budgets = listOf(sampleBudget(limit = 2_000_000L)),
            )
            val vm = createViewModel()

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onBudgetRowClick("cat_food")
                val sheet = awaitItem().budgetSheet
                assertThat(sheet?.categoryId).isEqualTo("cat_food")
                assertThat(sheet?.categoryLocked).isTrue()
                assertThat(sheet?.limitInput).isEqualTo("2000000")
                assertThat(sheet?.existingBudgetId).isEqualTo("b-1")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onPreviousMonth queries prior month range and budgets`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFamilyMember()
            val vm = createViewModel()
            val prior = YearMonth.now().minusMonths(1)
            val twoBack = prior.minusMonths(1)
            val priorRange = PeriodBounds.ofYearMonth(prior)
            val twoBackRange = PeriodBounds.ofYearMonth(twoBack)

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                awaitItem()
                vm.onPreviousMonth()
                advanceUntilIdle()
                val state = expectMostRecentItem()
                assertThat(state.selectedMonthLabel)
                    .isEqualTo(FamilyUiMapper.formatBudgetMonth(prior))
                assertThat(state.canSelectNextMonth).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            verify {
                getTransactions(
                    match {
                        it.familyId == "fam-1" &&
                            it.startDate == priorRange.start &&
                            it.endDate == priorRange.endInclusive
                    },
                )
            }
            verify {
                getTransactions(
                    match {
                        it.familyId == "fam-1" &&
                            it.startDate == twoBackRange.start &&
                            it.endDate == twoBackRange.endInclusive
                    },
                )
            }
            verify { getBudgetProgress(prior.toString()) }
        }

    @Test
    fun `onNextMonth from current month does not advance`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFamilyMember()
            val vm = createViewModel()
            val nowLabel = FamilyUiMapper.formatBudgetMonth(YearMonth.now())

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.selectedMonthLabel).isEqualTo(nowLabel)
                assertThat(state.canSelectNextMonth).isFalse()
                vm.onNextMonth()
                advanceUntilIdle()
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }

            verify(exactly = 0) {
                getBudgetProgress(YearMonth.now().plusMonths(1).toString())
            }
        }

    @Test
    fun `owner cannot open budget sheet or save on a past month`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubFamilyOwner()
            stubFamilyBudgetWrites()
            val vm = createViewModel()

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                assertThat(awaitItem().canEditBudgets).isTrue()
                vm.onPreviousMonth()
                advanceUntilIdle()
                val past = expectMostRecentItem()
                assertThat(past.canEditBudgets).isFalse()
                vm.onAdjustTargetsClick()
                vm.onBudgetRowClick("cat_food")
                expectNoEvents()
                assertThat(vm.uiState.value.budgetSheet).isNull()
                vm.onSaveBudget()
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            coVerify(exactly = 0) { budgetRepository.createBudget(any()) }
        }

    @Test
    fun `restores selected month from SavedStateHandle`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val prior = YearMonth.now().minusMonths(1)
            stubFamilyMember()
            val vm =
                createViewModel(
                    SavedStateHandle(mapOf("selectedMonth" to prior.toString())),
                )

            vm.uiState.test {
                skipItems(1)
                advanceUntilIdle()
                val state = awaitItem()
                assertThat(state.selectedMonthLabel)
                    .isEqualTo(FamilyUiMapper.formatBudgetMonth(prior))
                assertThat(state.canSelectNextMonth).isTrue()
                cancelAndIgnoreRemainingEvents()
            }

            verify { getBudgetProgress(prior.toString()) }
        }

    private fun stubFamilyMember(role: String = "owner") {
        stubFamilyUser(role = role, wallets = emptyList())
    }

    private fun stubFamilyOwner(budgets: List<Budget> = emptyList()) {
        stubFamilyUser(
            role = "owner",
            wallets = listOf(familyWallet()),
            budgets = budgets,
        )
    }

    private fun stubFamilyUser(
        role: String,
        wallets: List<Wallet>,
        budgets: List<Budget> = emptyList(),
    ) {
        every { userRepo.getCurrentUser() } returns flowOf(
            User("user-1", "Irul", "a@b.c", null, familyId = "fam-1", familyRole = role),
        )
        every { familyRepo.observeCurrentFamily() } returns flowOf(
            FamilyGroup(
                id = "fam-1",
                name = "Keluarga Irul",
                inviteCode = "KEU-ABC-DEF",
                ownerId = "user-1",
                memberIds = listOf("user-1"),
                createdAt = Instant.parse("2026-08-01T00:00:00Z"),
            ),
        )
        every { getWalletSummary() } returns flowOf(WalletSummary(null, wallets, 0L, 0L))
        every { getTransactions(any()) } returns flowOf(emptyList())
        every { getBudgetProgress(any()) } returns flowOf(budgets)
        every { getCategories() } returns flowOf(emptyList())
    }

    private fun stubFamilyBudgetWrites() {
        every { walletRepository.observeWalletsByType(WalletType.FAMILY) } returns
            flowOf(listOf(familyWallet()))
        every {
            transactionRepository.observeTransactions(
                walletId = null,
                familyId = "fam-1",
                type = TransactionType.EXPENSE,
                categoryId = "cat_food",
                startDate = null,
                endDate = null,
                limit = 1_000,
            )
        } returns flowOf(emptyList())
        coEvery { budgetRepository.findFamilyBudget(any(), any(), any()) } returns null
        coEvery { budgetRepository.createBudget(any()) } just runs
    }

    private fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = FamilyViewModel(
        savedStateHandle = savedStateHandle,
        userRepository = userRepo,
        familyRepository = familyRepo,
        getWalletSummary = getWalletSummary,
        getTransactions = getTransactions,
        getBudgetProgress = getBudgetProgress,
        getCategories = getCategories,
        createFamilyGroup = createFamilyGroup,
        joinFamilyGroup = joinFamilyGroup,
        syncFamilyData = syncFamilyData,
        upsertFamilyBudget = upsertFamilyBudget,
        deleteFamilyBudget = deleteFamilyBudget,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )

    private fun familyWallet() =
        Wallet(
            id = "w-fam",
            ownerId = "user-1",
            familyId = "fam-1",
            name = "Family",
            type = WalletType.FAMILY,
            balance = 0L,
        )

    private fun sampleBudget(limit: Long = 1_000_000L) =
        Budget(
            id = "b-1",
            userId = "user-1",
            familyId = "fam-1",
            categoryId = "cat_food",
            limit = limit,
            spent = 100_000L,
            month = YearMonth.now().toString(),
        )
}
