package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore sync for transactions.
 *
 * Strategy A: set transaction fields + [FieldValue.increment] for wallet/budget.
 * Create (missing doc) applies the new deltas only. Update uses snapshot-diff
 * increments so a retry after success is a no-op (Δ = 0).
 */
@Singleton
class TransactionFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun getById(id: String): Transaction? {
        val snapshot = firestore.collection(COLLECTION_TRANSACTIONS).document(id).get().await()
        val data = snapshot.data ?: return null
        return fromSnapshot(snapshot.id, data)
    }

    suspend fun upsertTransactionWithSideEffects(
        transaction: Transaction,
        remoteSnapshot: Transaction?,
        oldWalletId: String?,
        oldWalletDelta: Long,
        newWalletDelta: Long,
        oldBudgetId: String?,
        oldBudgetDelta: Long,
        newBudgetId: String?,
        newBudgetDelta: Long,
        summaries: List<CategorySummary>,
    ) {
        val txnRef = firestore.collection(COLLECTION_TRANSACTIONS).document(transaction.id)

        firestore.runTransaction { fsTxn ->
            val existing = fsTxn.get(txnRef)
            val isUpdate = existing.exists()
            if (isUpdate) {
                val actual = fromSnapshot(existing.id, existing.data.orEmpty())
                if (remoteSnapshot == null || !identityMatches(remoteSnapshot, actual)) {
                    throw SideEffectSnapshotMismatch(transaction.id)
                }
            }

            fsTxn.set(txnRef, transactionFields(transaction))

            val walletIncrements = netIncrements(
                applyOld = isUpdate,
                oldId = oldWalletId,
                oldDelta = oldWalletDelta,
                newId = transaction.walletId,
                newDelta = newWalletDelta,
            )
            walletIncrements.forEach { (walletId, delta) ->
                fsTxn.update(
                    firestore.collection(COLLECTION_WALLETS).document(walletId),
                    FIELD_BALANCE,
                    FieldValue.increment(delta),
                )
            }

            val budgetIncrements = netIncrements(
                applyOld = isUpdate,
                oldId = oldBudgetId,
                oldDelta = oldBudgetDelta,
                newId = newBudgetId,
                newDelta = newBudgetDelta,
            )
            budgetIncrements.forEach { (budgetId, delta) ->
                fsTxn.update(
                    firestore.collection(COLLECTION_BUDGETS).document(budgetId),
                    FIELD_SPENT,
                    FieldValue.increment(delta),
                )
            }

            summaries.forEach { summary ->
                val summaryRef = firestore
                    .collection(COLLECTION_USERS)
                    .document(summary.userId)
                    .collection(COLLECTION_CATEGORY_SUMMARIES)
                    .document(summary.period)
                fsTxn.set(summaryRef, summaryFields(summary))
            }
        }.await()
    }

    suspend fun deleteTransaction(transactionId: String) {
        firestore.collection(COLLECTION_TRANSACTIONS).document(transactionId).delete().await()
    }

    /**
     * Pull transactions authored by [userId] (newest first).
     *
     * Equality-only query avoids a composite index; sort + limit run client-side.
     * Filter `walletId` / `familyId` on the caller (personal restore vs family).
     */
    suspend fun getByUserId(userId: String, limit: Int = DEFAULT_FAMILY_PULL_LIMIT): List<Transaction> {
        val snapshot =
            firestore.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo(FIELD_USER_ID, userId)
                .get()
                .await()
        return snapshot.documents
            .mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                fromSnapshot(doc.id, data)
            }
            .sortedByDescending { it.date }
            .take(limit)
    }

    /**
     * Pull shared family transactions (newest first).
     *
     * Equality-only query avoids a composite index; sort + limit run client-side.
     * For large families later, add index `familyId` Asc + `date` Desc and orderBy remotely.
     */
    suspend fun getByFamilyId(familyId: String, limit: Int = DEFAULT_FAMILY_PULL_LIMIT): List<Transaction> {
        val snapshot =
            firestore.collection(COLLECTION_TRANSACTIONS)
                .whereEqualTo(FIELD_FAMILY_ID, familyId)
                .get()
                .await()
        return snapshot.documents
            .mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                fromSnapshot(doc.id, data)
            }
            .sortedByDescending { it.date }
            .take(limit)
    }

    fun walletDeltaFor(transaction: Transaction): Long =
        when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> -transaction.amount
        }

    private fun identityMatches(expected: Transaction, actual: Transaction): Boolean =
        expected.amount == actual.amount &&
            expected.type == actual.type &&
            expected.walletId == actual.walletId &&
            expected.categoryId == actual.categoryId &&
            expected.familyId == actual.familyId &&
            expected.date.toEpochMilli() == actual.date.toEpochMilli()

    private fun netIncrements(
        applyOld: Boolean,
        oldId: String?,
        oldDelta: Long,
        newId: String?,
        newDelta: Long,
    ): Map<String, Long> {
        val inc = mutableMapOf<String, Long>()
        if (applyOld && oldId != null) {
            inc[oldId] = (inc[oldId] ?: 0L) + oldDelta
        }
        if (newId != null) {
            inc[newId] = (inc[newId] ?: 0L) + newDelta
        }
        return inc.filterValues { it != 0L }
    }

    private fun transactionFields(transaction: Transaction): Map<String, Any?> =
        mapOf(
            FIELD_ID to transaction.id,
            FIELD_WALLET_ID to transaction.walletId,
            FIELD_USER_ID to transaction.userId,
            FIELD_FAMILY_ID to transaction.familyId,
            FIELD_TYPE to transaction.type.value,
            FIELD_AMOUNT to transaction.amount,
            FIELD_CATEGORY_ID to transaction.categoryId,
            FIELD_NOTE to transaction.note,
            FIELD_DATE to Timestamp(Date.from(transaction.date)),
            FIELD_ADDED_BY_NAME to transaction.addedByName,
            FIELD_CREATED_AT to Timestamp(Date.from(transaction.createdAt)),
        )

    private fun summaryFields(summary: CategorySummary): Map<String, Any?> =
        mapOf(
            FIELD_PERIOD to summary.period,
            FIELD_USER_ID to summary.userId,
            FIELD_FAMILY_ID to summary.familyId,
            FIELD_TOTAL_INCOME to summary.totalIncome,
            FIELD_TOTAL_EXPENSE to summary.totalExpense,
            FIELD_BY_CATEGORY to summary.byCategory.mapValues { (_, breakdown) ->
                mapOf(
                    FIELD_NAME to breakdown.name,
                    FIELD_TOTAL_EXPENSE to breakdown.totalExpense,
                    FIELD_TOTAL_INCOME to breakdown.totalIncome,
                    FIELD_TRANSACTION_COUNT to breakdown.transactionCount,
                )
            },
            FIELD_TOP_EXPENSE_CATEGORY_ID to summary.topExpenseCategoryId,
        )

    private fun fromSnapshot(id: String, data: Map<String, Any?>): Transaction {
        val date =
            (data[FIELD_DATE] as? Timestamp)?.toDate()?.toInstant()
                ?: Instant.EPOCH
        val createdAt =
            (data[FIELD_CREATED_AT] as? Timestamp)?.toDate()?.toInstant()
                ?: Instant.EPOCH
        return Transaction(
            id = (data[FIELD_ID] as? String) ?: id,
            walletId = (data[FIELD_WALLET_ID] as? String).orEmpty(),
            userId = (data[FIELD_USER_ID] as? String).orEmpty(),
            familyId = data[FIELD_FAMILY_ID] as? String,
            type = TransactionType.fromValue((data[FIELD_TYPE] as? String).orEmpty()),
            amount = (data[FIELD_AMOUNT] as? Number)?.toLong() ?: 0L,
            categoryId = (data[FIELD_CATEGORY_ID] as? String).orEmpty(),
            note = data[FIELD_NOTE] as? String,
            date = date,
            addedByName = (data[FIELD_ADDED_BY_NAME] as? String).orEmpty(),
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt,
        )
    }

    private companion object {
        const val DEFAULT_FAMILY_PULL_LIMIT = 200
        const val COLLECTION_TRANSACTIONS = "transactions"
        const val COLLECTION_WALLETS = "wallets"
        const val COLLECTION_BUDGETS = "budgets"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_CATEGORY_SUMMARIES = "category_summaries"

        const val FIELD_ID = "id"
        const val FIELD_WALLET_ID = "walletId"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FAMILY_ID = "familyId"
        const val FIELD_TYPE = "type"
        const val FIELD_AMOUNT = "amount"
        const val FIELD_CATEGORY_ID = "categoryId"
        const val FIELD_NOTE = "note"
        const val FIELD_DATE = "date"
        const val FIELD_ADDED_BY_NAME = "addedByName"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_BALANCE = "balance"
        const val FIELD_SPENT = "spent"
        const val FIELD_PERIOD = "period"
        const val FIELD_TOTAL_INCOME = "totalIncome"
        const val FIELD_TOTAL_EXPENSE = "totalExpense"
        const val FIELD_BY_CATEGORY = "byCategory"
        const val FIELD_NAME = "name"
        const val FIELD_TRANSACTION_COUNT = "transactionCount"
        const val FIELD_TOP_EXPENSE_CATEGORY_ID = "topExpenseCategoryId"
    }
}
