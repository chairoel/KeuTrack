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
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.data.mapper.BudgetMapper
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.mapper.TransactionMapper
import com.mascill.keutrack.core.data.mapper.WalletMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.Budget
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
import java.time.YearMonth

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

    @Test
    fun `syncFamilyData hydrates remote family budgets as synced`() = runTest {
        stubFamilyPull()
        val remote = familyBudget(limit = 1_000_000L, spent = 400_000L)
        coEvery { budgetRemote.getByFamilyId("fam-1", currentMonthKey()) } returns listOf(remote)
        coEvery { budgetRemote.getByFamilyId("fam-1", priorMonthKey()) } returns emptyList()
        coEvery { budgetLocal.getById("b-1") } returns null

        repo.syncFamilyData("fam-1")

        coVerify {
            budgetLocal.upsert(
                match { entity ->
                    entity.id == "b-1" &&
                        entity.familyId == "fam-1" &&
                        entity.categoryId == "cat_food" &&
                        entity.limit == 1_000_000L &&
                        entity.spent == 400_000L &&
                        entity.month == currentMonthKey() &&
                        entity.syncStatus == SyncStatus.SYNCED.name
                },
            )
        }
    }

    @Test
    fun `syncFamilyData pulls current and prior month family budgets`() = runTest {
        stubFamilyPull()
        val current = familyBudget(id = "b-now", month = currentMonthKey())
        val prior = familyBudget(id = "b-prior", month = priorMonthKey())
        coEvery { budgetRemote.getByFamilyId("fam-1", currentMonthKey()) } returns listOf(current)
        coEvery { budgetRemote.getByFamilyId("fam-1", priorMonthKey()) } returns listOf(prior)
        coEvery { budgetLocal.getById(any()) } returns null

        repo.syncFamilyData("fam-1")

        coVerify { budgetRemote.getByFamilyId("fam-1", currentMonthKey()) }
        coVerify { budgetRemote.getByFamilyId("fam-1", priorMonthKey()) }
        coVerify { budgetLocal.upsert(match { it.id == "b-now" }) }
        coVerify { budgetLocal.upsert(match { it.id == "b-prior" }) }
    }

    @Test
    fun `syncFamilyData skips budget overwrite when local is pending`() = runTest {
        stubFamilyPull()
        coEvery { budgetRemote.getByFamilyId("fam-1", currentMonthKey()) } returns
            listOf(familyBudget())
        coEvery { budgetRemote.getByFamilyId("fam-1", priorMonthKey()) } returns emptyList()
        coEvery { budgetLocal.getById("b-1") } returns
            familyBudgetEntity(syncStatus = SyncStatus.PENDING.name)

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) { budgetLocal.upsert(any()) }
    }

    @Test
    fun `syncFamilyData skips budget overwrite when local is failed`() = runTest {
        stubFamilyPull()
        coEvery { budgetRemote.getByFamilyId("fam-1", currentMonthKey()) } returns
            listOf(familyBudget())
        coEvery { budgetRemote.getByFamilyId("fam-1", priorMonthKey()) } returns emptyList()
        coEvery { budgetLocal.getById("b-1") } returns
            familyBudgetEntity(syncStatus = SyncStatus.FAILED.name)

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) { budgetLocal.upsert(any()) }
    }

    @Test
    fun `syncFamilyData ignores remote budgets that are not this family`() = runTest {
        stubFamilyPull()
        val otherFamily = familyBudget(familyId = "fam-2")
        coEvery { budgetRemote.getByFamilyId("fam-1", currentMonthKey()) } returns
            listOf(otherFamily)
        coEvery { budgetRemote.getByFamilyId("fam-1", priorMonthKey()) } returns emptyList()

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) { budgetLocal.upsert(any()) }
    }

    @Test
    fun `syncPersonalData is no-op when userId is blank`() = runTest {
        repo.syncPersonalData("  ")

        coVerify(exactly = 0) { walletRemote.getByOwnerId(any()) }
        coVerify(exactly = 0) { transactionRemote.getByUserId(any(), any()) }
    }

    @Test
    fun `syncPersonalData upserts canonical wallet and transactions`() = runTest {
        val remoteWallet = personalWallet(id = "w-old", createdAt = Instant.parse("2026-01-01T00:00:00Z"))
        val remoteTx = personalIncome(walletId = "w-old", amount = 50_000L)
        stubPersonalPull(wallets = listOf(remoteWallet), txs = listOf(remoteTx))
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns emptyList()
        coEvery { transactionLocal.getById(any()) } returns null

        repo.syncPersonalData("user-1")

        coVerify {
            walletLocal.upsert(
                match { entity ->
                    entity.id == "w-old" &&
                        entity.balance == 50_000L &&
                        entity.syncStatus == SyncStatus.SYNCED.name
                },
            )
        }
        coVerify {
            transactionLocal.upsert(match { it.id == "tx-personal" && it.syncStatus == "SYNCED" })
        }
    }

    @Test
    fun `syncPersonalData skips wallet overwrite when local is PENDING`() = runTest {
        stubPersonalPull(
            wallets = listOf(personalWallet(id = "w-old")),
            txs = emptyList(),
        )
        coEvery { walletLocal.getById("w-old") } returns pendingWallet().copy(id = "w-old")
        coEvery { walletLocal.getByType("personal") } returns emptyList()

        repo.syncPersonalData("user-1")

        coVerify(exactly = 0) { walletLocal.upsert(any()) }
        coVerify(exactly = 0) { walletRemote.setBalance(any(), any()) }
    }

    @Test
    fun `syncPersonalData picks oldest remote personal as canonical`() = runTest {
        val older = personalWallet(
            id = "w-old",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            balance = 10_000L,
        )
        val newer = personalWallet(
            id = "w-new",
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
            balance = 0L,
        )
        stubPersonalPull(
            wallets = listOf(newer, older),
            txs = listOf(personalIncome(walletId = "w-old", amount = 10_000L)),
        )
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns emptyList()
        coEvery { transactionLocal.getById(any()) } returns null

        repo.syncPersonalData("user-1")

        coVerify {
            walletLocal.upsert(match { it.id == "w-old" && it.balance == 10_000L })
        }
        coVerify(exactly = 0) { walletLocal.upsert(match { it.id == "w-new" }) }
    }

    @Test
    fun `syncPersonalData deletes extra local personal without pending txs`() = runTest {
        val canonical = personalWallet(
            id = "w-old",
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        stubPersonalPull(wallets = listOf(canonical), txs = emptyList())
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns listOf(
            pendingWallet().copy(id = "w-new", balance = 0L),
        )

        repo.syncPersonalData("user-1")

        coVerify { walletLocal.delete("w-new") }
        coVerify(exactly = 0) { walletLocal.delete("w-old") }
    }

    @Test
    fun `syncPersonalData keeps extra local personal that has pending txs`() = runTest {
        val canonical = personalWallet(id = "w-old")
        stubPersonalPull(wallets = listOf(canonical), txs = emptyList())
        coEvery { transactionLocal.getPending() } returns
            listOf(pendingTransaction(walletId = "w-new"))
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns listOf(
            pendingWallet().copy(id = "w-new"),
        )

        repo.syncPersonalData("user-1")

        coVerify(exactly = 0) { walletLocal.delete("w-new") }
    }

    @Test
    fun `syncPersonalData recomputes balance from pulled transactions`() = runTest {
        val remoteWallet = personalWallet(id = "w-old", balance = 99_000L)
        stubPersonalPull(
            wallets = listOf(remoteWallet),
            txs = listOf(
                personalIncome(walletId = "w-old", amount = 50_000L),
                personalExpense(walletId = "w-old", amount = 10_000L),
            ),
        )
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns emptyList()
        coEvery { transactionLocal.getById(any()) } returns null
        coEvery { walletRemote.setBalance(any(), any()) } just runs

        repo.syncPersonalData("user-1")

        coVerify {
            walletLocal.upsert(match { it.id == "w-old" && it.balance == 40_000L })
        }
        coVerify { walletRemote.setBalance("w-old", 40_000L) }
    }

    @Test
    fun `syncPersonalData rebuilds category summary for pulled periods`() = runTest {
        val remoteWallet = personalWallet(id = "w-old")
        stubPersonalPull(
            wallets = listOf(remoteWallet),
            txs = listOf(
                personalIncome(
                    walletId = "w-old",
                    amount = 50_000L,
                    date = Instant.parse("2026-08-16T12:00:00Z"),
                ),
                personalExpense(
                    walletId = "w-old",
                    amount = 10_000L,
                    date = Instant.parse("2026-08-16T15:00:00Z"),
                ),
            ),
        )
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns emptyList()
        coEvery { transactionLocal.getById(any()) } returns null

        repo.syncPersonalData("user-1")

        coVerify {
            summaryLocal.upsert(
                match { entity ->
                    entity.period == "2026-08" &&
                        entity.userId == "user-1" &&
                        entity.totalIncome == 50_000L &&
                        entity.totalExpense == 10_000L
                },
            )
        }
    }

    private fun stubFamilyPull() {
        coEvery { walletRemote.getByFamilyId("fam-1") } returns emptyList()
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()
    }

    private fun currentMonthKey(): String = YearMonth.now().toString()

    private fun priorMonthKey(): String = YearMonth.now().minusMonths(1).toString()

    private fun familyBudget(
        id: String = "b-1",
        familyId: String = "fam-1",
        month: String = currentMonthKey(),
        limit: Long = 1_000_000L,
        spent: Long = 0L,
    ) = Budget(
        id = id,
        userId = "user-1",
        familyId = familyId,
        categoryId = "cat_food",
        limit = limit,
        spent = spent,
        month = month,
        walletId = "w-fam",
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun familyBudgetEntity(syncStatus: String) =
        BudgetEntity(
            id = "b-1",
            userId = "user-1",
            familyId = "fam-1",
            categoryId = "cat_food",
            limit = 1_000_000L,
            spent = 0L,
            period = "monthly",
            month = currentMonthKey(),
            walletId = "w-fam",
            syncStatus = syncStatus,
            createdAtEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
        )

    private fun stubPersonalPull(
        wallets: List<Wallet>,
        txs: List<Transaction>,
    ) {
        coEvery { walletRemote.getByOwnerId("user-1") } returns wallets
        coEvery { transactionRemote.getByUserId("user-1", limit = 200) } returns txs
        coEvery { transactionLocal.getPending() } returns emptyList()
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

    private fun personalWallet(
        id: String,
        balance: Long = 0L,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = Wallet(
        id = id,
        ownerId = "user-1",
        familyId = null,
        name = "Dompet Utama",
        type = WalletType.PERSONAL,
        balance = balance,
        createdAt = createdAt,
    )

    private fun personalIncome(
        walletId: String,
        amount: Long,
        date: Instant = Instant.parse("2026-08-16T10:22:00Z"),
    ) = Transaction(
        id = "tx-personal",
        walletId = walletId,
        userId = "user-1",
        familyId = null,
        type = TransactionType.INCOME,
        amount = amount,
        categoryId = "cat_gaji",
        date = date,
        addedByName = "Irul",
        syncStatus = SyncStatus.SYNCED,
    )

    private fun personalExpense(
        walletId: String,
        amount: Long,
        date: Instant = Instant.parse("2026-08-16T10:22:00Z"),
    ) = Transaction(
        id = "tx-personal-exp",
        walletId = walletId,
        userId = "user-1",
        familyId = null,
        type = TransactionType.EXPENSE,
        amount = amount,
        categoryId = "cat_makan",
        date = date,
        addedByName = "Irul",
        syncStatus = SyncStatus.SYNCED,
    )
}
