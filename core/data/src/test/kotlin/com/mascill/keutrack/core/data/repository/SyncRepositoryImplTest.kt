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
import com.mascill.keutrack.core.data.db.entity.PendingTransactionDeleteEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.data.mapper.BudgetMapper
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.mapper.TransactionMapper
import com.mascill.keutrack.core.data.mapper.WalletMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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
    private val periodPreferences = mockk<PeriodPreferencesRepository> {
        every { observe() } returns flowOf(PeriodPreferences(cycleStartDay = 1))
    }
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
        periodPreferences = periodPreferences,
    )

    @Test
    fun `hasPendingSync is true when any local item is pending`() = runTest {
        coEvery { walletLocal.getPending() } returns listOf(pendingWallet())
        coEvery { budgetLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPendingDeletes() } returns emptyList()

        assertThat(repo.hasPendingSync()).isTrue()
    }

    @Test
    fun `hasPendingSync is false when queues are empty`() = runTest {
        coEvery { walletLocal.getPending() } returns emptyList()
        coEvery { budgetLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPendingDeletes() } returns emptyList()

        assertThat(repo.hasPendingSync()).isFalse()
    }

    @Test
    fun `hasPendingSync is true when only delete outbox is pending`() = runTest {
        coEvery { walletLocal.getPending() } returns emptyList()
        coEvery { budgetLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPendingDeletes() } returns listOf(pendingDelete())

        assertThat(repo.hasPendingSync()).isTrue()
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
    fun `hasPendingSync is true when a pending transaction is queued`() = runTest {
        coEvery { walletLocal.getPending() } returns emptyList()
        coEvery { budgetLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns listOf(pendingExpense())
        coEvery { transactionLocal.getPendingDeletes() } returns emptyList()

        assertThat(repo.hasPendingSync()).isTrue()
    }

    @Test
    fun `syncPendingTransactions amount edit passes reverse and apply wallet deltas`() = runTest {
        val local = pendingExpense(amount = 150_000L)
        val remote = remoteExpense(amount = 100_000L)
        val budget = personalExpenseBudget()
        stubPendingUpsert(local = local, remote = remote)
        coEvery { budgetLocal.getByMonthCategoryPersonal("2026-08", "cat_makan") } returns budget

        repo.syncPendingTransactions()

        coVerify {
            transactionRemote.upsertTransactionWithSideEffects(
                transaction = match { it.id == "tx-1" && it.amount == 150_000L },
                remoteSnapshot = match { it.amount == 100_000L },
                oldWalletId = "w-1",
                oldWalletDelta = 100_000L,
                newWalletDelta = -150_000L,
                oldBudgetId = "b-food",
                oldBudgetDelta = -100_000L,
                newBudgetId = "b-food",
                newBudgetDelta = 150_000L,
                summaries = match { it.size == 1 },
            )
        }
        coVerify { transactionLocal.updateSyncStatus("tx-1", SyncStatus.SYNCED) }
        coVerify(exactly = 0) { summaryRemote.upsertSummary(any()) }
    }

    @Test
    fun `syncPendingTransactions missing remote treats as create`() = runTest {
        val local = pendingExpense(amount = 150_000L)
        stubPendingUpsert(local = local, remote = null)

        repo.syncPendingTransactions()

        coVerify {
            transactionRemote.upsertTransactionWithSideEffects(
                transaction = match { it.amount == 150_000L },
                remoteSnapshot = null,
                oldWalletId = null,
                oldWalletDelta = 0L,
                newWalletDelta = -150_000L,
                oldBudgetId = null,
                oldBudgetDelta = 0L,
                newBudgetId = null,
                newBudgetDelta = 0L,
                summaries = match { it.size == 1 },
            )
        }
        coVerify { transactionLocal.updateSyncStatus("tx-1", SyncStatus.SYNCED) }
    }

    @Test
    fun `syncPendingTransactions note only nets wallet increment to zero`() = runTest {
        val local = pendingExpense(amount = 100_000L, note = "baru")
        val remote = remoteExpense(amount = 100_000L, note = "lama")
        stubPendingUpsert(local = local, remote = remote)

        repo.syncPendingTransactions()

        coVerify {
            transactionRemote.upsertTransactionWithSideEffects(
                transaction = match { it.note == "baru" && it.amount == 100_000L },
                remoteSnapshot = match { it.note == "lama" },
                oldWalletId = "w-1",
                oldWalletDelta = 100_000L,
                newWalletDelta = -100_000L,
                oldBudgetId = any(),
                oldBudgetDelta = any(),
                newBudgetId = any(),
                newBudgetDelta = any(),
                summaries = any(),
            )
        }
    }

    @Test
    fun `syncPendingTransactions still upserts when remote already exists`() = runTest {
        val local = pendingExpense(amount = 100_000L)
        val remote = remoteExpense(amount = 100_000L)
        stubPendingUpsert(local = local, remote = remote)

        repo.syncPendingTransactions()

        coVerify(exactly = 1) {
            transactionRemote.upsertTransactionWithSideEffects(
                transaction = match { it.id == "tx-1" },
                remoteSnapshot = match { it.id == "tx-1" },
                oldWalletId = "w-1",
                oldWalletDelta = 100_000L,
                newWalletDelta = -100_000L,
                oldBudgetId = any(),
                oldBudgetDelta = any(),
                newBudgetId = any(),
                newBudgetDelta = any(),
                summaries = any(),
            )
        }
        coVerify { transactionLocal.updateSyncStatus("tx-1", SyncStatus.SYNCED) }
    }

    @Test
    fun `syncPendingTransactions uses payday cycle month key for budget match`() = runTest {
        every { periodPreferences.observe() } returns flowOf(PeriodPreferences(cycleStartDay = 25))
        val date = LocalDate.of(2026, 7, 26).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val local = pendingExpense(amount = 15_000L, dateEpochMs = date.toEpochMilli())
        stubPendingUpsert(local = local, remote = null)

        repo.syncPendingTransactions()

        coVerify { budgetLocal.getByMonthCategoryPersonal("2026-08", "cat_makan") }
        coVerify(exactly = 0) { budgetLocal.getByMonthCategoryPersonal("2026-07", any()) }
    }

    @Test
    fun `syncPendingTransactions drains outbox with reverse then removes it`() = runTest {
        val remote = remoteExpense(amount = 15_000L)
        val budget = personalExpenseBudget()
        stubPendingDelete(outbox = pendingDelete(), remote = remote)
        coEvery { budgetLocal.getByMonthCategoryPersonal("2026-08", "cat_makan") } returns budget

        repo.syncPendingTransactions()

        coVerify {
            transactionRemote.deleteTransactionWithReverse(
                transactionId = "tx-1",
                expected = match { it.id == "tx-1" && it.amount == 15_000L },
                budgetId = "b-food",
                budgetDelta = -15_000L,
                walletDelta = 15_000L,
                summaries = match { it.size == 1 },
            )
        }
        coVerify { transactionLocal.removePendingDelete("tx-1") }
        coVerify(exactly = 0) {
            transactionLocal.updateDeleteSyncStatus("tx-1", SyncStatus.FAILED)
        }
    }

    @Test
    fun `syncPendingTransactions acks outbox when remote doc is missing`() = runTest {
        stubPendingDelete(outbox = pendingDelete(), remote = null)

        repo.syncPendingTransactions()

        coVerify {
            transactionRemote.deleteTransactionWithReverse(
                transactionId = "tx-1",
                expected = match {
                    it.id == "tx-1" &&
                        it.amount == 15_000L &&
                        it.walletId == "w-1"
                },
                budgetId = any(),
                budgetDelta = any(),
                walletDelta = 15_000L,
                summaries = any(),
            )
        }
        coVerify { transactionLocal.removePendingDelete("tx-1") }
        coVerify(exactly = 0) {
            transactionLocal.updateDeleteSyncStatus(any(), SyncStatus.FAILED)
        }
    }

    @Test
    fun `syncPendingTransactions skips upsert for id that is in outbox`() = runTest {
        stubPendingDelete(outbox = pendingDelete(), remote = remoteExpense(amount = 15_000L))
        coEvery { transactionLocal.getPending() } returns listOf(pendingExpense())

        repo.syncPendingTransactions()

        coVerify {
            transactionRemote.deleteTransactionWithReverse(
                transactionId = "tx-1",
                expected = any(),
                budgetId = any(),
                budgetDelta = any(),
                walletDelta = any(),
                summaries = any(),
            )
        }
        coVerify(exactly = 0) {
            transactionRemote.upsertTransactionWithSideEffects(
                transaction = any(),
                remoteSnapshot = any(),
                oldWalletId = any(),
                oldWalletDelta = any(),
                newWalletDelta = any(),
                oldBudgetId = any(),
                oldBudgetDelta = any(),
                newBudgetId = any(),
                newBudgetDelta = any(),
                summaries = any(),
            )
        }
    }

    @Test
    fun `syncPendingTransactions marks outbox failed and throws when remote delete fails`() =
        runTest {
            stubPendingDelete(outbox = pendingDelete(), remote = remoteExpense(amount = 15_000L))
            coEvery {
                transactionRemote.deleteTransactionWithReverse(
                    transactionId = any(),
                    expected = any(),
                    budgetId = any(),
                    budgetDelta = any(),
                    walletDelta = any(),
                    summaries = any(),
                )
            } throws IllegalStateException("offline")

            try {
                repo.syncPendingTransactions()
                org.junit.Assert.fail("Expected IllegalStateException")
            } catch (e: IllegalStateException) {
                assertThat(e.message).contains("transactions failed")
            }
            coVerify { transactionLocal.updateDeleteSyncStatus("tx-1", SyncStatus.FAILED) }
            coVerify(exactly = 0) { transactionLocal.removePendingDelete(any()) }
        }

    @Test
    fun `syncPendingTransactions drains outbox before pending upserts`() = runTest {
        val outbox = pendingDelete(id = "tx-del")
        val local = pendingExpense()
        coEvery { transactionLocal.getPendingDeletes() } returns listOf(outbox)
        coEvery { transactionLocal.getPending() } returns listOf(local)
        coEvery { transactionRemote.getById("tx-del") } returns null
        coEvery { transactionRemote.getById("tx-1") } returns null
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), any()) } returns null
        coEvery { budgetLocal.getByMonthCategoryAndFamily(any(), any(), any()) } returns null
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null

        repo.syncPendingTransactions()

        coVerifyOrder {
            transactionRemote.deleteTransactionWithReverse(
                transactionId = "tx-del",
                expected = any(),
                budgetId = any(),
                budgetDelta = any(),
                walletDelta = any(),
                summaries = any(),
            )
            transactionRemote.upsertTransactionWithSideEffects(
                transaction = match { it.id == "tx-1" },
                remoteSnapshot = any(),
                oldWalletId = any(),
                oldWalletDelta = any(),
                newWalletDelta = any(),
                oldBudgetId = any(),
                oldBudgetDelta = any(),
                newBudgetId = any(),
                newBudgetDelta = any(),
                summaries = any(),
            )
        }
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
    fun `syncFamilyData skips upsert when remote tx id is in outbox`() = runTest {
        stubFamilyPull()
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns
            listOf(familyIncome(amount = 11_000_000L))
        coEvery { transactionLocal.getPendingDeletes() } returns
            listOf(pendingDelete(id = "tx-fam"))

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) { transactionLocal.upsert(any()) }
        coVerify(exactly = 0) {
            transactionLocal.applyDeletedTransactionAtomically(
                id = any(),
                walletId = any(),
                walletDelta = any(),
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = any(),
            )
        }
    }

    @Test
    fun `syncFamilyData sweeps local SYNCED orphan in pull window without outbox`() = runTest {
        val orphan = localFamilyTx(id = "tx-orphan", amount = 5_000_000L)
        stubFamilyPull()
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns
            listOf(familyIncome(amount = 11_000_000L))
        coEvery { transactionLocal.getByFamilyId("fam-1") } returns listOf(orphan)

        repo.syncFamilyData("fam-1")

        coVerify {
            transactionLocal.applyDeletedTransactionAtomically(
                id = "tx-orphan",
                walletId = "w-fam",
                walletDelta = -5_000_000L,
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = null,
            )
        }
        coVerify { transactionLocal.upsert(match { it.id == "tx-fam" }) }
    }

    @Test
    fun `syncFamilyData does not sweep local PENDING create`() = runTest {
        val pending = localFamilyTx(id = "tx-new", syncStatus = SyncStatus.PENDING.name)
        stubFamilyPull()
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns
            listOf(familyIncome(amount = 11_000_000L))
        coEvery { transactionLocal.getByFamilyId("fam-1") } returns listOf(pending)

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) {
            transactionLocal.applyDeletedTransactionAtomically(
                id = any(),
                walletId = any(),
                walletDelta = any(),
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = any(),
            )
        }
    }

    @Test
    fun `syncFamilyData does not sweep SYNCED older than oldest pulled`() = runTest {
        val older = localFamilyTx(
            id = "tx-old",
            date = Instant.parse("2026-01-01T00:00:00Z"),
        )
        stubFamilyPull()
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns
            listOf(familyIncome(amount = 11_000_000L))
        coEvery { transactionLocal.getByFamilyId("fam-1") } returns listOf(older)

        repo.syncFamilyData("fam-1")

        coVerify(exactly = 0) {
            transactionLocal.applyDeletedTransactionAtomically(
                id = any(),
                walletId = any(),
                walletDelta = any(),
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = any(),
            )
        }
    }

    @Test
    fun `syncFamilyData sweeps all SYNCED when pull is empty`() = runTest {
        val orphan = localFamilyTx(id = "tx-orphan", amount = 5_000_000L)
        stubFamilyPull()
        coEvery { transactionLocal.getByFamilyId("fam-1") } returns listOf(orphan)

        repo.syncFamilyData("fam-1")

        coVerify {
            transactionLocal.applyDeletedTransactionAtomically(
                id = "tx-orphan",
                walletId = "w-fam",
                walletDelta = -5_000_000L,
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = null,
            )
        }
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

    @Test
    fun `syncPersonalData skips upsert when remote tx id is in outbox`() = runTest {
        stubPersonalPull(
            wallets = listOf(personalWallet(id = "w-old")),
            txs = listOf(personalIncome(walletId = "w-old", amount = 50_000L)),
        )
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns emptyList()
        coEvery { transactionLocal.getPendingDeletes() } returns
            listOf(pendingDelete(id = "tx-personal"))

        repo.syncPersonalData("user-1")

        coVerify(exactly = 0) { transactionLocal.upsert(any()) }
    }

    @Test
    fun `syncPersonalData sweeps local SYNCED orphan without writing outbox`() = runTest {
        val orphan = localPersonalTx(id = "tx-orphan", amount = 20_000L)
        stubPersonalPull(
            wallets = listOf(personalWallet(id = "w-old")),
            txs = listOf(personalIncome(walletId = "w-old", amount = 50_000L)),
        )
        coEvery { walletLocal.getById("w-old") } returns null
        coEvery { walletLocal.getByType("personal") } returns emptyList()
        coEvery { transactionLocal.getByWalletId("w-old") } returns listOf(orphan)

        repo.syncPersonalData("user-1")

        coVerify {
            transactionLocal.applyDeletedTransactionAtomically(
                id = "tx-orphan",
                walletId = "w-old",
                walletDelta = -20_000L,
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = null,
                pendingDelete = null,
            )
        }
    }

    private fun stubFamilyPull() {
        coEvery { walletRemote.getByFamilyId("fam-1") } returns emptyList()
        coEvery { transactionRemote.getByFamilyId("fam-1", limit = 200) } returns emptyList()
        coEvery { transactionLocal.getPending() } returns emptyList()
        coEvery { transactionLocal.getPendingDeletes() } returns emptyList()
        coEvery { transactionLocal.getByFamilyId("fam-1") } returns emptyList()
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
        coEvery { transactionLocal.getPendingDeletes() } returns emptyList()
        coEvery { transactionLocal.getByWalletId(any()) } returns emptyList()
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

    private fun stubPendingUpsert(
        local: TransactionEntity,
        remote: Transaction?,
    ) {
        coEvery { transactionLocal.getPendingDeletes() } returns emptyList()
        coEvery { transactionLocal.getPending() } returns listOf(local)
        coEvery { transactionRemote.getById(local.id) } returns remote
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), any()) } returns null
        coEvery { budgetLocal.getByMonthCategoryAndFamily(any(), any(), any()) } returns null
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null
    }

    private fun stubPendingDelete(
        outbox: PendingTransactionDeleteEntity,
        remote: Transaction?,
    ) {
        coEvery { transactionLocal.getPendingDeletes() } returns listOf(outbox)
        coEvery { transactionLocal.getPending() } returns emptyList()
        coEvery { transactionRemote.getById(outbox.id) } returns remote
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), any()) } returns null
        coEvery { budgetLocal.getByMonthCategoryAndFamily(any(), any(), any()) } returns null
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null
    }

    private fun pendingExpense(
        amount: Long = 100_000L,
        note: String? = null,
        dateEpochMs: Long = Instant.parse("2026-08-16T10:22:00Z").toEpochMilli(),
        categoryId: String = "cat_makan",
    ) = TransactionEntity(
        id = "tx-1",
        walletId = "w-1",
        userId = "user-1",
        familyId = null,
        type = TransactionType.EXPENSE.value,
        amount = amount,
        categoryId = categoryId,
        note = note,
        dateEpochMs = dateEpochMs,
        addedByName = "Irul",
        syncStatus = SyncStatus.PENDING.name,
        createdAtEpochMs = Instant.parse("2026-08-16T10:22:00Z").toEpochMilli(),
    )

    private fun remoteExpense(
        amount: Long = 100_000L,
        note: String? = null,
        date: Instant = Instant.parse("2026-08-16T10:22:00Z"),
        categoryId: String = "cat_makan",
    ) = Transaction(
        id = "tx-1",
        walletId = "w-1",
        userId = "user-1",
        familyId = null,
        type = TransactionType.EXPENSE,
        amount = amount,
        categoryId = categoryId,
        note = note,
        date = date,
        addedByName = "Irul",
        syncStatus = SyncStatus.SYNCED,
        createdAt = Instant.parse("2026-08-16T10:22:00Z"),
    )

    private fun personalExpenseBudget() = BudgetEntity(
        id = "b-food",
        userId = "user-1",
        familyId = null,
        categoryId = "cat_makan",
        limit = 1_000_000L,
        spent = 100_000L,
        period = "monthly",
        month = "2026-08",
        walletId = "w-1",
        syncStatus = SyncStatus.SYNCED.name,
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

    private fun pendingDelete(id: String = "tx-1") = PendingTransactionDeleteEntity(
        id = id,
        walletId = "w-1",
        userId = "user-1",
        familyId = "fam-1",
        type = "expense",
        amount = 15_000L,
        categoryId = "cat_makan",
        dateEpochMs = Instant.parse("2026-08-16T10:22:00Z").toEpochMilli(),
        queuedAtEpochMs = Instant.parse("2026-08-16T10:22:00Z").toEpochMilli(),
        syncStatus = SyncStatus.PENDING.name,
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

    private fun localFamilyTx(
        id: String,
        amount: Long = 5_000_000L,
        date: Instant = Instant.parse("2026-08-16T10:22:00Z"),
        syncStatus: String = SyncStatus.SYNCED.name,
    ) = TransactionEntity(
        id = id,
        walletId = "w-fam",
        userId = "user-1",
        familyId = "fam-1",
        type = TransactionType.INCOME.value,
        amount = amount,
        categoryId = "cat_gaji",
        note = null,
        dateEpochMs = date.toEpochMilli(),
        addedByName = "Irul",
        syncStatus = syncStatus,
        createdAtEpochMs = date.toEpochMilli(),
    )

    private fun localPersonalTx(
        id: String,
        amount: Long = 20_000L,
        date: Instant = Instant.parse("2026-08-16T10:22:00Z"),
        syncStatus: String = SyncStatus.SYNCED.name,
    ) = TransactionEntity(
        id = id,
        walletId = "w-old",
        userId = "user-1",
        familyId = null,
        type = TransactionType.INCOME.value,
        amount = amount,
        categoryId = "cat_gaji",
        note = null,
        dateEpochMs = date.toEpochMilli(),
        addedByName = "Irul",
        syncStatus = syncStatus,
        createdAtEpochMs = date.toEpochMilli(),
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
