package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.firestore.BudgetFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.firestore.CategorySummaryFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.firestore.TransactionFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.firestore.WalletFirestoreDataSource
import com.mascill.keutrack.core.data.datasource.local.BudgetLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.CategorySummaryLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.TransactionLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSource
import com.mascill.keutrack.core.data.datasource.local.findBudgetForExpense
import com.mascill.keutrack.core.data.mapper.BudgetMapper
import com.mascill.keutrack.core.data.mapper.CategorySummaryMapper
import com.mascill.keutrack.core.data.mapper.TransactionMapper
import com.mascill.keutrack.core.data.mapper.WalletMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.CategoryBreakdown
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.SyncRepository
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val transactionLocal: TransactionLocalDataSource,
    private val walletLocal: WalletLocalDataSource,
    private val budgetLocal: BudgetLocalDataSource,
    private val summaryLocal: CategorySummaryLocalDataSource,
    private val transactionRemote: TransactionFirestoreDataSource,
    private val walletRemote: WalletFirestoreDataSource,
    private val budgetRemote: BudgetFirestoreDataSource,
    private val summaryRemote: CategorySummaryFirestoreDataSource,
    private val transactionMapper: TransactionMapper,
    private val walletMapper: WalletMapper,
    private val budgetMapper: BudgetMapper,
    private val summaryMapper: CategorySummaryMapper,
    private val syncScheduler: SyncScheduler,
) : SyncRepository {

    override suspend fun syncPendingWallets() {
        try {
            var hasFailure = false
            walletLocal.getPending().forEach { entity ->
                try {
                    walletRemote.upsertWallet(walletMapper.toDomain(entity))
                    val hasPendingTx =
                        transactionLocal.getPending().any { it.walletId == entity.id }
                    if (!hasPendingTx) {
                        walletLocal.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    walletLocal.updateSyncStatus(entity.id, SyncStatus.FAILED)
                    hasFailure = true
                }
            }
            if (hasFailure) throw IllegalStateException("One or more wallets failed to sync")
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun syncPendingBudgets() {
        try {
            var hasFailure = false
            budgetLocal.getPending().forEach { entity ->
                try {
                    budgetRemote.upsertBudget(budgetMapper.toDomain(entity))
                    budgetLocal.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    budgetLocal.updateSyncStatus(entity.id, SyncStatus.FAILED)
                    hasFailure = true
                }
            }
            if (hasFailure) throw IllegalStateException("One or more budgets failed to sync")
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun syncPendingTransactions() {
        try {
            var hasFailure = false
            transactionLocal.getPending().forEach { entity ->
                try {
                    val transaction = transactionMapper.toDomain(entity)
                    val month = MONTH_FORMATTER.format(
                        transaction.date.atZone(ZoneId.systemDefault()),
                    )
                    val summary = summaryLocal.getByPeriod(month, transaction.userId)
                        ?.let(summaryMapper::toDomain)
                        ?: CategorySummary(
                            period = month,
                            userId = transaction.userId,
                            familyId = transaction.familyId,
                            totalIncome = 0L,
                            totalExpense = 0L,
                            byCategory = emptyMap(),
                        )

                    val budget = if (transaction.type == TransactionType.EXPENSE) {
                        budgetLocal.findBudgetForExpense(
                            month = month,
                            categoryId = transaction.categoryId,
                            familyId = transaction.familyId,
                        )
                    } else {
                        null
                    }

                    val walletDelta = transactionRemote.walletDeltaFor(transaction)
                    val budgetSpentDelta =
                        if (budget != null && transaction.type == TransactionType.EXPENSE) {
                            transaction.amount
                        } else {
                            0L
                        }

                    transactionRemote.upsertTransactionWithSideEffects(
                        transaction = transaction,
                        walletBalanceDelta = walletDelta,
                        budgetId = budget?.id,
                        budgetSpentDelta = budgetSpentDelta,
                        summary = summary,
                    )
                    summaryRemote.upsertSummary(summary)
                    transactionLocal.updateSyncStatus(entity.id, SyncStatus.SYNCED)
                    walletLocal.updateSyncStatus(transaction.walletId, SyncStatus.SYNCED)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    transactionLocal.updateSyncStatus(entity.id, SyncStatus.FAILED)
                    hasFailure = true
                }
            }
            if (hasFailure) throw IllegalStateException("One or more transactions failed to sync")
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun syncAll() {
        try {
            // Wallets/budgets first so remote docs exist before transaction increments.
            syncPendingWallets()
            syncPendingBudgets()
            syncPendingTransactions()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun syncFamilyData(familyId: String) {
        try {
            if (familyId.isBlank()) return

            val remoteWallets = walletRemote.getByFamilyId(familyId)
            val remoteTransactions =
                transactionRemote.getByFamilyId(familyId, limit = FAMILY_TX_PULL_LIMIT)
            val pendingWalletIds =
                transactionLocal.getPending().map { it.walletId }.toSet()

            remoteWallets.forEach { wallet ->
                val existing = walletLocal.getById(wallet.id)
                if (existing != null && existing.syncStatus == SyncStatus.PENDING.name) {
                    return@forEach
                }
                if (wallet.id in pendingWalletIds) {
                    return@forEach
                }
                val computedBalance =
                    remoteTransactions
                        .filter { it.walletId == wallet.id }
                        .sumOf { walletDeltaFor(it) }
                walletLocal.upsert(
                    walletMapper.toEntity(
                        wallet.copy(
                            balance = computedBalance,
                            syncStatus = SyncStatus.SYNCED,
                        ),
                    ),
                )
                if (wallet.balance != computedBalance) {
                    try {
                        walletRemote.setBalance(wallet.id, computedBalance)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Local is already corrected; remote can catch up on the next pull.
                    }
                }
            }

            // Keep one canonical wallet locally (oldest FAMILY). Drop extras even if remote
            // still has split-brain W_B from an earlier join race — new writes should target W_A.
            if (remoteWallets.isNotEmpty()) {
                val familyTyped =
                    remoteWallets.filter { it.type == WalletType.FAMILY }.ifEmpty { remoteWallets }
                val canonicalId =
                    familyTyped.minByOrNull { it.createdAt }?.id
                if (canonicalId != null) {
                    walletLocal.getByFamilyId(familyId)
                        .filter { it.id != canonicalId }
                        .forEach { extra -> walletLocal.delete(extra.id) }
                }
            }

            remoteTransactions.forEach { transaction ->
                val existing = transactionLocal.getById(transaction.id)
                if (existing != null && existing.syncStatus == SyncStatus.PENDING.name) {
                    return@forEach
                }
                transactionLocal.upsert(
                    transactionMapper.toEntity(
                        transaction.copy(syncStatus = SyncStatus.SYNCED),
                    ),
                )
            }

            hydrateFamilyBudgets(familyId)
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun syncPersonalData(userId: String) {
        try {
            if (userId.isBlank()) return

            val remoteWallets = walletRemote.getByOwnerId(userId)
                .filter { it.type == WalletType.PERSONAL }
            if (remoteWallets.isEmpty()) return

            val canonical = remoteWallets.minBy { it.createdAt }
            val remoteTxs = transactionRemote.getByUserId(userId, limit = PERSONAL_TX_PULL_LIMIT)
                .filter { it.walletId == canonical.id }
            val pendingTxs = transactionLocal.getPending()
            val pendingWalletIds = pendingTxs.map { it.walletId }.toSet()

            val existing = walletLocal.getById(canonical.id)
            if (existing?.syncStatus != SyncStatus.PENDING.name &&
                canonical.id !in pendingWalletIds
            ) {
                val computed = remoteTxs.sumOf { walletDeltaFor(it) }
                walletLocal.upsert(
                    walletMapper.toEntity(
                        canonical.copy(
                            balance = computed,
                            syncStatus = SyncStatus.SYNCED,
                        ),
                    ),
                )
                if (canonical.balance != computed) {
                    try {
                        walletRemote.setBalance(canonical.id, computed)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Local is already corrected; remote can catch up on the next pull.
                    }
                }
            }

            walletLocal.getByType(WalletType.PERSONAL.value)
                .filter { it.id != canonical.id && it.id !in pendingWalletIds }
                .forEach { extra -> walletLocal.delete(extra.id) }

            val upsertedTxs = mutableListOf<Transaction>()
            remoteTxs.forEach { transaction ->
                val existingTx = transactionLocal.getById(transaction.id)
                if (existingTx?.syncStatus == SyncStatus.PENDING.name) {
                    return@forEach
                }
                val synced = transaction.copy(syncStatus = SyncStatus.SYNCED)
                transactionLocal.upsert(transactionMapper.toEntity(synced))
                upsertedTxs += synced
            }

            val pendingCanonicalTxs = pendingTxs
                .filter { it.walletId == canonical.id }
                .map(transactionMapper::toDomain)
            rebuildPersonalSummaries(userId, upsertedTxs + pendingCanonicalTxs)
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun hasPendingSync(): Boolean =
        walletLocal.getPending().isNotEmpty() ||
            budgetLocal.getPending().isNotEmpty() ||
            transactionLocal.getPending().isNotEmpty()

    override fun enqueuePendingSync(force: Boolean) {
        syncScheduler.enqueueSync(force = force)
    }

    private fun walletDeltaFor(transaction: Transaction): Long =
        when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> -transaction.amount
        }

    private suspend fun rebuildPersonalSummaries(
        userId: String,
        transactions: List<Transaction>,
    ) {
        if (transactions.isEmpty()) return
        transactions
            .groupBy { monthKey(it) }
            .forEach { (period, periodTxs) ->
                var totalIncome = 0L
                var totalExpense = 0L
                val byCategory = mutableMapOf<String, CategoryBreakdown>()
                periodTxs.forEach { tx ->
                    val existing = byCategory[tx.categoryId] ?: CategoryBreakdown(
                        name = tx.categoryId,
                        totalExpense = 0L,
                        totalIncome = 0L,
                        transactionCount = 0,
                    )
                    when (tx.type) {
                        TransactionType.INCOME -> {
                            totalIncome += tx.amount
                            byCategory[tx.categoryId] = existing.copy(
                                totalIncome = existing.totalIncome + tx.amount,
                                transactionCount = existing.transactionCount + 1,
                            )
                        }
                        TransactionType.EXPENSE -> {
                            totalExpense += tx.amount
                            byCategory[tx.categoryId] = existing.copy(
                                totalExpense = existing.totalExpense + tx.amount,
                                transactionCount = existing.transactionCount + 1,
                            )
                        }
                    }
                }
                val topExpenseCategoryId = byCategory
                    .maxByOrNull { it.value.totalExpense }
                    ?.takeIf { it.value.totalExpense > 0 }
                    ?.key
                summaryLocal.upsert(
                    summaryMapper.toEntity(
                        CategorySummary(
                            period = period,
                            userId = userId,
                            familyId = null,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            byCategory = byCategory,
                            topExpenseCategoryId = topExpenseCategoryId,
                        ),
                    ),
                )
            }
    }

    private fun monthKey(transaction: Transaction): String =
        MONTH_FORMATTER.format(transaction.date.atZone(ZoneId.systemDefault()))

    private suspend fun hydrateFamilyBudgets(familyId: String) {
        val remoteBudgets =
            familyBudgetMonths().flatMap { month ->
                budgetRemote.getByFamilyId(familyId, month)
            }
        remoteBudgets.forEach { budget ->
            if (budget.familyId != familyId) return@forEach
            val existing = budgetLocal.getById(budget.id)
            if (existing != null && isUnsyncedLocal(existing.syncStatus)) {
                return@forEach
            }
            budgetLocal.upsert(
                budgetMapper.toEntity(
                    budget.copy(syncStatus = SyncStatus.SYNCED),
                ),
            )
        }
    }

    private fun familyBudgetMonths(): List<String> {
        val current = YearMonth.now()
        return listOf(current.toString(), current.minusMonths(1).toString())
    }

    private fun isUnsyncedLocal(status: String): Boolean =
        status == SyncStatus.PENDING.name || status == SyncStatus.FAILED.name

    private companion object {
        val MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        const val FAMILY_TX_PULL_LIMIT = 200
        const val PERSONAL_TX_PULL_LIMIT = 200
    }
}
