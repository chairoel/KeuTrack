package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.BudgetPeriod
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.tasks.await
import java.time.Instant
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

    /**
     * Pull shared family budgets. Optional [month] (`yyyy-MM`) needs composite index
     * `familyId` + `month`. Without [month], equality-only `familyId` avoids that index.
     */
    suspend fun getByFamilyId(familyId: String, month: String? = null): List<Budget> {
        if (familyId.isBlank()) return emptyList()
        var query =
            firestore.collection(COLLECTION_BUDGETS)
                .whereEqualTo(FIELD_FAMILY_ID, familyId)
        if (!month.isNullOrBlank()) {
            query = query.whereEqualTo(FIELD_MONTH, month)
        }
        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            fromSnapshot(doc.id, data)
        }
    }

    private fun fromSnapshot(id: String, data: Map<String, Any?>): Budget? {
        val familyId = (data[FIELD_FAMILY_ID] as? String)?.takeIf { it.isNotBlank() }
            ?: return null
        val createdAt =
            (data[FIELD_CREATED_AT] as? Timestamp)?.toDate()?.toInstant()
                ?: Instant.EPOCH
        return Budget(
            id = (data[FIELD_ID] as? String) ?: id,
            userId = (data[FIELD_USER_ID] as? String).orEmpty(),
            familyId = familyId,
            categoryId = (data[FIELD_CATEGORY_ID] as? String).orEmpty(),
            limit = (data[FIELD_LIMIT] as? Number)?.toLong() ?: 0L,
            spent = (data[FIELD_SPENT] as? Number)?.toLong() ?: 0L,
            period = BudgetPeriod.fromValue((data[FIELD_PERIOD] as? String).orEmpty()),
            month = (data[FIELD_MONTH] as? String).orEmpty(),
            walletId = data[FIELD_WALLET_ID] as? String,
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt,
        )
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
