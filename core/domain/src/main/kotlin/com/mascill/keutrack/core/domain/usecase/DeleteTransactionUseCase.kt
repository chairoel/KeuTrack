package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): TransactionWriteResult {
        if (id.isBlank()) {
            return TransactionWriteResult.Error.MissingId
        }
        return try {
            transactionRepository.deleteTransaction(id)
            TransactionWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TransactionWriteResult.Error.Unknown(e)
        }
    }
}
