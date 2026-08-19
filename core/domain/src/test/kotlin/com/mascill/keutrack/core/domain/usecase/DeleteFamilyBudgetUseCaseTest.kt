package com.mascill.keutrack.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.User
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.time.Instant

class DeleteFamilyBudgetUseCaseTest {

    private val userRepo = FakeUserRepository()
    private val budgetRepo = mockk<BudgetRepository>()
    private val useCase = DeleteFamilyBudgetUseCase(userRepo, budgetRepo)

    @Before
    fun setUp() {
        userRepo.currentUser.value = owner()
        coEvery { budgetRepo.getBudgetById(BUDGET_ID) } returns familyBudget()
        coEvery { budgetRepo.deleteBudget(any()) } just runs
    }

    @Test
    fun `member is rejected`() = runTest {
        userRepo.currentUser.value = owner(familyRole = FamilyRole.MEMBER.value)

        val result = useCase(BUDGET_ID)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("pemilik keluarga")
        coVerify(exactly = 0) { budgetRepo.deleteBudget(any()) }
    }

    @Test
    fun `owner deletes budget for current family`() = runTest {
        val result = useCase(BUDGET_ID)

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { budgetRepo.deleteBudget(BUDGET_ID) }
    }

    @Test
    fun `refuses to delete personal or other family budget`() = runTest {
        coEvery { budgetRepo.getBudgetById(BUDGET_ID) } returns
            familyBudget().copy(familyId = "fam-other")

        val result = useCase(BUDGET_ID)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("bukan milik keluarga")
        coVerify(exactly = 0) { budgetRepo.deleteBudget(any()) }
    }

    private fun owner(
        familyRole: String? = FamilyRole.OWNER.value,
    ) = User(
        uid = USER_ID,
        displayName = "Irul",
        email = "irul@example.com",
        photoUrl = null,
        familyId = FAMILY_ID,
        familyRole = familyRole,
    )

    private fun familyBudget() =
        Budget(
            id = BUDGET_ID,
            userId = USER_ID,
            familyId = FAMILY_ID,
            categoryId = "cat-food",
            limit = 1_000_000L,
            spent = 400_000L,
            month = "2026-08",
            createdAt = Instant.parse("2026-08-01T00:00:00Z"),
        )

    private companion object {
        const val USER_ID = "user-1"
        const val FAMILY_ID = "fam-1"
        const val BUDGET_ID = "b-1"
    }
}
