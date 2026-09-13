package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class GetTransactionByIdUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val useCase = GetTransactionByIdUseCase(repo)

    @Test
    fun `returns transaction when found`() = runTest {
        val transaction = sampleTransaction()
        coEvery { repo.getTransactionById("tx-1") } returns transaction

        val result = useCase("tx-1")

        assertThat(result).isEqualTo(transaction)
        coVerify(exactly = 1) { repo.getTransactionById("tx-1") }
    }

    @Test
    fun `returns null when not found`() = runTest {
        coEvery { repo.getTransactionById("missing") } returns null

        val result = useCase("missing")

        assertThat(result).isNull()
        coVerify(exactly = 1) { repo.getTransactionById("missing") }
    }

    @Test
    fun `blank id returns null without calling repository`() = runTest {
        val result = useCase("  ")

        assertThat(result).isNull()
        coVerify(exactly = 0) { repo.getTransactionById(any()) }
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { repo.getTransactionById(any()) } throws CancellationException("cancelled")

        try {
            useCase("tx-1")
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertThat(e.message).isEqualTo("cancelled")
        }
    }

    private fun sampleTransaction() = Transaction(
        id = "tx-1",
        walletId = "wallet-1",
        userId = "user-1",
        type = TransactionType.EXPENSE,
        amount = 15_000L,
        categoryId = "cat-food",
        date = Instant.parse("2026-08-01T00:00:00Z"),
        addedByName = "Irul",
    )
}
