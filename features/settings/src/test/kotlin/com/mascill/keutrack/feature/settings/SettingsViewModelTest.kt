package com.mascill.keutrack.feature.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.FamilyRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.usecase.CreateFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.GetWalletSummaryUseCase
import com.mascill.keutrack.core.domain.usecase.JoinFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.LeaveFamilyGroupUseCase
import com.mascill.keutrack.core.domain.usecase.WalletSummary
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.settings.presentation.SettingsViewModel
import com.mascill.keutrack.feature.settings.presentation.model.SignOutState
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
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>(relaxed = true)
    private val familyRepo = mockk<FamilyRepository>()
    private val getWalletSummary = mockk<GetWalletSummaryUseCase>()
    private val createFamilyGroup = mockk<CreateFamilyGroupUseCase>()
    private val joinFamilyGroup = mockk<JoinFamilyGroupUseCase>()
    private val leaveFamilyGroup = mockk<LeaveFamilyGroupUseCase>()

    @Test
    fun `combine emits profile from user`() = runTest(mainDispatcherRule.testDispatcher) {
        stubContent()
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.profile.displayName).isEqualTo("Irul")
            assertThat(state.profile.email).isEqualTo("irul@example.com")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `leave family failure surfaces membership message`() = runTest(mainDispatcherRule.testDispatcher) {
        stubContent()
        coEvery { leaveFamilyGroup() } throws IllegalStateException("Anda belum bergabung dengan keluarga")
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.leaveFamily()
            advanceUntilIdle()
            val state = expectMostRecentItem()
            assertThat(state.membershipMessage).isEqualTo("Anda belum bergabung dengan keluarga")
            assertThat(state.membershipLoading).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `sign out success updates state`() = runTest(mainDispatcherRule.testDispatcher) {
        stubContent()
        coEvery { userRepo.signOut() } just runs
        val vm = createViewModel()

        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.signOut()
            advanceUntilIdle()
            assertThat(expectMostRecentItem().signOutState).isEqualTo(SignOutState.Success)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { userRepo.signOut() }
    }

    @Test
    fun `sheets coming soon sets info message`() = runTest(mainDispatcherRule.testDispatcher) {
        stubContent()
        val vm = createViewModel()
        vm.uiState.test {
            skipItems(1)
            advanceUntilIdle()
            awaitItem()
            vm.onSheetsComingSoon()
            advanceUntilIdle()
            assertThat(expectMostRecentItem().infoMessage).isEqualTo("Segera hadir")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun stubContent() {
        every { userRepo.getCurrentUser() } returns flowOf(
            User("user-1", "Irul", "irul@example.com", null),
        )
        every { familyRepo.observeCurrentFamily() } returns flowOf(null)
        every { getWalletSummary() } returns flowOf(
            WalletSummary(null, emptyList(), 0L, 0L),
        )
        coEvery { userRepo.syncUserProfile() } just runs
    }

    private fun createViewModel() = SettingsViewModel(
        userRepository = userRepo,
        familyRepository = familyRepo,
        getWalletSummary = getWalletSummary,
        createFamilyGroup = createFamilyGroup,
        joinFamilyGroup = joinFamilyGroup,
        leaveFamilyGroup = leaveFamilyGroup,
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
    )
}
