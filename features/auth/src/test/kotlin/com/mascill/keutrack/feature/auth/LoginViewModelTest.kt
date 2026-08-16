package com.mascill.keutrack.feature.auth

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.auth.data.GoogleSignInTokenProvider
import com.mascill.keutrack.feature.auth.presentation.LoginViewModel
import com.mascill.keutrack.feature.auth.presentation.model.AuthMethod
import com.mascill.keutrack.feature.auth.presentation.model.AuthState
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val tokenProvider = mockk<GoogleSignInTokenProvider>()

    @Test
    fun `initial auth state is idle`() = runTest(mainDispatcherRule.testDispatcher) {
        val vm = createViewModel()
        assertThat(vm.authUIState.value.authState).isEqualTo(AuthState.Idle)
    }

    @Test
    fun `email sign-in success emits Success`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { userRepo.signInWithEmail("a@b.c", "secret") } returns AuthResult.Success(user())
        val vm = createViewModel()

        vm.authUIState.test {
            skipItems(1)
            vm.signInWithEmail("a@b.c", "secret")
            advanceUntilIdle()
            assertThat(expectMostRecentItem().authState).isEqualTo(AuthState.Success(AuthMethod.Email))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invalid credential maps to error message`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { userRepo.signInWithEmail(any(), any()) } returns AuthResult.Error.InvalidCredential
        val vm = createViewModel()

        vm.authUIState.test {
            skipItems(1)
            vm.signInWithEmail("a@b.c", "bad")
            advanceUntilIdle()
            val error = expectMostRecentItem()
            assertThat(error.authState).isEqualTo(AuthState.Error("Invalid email or password."))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `network error maps to connection message`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery { userRepo.signInWithEmail(any(), any()) } returns AuthResult.Error.Network
        val vm = createViewModel()

        vm.authUIState.test {
            skipItems(1)
            vm.signInWithEmail("a@b.c", "secret")
            advanceUntilIdle()
            assertThat(expectMostRecentItem().authState)
                .isEqualTo(AuthState.Error("No internet connection. Please try again."))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel() = LoginViewModel(
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
        googleSignInTokenProvider = tokenProvider,
        userRepository = userRepo,
        environment = "dev",
    )

    private fun user() = User("u", "Irul", "a@b.c", null)
}
