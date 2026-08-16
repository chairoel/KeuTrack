package com.mascill.keutrack.feature.auth

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.testing.MainDispatcherRule
import com.mascill.keutrack.core.testing.testCommonDispatcher
import com.mascill.keutrack.feature.auth.data.GoogleSignInTokenProvider
import com.mascill.keutrack.feature.auth.presentation.RegisterViewModel
import com.mascill.keutrack.feature.auth.presentation.model.AuthMethod
import com.mascill.keutrack.feature.auth.presentation.model.AuthState
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userRepo = mockk<UserRepository>()
    private val tokenProvider = mockk<GoogleSignInTokenProvider>()

    @Test
    fun `register success emits Success`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery {
            userRepo.registerWithEmail("Irul", "a@b.c", "secret")
        } returns AuthResult.Success(User("u", "Irul", "a@b.c", null))
        val vm = createViewModel()

        vm.authUIState.test {
            skipItems(1)
            vm.registerWithEmail("Irul", "a@b.c", "secret")
            advanceUntilIdle()
            assertThat(expectMostRecentItem().authState).isEqualTo(AuthState.Success(AuthMethod.Email))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `duplicate email maps to invalid credential error`() = runTest(mainDispatcherRule.testDispatcher) {
        coEvery {
            userRepo.registerWithEmail(any(), any(), any())
        } returns AuthResult.Error.InvalidCredential
        val vm = createViewModel()

        vm.authUIState.test {
            skipItems(1)
            vm.registerWithEmail("Irul", "a@b.c", "secret")
            advanceUntilIdle()
            assertThat(expectMostRecentItem().authState).isEqualTo(
                AuthState.Error(
                    "Unable to create account. Email may already be in use or password is too weak.",
                ),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel() = RegisterViewModel(
        dispatcher = testCommonDispatcher(mainDispatcherRule.testDispatcher),
        googleSignInTokenProvider = tokenProvider,
        userRepository = userRepo,
        environment = "dev",
    )
}
