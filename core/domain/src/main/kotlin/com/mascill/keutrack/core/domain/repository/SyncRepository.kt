package com.mascill.keutrack.core.domain.repository

interface SyncRepository {
    suspend fun syncPendingTransactions() // Firebase Batch Write as defined in KeuTrack_Data_Design
}
