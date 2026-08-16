package com.mascill.keutrack.core.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Category
import com.mascill.keutrack.core.domain.model.CategoryType
import com.mascill.keutrack.core.domain.repository.CategoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCategoriesUseCaseTest {

    private val repo = mockk<CategoryRepository>()
    private val useCase = GetCategoriesUseCase(repo)

    @Test
    fun `null type observes all categories`() = runTest {
        val categories = listOf(category("c-1", CategoryType.EXPENSE), category("c-2", CategoryType.INCOME))
        every { repo.observeCategories() } returns flowOf(categories)

        useCase().test {
            assertThat(awaitItem()).isEqualTo(categories)
            awaitComplete()
        }
        verify(exactly = 1) { repo.observeCategories() }
        verify(exactly = 0) { repo.observeCategoriesByType(any()) }
    }

    @Test
    fun `filters by income type`() = runTest {
        val income = listOf(category("c-inc", CategoryType.INCOME))
        every { repo.observeCategoriesByType(CategoryType.INCOME) } returns flowOf(income)

        useCase(CategoryType.INCOME).test {
            assertThat(awaitItem()).isEqualTo(income)
            awaitComplete()
        }
        verify(exactly = 1) { repo.observeCategoriesByType(CategoryType.INCOME) }
    }

    @Test
    fun `filters by expense type`() = runTest {
        val expense = listOf(category("c-exp", CategoryType.EXPENSE))
        every { repo.observeCategoriesByType(CategoryType.EXPENSE) } returns flowOf(expense)

        useCase(CategoryType.EXPENSE).test {
            assertThat(awaitItem()).isEqualTo(expense)
            awaitComplete()
        }
    }

    private fun category(id: String, type: CategoryType) = Category(
        id = id,
        name = id,
        icon = "icon",
        color = "#000",
        type = type,
        isDefault = true,
    )
}
