package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.repository.SyncRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class RetryPendingSyncUseCaseTest {

    private val syncRepo = mockk<SyncRepository>(relaxed = true)
    private val useCase = RetryPendingSyncUseCase(syncRepo)

    @Test
    fun `enqueues sync when pending items exist`() = runTest {
        coEvery { syncRepo.hasPendingSync() } returns true

        useCase()

        verify(exactly = 1) { syncRepo.enqueuePendingSync(force = true) }
    }

    @Test
    fun `no-ops when nothing is pending`() = runTest {
        coEvery { syncRepo.hasPendingSync() } returns false

        useCase()

        verify(exactly = 0) { syncRepo.enqueuePendingSync(any()) }
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { syncRepo.hasPendingSync() } throws CancellationException("cancelled")

        try {
            useCase()
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }
}
