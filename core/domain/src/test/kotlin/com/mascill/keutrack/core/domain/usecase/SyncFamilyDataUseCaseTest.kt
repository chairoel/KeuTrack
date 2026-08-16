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

class SyncFamilyDataUseCaseTest {

    private val userRepo = mockk<UserRepository>()
    private val syncRepo = mockk<SyncRepository>()
    private val useCase = SyncFamilyDataUseCase(userRepo, syncRepo)

    @Test
    fun `explicit familyId pulls family data`() = runTest {
        coEvery { syncRepo.syncFamilyData("fam-1") } just runs

        val result = useCase("fam-1")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { syncRepo.syncFamilyData("fam-1") }
    }

    @Test
    fun `blank familyId falls back to current user family`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(
            User(
                uid = "user-1",
                displayName = "Irul",
                email = "irul@example.com",
                photoUrl = null,
                familyId = "fam-from-user",
            ),
        )
        coEvery { syncRepo.syncFamilyData("fam-from-user") } just runs

        val result = useCase("  ")

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { syncRepo.syncFamilyData("fam-from-user") }
    }

    @Test
    fun `no family id is a successful no-op`() = runTest {
        every { userRepo.getCurrentUser() } returns flowOf(
            User(
                uid = "user-1",
                displayName = "Irul",
                email = "irul@example.com",
                photoUrl = null,
                familyId = null,
            ),
        )

        val result = useCase()

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 0) { syncRepo.syncFamilyData(any()) }
    }

    @Test
    fun `repository exception returns failure`() = runTest {
        coEvery { syncRepo.syncFamilyData("fam-1") } throws IllegalStateException("network")

        val result = useCase("fam-1")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("network")
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { syncRepo.syncFamilyData("fam-1") } throws CancellationException("cancelled")

        try {
            useCase("fam-1")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
