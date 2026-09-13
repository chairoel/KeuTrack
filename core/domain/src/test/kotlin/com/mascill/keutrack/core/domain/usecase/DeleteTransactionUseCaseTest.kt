package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.TransactionRepository
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

class DeleteTransactionUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val userRepo = mockk<UserRepository>()
    private val useCase = DeleteTransactionUseCase(repo, userRepo)

    @Test
    fun `blank id returns MissingId`() = runTest {
        val result = useCase("  ")

        assertThat(result).isEqualTo(TransactionWriteResult.Error.MissingId)
        coVerify(exactly = 0) { repo.deleteTransaction(any()) }
    }

    @Test
    fun `other author returns NotOwner`() = runTest {
        stubCurrentUser()
        coEvery { repo.getTransactionById("tx-1") } returns ownedTransaction().copy(userId = "user-2")

        val result = useCase("tx-1")

        assertThat(result).isEqualTo(TransactionWriteResult.Error.NotOwner)
        coVerify(exactly = 0) { repo.deleteTransaction(any()) }
    }

    @Test
    fun `valid id delegates to repository`() = runTest {
        stubCurrentUser()
        coEvery { repo.getTransactionById("tx-1") } returns ownedTransaction()
        coEvery { repo.deleteTransaction("tx-1") } just runs

        val result = useCase("tx-1")

        assertThat(result).isEqualTo(TransactionWriteResult.Success)
        coVerify(exactly = 1) { repo.deleteTransaction("tx-1") }
    }

    @Test
    fun `repository exception returns Unknown`() = runTest {
        stubCurrentUser()
        coEvery { repo.getTransactionById("tx-1") } returns ownedTransaction()
        coEvery { repo.deleteTransaction(any()) } throws IllegalStateException("db down")

        val result = useCase("tx-1")

        assertThat(result).isInstanceOf(TransactionWriteResult.Error.Unknown::class.java)
        assertThat((result as TransactionWriteResult.Error.Unknown).cause.message)
            .isEqualTo("db down")
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        stubCurrentUser()
        coEvery { repo.getTransactionById("tx-1") } returns ownedTransaction()
        coEvery { repo.deleteTransaction(any()) } throws CancellationException("cancelled")

        try {
            useCase("tx-1")
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertThat(e.message).isEqualTo("cancelled")
        }
    }

    private fun stubCurrentUser() {
        every { userRepo.getCurrentUser() } returns
            flowOf(User("user-1", "Irul", "irul@example.com", null))
    }

    private fun ownedTransaction() = Transaction(
        id = "tx-1",
        walletId = "wallet-1",
        userId = "user-1",
        type = TransactionType.EXPENSE,
        amount = 15_000L,
        categoryId = "cat-food",
        date = java.time.Instant.parse("2026-08-01T00:00:00Z"),
        addedByName = "Irul",
    )
}
