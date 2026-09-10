package com.mascill.keutrack.core.data.datasource.local

import androidx.room.withTransaction
import com.mascill.keutrack.core.data.db.AppDatabase
import com.mascill.keutrack.core.data.db.dao.BudgetDao
import com.mascill.keutrack.core.data.db.dao.CategorySummaryDao
import com.mascill.keutrack.core.data.db.dao.PendingTransactionDeleteDao
import com.mascill.keutrack.core.data.db.dao.TransactionDao
import com.mascill.keutrack.core.data.db.dao.WalletDao
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
import com.mascill.keutrack.core.data.db.entity.PendingTransactionDeleteEntity
import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.data.db.model.AmountByTypeRow
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionLocalDataSourceImpl @Inject constructor(
    private val db: AppDatabase,
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val budgetDao: BudgetDao,
    private val categorySummaryDao: CategorySummaryDao,
    private val pendingDeleteDao: PendingTransactionDeleteDao,
) : TransactionLocalDataSource {

    override fun observeFiltered(
        walletId: String?,
        familyId: String?,
        type: String?,
        categoryId: String?,
        startMs: Long?,
        endMs: Long?,
        limit: Int,
    ): Flow<List<TransactionEntity>> =
        transactionDao.observeFiltered(
            walletId,
            familyId,
            type,
            categoryId,
            startMs,
            endMs,
            limit,
        )

    override fun observeRecent(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.observeRecent(limit)

    override fun observeSumsByType(
        walletId: String?,
        familyId: String?,
        startMs: Long?,
        endMs: Long?,
    ): Flow<List<AmountByTypeRow>> =
        transactionDao.observeSumsByType(
            walletId = walletId,
            familyId = familyId,
            startMs = startMs,
            endMs = endMs,
        )

    override suspend fun getById(id: String): TransactionEntity? =
        transactionDao.getById(id)

    override suspend fun upsert(entity: TransactionEntity) {
        transactionDao.upsert(entity)
    }

    override suspend fun delete(id: String) {
        transactionDao.deleteById(id)
    }

    override suspend fun getPending(): List<TransactionEntity> =
        transactionDao.getPending()

    override suspend fun getPendingDeletes(): List<PendingTransactionDeleteEntity> =
        pendingDeleteDao.getPending()

    override suspend fun removePendingDelete(id: String) {
        pendingDeleteDao.deleteById(id)
    }

    override suspend fun updateDeleteSyncStatus(id: String, status: SyncStatus) {
        pendingDeleteDao.updateSyncStatus(id, status.name)
    }

    override suspend fun updateSyncStatus(id: String, status: SyncStatus) {
        transactionDao.updateSyncStatus(id, status.name)
    }

    override suspend fun applyNewTransactionAtomically(
        transaction: TransactionEntity,
        walletDelta: Long,
        budgetIdToIncrement: String?,
        summaryUpsert: CategorySummaryEntity,
    ) {
        db.withTransaction {
            transactionDao.upsert(transaction)
            walletDao.applyBalanceDelta(
                walletId = transaction.walletId,
                delta = walletDelta,
                syncStatus = SyncStatus.PENDING.name,
            )
            if (budgetIdToIncrement != null) {
                budgetDao.applySpentDelta(
                    budgetId = budgetIdToIncrement,
                    delta = transaction.amount,
                    syncStatus = SyncStatus.PENDING.name,
                )
            }
            categorySummaryDao.upsert(summaryUpsert)
        }
    }

    override suspend fun applyUpdatedTransactionAtomically(
        updated: TransactionEntity,
        oldWalletId: String,
        oldWalletDelta: Long,
        newWalletDelta: Long,
        oldBudgetId: String?,
        oldBudgetDelta: Long,
        newBudgetId: String?,
        newBudgetDelta: Long,
        summaryUpserts: List<CategorySummaryEntity>,
    ) {
        db.withTransaction {
            transactionDao.upsert(updated)
            applyWalletDeltas(
                oldWalletId = oldWalletId,
                newWalletId = updated.walletId,
                oldWalletDelta = oldWalletDelta,
                newWalletDelta = newWalletDelta,
            )
            applyBudgetDeltas(
                oldBudgetId = oldBudgetId,
                oldBudgetDelta = oldBudgetDelta,
                newBudgetId = newBudgetId,
                newBudgetDelta = newBudgetDelta,
            )
            summaryUpserts.forEach { categorySummaryDao.upsert(it) }
        }
    }

    override suspend fun applyDeletedTransactionAtomically(
        id: String,
        walletId: String,
        walletDelta: Long,
        budgetId: String?,
        budgetDelta: Long,
        summaryUpsert: CategorySummaryEntity?,
        pendingDelete: PendingTransactionDeleteEntity?,
    ) {
        db.withTransaction {
            if (pendingDelete != null) {
                pendingDeleteDao.upsert(pendingDelete)
            }
            applyWalletDelta(walletId, walletDelta)
            applyBudgetDelta(budgetId, budgetDelta)
            if (summaryUpsert != null) {
                categorySummaryDao.upsert(summaryUpsert)
            }
            transactionDao.deleteById(id)
        }
    }

    private suspend fun applyWalletDeltas(
        oldWalletId: String,
        newWalletId: String,
        oldWalletDelta: Long,
        newWalletDelta: Long,
    ) {
        if (oldWalletId == newWalletId) {
            applyWalletDelta(newWalletId, oldWalletDelta + newWalletDelta)
        } else {
            applyWalletDelta(oldWalletId, oldWalletDelta)
            applyWalletDelta(newWalletId, newWalletDelta)
        }
    }

    private suspend fun applyBudgetDeltas(
        oldBudgetId: String?,
        oldBudgetDelta: Long,
        newBudgetId: String?,
        newBudgetDelta: Long,
    ) {
        if (oldBudgetId != null && oldBudgetId == newBudgetId) {
            applyBudgetDelta(oldBudgetId, oldBudgetDelta + newBudgetDelta)
        } else {
            applyBudgetDelta(oldBudgetId, oldBudgetDelta)
            applyBudgetDelta(newBudgetId, newBudgetDelta)
        }
    }

    private suspend fun applyWalletDelta(walletId: String, delta: Long) {
        if (delta == 0L) return
        walletDao.applyBalanceDelta(
            walletId = walletId,
            delta = delta,
            syncStatus = SyncStatus.PENDING.name,
        )
    }

    private suspend fun applyBudgetDelta(budgetId: String?, delta: Long) {
        if (budgetId == null || delta == 0L) return
        budgetDao.applySpentDelta(
            budgetId = budgetId,
            delta = delta,
            syncStatus = SyncStatus.PENDING.name,
        )
    }
}
