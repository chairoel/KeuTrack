package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {

    fun observeCategories(): Flow<List<Category>>

    fun observeCategoriesByType(type: CategoryType): Flow<List<Category>>

    suspend fun getCategoryById(id: String): Category?

    suspend fun seedDefaultCategories()
}
