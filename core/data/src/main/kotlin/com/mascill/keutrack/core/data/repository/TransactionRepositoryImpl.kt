package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.findBudgetForExpense
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.mapper.TransactionMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.CategoryBreakdown
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.PeriodTotals
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.PeriodPreferencesRepository
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val local: TransactionLocalDataSource,
    private val walletLocal: WalletLocalDataSource,
    private val budgetLocal: BudgetLocalDataSource,
    private val summaryLocal: CategorySummaryLocalDataSource,
    private val categoryLocal: CategoryLocalDataSource,
    private val mapper: TransactionMapper,
    private val summaryMapper: CategorySummaryMapper,
    private val syncScheduler: SyncScheduler,
    private val periodPreferences: PeriodPreferencesRepository,
) : TransactionRepository {

    override fun observeTransactions(
        walletId: String?,
        familyId: String?,
        type: TransactionType?,
        categoryId: String?,
        startDate: Instant?,
        endDate: Instant?,
        limit: Int,
    ): Flow<List<Transaction>> =
        local.observeFiltered(
            walletId = walletId,
            familyId = familyId,
            type = type?.value,
            categoryId = categoryId,
            startMs = startDate?.toEpochMilli(),
            endMs = endDate?.toEpochMilli(),
            limit = limit,
        ).map { entities -> entities.map(mapper::toDomain) }

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        local.observeRecent(limit).map { entities -> entities.map(mapper::toDomain) }

    override fun observePeriodTotals(
        walletId: String?,
        familyId: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Flow<PeriodTotals> =
        local.observeSumsByType(
            walletId = walletId,
            familyId = familyId,
            startMs = startDate?.toEpochMilli(),
            endMs = endDate?.toEpochMilli(),
        ).map { rows ->
            var income = 0L
            var expense = 0L
            rows.forEach { row ->
                when (row.type) {
                    TransactionType.INCOME.value -> income = row.total
                    TransactionType.EXPENSE.value -> expense = row.total
                }
            }
            PeriodTotals(incomeTotal = income, expenseTotal = expense)
        }

    override suspend fun getTransactionById(id: String): Transaction? =
        local.getById(id)?.let(mapper::toDomain)

    override suspend fun addTransaction(transaction: Transaction) {
        try {
            val pending = transaction.copy(syncStatus = SyncStatus.PENDING)
            val walletDelta = walletDeltaFor(pending)
            val month = monthKey(pending)
            val budget =
                if (pending.type == TransactionType.EXPENSE) {
                    budgetLocal.findBudgetForExpense(
                        month = month,
                        categoryId = pending.categoryId,
                        familyId = pending.familyId,
                    )
                } else {
                    null
                }
            val budgetId = budget?.id
            val summaryEntity = buildUpdatedSummary(pending, month)

            local.applyNewTransactionAtomically(
                transaction = mapper.toEntity(pending),
                walletDelta = walletDelta,
                budgetIdToIncrement = budgetId,
                summaryUpsert = summaryMapper.toEntity(summaryEntity),
            )
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    /**
     * MVP: replace entity + mark PENDING. Does not reverse/reapply side-effects for amount/category
     * changes — callers should prefer delete+add for material edits until Phase 5 hardens this.
     */
    override suspend fun updateTransaction(transaction: Transaction) {
        try {
            val pending = transaction.copy(syncStatus = SyncStatus.PENDING)
            local.upsert(mapper.toEntity(pending))
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    /**
     * MVP: delete local row and reverse wallet balance; summary/budget not fully recomputed.
     */
    override suspend fun deleteTransaction(id: String) {
        try {
            val existing = local.getById(id) ?: return
            val domain = mapper.toDomain(existing)
            val reverseDelta = -walletDeltaFor(domain)
            walletLocal.applyBalanceDelta(
                walletId = domain.walletId,
                delta = reverseDelta,
                syncStatus = SyncStatus.PENDING,
            )
            local.delete(id)
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    private suspend fun buildUpdatedSummary(
        transaction: Transaction,
        month: String,
    ): CategorySummary {
        val current = summaryLocal.getByPeriod(month, transaction.userId)
            ?.let(summaryMapper::toDomain)
            ?: CategorySummary(
                period = month,
                userId = transaction.userId,
                familyId = transaction.familyId,
                totalIncome = 0L,
                totalExpense = 0L,
                byCategory = emptyMap(),
            )

        val incomeDelta = if (transaction.type == TransactionType.INCOME) transaction.amount else 0L
        val expenseDelta = if (transaction.type == TransactionType.EXPENSE) transaction.amount else 0L
        val categoryName = categoryLocal.getById(transaction.categoryId)?.name
            ?: transaction.categoryId

        val existingBreakdown = current.byCategory[transaction.categoryId]
            ?: CategoryBreakdown(
                name = categoryName,
                totalExpense = 0L,
                totalIncome = 0L,
                transactionCount = 0,
            )

        val updatedBreakdown = existingBreakdown.copy(
            name = categoryName,
            totalExpense = existingBreakdown.totalExpense + expenseDelta,
            totalIncome = existingBreakdown.totalIncome + incomeDelta,
            transactionCount = existingBreakdown.transactionCount + 1,
        )

        val byCategory = current.byCategory + (transaction.categoryId to updatedBreakdown)
        val totalExpense = current.totalExpense + expenseDelta
        val topExpenseCategoryId = byCategory
            .maxByOrNull { it.value.totalExpense }
            ?.takeIf { it.value.totalExpense > 0 }
            ?.key

        return current.copy(
            totalIncome = current.totalIncome + incomeDelta,
            totalExpense = totalExpense,
            byCategory = byCategory,
            topExpenseCategoryId = topExpenseCategoryId,
        )
    }

    private fun walletDeltaFor(transaction: Transaction): Long =
        when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> -transaction.amount
        }

    private suspend fun monthKey(transaction: Transaction): String {
        val startDay = periodPreferences.observe().first().cycleStartDay
        val date = transaction.date.atZone(ZoneId.systemDefault()).toLocalDate()
        return PeriodBounds.periodKey(date, startDay)
    }
}
