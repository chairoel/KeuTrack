package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class Wallet(
    val id: String,
    val ownerId: String,
    val familyId: String? = null,
    val name: String,
    val type: WalletType,
    val balance: Long,
    val currency: String = "IDR",
    val icon: String? = null,
    val color: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Instant = Instant.now(),
)
