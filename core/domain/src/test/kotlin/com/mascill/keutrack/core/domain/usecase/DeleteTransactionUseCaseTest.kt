package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class DeleteTransactionUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val useCase = DeleteTransactionUseCase(repo)

    @Test
    fun `blank id returns MissingId`() = runTest {
        val result = useCase("  ")

        assertThat(result).isEqualTo(TransactionWriteResult.Error.MissingId)
        coVerify(exactly = 0) { repo.deleteTransaction(any()) }
    }

    @Test
    fun `valid id delegates to repository`() = runTest {
        coEvery { repo.deleteTransaction("tx-1") } just runs

        val result = useCase("tx-1")

        assertThat(result).isEqualTo(TransactionWriteResult.Success)
        coVerify(exactly = 1) { repo.deleteTransaction("tx-1") }
    }

    @Test
    fun `repository exception returns Unknown`() = runTest {
        coEvery { repo.deleteTransaction(any()) } throws IllegalStateException("db down")

        val result = useCase("tx-1")

        assertThat(result).isInstanceOf(TransactionWriteResult.Error.Unknown::class.java)
        assertThat((result as TransactionWriteResult.Error.Unknown).cause.message)
            .isEqualTo("db down")
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { repo.deleteTransaction(any()) } throws CancellationException("cancelled")

        try {
            useCase("tx-1")
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertThat(e.message).isEqualTo("cancelled")
        }
    }
}
