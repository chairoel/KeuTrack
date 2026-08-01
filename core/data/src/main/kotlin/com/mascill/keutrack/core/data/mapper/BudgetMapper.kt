package com.mascill.keutrack.core.data.mapper

import com.mascill.keutrack.core.data.db.entity.BudgetEntity
import com.mascill.keutrack.core.domain.model.Budget
import com.mascill.keutrack.core.domain.model.BudgetPeriod
import com.mascill.keutrack.core.domain.model.SyncStatus
import java.time.Instant
import javax.inject.Inject

class BudgetMapper @Inject constructor() {

    fun toDomain(entity: BudgetEntity): Budget =
        Budget(
            id = entity.id,
            userId = entity.userId,
            familyId = entity.familyId,
            categoryId = entity.categoryId,
            limit = entity.limit,
            spent = entity.spent,
            period = BudgetPeriod.fromValue(entity.period),
            month = entity.month,
            walletId = entity.walletId,
            syncStatus = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.SYNCED),
            createdAt = Instant.ofEpochMilli(entity.createdAtEpochMs),
        )

    fun toEntity(domain: Budget): BudgetEntity =
        BudgetEntity(
            id = domain.id,
            userId = domain.userId,
            familyId = domain.familyId,
            categoryId = domain.categoryId,
            limit = domain.limit,
            spent = domain.spent,
            period = domain.period.value,
            month = domain.month,
            walletId = domain.walletId,
            syncStatus = domain.syncStatus.name,
            createdAtEpochMs = domain.createdAt.toEpochMilli(),
        )
}
