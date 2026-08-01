package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.dao.BudgetDao
import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BudgetLocalDataSourceImpl @Inject constructor(
    private val dao: BudgetDao,
) : BudgetLocalDataSource {

    override fun observeByMonth(month: String): Flow<List<BudgetEntity>> =
        dao.observeByMonth(month)

    override suspend fun getById(id: String): BudgetEntity? = dao.getById(id)

    override suspend fun getByMonthAndCategory(
        month: String,
        categoryId: String,
    ): BudgetEntity? = dao.getByMonthAndCategory(month, categoryId)

    override suspend fun upsert(entity: BudgetEntity) {
        dao.upsert(entity)
    }

    override suspend fun delete(budgetId: String) {
        dao.deleteById(budgetId)
    }

    override suspend fun getPending(): List<BudgetEntity> = dao.getPending()

    override suspend fun updateSyncStatus(id: String, status: SyncStatus) {
        dao.updateSyncStatus(id, status.name)
    }

    override suspend fun applySpentDelta(
        budgetId: String,
        delta: Long,
        syncStatus: SyncStatus,
    ) {
        dao.applySpentDelta(budgetId, delta, syncStatus.name)
    }
}
