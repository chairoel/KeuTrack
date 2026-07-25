package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.CategorySummary
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore sync for transactions.
 *
 * Strategy A (MVP): set transaction doc + FieldValue.increment for wallet/budget.
 * Idempotent: if transaction doc already exists, skip side-effect increments.
 */
@Singleton
class TransactionFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun upsertTransactionWithSideEffects(
        transaction: Transaction,
        walletBalanceDelta: Long,
        budgetId: String?,
        budgetSpentDelta: Long,
        summary: CategorySummary,
    ) {
        val txnRef = firestore.collection(COLLECTION_TRANSACTIONS).document(transaction.id)
        val walletRef = firestore.collection(COLLECTION_WALLETS).document(transaction.walletId)
        val summaryRef = firestore
            .collection(COLLECTION_USERS)
            .document(transaction.userId)
            .collection(COLLECTION_CATEGORY_SUMMARIES)
            .document(summary.period)

        firestore.runTransaction { fsTxn ->
            val existing = fsTxn.get(txnRef)
            if (existing.exists()) {
                return@runTransaction
            }

            fsTxn.set(
                txnRef,
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
                ),
            )

            fsTxn.update(
                walletRef,
                FIELD_BALANCE,
                FieldValue.increment(walletBalanceDelta),
            )

            if (budgetId != null && budgetSpentDelta != 0L) {
                val budgetRef = firestore.collection(COLLECTION_BUDGETS).document(budgetId)
                fsTxn.update(
                    budgetRef,
                    FIELD_SPENT,
                    FieldValue.increment(budgetSpentDelta),
                )
            }

            fsTxn.set(
                summaryRef,
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
                ),
            )
        }.await()
    }

    suspend fun deleteTransaction(transactionId: String) {
        firestore.collection(COLLECTION_TRANSACTIONS).document(transactionId).delete().await()
    }

    fun walletDeltaFor(transaction: Transaction): Long =
        when (transaction.type) {
            TransactionType.INCOME -> transaction.amount
            TransactionType.EXPENSE -> -transaction.amount
        }

    private companion object {
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
