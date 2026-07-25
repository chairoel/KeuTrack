package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.Wallet
import com.mascill.keutrack.core.domain.model.WalletType
import kotlinx.coroutines.flow.Flow

interface WalletRepository {

    fun observeWallets(): Flow<List<Wallet>>

    fun observeWalletsByType(type: WalletType): Flow<List<Wallet>>

    fun observeWalletById(walletId: String): Flow<Wallet?>

    suspend fun getPersonalWallet(): Wallet?

    suspend fun createWallet(wallet: Wallet)

    suspend fun updateWallet(wallet: Wallet)

    suspend fun deleteWallet(walletId: String)
}
