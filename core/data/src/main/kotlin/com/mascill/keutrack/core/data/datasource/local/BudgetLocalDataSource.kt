package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface BudgetLocalDataSource {
    fun observeByMonth(month: String): Flow<List<BudgetEntity>>
    suspend fun getById(id: String): BudgetEntity?
    suspend fun getByMonthAndCategory(month: String, categoryId: String): BudgetEntity?
    suspend fun upsert(entity: BudgetEntity)
    suspend fun delete(budgetId: String)
    suspend fun getPending(): List<BudgetEntity>
    suspend fun updateSyncStatus(id: String, status: SyncStatus)
    suspend fun applySpentDelta(budgetId: String, delta: Long, syncStatus: SyncStatus)
}
