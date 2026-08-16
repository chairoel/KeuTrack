package com.mascill.keutrack.core.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.data.datasource.local.CategoryLocalDataSource
import com.mascill.keutrack.core.data.db.entity.CategoryEntity
import com.mascill.keutrack.core.data.mapper.CategoryMapper
import com.mascill.keutrack.core.domain.model.CategoryType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CategoryRepositoryImplTest {

    private val local = mockk<CategoryLocalDataSource>(relaxed = true)
    private val mapper = CategoryMapper()
    private val repo = CategoryRepositoryImpl(local, mapper)

    @Test
    fun `observeCategories seeds defaults when table is empty`() = runTest {
        coEvery { local.count() } returns 0
        coEvery { local.insertAll(any()) } just runs
        every { local.observeAll() } returns flowOf(emptyList())

        repo.observeCategories().test {
            awaitItem()
            awaitComplete()
        }

        coVerify { local.insertAll(match { it.size == 10 }) }
    }

    @Test
    fun `observeCategories does not reseed when rows exist`() = runTest {
        coEvery { local.count() } returns 3
        every { local.observeAll() } returns flowOf(listOf(entity("cat_makanan", "expense")))

        repo.observeCategories().test {
            val items = awaitItem()
            assertThat(items.first().id).isEqualTo("cat_makanan")
            awaitComplete()
        }
        coVerify(exactly = 0) { local.insertAll(any()) }
    }

    @Test
    fun `observeCategoriesByType filters via local source`() = runTest {
        every { local.observeByType("income") } returns flowOf(listOf(entity("cat_gaji", "income")))
        coEvery { local.count() } returns 1

        repo.observeCategoriesByType(CategoryType.INCOME).test {
            val items = awaitItem()
            assertThat(items.first().type).isEqualTo(CategoryType.INCOME)
            awaitComplete()
        }
    }

    private fun entity(id: String, type: String) = CategoryEntity(
        id = id,
        userId = null,
        familyId = null,
        name = id,
        icon = "icon",
        color = "#000",
        type = type,
        isDefault = true,
    )
}
