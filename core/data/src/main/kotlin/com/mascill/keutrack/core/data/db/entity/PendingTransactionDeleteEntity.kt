package com.mascill.keutrack.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transaction_deletes")
data class PendingTransactionDeleteEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val userId: String,
    val familyId: String?,
    val type: String,
    val amount: Long,
    val categoryId: String,
    val dateEpochMs: Long,
    val queuedAtEpochMs: Long,
    val syncStatus: String,
)
