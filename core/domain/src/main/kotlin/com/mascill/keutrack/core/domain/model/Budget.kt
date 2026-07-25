package com.mascill.keutrack.core.domain.model

import java.time.Instant

data class Budget(
    val id: String,
    val userId: String,
    val familyId: String? = null,
    val categoryId: String,
    val limit: Long,
    val spent: Long = 0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    val month: String,
    val walletId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val createdAt: Instant = Instant.now(),
) {
    val remaining: Long get() = limit - spent
    val progressPercent: Float get() = if (limit > 0) (spent.toFloat() / limit) else 0f
    val isOverBudget: Boolean get() = spent > limit
}
