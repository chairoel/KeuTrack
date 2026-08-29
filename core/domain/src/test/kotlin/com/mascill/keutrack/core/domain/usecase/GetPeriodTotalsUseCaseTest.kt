package com.mascill.keutrack.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class GetPeriodTotalsUseCaseTest {

    private val repo = mockk<TransactionRepository>()
    private val useCase = GetPeriodTotalsUseCase(repo)

    @Test
    fun `passes scoped params and returns repo flow`() = runTest {
        val totals = PeriodTotals(incomeTotal = 1_000_000L, expenseTotal = 250_000L)
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = Instant.parse("2026-08-31T23:59:59Z")
        every {
            repo.observePeriodTotals(
                walletId = "wallet-1",
                familyId = null,
                startDate = start,
                endDate = end,
            )
        } returns flowOf(totals)

        useCase(
            GetPeriodTotalsUseCase.Params(
                walletId = "wallet-1",
                startDate = start,
                endDate = end,
            ),
        ).test {
            assertThat(awaitItem()).isEqualTo(totals)
            awaitComplete()
        }

        verify(exactly = 1) {
            repo.observePeriodTotals(
                walletId = "wallet-1",
                familyId = null,
                startDate = start,
                endDate = end,
            )
        }
    }

    @Test
    fun `default params observe all-time unscoped totals`() = runTest {
        every {
            repo.observePeriodTotals(
                walletId = null,
                familyId = null,
                startDate = null,
                endDate = null,
            )
        } returns flowOf(PeriodTotals())

        useCase().test {
            assertThat(awaitItem()).isEqualTo(PeriodTotals())
            awaitComplete()
        }
    }

    @Test
    fun `passes familyId without wallet filter`() = runTest {
        every {
            repo.observePeriodTotals(
                walletId = null,
                familyId = "fam-1",
                startDate = null,
                endDate = null,
            )
        } returns flowOf(PeriodTotals(expenseTotal = 80_000L))

        useCase(GetPeriodTotalsUseCase.Params(familyId = "fam-1")).test {
            assertThat(awaitItem().expenseTotal).isEqualTo(80_000L)
            awaitComplete()
        }
    }
}
