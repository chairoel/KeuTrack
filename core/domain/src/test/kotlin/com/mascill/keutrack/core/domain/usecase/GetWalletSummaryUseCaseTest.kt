package com.mascill.keutrack.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.WalletRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class GetWalletSummaryUseCaseTest {

    private val walletRepo = mockk<WalletRepository>()
    private val useCase = GetWalletSummaryUseCase(walletRepo)

    @Test
    fun `returns personal and family wallets`() = runTest {
        val personal = wallet("w-p", WalletType.PERSONAL, 50_000L)
        val family = wallet("w-f", WalletType.FAMILY, 20_000L, familyId = "fam-1")
        every { walletRepo.observeWallets() } returns flowOf(listOf(personal, family))

        useCase().test {
            val summary = awaitItem()
            assertThat(summary.personalWallet).isEqualTo(personal)
            assertThat(summary.familyWallets).containsExactly(family)
            awaitComplete()
        }
    }

    @Test
    fun `empty wallets returns empty summary`() = runTest {
        every { walletRepo.observeWallets() } returns flowOf(emptyList())

        useCase().test {
            val summary = awaitItem()
            assertThat(summary.personalWallet).isNull()
            assertThat(summary.familyWallets).isEmpty()
            assertThat(summary.totalPersonalBalance).isEqualTo(0L)
            assertThat(summary.totalFamilyBalance).isEqualTo(0L)
            awaitComplete()
        }
    }

    @Test
    fun `calculates total balances correctly`() = runTest {
        val personalA = wallet("w-p1", WalletType.PERSONAL, 10_000L)
        val personalB = wallet("w-p2", WalletType.PERSONAL, 5_000L)
        val familyA = wallet("w-f1", WalletType.FAMILY, 7_000L, familyId = "fam-1")
        val familyB = wallet("w-f2", WalletType.FAMILY, 3_000L, familyId = "fam-2")
        every { walletRepo.observeWallets() } returns flowOf(
            listOf(personalA, personalB, familyA, familyB),
        )

        useCase().test {
            val summary = awaitItem()
            assertThat(summary.personalWallet).isEqualTo(personalA)
            assertThat(summary.totalPersonalBalance).isEqualTo(15_000L)
            assertThat(summary.totalFamilyBalance).isEqualTo(10_000L)
            awaitComplete()
        }
    }

    private fun wallet(
        id: String,
        type: WalletType,
        balance: Long,
        familyId: String? = null,
    ) = Wallet(
        id = id,
        ownerId = "user-1",
        familyId = familyId,
        name = id,
        type = type,
        balance = balance,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )
}
