package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.BudgetPeriod
import com.mascill.keutrack.core.domain.model.FamilyRole
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.BudgetRepository
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.first
import java.time.DateTimeException
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class UpsertFamilyBudgetUseCase @Inject constructor(
    private val userRepository: UserRepository,
    private val budgetRepository: BudgetRepository,
    private val walletRepository: WalletRepository,
    private val transactionRepository: TransactionRepository,
) {
    data class Params(
        val categoryId: String,
        val limit: Long,
        val month: String,
    )

    suspend operator fun invoke(params: Params): Result<Budget> {
        if (params.limit <= 0L) {
            return Result.failure(IllegalArgumentException("Limit harus lebih dari 0"))
        }
        if (params.categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Kategori wajib dipilih"))
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

        val familyWallet =
            walletRepository.observeWalletsByType(WalletType.FAMILY).first()
                .filter { it.familyId == familyId }
                .minByOrNull { it.createdAt }
                ?: return Result.failure(IllegalStateException("Dompet keluarga belum siap"))

        if (!isValidMonth(params.month)) {
            return Result.failure(IllegalArgumentException("Format bulan tidak valid"))
        }

        return try {
            val existing =
                budgetRepository.findFamilyBudget(
                    familyId = familyId,
                    categoryId = params.categoryId,
                    month = params.month,
                )
            if (existing != null) {
                val updated = existing.copy(limit = params.limit)
                budgetRepository.updateBudget(updated)
                Result.success(updated)
            } else {
                val created =
                    Budget(
                        id = UUID.randomUUID().toString(),
                        userId = user.uid,
                        familyId = familyId,
                        categoryId = params.categoryId,
                        limit = params.limit,
                        spent = seedSpent(familyId, params.categoryId, params.month),
                        period = BudgetPeriod.MONTHLY,
                        month = params.month,
                        walletId = familyWallet.id,
                        syncStatus = SyncStatus.PENDING,
                    )
                budgetRepository.createBudget(created)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun seedSpent(
        familyId: String,
        categoryId: String,
        month: String,
    ): Long =
        transactionRepository
            .observeTransactions(
                familyId = familyId,
                type = TransactionType.EXPENSE,
                categoryId = categoryId,
                limit = SEED_TX_LIMIT,
            )
            .first()
            .filter { it.toMonthKey() == month }
            .sumOf { it.amount }

    private fun Transaction.toMonthKey(): String =
        YearMonth.from(date.atZone(ZoneId.systemDefault())).toString()

    private fun isValidMonth(month: String): Boolean =
        try {
            YearMonth.parse(month)
            true
        } catch (_: DateTimeException) {
            false
        }

    private companion object {
        const val SEED_TX_LIMIT = 1_000
    }
}
