package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(transaction: Transaction): TransactionWriteResult {
        if (transaction.id.isBlank()) {
            return TransactionWriteResult.Error.MissingId
        }
        if (transaction.amount <= 0) {
            return TransactionWriteResult.Error.InvalidAmount
        }
        if (transaction.walletId.isBlank()) {
            return TransactionWriteResult.Error.MissingWallet
        }
        if (transaction.categoryId.isBlank()) {
            return TransactionWriteResult.Error.MissingCategory
        }
        return try {
            transactionRepository.getTransactionById(transaction.id)
                ?: return TransactionWriteResult.Error.NotFound
            transactionRepository.updateTransaction(transaction)
            TransactionWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TransactionWriteResult.Error.Unknown(e)
        }
    }
}
