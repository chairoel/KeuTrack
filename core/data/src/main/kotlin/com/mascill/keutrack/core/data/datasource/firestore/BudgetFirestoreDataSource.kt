package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.Budget
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun upsertBudget(budget: Budget) {
        firestore.collection(COLLECTION_BUDGETS)
            .document(budget.id)
            .set(
                mapOf(
                    FIELD_ID to budget.id,
                    FIELD_USER_ID to budget.userId,
                    FIELD_FAMILY_ID to budget.familyId,
                    FIELD_CATEGORY_ID to budget.categoryId,
                    FIELD_LIMIT to budget.limit,
                    FIELD_SPENT to budget.spent,
                    FIELD_PERIOD to budget.period.value,
                    FIELD_MONTH to budget.month,
                    FIELD_WALLET_ID to budget.walletId,
                    FIELD_CREATED_AT to Timestamp(Date.from(budget.createdAt)),
                ),
            )
            .await()
    }

    suspend fun deleteBudget(budgetId: String) {
        firestore.collection(COLLECTION_BUDGETS).document(budgetId).delete().await()
    }

    private companion object {
        const val COLLECTION_BUDGETS = "budgets"
        const val FIELD_ID = "id"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FAMILY_ID = "familyId"
        const val FIELD_CATEGORY_ID = "categoryId"
        const val FIELD_LIMIT = "limit"
        const val FIELD_SPENT = "spent"
        const val FIELD_PERIOD = "period"
        const val FIELD_MONTH = "month"
        const val FIELD_WALLET_ID = "walletId"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
