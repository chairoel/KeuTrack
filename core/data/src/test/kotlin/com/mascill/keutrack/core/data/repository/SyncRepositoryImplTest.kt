package com.mascill.keutrack.core.data.repository

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.datasource.firestore.BudgetFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.firestore.CategorySummaryFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.firestore.TransactionFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.firestore.WalletFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSource
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.data.mapper.BudgetMapper
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.mapper.TransactionMapper
import com.mascill.keutrack.core.data.mapper.WalletMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class SyncRepositoryImplTest {

    private val transactionLocal = mockk<TransactionLocalDataSource>(relaxed = true)
    private val walletLocal = mockk<WalletLocalDataSource>(relaxed = true)
    private val budgetLocal = mockk<BudgetLocalDataSource>(relaxed = true)
    private val summaryLocal = mockk<CategorySummaryLocalDataSource>(relaxed = true)
    private val transactionRemote = mockk<TransactionFirestoreDataSource>(relaxed = true)
    private val walletRemote = mockk<WalletFirestoreDataSource>(relaxed = true)
    private val budgetRemote = mockk<BudgetFirestoreDataSource>(relaxed = true)
    private val summaryRemote = mockk<CategorySummaryFirestoreDataSource>(relaxed = true)
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val repo = SyncRepositoryImpl(
        transactionLocal = transactionLocal,
        walletLocal = walletLocal,
        budgetLocal = budgetLocal,
        summaryLocal = summaryLocal,
        transactionRemote = transactionRemote,
        walletRemote = walletRemote,
        budgetRemote = budgetRemote,
        summaryRemote = summaryRemote,
        transactionMapper = TransactionMapper(),
        walletMapper = WalletMapper(),
        budgetMapper = BudgetMapper(),
        summaryMapper = CategorySummaryMapper(),
        syncScheduler = syncScheduler,
    )

    @Test
    fun `hasPendingSync is true when any local item is pending`() = runTest {
        coEvery { walletLocal.getPending() } returns listOf(pendingWallet())
        coEvery { budgetLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()

        assertThat(repo.hasPendingSync()).isTrue()
    }

    @Test
    fun `hasPendingSync is false when queues are empty`() = runTest {
        coEvery { walletLocal.getPending() } returns emptyList()
        coEvery { budgetLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()

        assertThat(repo.hasPendingSync()).isFalse()
    }

    @Test
    fun `enqueuePendingSync delegates to scheduler`() {
        repo.enqueuePendingSync(force = true)
        verify { syncScheduler.enqueueSync(force = true) }
    }

    @Test
    fun `syncPendingWallets marks synced after remote upsert`() = runTest {
        val pending = pendingWallet()
        coEvery { walletLocal.getPending() } returns listOf(pending)
        coEvery { walletRemote.upsertWallet(any()) } just runs
        coEvery { walletLocal.updateSyncStatus(any(), any()) } just runs

        repo.syncPendingWallets()

        coVerify { walletRemote.upsertWallet(match { it.id == "w-1" }) }
        coVerify { walletLocal.updateSyncStatus("w-1", SyncStatus.SYNCED) }
    }

    @Test
    fun `syncPendingWallets marks failed and throws when remote fails`() = runTest {
        coEvery { walletLocal.getPending() } returns listOf(pendingWallet())
        coEvery { walletRemote.upsertWallet(any()) } throws IllegalStateException("offline")

        try {
            repo.syncPendingWallets()
            org.junit.Assert.fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("wallets failed")
        }
        coVerify { walletLocal.updateSyncStatus("w-1", SyncStatus.FAILED) }
    }

    @Test
    fun `syncPendingWallets keeps pending when wallet still has pending transactions`() =
        runTest {
            coEvery { walletLocal.getPending() } returns listOf(pendingWallet())
            coEvery { transactionLocal.getPending() } returns listOf(pendingTransaction())
            coEvery { walletRemote.upsertWallet(any()) } just runs

            repo.syncPendingWallets()

            coVerify { walletRemote.upsertWallet(match { it.id == "w-1" }) }
            coVerify(exactly = 0) { walletLocal.updateSyncStatus("w-1", SyncStatus.SYNCED) }
        }

    @Test
    fun `syncFamilyData repairs doubled remote wallet balance from transactions`() = runTest {
        val remoteWallet = familyWallet(balance = 22_000_000L)
        val remoteTx = familyIncome(amount = 11_000_000L)
        coEvery { walletRemote.getByFamilyId("fam-1") } returns listOf(remoteWallet)
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns listOf(remoteTx)
        coEvery { transactionLocal.getPending() } returns emptyList()
        coEvery { walletLocal.getById("w-fam") } returns null
        coEvery { walletLocal.getByFamilyId("fam-1") } returns emptyList()
        coEvery { transactionLocal.getById(any()) } returns null
        coEvery { walletRemote.setBalance(any(), any()) } just runs

        repo.syncFamilyData("fam-1")

        coVerify {
            walletLocal.upsert(
                match { entity ->
                    entity.id == "w-fam" && entity.balance == 11_000_000L
                },
            )
        }
        coVerify { walletRemote.setBalance("w-fam", 11_000_000L) }
    }

    @Test
    fun `syncFamilyData skips wallet overwrite when local transactions are pending`() = runTest {
        coEvery { walletRemote.getByFamilyId("fam-1") } returns
            listOf(familyWallet(balance = 0L))
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns emptyList()
        coEvery { transactionLocal.getPending() } returns listOf(pendingTransaction(walletId = "w-fam"))
        coEvery { walletLocal.getById("w-fam") } returns null
        coEvery { walletLocal.getByFamilyId("fam-1") } returns emptyList()

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) { walletLocal.upsert(any()) }
        coVerify(exactly = 0) { walletRemote.setBalance(any(), any()) }
    }

    private fun pendingWallet() = WalletEntity(
        id = "w-1",
        ownerId = "user-1",
        familyId = null,
        name = "Dompet",
        type = "personal",
        balance = 0L,
        currency = "IDR",
        icon = null,
        color = null,
        syncStatus = "PENDING",
        createdAtEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
    )

    private fun pendingTransaction(walletId: String = "w-1") = TransactionEntity(
        id = "tx-1",
        walletId = walletId,
        userId = "user-1",
        familyId = "fam-1",
        type = "income",
        amount = 11_000_000L,
        categoryId = "cat_gaji",
        note = null,
        dateEpochMs = Instant.parse("2026-08-16T10:22:00Z").toEpochMilli(),
        addedByName = "Irul",
        syncStatus = "PENDING",
        createdAtEpochMs = Instant.parse("2026-08-16T10:22:00Z").toEpochMilli(),
    )

    private fun familyWallet(balance: Long) = Wallet(
        id = "w-fam",
        ownerId = "user-1",
        familyId = "fam-1",
        name = "Dompet Keluarga",
        type = WalletType.FAMILY,
        balance = balance,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun familyIncome(amount: Long) = Transaction(
        id = "tx-fam",
        walletId = "w-fam",
        userId = "user-1",
        familyId = "fam-1",
        type = TransactionType.INCOME,
        amount = amount,
        categoryId = "cat_gaji",
        date = Instant.parse("2026-08-16T10:22:00Z"),
        addedByName = "Irul",
        syncStatus = SyncStatus.SYNCED,
    )
}
