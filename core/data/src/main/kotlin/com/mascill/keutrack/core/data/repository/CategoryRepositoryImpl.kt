package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSource
import com.mascill.keutrack.core.data.mapper.CategoryMapper
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Categories are local-first. Default seed runs once when the table is empty
 * (lazy on first [observeCategories] / [seedDefaultCategories]).
 */
@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val local: CategoryLocalDataSource,
    private val mapper: CategoryMapper,
) : CategoryRepository {

    private val seedMutex = Mutex()

    override fun observeCategories(): Flow<List<Category>> =
        local.observeAll()
            .onStart { ensureDefaultsSeeded() }
            .map { entities -> entities.map(mapper::toDomain) }

    override fun observeCategoriesByType(type: CategoryType): Flow<List<Category>> =
        local.observeByType(type.value)
            .onStart { ensureDefaultsSeeded() }
            .map { entities -> entities.map(mapper::toDomain) }

    override suspend fun getCategoryById(id: String): Category? =
        local.getById(id)?.let(mapper::toDomain)

    override suspend fun seedDefaultCategories() {
        ensureDefaultsSeeded()
    }

    private suspend fun ensureDefaultsSeeded() {
        try {
            seedMutex.withLock {
                if (local.count() > 0) return
                local.insertAll(DEFAULT_CATEGORIES.map(mapper::toEntity))
            }
        } catch (e: CancellationException) {
            throw e
        }
    }

    private companion object {
        val DEFAULT_CATEGORIES = listOf(
            Category(
                id = "cat_makanan",
                name = "Makanan",
                icon = "Restaurant",
                color = "#FF7043",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_transport",
                name = "Transport",
                icon = "DirectionsCar",
                color = "#42A5F5",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_tagihan",
                name = "Tagihan",
                icon = "Receipt",
                color = "#AB47BC",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_pendidikan",
                name = "Pendidikan",
                icon = "School",
                color = "#26A69A",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_hiburan",
                name = "Hiburan",
                icon = "Movie",
                color = "#EC407A",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_kesehatan",
                name = "Kesehatan",
                icon = "LocalHospital",
                color = "#EF5350",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_belanja",
                name = "Belanja",
                icon = "ShoppingCart",
                color = "#FFA726",
                type = CategoryType.EXPENSE,
                isDefault = true,
            ),
            Category(
                id = "cat_gaji",
                name = "Gaji",
                icon = "Payments",
                color = "#66BB6A",
                type = CategoryType.INCOME,
                isDefault = true,
            ),
            Category(
                id = "cat_investasi",
                name = "Investasi",
                icon = "TrendingUp",
                color = "#29B6F6",
                type = CategoryType.INCOME,
                isDefault = true,
            ),
            Category(
                id = "cat_lainnya",
                name = "Lainnya",
                icon = "MoreHoriz",
                color = "#78909C",
                type = CategoryType.BOTH,
                isDefault = true,
            ),
        )
    }
}
