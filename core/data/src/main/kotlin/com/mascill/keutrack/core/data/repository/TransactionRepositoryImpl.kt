package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.common.utils.PeriodBounds
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.findBudgetForExpense
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
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
            val month = monthKey(pending)
            val summary = summaryAfterDelta(
                base = summaryOrEmpty(month, pending),
                transaction = pending,
                sign = +1,
            )

            local.applyNewTransactionAtomically(
                transaction = mapper.toEntity(pending),
                walletDelta = walletDeltaFor(pending),
                budgetIdToIncrement = budgetMatch(pending, month)?.id,
                summaryUpsert = summaryMapper.toEntity(summary),
            )
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        try {
            val existing = local.getById(transaction.id) ?: return
            val old = mapper.toDomain(existing)
            val pending = transaction.copy(syncStatus = SyncStatus.PENDING)
            val oldMonth = monthKey(old)
            val newMonth = monthKey(pending)
            val oldBudget = budgetMatch(old, oldMonth)
            val newBudget = budgetMatch(pending, newMonth)

            local.applyUpdatedTransactionAtomically(
                updated = mapper.toEntity(pending),
                oldWalletId = old.walletId,
                oldWalletDelta = -walletDeltaFor(old),
                newWalletDelta = walletDeltaFor(pending),
                oldBudgetId = oldBudget?.id,
                oldBudgetDelta = budgetDeltaFor(old, oldBudget, sign = -1),
                newBudgetId = newBudget?.id,
                newBudgetDelta = budgetDeltaFor(pending, newBudget, sign = +1),
                summaryUpserts = buildSummaryUpserts(old, pending, oldMonth, newMonth),
            )
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun deleteTransaction(id: String) {
        try {
            val existing = local.getById(id) ?: return
            val old = mapper.toDomain(existing)
            val month = monthKey(old)
            val budget = budgetMatch(old, month)
            val summary = summaryAfterDelta(
                base = summaryOrEmpty(month, old),
                transaction = old,
                sign = -1,
            )

            local.applyDeletedTransactionAtomically(
                id = id,
                walletId = old.walletId,
                walletDelta = -walletDeltaFor(old),
                budgetId = budget?.id,
                budgetDelta = budgetDeltaFor(old, budget, sign = -1),
                summaryUpsert = summaryMapper.toEntity(summary),
            )
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    private suspend fun buildSummaryUpserts(
        old: Transaction,
        pending: Transaction,
        oldMonth: String,
        newMonth: String,
    ): List<CategorySummaryEntity> {
        val sameSummaryRow = oldMonth == newMonth && old.userId == pending.userId
        return if (sameSummaryRow) {
            val afterReverse = summaryAfterDelta(
                base = summaryOrEmpty(oldMonth, pending),
                transaction = old,
                sign = -1,
            )
            val afterApply = summaryAfterDelta(
                base = afterReverse,
                transaction = pending,
                sign = +1,
            )
            listOf(summaryMapper.toEntity(afterApply))
        } else {
            listOf(
                summaryMapper.toEntity(
                    summaryAfterDelta(
                        base = summaryOrEmpty(oldMonth, old),
                        transaction = old,
                        sign = -1,
                    ),
                ),
                summaryMapper.toEntity(
                    summaryAfterDelta(
                        base = summaryOrEmpty(newMonth, pending),
                        transaction = pending,
                        sign = +1,
                    ),
                ),
            )
        }
    }

    private suspend fun summaryOrEmpty(month: String, transaction: Transaction): CategorySummary =
        summaryLocal.getByPeriod(month, transaction.userId)
            ?.let(summaryMapper::toDomain)
            ?: CategorySummary(
                period = month,
                userId = transaction.userId,
                familyId = transaction.familyId,
                totalIncome = 0L,
                totalExpense = 0L,
                byCategory = emptyMap(),
            )

    private suspend fun summaryAfterDelta(
        base: CategorySummary,
        transaction: Transaction,
        sign: Int,
    ): CategorySummary {
        val incomeDelta =
            if (transaction.type == TransactionType.INCOME) transaction.amount * sign else 0L
        val expenseDelta =
            if (transaction.type == TransactionType.EXPENSE) transaction.amount * sign else 0L
        val categoryName = categoryLocal.getById(transaction.categoryId)?.name
            ?: transaction.categoryId

        val existingBreakdown = base.byCategory[transaction.categoryId]
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
            transactionCount = (existingBreakdown.transactionCount + sign).coerceAtLeast(0),
        )

        val byCategory = base.byCategory + (transaction.categoryId to updatedBreakdown)
        val totalExpense = base.totalExpense + expenseDelta
        val topExpenseCategoryId = byCategory
            .maxByOrNull { it.value.totalExpense }
            ?.takeIf { it.value.totalExpense > 0 }
            ?.key

        return base.copy(
            totalIncome = base.totalIncome + incomeDelta,
            totalExpense = totalExpense,
            byCategory = byCategory,
            topExpenseCategoryId = topExpenseCategoryId,
        )
    }

    private suspend fun budgetMatch(transaction: Transaction, month: String): BudgetEntity? =
        if (transaction.type == TransactionType.EXPENSE) {
            budgetLocal.findBudgetForExpense(
                month = month,
                categoryId = transaction.categoryId,
                familyId = transaction.familyId,
            )
        } else {
            null
        }

    private fun budgetDeltaFor(
        transaction: Transaction,
        budget: BudgetEntity?,
        sign: Int,
    ): Long =
        if (transaction.type == TransactionType.EXPENSE && budget != null) {
            transaction.amount * sign
        } else {
            0L
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
