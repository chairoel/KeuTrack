package com.mascill.keutrack.core.data.datasource

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreNetworkDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun upsertUserProfile(user: User) {
        val userRef = firestore.collection(COLLECTION_USERS).document(user.uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (snapshot.exists()) {
                transaction.update(
                    userRef,
                    mapOf(
                        FIELD_DISPLAY_NAME to user.displayName,
                        FIELD_EMAIL to user.email,
                        FIELD_PHOTO_URL to user.photoUrl,
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                transaction.set(
                    userRef,
                    mapOf(
                        FIELD_UID to user.uid,
                        FIELD_DISPLAY_NAME to user.displayName,
                        FIELD_EMAIL to user.email,
                        FIELD_PHOTO_URL to user.photoUrl,
                        FIELD_CURRENCY to DEFAULT_CURRENCY,
                        FIELD_FAMILY_ID to null,
                        FIELD_FAMILY_ROLE to null,
                        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    )
                )
            }
        }.await()
    }

    suspend fun getUserProfile(uid: String): User? {
        val snapshot = firestore.collection(COLLECTION_USERS).document(uid).get().await()
        if (!snapshot.exists()) return null

        return User(
            uid = snapshot.getString(FIELD_UID) ?: uid,
            displayName = snapshot.getString(FIELD_DISPLAY_NAME).orEmpty(),
            email = snapshot.getString(FIELD_EMAIL).orEmpty(),
            photoUrl = snapshot.getString(FIELD_PHOTO_URL),
            currency = snapshot.getString(FIELD_CURRENCY) ?: DEFAULT_CURRENCY,
            familyId = snapshot.getString(FIELD_FAMILY_ID),
            familyRole = snapshot.getString(FIELD_FAMILY_ROLE),
        )
    }

    /**
     * Dedicated membership write — does not touch identity fields used by auth upsert.
     */
    suspend fun updateFamilyMembership(
        uid: String,
        familyId: String?,
        familyRole: String?,
    ) {
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .update(
                mapOf(
                    FIELD_FAMILY_ID to familyId,
                    FIELD_FAMILY_ROLE to familyRole,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    /**
     * Dedicated currency preference write — does not touch identity or membership fields.
     */
    suspend fun updateCurrency(uid: String, currency: String) {
        firestore.collection(COLLECTION_USERS)
            .document(uid)
            .update(
                mapOf(
                    FIELD_CURRENCY to currency,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    private companion object {
        const val COLLECTION_USERS = "users"
        const val FIELD_UID = "uid"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_EMAIL = "email"
        const val FIELD_PHOTO_URL = "photoUrl"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_FAMILY_ID = "familyId"
        const val FIELD_FAMILY_ROLE = "familyRole"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val DEFAULT_CURRENCY = "IDR"
    }
}
