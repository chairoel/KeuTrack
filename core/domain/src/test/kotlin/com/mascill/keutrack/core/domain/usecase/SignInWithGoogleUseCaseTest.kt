package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.AuthResult
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SignInWithGoogleUseCaseTest {

    private val userRepo = mockk<UserRepository>()
    private val useCase = SignInWithGoogleUseCase(userRepo)

    @Test
    fun `delegates id token to user repository`() = runTest {
        val user = User(
            uid = "user-1",
            displayName = "Irul",
            email = "irul@example.com",
            photoUrl = null,
        )
        coEvery { userRepo.signInWithGoogle("id-token") } returns AuthResult.Success(user)

        val result = useCase("id-token")

        assertThat(result).isEqualTo(AuthResult.Success(user))
        coVerify(exactly = 1) { userRepo.signInWithGoogle("id-token") }
    }

    @Test
    fun `propagates network error`() = runTest {
        coEvery { userRepo.signInWithGoogle(any()) } returns AuthResult.Error.Network

        val result = useCase("id-token")

        assertThat(result).isEqualTo(AuthResult.Error.Network)
    }

    @Test
    fun `propagates repository exception`() = runTest {
        coEvery { userRepo.signInWithGoogle(any()) } throws IllegalStateException("auth down")

        try {
            useCase("id-token")
            org.junit.Assert.fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertThat(e.message).isEqualTo("auth down")
        }
    }
}
