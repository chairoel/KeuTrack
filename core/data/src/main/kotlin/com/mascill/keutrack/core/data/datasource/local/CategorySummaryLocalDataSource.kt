package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import kotlinx.coroutines.flow.Flow

interface CategorySummaryLocalDataSource {
    fun observeByPeriod(period: String, userId: String): Flow<CategorySummaryEntity?>
    fun observeByPeriods(userId: String, periods: List<String>): Flow<List<CategorySummaryEntity>>
    suspend fun getByPeriod(period: String, userId: String): CategorySummaryEntity?
    suspend fun upsert(entity: CategorySummaryEntity)
}
