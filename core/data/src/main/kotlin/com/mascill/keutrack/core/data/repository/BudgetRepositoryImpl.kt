package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.mapper.BudgetMapper
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val local: BudgetLocalDataSource,
    private val summaryLocal: CategorySummaryLocalDataSource,
    private val budgetMapper: BudgetMapper,
    private val summaryMapper: CategorySummaryMapper,
    private val authNetworkDataSource: AuthNetworkDataSource,
    private val syncScheduler: SyncScheduler,
) : BudgetRepository {

    override fun observeBudgets(month: String): Flow<List<Budget>> =
        local.observeByMonth(month).map { entities -> entities.map(budgetMapper::toDomain) }

    override suspend fun getBudgetById(id: String): Budget? =
        local.getById(id)?.let(budgetMapper::toDomain)

    override suspend fun findFamilyBudget(
        familyId: String,
        categoryId: String,
        month: String,
    ): Budget? =
        local.getByMonthCategoryAndFamily(month, categoryId, familyId)
            ?.let(budgetMapper::toDomain)

    override suspend fun createBudget(budget: Budget) {
        try {
            val pending =
                budget.copy(
                    spent = budget.spent.coerceAtLeast(0L),
                    syncStatus = SyncStatus.PENDING,
                )
            local.upsert(budgetMapper.toEntity(pending))
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun updateBudget(budget: Budget) {
        try {
            val pending = budget.copy(syncStatus = SyncStatus.PENDING)
            local.upsert(budgetMapper.toEntity(pending))
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun deleteBudget(budgetId: String) {
        try {
            local.delete(budgetId)
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override fun observeMonthlySummary(month: String): Flow<CategorySummary?> {
        val userId = authNetworkDataSource.getCurrentUser()?.uid
            ?: return flowOf(null)
        return summaryLocal.observeByPeriod(month, userId)
            .map { entity -> entity?.let(summaryMapper::toDomain) }
    }

    override fun observeMonthlySummaries(months: List<String>): Flow<List<CategorySummary>> {
        val userId = authNetworkDataSource.getCurrentUser()?.uid
            ?: return flowOf(emptyList())
        if (months.isEmpty()) return flowOf(emptyList())
        return summaryLocal.observeByPeriods(userId, months)
            .map { entities -> entities.map(summaryMapper::toDomain) }
    }
}
