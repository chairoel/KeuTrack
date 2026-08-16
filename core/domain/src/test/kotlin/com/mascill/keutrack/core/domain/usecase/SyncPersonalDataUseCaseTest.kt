package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.SyncRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class SyncPersonalDataUseCaseTest {

    private val userRepo = mockk<UserRepository>()
    private val syncRepo = mockk<SyncRepository>()
    private val useCase = SyncPersonalDataUseCase(userRepo, syncRepo)

    @Test
    fun `explicit userId pulls personal data`() = runTest {
        coEvery { syncRepo.syncPersonalData("user-1") } just runs

        val result = useCase("user-1")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { syncRepo.syncPersonalData("user-1") }
    }

    @Test
    fun `blank userId falls back to current user`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(
            User(
                uid = "user-from-session",
                displayName = "Irul",
                email = "irul@example.com",
                photoUrl = null,
            ),
        )
        coEvery { syncRepo.syncPersonalData("user-from-session") } just runs

        val result = useCase("  ")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { syncRepo.syncPersonalData("user-from-session") }
    }

    @Test
    fun `no current user is a successful no-op`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(null)

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { syncRepo.syncPersonalData(any()) }
    }

    @Test
    fun `repository exception returns failure`() = runTest {
        coEvery { syncRepo.syncPersonalData("user-1") } throws IllegalStateException("network")

        val result = useCase("user-1")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("network")
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { syncRepo.syncPersonalData("user-1") } throws CancellationException("cancelled")

        try {
            useCase("user-1")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
