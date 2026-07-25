package com.mascill.keutrack.core.domain.repository

interface SyncRepository {
    suspend fun syncPendingTransactions()
    suspend fun syncPendingWallets()
    suspend fun syncPendingBudgets()
    suspend fun syncAll()

    /** True when any wallet, budget, or transaction is PENDING or FAILED. */
    suspend fun hasPendingSync(): Boolean

    /**
     * Enqueues a background sync via WorkManager.
     *
     * @param force when true, replaces any existing unique sync work so a retry
     * starts promptly (e.g. when a screen opens with unsynced data).
     */
    fun enqueuePendingSync(force: Boolean = false)
}
