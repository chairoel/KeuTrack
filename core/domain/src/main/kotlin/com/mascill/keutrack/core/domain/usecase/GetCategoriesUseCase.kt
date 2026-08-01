package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(type: CategoryType? = null): Flow<List<Category>> =
        if (type != null) {
            categoryRepository.observeCategoriesByType(type)
        } else {
            categoryRepository.observeCategories()
        }
}
