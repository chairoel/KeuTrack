package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.FamilyGroup
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FamilyGroupFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun createFamily(family: FamilyGroup) {
        firestore.collection(COLLECTION_FAMILY_GROUPS)
            .document(family.id)
            .set(toMap(family))
            .await()
    }

    suspend fun getFamilyById(familyId: String): FamilyGroup? {
        val snapshot =
            firestore.collection(COLLECTION_FAMILY_GROUPS)
                .document(familyId)
                .get()
                .await()
        if (!snapshot.exists()) return null
        return fromSnapshot(snapshot.id, snapshot.data.orEmpty())
    }

    suspend fun findByInviteCode(inviteCode: String): FamilyGroup? {
        val snapshot =
            firestore.collection(COLLECTION_FAMILY_GROUPS)
                .whereEqualTo(FIELD_INVITE_CODE, inviteCode)
                .limit(1)
                .get()
                .await()
        val doc = snapshot.documents.firstOrNull() ?: return null
        return fromSnapshot(doc.id, doc.data.orEmpty())
    }

    suspend fun addMember(familyId: String, userId: String) {
        firestore.collection(COLLECTION_FAMILY_GROUPS)
            .document(familyId)
            .update(FIELD_MEMBER_IDS, FieldValue.arrayUnion(userId))
            .await()
    }

    fun observeFamily(familyId: String): Flow<FamilyGroup?> =
        callbackFlow {
            val registration =
                firestore.collection(COLLECTION_FAMILY_GROUPS)
                    .document(familyId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        if (snapshot == null || !snapshot.exists()) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        trySend(fromSnapshot(snapshot.id, snapshot.data.orEmpty()))
                    }
            awaitClose { registration.remove() }
        }

    private fun toMap(family: FamilyGroup): Map<String, Any?> =
        mapOf(
            FIELD_ID to family.id,
            FIELD_NAME to family.name,
            FIELD_INVITE_CODE to family.inviteCode,
            FIELD_OWNER_ID to family.ownerId,
            FIELD_MEMBER_IDS to family.memberIds,
            FIELD_CREATED_AT to Timestamp(Date.from(family.createdAt)),
        )

    @Suppress("UNCHECKED_CAST")
    private fun fromSnapshot(id: String, data: Map<String, Any?>): FamilyGroup {
        val createdAt =
            (data[FIELD_CREATED_AT] as? Timestamp)?.toDate()?.toInstant()
                ?: Instant.EPOCH
        val memberIds =
            (data[FIELD_MEMBER_IDS] as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()
        return FamilyGroup(
            id = (data[FIELD_ID] as? String) ?: id,
            name = (data[FIELD_NAME] as? String).orEmpty(),
            inviteCode = (data[FIELD_INVITE_CODE] as? String).orEmpty(),
            ownerId = (data[FIELD_OWNER_ID] as? String).orEmpty(),
            memberIds = memberIds,
            createdAt = createdAt,
        )
    }

    private companion object {
        const val COLLECTION_FAMILY_GROUPS = "family_groups"
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_INVITE_CODE = "inviteCode"
        const val FIELD_OWNER_ID = "ownerId"
        const val FIELD_MEMBER_IDS = "memberIds"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
