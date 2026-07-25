package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.CategorySummary
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategorySummaryFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun upsertSummary(summary: CategorySummary) {
        firestore.collection(COLLECTION_USERS)
            .document(summary.userId)
            .collection(COLLECTION_CATEGORY_SUMMARIES)
            .document(summary.period)
            .set(
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
            .await()
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val COLLECTION_CATEGORY_SUMMARIES = "category_summaries"
        const val FIELD_PERIOD = "period"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FAMILY_ID = "familyId"
        const val FIELD_TOTAL_INCOME = "totalIncome"
        const val FIELD_TOTAL_EXPENSE = "totalExpense"
        const val FIELD_BY_CATEGORY = "byCategory"
        const val FIELD_NAME = "name"
        const val FIELD_TRANSACTION_COUNT = "transactionCount"
        const val FIELD_TOP_EXPENSE_CATEGORY_ID = "topExpenseCategoryId"
    }
}
