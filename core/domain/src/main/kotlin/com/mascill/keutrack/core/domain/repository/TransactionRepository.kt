package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface TransactionRepository {

    fun observeTransactions(
        walletId: String? = null,
        type: TransactionType? = null,
        categoryId: String? = null,
        startDate: Instant? = null,
        endDate: Instant? = null,
        limit: Int = 50,
    ): Flow<List<Transaction>>

    fun observeRecentTransactions(limit: Int = 5): Flow<List<Transaction>>

    suspend fun getTransactionById(id: String): Transaction?

    suspend fun addTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(id: String)
}
