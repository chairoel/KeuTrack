package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import java.util.concurrent.CancellationException
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Unit> {
        if (transaction.amount <= 0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        if (transaction.walletId.isBlank()) {
            return Result.failure(IllegalArgumentException("Wallet must be selected"))
        }
        if (transaction.categoryId.isBlank()) {
            return Result.failure(IllegalArgumentException("Category must be selected"))
        }
        return try {
            transactionRepository.addTransaction(transaction)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
