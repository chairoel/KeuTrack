package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class DeleteFamilyBudgetUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
) {
    suspend operator fun invoke(budgetId: String): Result<Unit> {
        if (budgetId.isBlank()) {
            return Result.failure(IllegalArgumentException("Budget tidak ditemukan"))
        }

        val user = userRepository.getCurrentUser().first()
            ?: return Result.failure(IllegalStateException("User belum masuk"))
        val familyId = user.familyId?.takeIf { it.isNotBlank() }
            ?: return Result.failure(IllegalStateException("Buat atau gabung keluarga dulu"))
        if (FamilyRole.fromValue(user.familyRole.orEmpty()) != FamilyRole.OWNER) {
            return Result.failure(
                IllegalStateException("Hanya pemilik keluarga yang bisa mengatur target"),
            )
        }

        return try {
            val budget = budgetRepository.getBudgetById(budgetId)
                ?: return Result.failure(IllegalStateException("Budget tidak ditemukan"))
            if (budget.familyId != familyId) {
                return Result.failure(
                    IllegalStateException("Target ini bukan milik keluarga Anda"),
                )
            }
            budgetRepository.deleteBudget(budgetId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
