package com.mascill.keutrack.core.data.datasource.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.mascill.keutrack.core.domain.model.Wallet
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun upsertWallet(wallet: Wallet) {
        firestore.collection(COLLECTION_WALLETS)
            .document(wallet.id)
            .set(
                mapOf(
                    FIELD_ID to wallet.id,
                    FIELD_OWNER_ID to wallet.ownerId,
                    FIELD_FAMILY_ID to wallet.familyId,
                    FIELD_NAME to wallet.name,
                    FIELD_TYPE to wallet.type.value,
                    FIELD_BALANCE to wallet.balance,
                    FIELD_CURRENCY to wallet.currency,
                    FIELD_ICON to wallet.icon,
                    FIELD_COLOR to wallet.color,
                    FIELD_CREATED_AT to Timestamp(Date.from(wallet.createdAt)),
                ),
            )
            .await()
    }

    suspend fun deleteWallet(walletId: String) {
        firestore.collection(COLLECTION_WALLETS).document(walletId).delete().await()
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
