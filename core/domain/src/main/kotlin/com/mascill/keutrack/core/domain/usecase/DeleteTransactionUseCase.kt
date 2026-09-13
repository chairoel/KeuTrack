package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.TransactionWriteResult
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import com.mascill.keutrack.core.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(id: String): TransactionWriteResult {
        if (id.isBlank()) {
            return TransactionWriteResult.Error.MissingId
        }
        return try {
            val existing = transactionRepository.getTransactionById(id)
            if (existing != null) {
                val currentUid = userRepository.getCurrentUser().first()?.uid
                if (currentUid.isNullOrBlank() || currentUid != existing.userId) {
                    return TransactionWriteResult.Error.NotOwner
                }
            }
            transactionRepository.deleteTransaction(id)
            TransactionWriteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TransactionWriteResult.Error.Unknown(e)
        }
    }
}
