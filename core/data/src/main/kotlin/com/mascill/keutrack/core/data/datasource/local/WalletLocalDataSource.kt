package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface WalletLocalDataSource {
    fun observeAll(): Flow<List<WalletEntity>>
    fun observeByType(type: String): Flow<List<WalletEntity>>
    fun observeById(walletId: String): Flow<WalletEntity?>
    suspend fun getById(walletId: String): WalletEntity?
    suspend fun getPersonal(): WalletEntity?
    suspend fun upsert(entity: WalletEntity)
    suspend fun delete(walletId: String)
    suspend fun getPending(): List<WalletEntity>
    suspend fun updateSyncStatus(id: String, status: SyncStatus)
    suspend fun applyBalanceDelta(walletId: String, delta: Long, syncStatus: SyncStatus)
}
