package com.mascill.keutrack.core.domain.repository

interface SyncRepository {
    suspend fun syncPendingTransactions()
    suspend fun syncPendingWallets()
    suspend fun syncPendingBudgets()
    suspend fun syncAll()
}
