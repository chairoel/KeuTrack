package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

interface CategoryLocalDataSource {
    fun observeAll(): Flow<List<CategoryEntity>>
    fun observeByType(type: String): Flow<List<CategoryEntity>>
    suspend fun getById(id: String): CategoryEntity?
    suspend fun count(): Int
    suspend fun insertAll(entities: List<CategoryEntity>)
    suspend fun upsert(entity: CategoryEntity)
}
