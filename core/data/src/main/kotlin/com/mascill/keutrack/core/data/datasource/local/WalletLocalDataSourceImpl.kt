package com.mascill.keutrack.core.data.datasource.local

import com.mascill.keutrack.core.data.db.dao.WalletDao
import com.mascill.keutrack.core.data.db.entity.WalletEntity
import com.mascill.keutrack.core.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WalletLocalDataSourceImpl @Inject constructor(
    private val dao: WalletDao,
) : WalletLocalDataSource {

    override fun observeAll(): Flow<List<WalletEntity>> = dao.observeAll()

    override fun observeByType(type: String): Flow<List<WalletEntity>> = dao.observeByType(type)

    override fun observeById(walletId: String): Flow<WalletEntity?> = dao.observeById(walletId)

    override suspend fun getById(walletId: String): WalletEntity? = dao.getById(walletId)

    override suspend fun getPersonal(): WalletEntity? = dao.getPersonal()

    override suspend fun upsert(entity: WalletEntity) {
        dao.upsert(entity)
    }

    override suspend fun delete(walletId: String) {
        dao.deleteById(walletId)
    }

    override suspend fun getPending(): List<WalletEntity> = dao.getPending()

    override suspend fun updateSyncStatus(id: String, status: SyncStatus) {
        dao.updateSyncStatus(id, status.name)
    }

    override suspend fun applyBalanceDelta(
        walletId: String,
        delta: Long,
        syncStatus: SyncStatus,
    ) {
        dao.applyBalanceDelta(walletId, delta, syncStatus.name)
    }
}
