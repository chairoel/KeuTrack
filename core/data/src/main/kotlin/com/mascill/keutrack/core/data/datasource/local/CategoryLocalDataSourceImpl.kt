package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.dao.CategoryDao
import com.mascill.keutrack.core.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CategoryLocalDataSourceImpl @Inject constructor(
    private val dao: CategoryDao,
) : CategoryLocalDataSource {

    override fun observeAll(): Flow<List<CategoryEntity>> = dao.observeAll()

    override fun observeByType(type: String): Flow<List<CategoryEntity>> = dao.observeByType(type)

    override suspend fun getById(id: String): CategoryEntity? = dao.getById(id)

    override suspend fun count(): Int = dao.count()

    override suspend fun insertAll(entities: List<CategoryEntity>) {
        dao.insertAll(entities)
    }

    override suspend fun upsert(entity: CategoryEntity) {
        dao.upsert(entity)
    }
}
