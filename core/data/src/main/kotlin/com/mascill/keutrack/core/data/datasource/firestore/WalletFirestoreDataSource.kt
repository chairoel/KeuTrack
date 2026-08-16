package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    /**
     * Pushes wallet metadata. Remote balance is owned by transaction increment
     * side-effects — writing the local (already incremented) balance here would
     * double-count after the transaction sync.
     */
    suspend fun upsertWallet(wallet: Wallet) {
        val ref = firestore.collection(COLLECTION_WALLETS).document(wallet.id)
        val snapshot = ref.get().await()
        val metadata =
            mapOf(
                FIELD_ID to wallet.id,
                FIELD_OWNER_ID to wallet.ownerId,
                FIELD_FAMILY_ID to wallet.familyId,
                FIELD_NAME to wallet.name,
                FIELD_TYPE to wallet.type.value,
                FIELD_CURRENCY to wallet.currency,
                FIELD_ICON to wallet.icon,
                FIELD_COLOR to wallet.color,
                FIELD_CREATED_AT to Timestamp(Date.from(wallet.createdAt)),
            )
        if (snapshot.exists()) {
            ref.set(metadata, SetOptions.merge()).await()
        } else {
            ref.set(metadata + (FIELD_BALANCE to 0L)).await()
        }
    }

    suspend fun setBalance(walletId: String, balance: Long) {
        firestore.collection(COLLECTION_WALLETS)
            .document(walletId)
            .update(FIELD_BALANCE, balance)
            .await()
    }

    /**
     * Pull wallets owned by [ownerId] (equality-only; filter `type` on the client).
     */
    suspend fun getByOwnerId(ownerId: String): List<Wallet> {
        val snapshot =
            firestore.collection(COLLECTION_WALLETS)
                .whereEqualTo(FIELD_OWNER_ID, ownerId)
                .get()
                .await()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            fromSnapshot(doc.id, data)
        }
    }

    /**
     * Pull wallets shared for a family (canonical family wallet expected: 0–1 docs).
     */
    suspend fun getByFamilyId(familyId: String): List<Wallet> {
        val snapshot =
            firestore.collection(COLLECTION_WALLETS)
                .whereEqualTo(FIELD_FAMILY_ID, familyId)
                .get()
                .await()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            fromSnapshot(doc.id, data)
        }
    }

    suspend fun deleteWallet(walletId: String) {
        firestore.collection(COLLECTION_WALLETS).document(walletId).delete().await()
    }

    private fun fromSnapshot(id: String, data: Map<String, Any?>): Wallet {
        val createdAt =
            (data[FIELD_CREATED_AT] as? Timestamp)?.toDate()?.toInstant()
                ?: Instant.EPOCH
        return Wallet(
            id = (data[FIELD_ID] as? String) ?: id,
            ownerId = (data[FIELD_OWNER_ID] as? String).orEmpty(),
            familyId = data[FIELD_FAMILY_ID] as? String,
            name = (data[FIELD_NAME] as? String).orEmpty(),
            type = WalletType.fromValue((data[FIELD_TYPE] as? String).orEmpty()),
            balance = (data[FIELD_BALANCE] as? Number)?.toLong() ?: 0L,
            currency = (data[FIELD_CURRENCY] as? String) ?: "IDR",
            icon = data[FIELD_ICON] as? String,
            color = data[FIELD_COLOR] as? String,
            syncStatus = SyncStatus.SYNCED,
            createdAt = createdAt,
        )
    }

    private companion object {
        const val COLLECTION_WALLETS = "wallets"
        const val FIELD_ID = "id"
        const val FIELD_OWNER_ID = "ownerId"
        const val FIELD_FAMILY_ID = "familyId"
        const val FIELD_NAME = "name"
        const val FIELD_TYPE = "type"
        const val FIELD_BALANCE = "balance"
        const val FIELD_CURRENCY = "currency"
        const val FIELD_ICON = "icon"
        const val FIELD_COLOR = "color"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
