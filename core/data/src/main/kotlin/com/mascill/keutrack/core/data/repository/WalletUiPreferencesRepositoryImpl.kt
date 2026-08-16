package com.mascill.keutrack.core.data.repository

import com.mascill.keutrack.core.data.datasource.WalletUiPreferencesLocalDataSource
import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import com.mascill.keutrack.core.domain.repository.WalletUiPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletUiPreferencesRepositoryImpl @Inject constructor(
    private val local: WalletUiPreferencesLocalDataSource,
) : WalletUiPreferencesRepository {

    override fun observe(): Flow<WalletUiPreferences> = local.observe()

    override suspend fun setBalanceVisible(
        walletType: WalletType,
        visible: Boolean,
    ) {
        local.setBalanceVisible(walletType = walletType, visible = visible)
    }
}
