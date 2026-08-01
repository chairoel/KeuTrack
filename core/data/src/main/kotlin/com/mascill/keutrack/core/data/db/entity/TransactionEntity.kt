package com.mascill.keutrack.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("walletId"),
        Index("userId"),
        Index("categoryId"),
        Index("dateEpochMs"),
        Index("syncStatus"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val walletId: String,
    val userId: String,
    val familyId: String?,
    val type: String,
    val amount: Long,
    val categoryId: String,
    val note: String?,
    val dateEpochMs: Long,
    val addedByName: String,
    val syncStatus: String,
    val createdAtEpochMs: Long,
)
