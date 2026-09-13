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
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

class UpdateTransactionUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val userRepo = mockk<UserRepository>()
    private val useCase = UpdateTransactionUseCase(repo, userRepo)

    @Test
    fun `blank id returns MissingId`() = runTest {
        val result = useCase(validTransaction().copy(id = "  "))

        assertThat(result).isEqualTo(TransactionWriteResult.Error.MissingId)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `amount zero returns InvalidAmount`() = runTest {
        val result = useCase(validTransaction().copy(amount = 0L))

        assertThat(result).isEqualTo(TransactionWriteResult.Error.InvalidAmount)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `amount negative returns InvalidAmount`() = runTest {
        val result = useCase(validTransaction().copy(amount = -1L))

        assertThat(result).isEqualTo(TransactionWriteResult.Error.InvalidAmount)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `blank walletId returns MissingWallet`() = runTest {
        val result = useCase(validTransaction().copy(walletId = "  "))

        assertThat(result).isEqualTo(TransactionWriteResult.Error.MissingWallet)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `blank categoryId returns MissingCategory`() = runTest {
        val result = useCase(validTransaction().copy(categoryId = ""))

        assertThat(result).isEqualTo(TransactionWriteResult.Error.MissingCategory)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `missing transaction returns NotFound`() = runTest {
        stubCurrentUser()
        coEvery { repo.getTransactionById("tx-1") } returns null

        val result = useCase(validTransaction())

        assertThat(result).isEqualTo(TransactionWriteResult.Error.NotFound)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `other author returns NotOwner`() = runTest {
        stubCurrentUser()
        val transaction = validTransaction()
        coEvery { repo.getTransactionById(transaction.id) } returns
            transaction.copy(userId = "user-2")

        val result = useCase(transaction)

        assertThat(result).isEqualTo(TransactionWriteResult.Error.NotOwner)
        coVerify(exactly = 0) { repo.updateTransaction(any()) }
    }

    @Test
    fun `valid transaction delegates to repository`() = runTest {
        stubCurrentUser()
        val transaction = validTransaction()
        coEvery { repo.getTransactionById(transaction.id) } returns transaction
        coEvery { repo.updateTransaction(transaction) } just runs

        val result = useCase(transaction)

        assertThat(result).isEqualTo(TransactionWriteResult.Success)
        coVerify(exactly = 1) { repo.updateTransaction(transaction) }
    }

    @Test
    fun `repository exception returns Unknown`() = runTest {
        stubCurrentUser()
        val transaction = validTransaction()
        coEvery { repo.getTransactionById(transaction.id) } returns transaction
        coEvery { repo.updateTransaction(any()) } throws IllegalStateException("db down")

        val result = useCase(transaction)

        assertThat(result).isInstanceOf(TransactionWriteResult.Error.Unknown::class.java)
        assertThat((result as TransactionWriteResult.Error.Unknown).cause.message)
            .isEqualTo("db down")
    }

    @Test
    fun `CancellationException is rethrown`() = runTest {
        stubCurrentUser()
        val transaction = validTransaction()
        coEvery { repo.getTransactionById(transaction.id) } returns transaction
        coEvery { repo.updateTransaction(any()) } throws CancellationException("cancelled")

        try {
            useCase(transaction)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertThat(e.message).isEqualTo("cancelled")
        }
    }

    private fun stubCurrentUser() {
        every { userRepo.getCurrentUser() } returns
            flowOf(User("user-1", "Irul", "irul@example.com", null))
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
