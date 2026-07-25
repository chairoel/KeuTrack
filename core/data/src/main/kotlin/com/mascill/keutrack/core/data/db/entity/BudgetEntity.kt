package com.mascill.keutrack.core.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["month", "categoryId"]),
        Index("syncStatus"),
        Index("userId"),
    ],
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val familyId: String?,
    val categoryId: String,
    val limit: Long,
    val spent: Long,
    val period: String,
    val month: String,
    val walletId: String?,
    val syncStatus: String,
    val createdAtEpochMs: Long,
)
