package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class AddTransactionUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val useCase = AddTransactionUseCase(repo)

    @Test
    fun `amount zero returns failure`() = runTest {
        val result = useCase(validTransaction().copy(amount = 0L))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(result.exceptionOrNull()?.message).contains("greater than 0")
        coVerify(exactly = 0) { repo.addTransaction(any()) }
    }

    @Test
    fun `amount negative returns failure`() = runTest {
        val result = useCase(validTransaction().copy(amount = -1L))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("greater than 0")
        coVerify(exactly = 0) { repo.addTransaction(any()) }
    }

    @Test
    fun `blank walletId returns failure`() = runTest {
        val result = useCase(validTransaction().copy(walletId = "  "))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Wallet")
        coVerify(exactly = 0) { repo.addTransaction(any()) }
    }

    @Test
    fun `blank categoryId returns failure`() = runTest {
        val result = useCase(validTransaction().copy(categoryId = ""))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Category")
        coVerify(exactly = 0) { repo.addTransaction(any()) }
    }

    @Test
    fun `valid transaction delegates to repository`() = runTest {
        val transaction = validTransaction()
        coEvery { repo.addTransaction(transaction) } just runs

        val result = useCase(transaction)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { repo.addTransaction(transaction) }
    }

    @Test
    fun `repository exception returns failure`() = runTest {
        coEvery { repo.addTransaction(any()) } throws IllegalStateException("db down")

        val result = useCase(validTransaction())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("db down")
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        coEvery { repo.addTransaction(any()) } throws CancellationException("cancelled")

        try {
            useCase(validTransaction())
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertThat(e.message).isEqualTo("cancelled")
        }
    }

    private fun validTransaction() = Transaction(
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
