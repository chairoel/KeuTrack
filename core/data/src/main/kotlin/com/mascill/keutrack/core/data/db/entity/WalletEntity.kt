package com.mascill.keutrack.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wallets",
    indices = [
        Index("ownerId"),
        Index("type"),
        Index("syncStatus"),
    ],
)
data class WalletEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val familyId: String?,
    val name: String,
    val type: String,
    val balance: Long,
    val currency: String,
    val icon: String?,
    val color: String?,
    val syncStatus: String,
    val createdAtEpochMs: Long,
)
