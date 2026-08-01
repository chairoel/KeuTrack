package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.AuthNetworkDataSource
import com.mascill.keutrack.core.data.datasource.local.WalletLocalDataSource
import com.mascill.keutrack.core.data.mapper.WalletMapper
import com.mascill.keutrack.core.data.sync.SyncScheduler
import com.mascill.keutrack.core.domain.model.SyncStatus
import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.WalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Wallets are local-first. A default personal wallet is created lazily on first
 * [observeWallets] / [getPersonalWallet] when none exists yet.
 */
@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val local: WalletLocalDataSource,
    private val mapper: WalletMapper,
    private val authNetworkDataSource: AuthNetworkDataSource,
    private val syncScheduler: SyncScheduler,
) : WalletRepository {

    private val ensureWalletMutex = Mutex()

    override fun observeWallets(): Flow<List<Wallet>> =
        local.observeAll()
            .onStart { ensureDefaultPersonalWallet() }
            .map { entities -> entities.map(mapper::toDomain) }

    override fun observeWalletsByType(type: WalletType): Flow<List<Wallet>> =
        local.observeByType(type.value)
            .onStart { ensureDefaultPersonalWallet() }
            .map { entities -> entities.map(mapper::toDomain) }

    override fun observeWalletById(walletId: String): Flow<Wallet?> =
        local.observeById(walletId).map { entity -> entity?.let(mapper::toDomain) }

    override suspend fun getPersonalWallet(): Wallet? {
        try {
            local.getPersonal()?.let { return mapper.toDomain(it) }
            return ensureDefaultPersonalWallet()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun createWallet(wallet: Wallet) {
        try {
            val pending = wallet.copy(syncStatus = SyncStatus.PENDING)
            local.upsert(mapper.toEntity(pending))
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun updateWallet(wallet: Wallet) {
        try {
            val pending = wallet.copy(syncStatus = SyncStatus.PENDING)
            local.upsert(mapper.toEntity(pending))
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    override suspend fun deleteWallet(walletId: String) {
        try {
            local.delete(walletId)
            syncScheduler.enqueueSync()
        } catch (e: CancellationException) {
            throw e
        }
    }

    private suspend fun ensureDefaultPersonalWallet(): Wallet? {
        try {
            return ensureWalletMutex.withLock {
                local.getPersonal()?.let { return@withLock mapper.toDomain(it) }

                val uid = authNetworkDataSource.getCurrentUser()?.uid ?: return@withLock null

                val wallet = Wallet(
                    id = UUID.randomUUID().toString(),
                    ownerId = uid,
                    name = "Dompet Utama",
                    type = WalletType.PERSONAL,
                    balance = 0L,
                    currency = "IDR",
                    syncStatus = SyncStatus.PENDING,
                    createdAt = Instant.now(),
                )
                local.upsert(mapper.toEntity(wallet))
                syncScheduler.enqueueSync()
                wallet
            }
        } catch (e: CancellationException) {
            throw e
        }
    }
}
