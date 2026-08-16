package com.mascill.keutrack.core.data.datasource

import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import kotlinx.coroutines.flow.Flow

interface WalletUiPreferencesLocalDataSource {
    fun observe(): Flow<WalletUiPreferences>

    suspend fun setBalanceVisible(
        walletType: WalletType,
        visible: Boolean,
    )
}
