package com.mascill.keutrack.core.data.datasource.local

import androidx.room.withTransaction
import com.mascill.keutrack.core.data.db.AppDatabase
import com.mascill.keutrack.core.data.db.dao.BudgetDao
import com.mascill.keutrack.core.data.db.dao.CategorySummaryDao
import com.mascill.keutrack.core.data.db.dao.TransactionDao
import com.mascill.keutrack.core.data.db.dao.WalletDao
import com.mascill.keutrack.core.data.db.entity.CategorySummaryEntity
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

    override fun observeSumsByType(startMs: Long, endMs: Long): Flow<List<AmountByTypeRow>> =
        transactionDao.observeSumsByType(startMs, endMs)

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
}
