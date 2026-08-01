package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.data.db.entity.TransactionEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Transaction
import com.mascill.keutrack.core.domain.model.TransactionType
import java.time.Instant
import javax.inject.Inject

class TransactionMapper @Inject constructor() {

    fun toDomain(entity: TransactionEntity): Transaction =
        Transaction(
            id = entity.id,
            walletId = entity.walletId,
            userId = entity.userId,
            familyId = entity.familyId,
            type = TransactionType.fromValue(entity.type),
            amount = entity.amount,
            categoryId = entity.categoryId,
            note = entity.note,
            date = Instant.ofEpochMilli(entity.dateEpochMs),
            addedByName = entity.addedByName,
            syncStatus = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.PENDING),
            createdAt = Instant.ofEpochMilli(entity.createdAtEpochMs),
        )

    fun toEntity(domain: Transaction): TransactionEntity =
        TransactionEntity(
            id = domain.id,
            walletId = domain.walletId,
            userId = domain.userId,
            familyId = domain.familyId,
            type = domain.type.value,
            amount = domain.amount,
            categoryId = domain.categoryId,
            note = domain.note,
            dateEpochMs = domain.date.toEpochMilli(),
            addedByName = domain.addedByName,
            syncStatus = domain.syncStatus.name,
            createdAtEpochMs = domain.createdAt.toEpochMilli(),
        )
}
