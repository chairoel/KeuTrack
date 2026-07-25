package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface TransactionLocalDataSource {
    fun observeFiltered(
        walletId: String?,
        type: String?,
        categoryId: String?,
        startMs: Long?,
        endMs: Long?,
        limit: Int,
    ): Flow<List<TransactionEntity>>

    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    suspend fun getById(id: String): TransactionEntity?

    suspend fun upsert(entity: TransactionEntity)

    suspend fun delete(id: String)

    suspend fun getPending(): List<TransactionEntity>

    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    /**
     * Atomic local write for a new transaction:
     * insert txn + wallet balance delta + optional budget spent + category summary upsert.
     */
    suspend fun applyNewTransactionAtomically(
        transaction: TransactionEntity,
        walletDelta: Long,
        budgetIdToIncrement: String?,
        summaryUpsert: CategorySummaryEntity,
    )
}
