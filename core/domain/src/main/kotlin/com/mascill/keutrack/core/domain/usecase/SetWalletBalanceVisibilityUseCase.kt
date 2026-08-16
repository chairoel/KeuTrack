package com.mascill.keutrack.core.domain.usecase

import com.mascill.keutrack.core.domain.model.WalletType
import com.mascill.keutrack.core.domain.repository.WalletUiPreferencesRepository
import javax.inject.Inject

class SetWalletBalanceVisibilityUseCase @Inject constructor(
    private val repository: WalletUiPreferencesRepository,
) {
    suspend operator fun invoke(
        walletType: WalletType,
        visible: Boolean,
    ) {
        repository.setBalanceVisible(walletType = walletType, visible = visible)
    }
}
