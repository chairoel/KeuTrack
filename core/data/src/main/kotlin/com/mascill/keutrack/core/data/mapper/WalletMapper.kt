package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import java.time.Instant
import javax.inject.Inject

class WalletMapper @Inject constructor() {

    fun toDomain(entity: WalletEntity): Wallet =
        Wallet(
            id = entity.id,
            ownerId = entity.ownerId,
            familyId = entity.familyId,
            name = entity.name,
            type = WalletType.fromValue(entity.type),
            balance = entity.balance,
            currency = entity.currency,
            icon = entity.icon,
            color = entity.color,
            syncStatus = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.SYNCED),
            createdAt = Instant.ofEpochMilli(entity.createdAtEpochMs),
        )

    fun toEntity(domain: Wallet): WalletEntity =
        WalletEntity(
            id = domain.id,
            ownerId = domain.ownerId,
            familyId = domain.familyId,
            name = domain.name,
            type = domain.type.value,
            balance = domain.balance,
            currency = domain.currency,
            icon = domain.icon,
            color = domain.color,
            syncStatus = domain.syncStatus.name,
            createdAtEpochMs = domain.createdAt.toEpochMilli(),
        )
}
