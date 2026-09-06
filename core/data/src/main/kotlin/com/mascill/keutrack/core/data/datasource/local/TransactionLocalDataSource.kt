package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.model.AmountByTypeRow
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface TransactionLocalDataSource {
    fun observeFiltered(
        walletId: String?,
        familyId: String? = null,
        type: String?,
        categoryId: String?,
        startMs: Long?,
        endMs: Long?,
        limit: Int,
    ): Flow<List<TransactionEntity>>

    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    fun observeSumsByType(
        walletId: String? = null,
        familyId: String? = null,
        startMs: Long? = null,
        endMs: Long? = null,
    ): Flow<List<AmountByTypeRow>>

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

    /**
     * Atomic local write for an edit: upsert the row (same id), reverse old wallet/budget
     * effects, apply new ones, and upsert one or two category summaries (period may change).
     */
    suspend fun applyUpdatedTransactionAtomically(
        updated: TransactionEntity,
        oldWalletId: String,
        oldWalletDelta: Long,
        newWalletDelta: Long,
        oldBudgetId: String?,
        oldBudgetDelta: Long,
        newBudgetId: String?,
        newBudgetDelta: Long,
        summaryUpserts: List<CategorySummaryEntity>,
    )

    /**
     * Atomic local write for a delete: reverse wallet/budget/summary, then remove the row.
     */
    suspend fun applyDeletedTransactionAtomically(
        id: String,
        walletId: String,
        walletDelta: Long,
        budgetId: String?,
        budgetDelta: Long,
        summaryUpsert: CategorySummaryEntity?,
    )
}
