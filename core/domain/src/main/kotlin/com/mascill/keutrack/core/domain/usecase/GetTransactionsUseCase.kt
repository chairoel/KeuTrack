package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
) {
    data class Params(
        val walletId: String? = null,
        val type: TransactionType? = null,
        val categoryId: String? = null,
        val startDate: Instant? = null,
        val endDate: Instant? = null,
        val limit: Int = 50,
    )

    operator fun invoke(params: Params = Params()): Flow<List<Transaction>> =
        transactionRepository.observeTransactions(
            walletId = params.walletId,
            type = params.type,
            categoryId = params.categoryId,
            startDate = params.startDate,
            endDate = params.endDate,
            limit = params.limit,
        )
}
