package com.mascill.keutrack.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class GetTransactionsUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val useCase = GetTransactionsUseCase(repo)

    @Test
    fun `passes limit param and returns repo flow`() = runTest {
        val transactions = listOf(sampleTransaction())
        every {
            repo.observeTransactions(
                walletId = "wallet-1",
                familyId = null,
                type = TransactionType.EXPENSE,
                categoryId = null,
                startDate = null,
                endDate = null,
                limit = 10,
                userId = null,
            )
        } returns flowOf(transactions)

        useCase(
            GetTransactionsUseCase.Params(
                walletId = "wallet-1",
                type = TransactionType.EXPENSE,
                limit = 10,
            ),
        ).test {
            assertThat(awaitItem()).isEqualTo(transactions)
            awaitComplete()
        }

        verify(exactly = 1) {
            repo.observeTransactions(
                walletId = "wallet-1",
                familyId = null,
                type = TransactionType.EXPENSE,
                categoryId = null,
                startDate = null,
                endDate = null,
                limit = 10,
                userId = null,
            )
        }
    }

    @Test
    fun `default params delegate with limit 50 and null userId`() = runTest {
        every {
            repo.observeTransactions(
                walletId = null,
                familyId = null,
                type = null,
                categoryId = null,
                startDate = null,
                endDate = null,
                limit = 50,
                userId = null,
            )
        } returns flowOf(emptyList())

        useCase().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }

        verify(exactly = 1) {
            repo.observeTransactions(
                walletId = null,
                familyId = null,
                type = null,
                categoryId = null,
                startDate = null,
                endDate = null,
                limit = 50,
                userId = null,
            )
        }
    }

    @Test
    fun `forwards userId to repository`() = runTest {
        every {
            repo.observeTransactions(
                walletId = "wallet-1",
                familyId = "fam-1",
                type = null,
                categoryId = null,
                startDate = null,
                endDate = null,
                limit = 50,
                userId = "u-2",
            )
        } returns flowOf(emptyList())

        useCase(
            GetTransactionsUseCase.Params(
                walletId = "wallet-1",
                familyId = "fam-1",
                userId = "u-2",
            ),
        ).test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }

        verify(exactly = 1) {
            repo.observeTransactions(
                walletId = "wallet-1",
                familyId = "fam-1",
                type = null,
                categoryId = null,
                startDate = null,
                endDate = null,
                limit = 50,
                userId = "u-2",
            )
        }
    }

    private fun sampleTransaction() = Transaction(
        id = "tx-1",
        walletId = "wallet-1",
        userId = "user-1",
        type = TransactionType.EXPENSE,
        amount = 5_000L,
        categoryId = "cat-1",
        date = Instant.parse("2026-08-01T00:00:00Z"),
        addedByName = "Irul",
    )
}
