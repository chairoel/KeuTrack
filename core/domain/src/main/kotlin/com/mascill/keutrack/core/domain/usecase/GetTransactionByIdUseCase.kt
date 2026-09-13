package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionByIdUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): Transaction? {
        if (id.isBlank()) return null
        return transactionRepository.getTransactionById(id)
    }
}
