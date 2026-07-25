package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.Category
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional pull/push for categories. Local seed remains the source of truth for defaults.
 */
@Singleton
class CategoryFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun upsertCategory(category: Category) {
        firestore.collection(COLLECTION_CATEGORIES)
            .document(category.id)
            .set(
                mapOf(
                    FIELD_ID to category.id,
                    FIELD_USER_ID to category.userId,
                    FIELD_FAMILY_ID to category.familyId,
                    FIELD_NAME to category.name,
                    FIELD_ICON to category.icon,
                    FIELD_COLOR to category.color,
                    FIELD_TYPE to category.type.value,
                    FIELD_IS_DEFAULT to category.isDefault,
                ),
            )
            .await()
    }

    private companion object {
        const val COLLECTION_CATEGORIES = "categories"
        const val FIELD_ID = "id"
        const val FIELD_USER_ID = "userId"
        const val FIELD_FAMILY_ID = "familyId"
        const val FIELD_NAME = "name"
        const val FIELD_ICON = "icon"
        const val FIELD_COLOR = "color"
        const val FIELD_TYPE = "type"
        const val FIELD_IS_DEFAULT = "isDefault"
    }
}
