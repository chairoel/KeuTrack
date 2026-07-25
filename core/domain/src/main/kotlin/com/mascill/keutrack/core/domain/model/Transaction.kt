package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class Transaction(
    val id: String,
    val walletId: String,
    val userId: String,
    val familyId: String? = null,
    val type: TransactionType,
    val amount: Long,
    val categoryId: String,
    val note: String? = null,
    val date: Instant,
    val addedByName: String,
    val syncStatus: SyncStatus = SyncStatus.PENDING,
    val createdAt: Instant = Instant.now(),
)
