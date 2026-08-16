package com.mascill.keutrack.feature.family

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.FamilyGroup
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.CreateFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.GetBudgetProgressUseCase
import com.mascill.keutrack.core.domain.usecase.GetCategoriesUseCase
import com.mascill.keutrack.core.domain.usecase.GetTransactionsUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.JoinFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.SyncFamilyDataUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.family.presentation.FamilyViewModel
import io.mockk.coEvery
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

    private fun stubFamilyMember() {
        every { userRepo.getCurrentUser() } returns flowOf(
            User("user-1", "Irul", "a@b.c", null, familyId = "fam-1", familyRole = "owner"),
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
        every { getWalletSummary() } returns flowOf(WalletSummary(null, emptyList(), 0L, 0L))
        every { getTransactions(any()) } returns flowOf(emptyList())
        every { getBudgetProgress(any()) } returns flowOf(emptyList())
        every { getCategories() } returns flowOf(emptyList())
    }

    private fun createViewModel() = FamilyViewModel(
        userRepository = userRepo,
        familyRepository = familyRepo,
        getWalletSummary = getWalletSummary,
        getTransactions = getTransactions,
        getBudgetProgress = getBudgetProgress,
        getCategories = getCategories,
        createFamilyGroup = createFamilyGroup,
        joinFamilyGroup = joinFamilyGroup,
        syncFamilyData = syncFamilyData,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )
}
