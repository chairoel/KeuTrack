package com.mascill.keutrack.core.domain.repository

import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.model.WalletUiPreferences
import kotlinx.coroutines.flow.Flow

interface WalletUiPreferencesRepository {
    fun observe(): Flow<WalletUiPreferences>

    suspend fun setBalanceVisible(
        walletType: WalletType,
        visible: Boolean,
    )
}
