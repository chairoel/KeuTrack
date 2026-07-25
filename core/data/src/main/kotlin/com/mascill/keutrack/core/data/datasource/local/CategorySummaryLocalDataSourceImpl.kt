package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.dao.CategorySummaryDao
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategorySummaryLocalDataSourceImpl @Inject constructor(
    private val dao: CategorySummaryDao,
) : CategorySummaryLocalDataSource {

    override fun observeByPeriod(
        period: String,
        userId: String,
    ): Flow<CategorySummaryEntity?> = dao.observeByPeriod(period, userId)

    override fun observeByPeriods(
        userId: String,
        periods: List<String>,
    ): Flow<List<CategorySummaryEntity>> = dao.observeByPeriods(userId, periods)

    override suspend fun getByPeriod(
        period: String,
        userId: String,
    ): CategorySummaryEntity? = dao.getByPeriod(period, userId)

    override suspend fun upsert(entity: CategorySummaryEntity) {
        dao.upsert(entity)
    }
}
