package com.mascill.keutrack.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSource
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.model.AmountByTypeRow
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.mapper.TransactionMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.CategoryBreakdown
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.PeriodPreferences
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import java.time.ZoneId

class TransactionRepositoryImplTest {

    private val local = mockk<TransactionLocalDataSource>(relaxed = true)
    private val budgetLocal = mockk<BudgetLocalDataSource>(relaxed = true)
    private val summaryLocal = mockk<CategorySummaryLocalDataSource>(relaxed = true)
    private val categoryLocal = mockk<CategoryLocalDataSource>(relaxed = true)
    private val mapper = TransactionMapper()
    private val summaryMapper = CategorySummaryMapper()
    private val syncScheduler = mockk<SyncScheduler>(relaxed = true)
    private val periodPreferences = mockk<PeriodPreferencesRepository>()
    private val repo = TransactionRepositoryImpl(
        local = local,
        budgetLocal = budgetLocal,
        summaryLocal = summaryLocal,
        categoryLocal = categoryLocal,
        mapper = mapper,
        summaryMapper = summaryMapper,
        syncScheduler = syncScheduler,
        periodPreferences = periodPreferences,
    )

    @Test
    fun `addTransaction inserts entity via DAO and enqueues sync`() = runTest {
        val transaction = domainTransaction()
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), any()) } returns null
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null
        coEvery { categoryLocal.getById(any()) } returns null
        coEvery { local.applyNewTransactionAtomically(any(), any(), any(), any()) } just runs
        stubCycleStartDay(1)

        repo.addTransaction(transaction)

        coVerify {
            local.applyNewTransactionAtomically(
                transaction = match { it.id == "tx-1" && it.syncStatus == SyncStatus.PENDING.name },
                walletDelta = -15_000L,
                budgetIdToIncrement = null,
                summaryUpsert = any(),
            )
        }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `addTransaction family expense increments family budget not personal`() = runTest {
        val familyBudget = budgetEntity(id = "b-fam", familyId = "fam-1")
        val personalBudget = budgetEntity(id = "b-personal", familyId = null)
        coEvery {
            budgetLocal.getByMonthCategoryAndFamily(any(), "cat-food", "fam-1")
        } returns familyBudget
        coEvery {
            budgetLocal.getByMonthCategoryPersonal(any(), "cat-food")
        } returns personalBudget
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null
        coEvery { categoryLocal.getById(any()) } returns null
        coEvery { local.applyNewTransactionAtomically(any(), any(), any(), any()) } just runs
        stubCycleStartDay(1)

        repo.addTransaction(domainTransaction(familyId = "fam-1"))

        coVerify {
            local.applyNewTransactionAtomically(
                transaction = match { it.familyId == "fam-1" },
                walletDelta = -15_000L,
                budgetIdToIncrement = "b-fam",
                summaryUpsert = any(),
            )
        }
        coVerify(exactly = 1) {
            budgetLocal.getByMonthCategoryAndFamily(any(), "cat-food", "fam-1")
        }
        coVerify(exactly = 0) { budgetLocal.getByMonthCategoryPersonal(any(), any()) }
    }

    @Test
    fun `addTransaction personal expense increments personal budget not family`() = runTest {
        val familyBudget = budgetEntity(id = "b-fam", familyId = "fam-1")
        val personalBudget = budgetEntity(id = "b-personal", familyId = null)
        coEvery {
            budgetLocal.getByMonthCategoryAndFamily(any(), "cat-food", any())
        } returns familyBudget
        coEvery {
            budgetLocal.getByMonthCategoryPersonal(any(), "cat-food")
        } returns personalBudget
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null
        coEvery { categoryLocal.getById(any()) } returns null
        coEvery { local.applyNewTransactionAtomically(any(), any(), any(), any()) } just runs
        stubCycleStartDay(1)

        repo.addTransaction(domainTransaction(familyId = null))

        coVerify {
            local.applyNewTransactionAtomically(
                transaction = match { it.familyId == null },
                walletDelta = -15_000L,
                budgetIdToIncrement = "b-personal",
                summaryUpsert = any(),
            )
        }
        coVerify(exactly = 1) { budgetLocal.getByMonthCategoryPersonal(any(), "cat-food") }
        coVerify(exactly = 0) { budgetLocal.getByMonthCategoryAndFamily(any(), any(), any()) }
    }

    @Test
    fun `observeTransactions emits mapped domain`() = runTest {
        val entity = mapper.toEntity(domainTransaction())
        every {
            local.observeFiltered(
                walletId = "wallet-1",
                familyId = null,
                type = null,
                categoryId = null,
                startMs = null,
                endMs = null,
                limit = 20,
            )
        } returns flowOf(listOf(entity))

        repo.observeTransactions(walletId = "wallet-1", limit = 20).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("tx-1")
            assertThat(items.first().amount).isEqualTo(15_000L)
            awaitComplete()
        }
    }

    @Test
    fun `observeRecentTransactions maps entities`() = runTest {
        every { local.observeRecent(5) } returns flowOf(listOf(sampleEntity()))

        repo.observeRecentTransactions(5).test {
            assertThat(awaitItem().map { it.id }).containsExactly("tx-1")
            awaitComplete()
        }
    }

    @Test
    fun `observePeriodTotals forwards wallet family and range to local`() = runTest {
        val start = Instant.parse("2026-08-01T00:00:00Z")
        val end = Instant.parse("2026-08-07T23:59:59.999Z")
        every {
            local.observeSumsByType(
                walletId = "wallet-p",
                familyId = null,
                startMs = start.toEpochMilli(),
                endMs = end.toEpochMilli(),
            )
        } returns flowOf(emptyList())

        repo.observePeriodTotals(
            walletId = "wallet-p",
            startDate = start,
            endDate = end,
        ).test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) {
            local.observeSumsByType(
                walletId = "wallet-p",
                familyId = null,
                startMs = start.toEpochMilli(),
                endMs = end.toEpochMilli(),
            )
        }
    }

    @Test
    fun `observePeriodTotals forwards familyId without mixing personal filter`() = runTest {
        every {
            local.observeSumsByType(
                walletId = null,
                familyId = "fam-1",
                startMs = null,
                endMs = null,
            )
        } returns flowOf(emptyList())

        repo.observePeriodTotals(familyId = "fam-1").test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) {
            local.observeSumsByType(
                walletId = null,
                familyId = "fam-1",
                startMs = null,
                endMs = null,
            )
        }
    }

    @Test
    fun `observePeriodTotals null range is all-time`() = runTest {
        every {
            local.observeSumsByType(
                walletId = null,
                familyId = null,
                startMs = null,
                endMs = null,
            )
        } returns flowOf(emptyList())

        repo.observePeriodTotals().test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) {
            local.observeSumsByType(
                walletId = null,
                familyId = null,
                startMs = null,
                endMs = null,
            )
        }
    }

    @Test
    fun `observePeriodTotals folds income and expense rows`() = runTest {
        every {
            local.observeSumsByType(
                walletId = null,
                familyId = null,
                startMs = null,
                endMs = null,
            )
        } returns flowOf(
            listOf(
                AmountByTypeRow(type = TransactionType.INCOME.value, total = 1_200_000L),
                AmountByTypeRow(type = TransactionType.EXPENSE.value, total = 350_000L),
            ),
        )

        repo.observePeriodTotals().test {
            val totals = awaitItem()
            assertThat(totals.incomeTotal).isEqualTo(1_200_000L)
            assertThat(totals.expenseTotal).isEqualTo(350_000L)
            awaitComplete()
        }
    }

    @Test
    fun `observePeriodTotals income only leaves expense at zero`() = runTest {
        every {
            local.observeSumsByType(
                walletId = null,
                familyId = null,
                startMs = null,
                endMs = null,
            )
        } returns flowOf(
            listOf(AmountByTypeRow(type = TransactionType.INCOME.value, total = 500_000L)),
        )

        repo.observePeriodTotals().test {
            val totals = awaitItem()
            assertThat(totals.incomeTotal).isEqualTo(500_000L)
            assertThat(totals.expenseTotal).isEqualTo(0L)
            awaitComplete()
        }
    }

    @Test
    fun `addTransaction 26 Jul with startDay 25 uses August month key`() = runTest {
        stubCycleStartDay(25)
        stubBudgetLookups()
        val date = LocalDate.of(2026, 7, 26).atStartOfDay(ZoneId.systemDefault()).toInstant()

        repo.addTransaction(domainTransaction(date = date))

        coVerify { budgetLocal.getByMonthCategoryPersonal("2026-08", "cat-food") }
    }

    @Test
    fun `addTransaction 26 Jul with startDay 1 uses July month key`() = runTest {
        stubCycleStartDay(1)
        stubBudgetLookups()
        val date = LocalDate.of(2026, 7, 26).atStartOfDay(ZoneId.systemDefault()).toInstant()

        repo.addTransaction(domainTransaction(date = date))

        coVerify { budgetLocal.getByMonthCategoryPersonal("2026-07", "cat-food") }
    }

    @Test
    fun `updateTransaction amount expense reverses old and applies new wallet and budget`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val personalBudget = budgetEntity(id = "b-personal", familyId = null)
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), "cat-food") } returns personalBudget
        val old = domainTransaction(amount = 100_000L)
        stubExistingTransaction(old)
        stubSummary(period = "2026-08", totalExpense = 100_000L, categoryId = "cat-food")

        repo.updateTransaction(old.copy(amount = 150_000L))

        coVerify {
            local.applyUpdatedTransactionAtomically(
                updated = match {
                    it.id == "tx-1" &&
                        it.amount == 150_000L &&
                        it.syncStatus == SyncStatus.PENDING.name
                },
                oldWalletId = "wallet-1",
                oldWalletDelta = 100_000L,
                newWalletDelta = -150_000L,
                oldBudgetId = "b-personal",
                oldBudgetDelta = -100_000L,
                newBudgetId = "b-personal",
                newBudgetDelta = 150_000L,
                summaryUpserts = match { it.size == 1 && it.first().totalExpense == 150_000L },
            )
        }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `updateTransaction expense to income reverses budget and doubles wallet`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val personalBudget = budgetEntity(id = "b-personal", familyId = null)
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), "cat-food") } returns personalBudget
        val old = domainTransaction(amount = 15_000L)
        stubExistingTransaction(old)

        repo.updateTransaction(old.copy(type = TransactionType.INCOME))

        coVerify {
            local.applyUpdatedTransactionAtomically(
                updated = match { it.type == TransactionType.INCOME.value },
                oldWalletId = "wallet-1",
                oldWalletDelta = 15_000L,
                newWalletDelta = 15_000L,
                oldBudgetId = "b-personal",
                oldBudgetDelta = -15_000L,
                newBudgetId = null,
                newBudgetDelta = 0L,
                summaryUpserts = any(),
            )
        }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `updateTransaction wallet change passes both wallet ids`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val old = domainTransaction(walletId = "wallet-1", familyId = null)
        stubExistingTransaction(old)
        val familyBudget = budgetEntity(id = "b-fam", familyId = "fam-1")
        coEvery {
            budgetLocal.getByMonthCategoryPersonal(any(), "cat-food")
        } returns budgetEntity(id = "b-personal", familyId = null)
        coEvery {
            budgetLocal.getByMonthCategoryAndFamily(any(), "cat-food", "fam-1")
        } returns familyBudget

        repo.updateTransaction(old.copy(walletId = "wallet-fam", familyId = "fam-1"))

        coVerify {
            local.applyUpdatedTransactionAtomically(
                updated = match { it.walletId == "wallet-fam" && it.familyId == "fam-1" },
                oldWalletId = "wallet-1",
                oldWalletDelta = 15_000L,
                newWalletDelta = -15_000L,
                oldBudgetId = "b-personal",
                oldBudgetDelta = -15_000L,
                newBudgetId = "b-fam",
                newBudgetDelta = 15_000L,
                summaryUpserts = any(),
            )
        }
    }

    @Test
    fun `updateTransaction date across payday cycle writes two summary periods`() = runTest {
        stubCycleStartDay(25)
        stubLookupsForWrite()
        val oldDate = LocalDate.of(2026, 7, 26).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val newDate = LocalDate.of(2026, 7, 20).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val old = domainTransaction(date = oldDate)
        stubExistingTransaction(old)
        stubSummary(
            period = "2026-08",
            totalExpense = 15_000L,
            categoryId = "cat-food",
        )

        repo.updateTransaction(old.copy(date = newDate))

        coVerify { budgetLocal.getByMonthCategoryPersonal("2026-08", "cat-food") }
        coVerify { budgetLocal.getByMonthCategoryPersonal("2026-07", "cat-food") }
        coVerify {
            local.applyUpdatedTransactionAtomically(
                updated = any(),
                oldWalletId = "wallet-1",
                oldWalletDelta = 15_000L,
                newWalletDelta = -15_000L,
                oldBudgetId = null,
                oldBudgetDelta = 0L,
                newBudgetId = null,
                newBudgetDelta = 0L,
                summaryUpserts = match { upserts ->
                    upserts.map { it.period }.containsAll(listOf("2026-08", "2026-07")) &&
                        upserts.size == 2
                },
            )
        }
    }

    @Test
    fun `updateTransaction category change reverses old budget and applies new`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val oldBudget = budgetEntity(id = "b-food", familyId = null, categoryId = "cat-food")
        val newBudget = budgetEntity(id = "b-transport", familyId = null, categoryId = "cat-transport")
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), "cat-food") } returns oldBudget
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), "cat-transport") } returns newBudget
        val old = domainTransaction(categoryId = "cat-food")
        stubExistingTransaction(old)

        repo.updateTransaction(old.copy(categoryId = "cat-transport"))

        coVerify {
            local.applyUpdatedTransactionAtomically(
                updated = match { it.categoryId == "cat-transport" },
                oldWalletId = "wallet-1",
                oldWalletDelta = 15_000L,
                newWalletDelta = -15_000L,
                oldBudgetId = "b-food",
                oldBudgetDelta = -15_000L,
                newBudgetId = "b-transport",
                newBudgetDelta = 15_000L,
                summaryUpserts = any(),
            )
        }
    }

    @Test
    fun `updateTransaction note only still upserts pending with cancelling deltas`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val old = domainTransaction(note = "lama")
        stubExistingTransaction(old)

        repo.updateTransaction(old.copy(note = "baru"))

        coVerify {
            local.applyUpdatedTransactionAtomically(
                updated = match { it.note == "baru" && it.syncStatus == SyncStatus.PENDING.name },
                oldWalletId = "wallet-1",
                oldWalletDelta = 15_000L,
                newWalletDelta = -15_000L,
                oldBudgetId = null,
                oldBudgetDelta = 0L,
                newBudgetId = null,
                newBudgetDelta = 0L,
                summaryUpserts = match { it.size == 1 },
            )
        }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `updateTransaction missing id is no-op`() = runTest {
        stubCycleStartDay(1)
        coEvery { local.getById("tx-missing") } returns null

        repo.updateTransaction(domainTransaction(id = "tx-missing"))

        coVerify(exactly = 0) {
            local.applyUpdatedTransactionAtomically(
                updated = any(),
                oldWalletId = any(),
                oldWalletDelta = any(),
                newWalletDelta = any(),
                oldBudgetId = any(),
                oldBudgetDelta = any(),
                newBudgetId = any(),
                newBudgetDelta = any(),
                summaryUpserts = any(),
            )
        }
        verify(exactly = 0) { syncScheduler.enqueueSync() }
    }

    @Test
    fun `deleteTransaction reverses wallet budget and summary then deletes`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val personalBudget = budgetEntity(id = "b-personal", familyId = null)
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), "cat-food") } returns personalBudget
        stubExistingTransaction(domainTransaction().copy(syncStatus = SyncStatus.SYNCED))
        stubSummary(period = "2026-08", totalExpense = 15_000L, categoryId = "cat-food")

        repo.deleteTransaction("tx-1")

        coVerify {
            local.applyDeletedTransactionAtomically(
                id = "tx-1",
                walletId = "wallet-1",
                walletDelta = 15_000L,
                budgetId = "b-personal",
                budgetDelta = -15_000L,
                summaryUpsert = match { it.totalExpense == 0L },
                pendingDelete = match {
                    it.id == "tx-1" &&
                        it.walletId == "wallet-1" &&
                        it.userId == "user-1" &&
                        it.familyId == null &&
                        it.type == TransactionType.EXPENSE.value &&
                        it.amount == 15_000L &&
                        it.categoryId == "cat-food" &&
                        it.dateEpochMs == Instant.parse("2026-08-01T00:00:00Z").toEpochMilli() &&
                        it.syncStatus == SyncStatus.PENDING.name
                },
            )
        }
        coVerify(exactly = 0) { local.getPending() }
        verify { syncScheduler.enqueueSync() }
    }

    @Test
    fun `deleteTransaction missing id does not write or enqueue sync`() = runTest {
        coEvery { local.getById("tx-missing") } returns null

        repo.deleteTransaction("tx-missing")

        coVerify(exactly = 0) {
            local.applyDeletedTransactionAtomically(
                id = any(),
                walletId = any(),
                walletDelta = any(),
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = any(),
            )
        }
        coVerify(exactly = 0) { local.getPendingDeletes() }
        verify(exactly = 0) { syncScheduler.enqueueSync() }
    }

    @Test
    fun `updateTransaction does not write delete outbox`() = runTest {
        stubCycleStartDay(1)
        stubLookupsForWrite()
        val old = domainTransaction(note = "lama")
        stubExistingTransaction(old)

        repo.updateTransaction(old.copy(note = "baru"))

        coVerify(exactly = 0) {
            local.applyDeletedTransactionAtomically(
                id = any(),
                walletId = any(),
                walletDelta = any(),
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = any(),
            )
        }
        coVerify(exactly = 0) { local.getPendingDeletes() }
        verify { syncScheduler.enqueueSync() }
    }

    private fun stubCycleStartDay(day: Int) {
        every { periodPreferences.observe() } returns flowOf(PeriodPreferences(cycleStartDay = day))
    }

    private fun stubBudgetLookups() {
        stubLookupsForWrite()
        coEvery { local.applyNewTransactionAtomically(any(), any(), any(), any()) } just runs
    }

    private fun stubLookupsForWrite() {
        coEvery { budgetLocal.getByMonthCategoryPersonal(any(), any()) } returns null
        coEvery { budgetLocal.getByMonthCategoryAndFamily(any(), any(), any()) } returns null
        coEvery { summaryLocal.getByPeriod(any(), any()) } returns null
        coEvery { categoryLocal.getById(any()) } returns null
        coEvery {
            local.applyUpdatedTransactionAtomically(
                updated = any(),
                oldWalletId = any(),
                oldWalletDelta = any(),
                newWalletDelta = any(),
                oldBudgetId = any(),
                oldBudgetDelta = any(),
                newBudgetId = any(),
                newBudgetDelta = any(),
                summaryUpserts = any(),
            )
        } just runs
        coEvery {
            local.applyDeletedTransactionAtomically(
                id = any(),
                walletId = any(),
                walletDelta = any(),
                budgetId = any(),
                budgetDelta = any(),
                summaryUpsert = any(),
                pendingDelete = any(),
            )
        } just runs
    }

    private fun stubExistingTransaction(transaction: Transaction) {
        coEvery { local.getById(transaction.id) } returns mapper.toEntity(transaction)
    }

    private fun stubSummary(
        period: String,
        totalExpense: Long,
        categoryId: String,
    ) {
        val summary = CategorySummary(
            period = period,
            userId = "user-1",
            familyId = null,
            totalIncome = 0L,
            totalExpense = totalExpense,
            byCategory = mapOf(
                categoryId to CategoryBreakdown(
                    name = categoryId,
                    totalExpense = totalExpense,
                    totalIncome = 0L,
                    transactionCount = 1,
                ),
            ),
            topExpenseCategoryId = categoryId,
        )
        coEvery { summaryLocal.getByPeriod(period, "user-1") } returns summaryMapper.toEntity(summary)
    }

    private fun domainTransaction(
        id: String = "tx-1",
        walletId: String = "wallet-1",
        familyId: String? = null,
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 15_000L,
        categoryId: String = "cat-food",
        note: String? = null,
        date: Instant = Instant.parse("2026-08-01T00:00:00Z"),
    ) = Transaction(
        id = id,
        walletId = walletId,
        userId = "user-1",
        familyId = familyId,
        type = type,
        amount = amount,
        categoryId = categoryId,
        note = note,
        date = date,
        addedByName = "Irul",
        createdAt = Instant.parse("2026-08-01T00:00:00Z"),
    )

    private fun budgetEntity(
        id: String,
        familyId: String?,
        categoryId: String = "cat-food",
    ) = BudgetEntity(
        id = id,
        userId = "user-1",
        familyId = familyId,
        categoryId = categoryId,
        limit = 1_000_000L,
        spent = 0L,
        period = "monthly",
        month = "2026-08",
        walletId = "wallet-1",
        syncStatus = "SYNCED",
        createdAtEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
    )

    private fun sampleEntity() = TransactionEntity(
        id = "tx-1",
        walletId = "wallet-1",
        userId = "user-1",
        familyId = null,
        type = "expense",
        amount = 15_000L,
        categoryId = "cat-food",
        note = null,
        dateEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
        addedByName = "Irul",
        syncStatus = "PENDING",
        createdAtEpochMs = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli(),
    )
}
